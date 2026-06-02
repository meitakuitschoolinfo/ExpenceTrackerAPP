// このクラスが属するパッケージを宣言する（controller パッケージ＝HTTPの入口を置く場所）
package com.example.expensetracker.controller;

// 新規登録画面の入力を表す Form をインポートする
import com.example.expensetracker.form.UserRegisterForm;
// 利用者の登録処理を担う Service をインポートする
import com.example.expensetracker.service.UserService;

// 入力チェックを発動させるアノテーション
import jakarta.validation.Valid;
// このクラスを Controller として認識させるアノテーション
import org.springframework.stereotype.Controller;
// テンプレートに値を渡す入れ物
import org.springframework.ui.Model;
// バリデーション結果を受け取る型
import org.springframework.validation.BindingResult;
// HTTP GET 用アノテーション
import org.springframework.web.bind.annotation.GetMapping;
// HTTP POST 用アノテーション
import org.springframework.web.bind.annotation.PostMapping;
// リダイレクト先に1回限りのメッセージを渡す型
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// final フィールドを引数に取るコンストラクタを Lombok に生成させる
import lombok.RequiredArgsConstructor;

// Spring に Controller として登録する
@Controller
// 依存先（final フィールド）を引数に取るコンストラクタを Lombok に生成させる
@RequiredArgsConstructor
// 利用者の「新規登録（サインアップ）」を担当する Controller
public class UserRegisterController {

    // 登録処理を担う Service
    private final UserService userService;

    // HTTP GET "/register" にマッピング：新規登録フォームを表示する
    @GetMapping("/register")
    public String showForm(Model model) {
        // 空の Form をモデルに入れる（テンプレートの th:object="${userRegisterForm}" と紐づける）
        model.addAttribute("userRegisterForm", new UserRegisterForm());
        // テンプレート templates/auth/register.html を返す
        return "auth/register";
    }

    // HTTP POST "/register" にマッピング：フォーム送信された内容で新規登録する
    @PostMapping("/register")
    public String register(
            // @Valid で Form のバリデーション（@NotBlank等）を発動させる
            @Valid UserRegisterForm userRegisterForm,
            // ★バリデーション結果は @Valid の直後に置く★（順番を守らないとエラーが取れない）
            BindingResult bindingResult,
            // エラー時の再描画に使うモデル
            Model model,
            // 成功時のフラッシュメッセージ用
            RedirectAttributes redirectAttributes) {

        // 入力チェックでエラーがあれば、登録画面を再描画する
        if (bindingResult.hasErrors()) {
            // 入力値とエラーは bindingResult から自動で復元されるので、そのまま画面名を返す
            return "auth/register";
        }
        try {
            // 業務ロジック（重複チェック＋ハッシュ化＋初期カテゴリー作成）を Service に委譲する
            userService.register(userRegisterForm);
        } catch (IllegalArgumentException e) {
            // Service が「メール重複」等の業務エラーを投げたら、フォーム全体のエラーとして再描画する
            bindingResult.reject("global.error", e.getMessage());
            return "auth/register";
        }
        // 成功メッセージをフラッシュ属性に詰める（次の画面で1回だけ取れる）
        redirectAttributes.addFlashAttribute("message", "登録が完了しました。ログインしてください。");
        // 登録後はログイン画面へリダイレクトする（POST後リダイレクト＝PRGパターン）
        return "redirect:/login";
    }
}
