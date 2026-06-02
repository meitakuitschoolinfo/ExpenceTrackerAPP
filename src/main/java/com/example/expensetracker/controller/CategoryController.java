// このクラスが属するパッケージを宣言する（controller パッケージ＝HTTPの入口を置く場所）
package com.example.expensetracker.controller;

// ログイン中の利用者Entityを取得するため User をインポートする
import com.example.expensetracker.entity.User;
// 収支の種類（EXPENSE/INCOME）をインポートする
import com.example.expensetracker.entity.TransactionType;
// カテゴリーの入力を表す Form をインポートする
import com.example.expensetracker.form.CategoryForm;
// カテゴリーのCRUDを担う Service をインポートする
import com.example.expensetracker.service.CategoryService;
// ログイン中ユーザーをemailから引き直す Service をインポートする
import com.example.expensetracker.service.UserService;

// 入力チェックを発動させるアノテーション
import jakarta.validation.Valid;
// このクラスを Controller として認識させるアノテーション
import org.springframework.stereotype.Controller;
// テンプレートに値を渡す入れ物
import org.springframework.ui.Model;
// ログイン中ユーザーの認証情報を受け取るアノテーション
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// Spring Security が保持する「ログイン中ユーザー」の型
import org.springframework.security.core.userdetails.UserDetails;
// バリデーション結果を受け取る型
import org.springframework.validation.BindingResult;
// HTTP GET 用アノテーション
import org.springframework.web.bind.annotation.GetMapping;
// URLパスから値を取り出すアノテーション
import org.springframework.web.bind.annotation.PathVariable;
// HTTP POST 用アノテーション
import org.springframework.web.bind.annotation.PostMapping;
// クラスレベルで共通URLプレフィックスを指定するアノテーション
import org.springframework.web.bind.annotation.RequestMapping;
// URLのクエリパラメータを受け取るアノテーション
import org.springframework.web.bind.annotation.RequestParam;
// リダイレクト時に1回限りのデータを渡す型
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// final フィールドを引数に取るコンストラクタを Lombok に生成させる
import lombok.RequiredArgsConstructor;

// Spring に Controller として登録する
@Controller
// このクラスのすべての @GetMapping/@PostMapping の先頭に "/categories" を付ける
@RequestMapping("/categories")
// 依存先（final フィールド）を引数に取るコンストラクタを Lombok に生成させる
@RequiredArgsConstructor
// カテゴリー管理（一覧・登録・編集・削除）を1つにまとめた Controller
// ※機能が少ないので、記録(Transaction)のように分割せず集約する
public class CategoryController {

    // カテゴリーのCRUDを担う Service
    private final CategoryService categoryService;
    // ログイン中ユーザーをemailから引き直す Service
    private final UserService userService;

    // HTTP GET "/categories" にマッピング：カテゴリー一覧（支出＋収入）を表示する
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails principal, Model model) {
        // 本人の User を取得する
        User user = userService.findByEmail(principal.getUsername());
        // 表示用の利用者名を渡す
        model.addAttribute("userName", user.getName());
        // 支出カテゴリーの一覧を渡す
        model.addAttribute("expenseCategories",
                categoryService.findByUserAndType(user, TransactionType.EXPENSE));
        // 収入カテゴリーの一覧を渡す
        model.addAttribute("incomeCategories",
                categoryService.findByUserAndType(user, TransactionType.INCOME));
        // テンプレート templates/category/list.html を返す
        return "category/list";
    }

    // HTTP GET "/categories/new" にマッピング：新規登録フォームを表示する
    @GetMapping("/new")
    public String showRegisterForm(
            // ?type=income のように初期タイプを受け取る（無ければ支出）
            @RequestParam(name = "type", required = false, defaultValue = "EXPENSE") TransactionType type,
            Model model) {

        // 空の Form を生成する
        CategoryForm form = new CategoryForm();
        // 種類の初期値をセットする
        form.setType(type);
        // 色の初期値をセットする（パレットの先頭色）
        form.setColor("#f87171");
        // フォームをモデルに入れる
        model.addAttribute("categoryForm", form);
        // テンプレート側で「これは編集ではない」と判定するフラグ
        model.addAttribute("isEdit", false);
        // 登録／編集兼用テンプレートを返す
        return "category/form";
    }

    // HTTP POST "/categories" にマッピング：新規カテゴリーを登録する
    @PostMapping
    public String register(
            @Valid CategoryForm categoryForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        // 本人の User を取得する
        User user = userService.findByEmail(principal.getUsername());
        // 入力チェックでエラーがあれば再描画する
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "category/form";
        }
        try {
            // 業務ロジック（重複チェック＋保存）を Service に委譲する
            categoryService.register(user, categoryForm);
        } catch (IllegalArgumentException e) {
            // 業務エラー（重複など）はフォーム全体のエラーとして再描画する
            bindingResult.reject("global.error", e.getMessage());
            model.addAttribute("isEdit", false);
            return "category/form";
        }
        // 成功メッセージをフラッシュ属性に詰める
        redirectAttributes.addFlashAttribute("message", "カテゴリーを登録しました");
        // 一覧にリダイレクトする（PRG）
        return "redirect:/categories";
    }

    // HTTP GET "/categories/{id}/edit" にマッピング：編集フォームを表示する
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        // 本人の User を取得する
        User user = userService.findByEmail(principal.getUsername());
        // Service 経由で「DBの Category → CategoryForm」に詰め替えて渡す（他人のものなら例外）
        model.addAttribute("categoryForm", categoryService.toEditForm(user, id));
        // テンプレート側で「これは編集モード」と判定するフラグ
        model.addAttribute("isEdit", true);
        // 登録／編集兼用テンプレートを返す
        return "category/form";
    }

    // HTTP POST "/categories/{id}/edit" にマッピング：カテゴリーを更新する
    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid CategoryForm categoryForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        // 本人の User を取得する
        User user = userService.findByEmail(principal.getUsername());
        // URL の id を Form にも反映させる（hidden で来るが念のため上書きする）
        categoryForm.setId(id);
        // 入力チェックでエラーがあれば再描画する
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "category/form";
        }
        try {
            // 業務ロジック（重複チェック＋更新）を Service に委譲する
            categoryService.update(user, categoryForm);
        } catch (IllegalArgumentException e) {
            // 業務エラーはフォーム全体のエラーとして再描画する
            bindingResult.reject("global.error", e.getMessage());
            model.addAttribute("isEdit", true);
            return "category/form";
        }
        // 成功メッセージをフラッシュ属性に詰める
        redirectAttributes.addFlashAttribute("message", "カテゴリーを更新しました");
        // 一覧にリダイレクトする
        return "redirect:/categories";
    }

    // HTTP POST "/categories/{id}/delete" にマッピング：1件削除する（削除は必ず POST）
    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {

        // 本人の User を取得する
        User user = userService.findByEmail(principal.getUsername());
        try {
            // Service に削除を委譲する（使用中なら IllegalStateException）
            categoryService.delete(user, id);
            // 成功メッセージ
            redirectAttributes.addFlashAttribute("message", "カテゴリーを削除しました");
        } catch (IllegalStateException e) {
            // 使用中で削除できない場合はエラーメッセージをフラッシュに詰める
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        // 結果に関わらず一覧にリダイレクトする
        return "redirect:/categories";
    }
}
