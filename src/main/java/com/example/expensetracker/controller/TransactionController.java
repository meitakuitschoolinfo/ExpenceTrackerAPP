// このクラスが属するパッケージを宣言する（controller パッケージ＝HTTPの入口を置く場所）
package com.example.expensetracker.controller;

// ログイン中の利用者Entityを取得するため User をインポートする
import com.example.expensetracker.entity.User;
// 収支の種類（EXPENSE/INCOME）をインポートする
import com.example.expensetracker.entity.TransactionType;
// 記録の入力を表す Form をインポートする
import com.example.expensetracker.form.TransactionForm;
// 記録の集計・登録・削除を担う Service をインポートする
import com.example.expensetracker.service.TransactionService;
// カテゴリー選択肢を取得する Service をインポートする
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
// HTTP POST 用アノテーション
import org.springframework.web.bind.annotation.PostMapping;
// URLパスから値を取り出すアノテーション
import org.springframework.web.bind.annotation.PathVariable;
// URLのクエリパラメータを受け取るアノテーション
import org.springframework.web.bind.annotation.RequestParam;
// リダイレクト時に1回限りのデータを渡す型
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// final フィールドを引数に取るコンストラクタを Lombok に生成させる
import lombok.RequiredArgsConstructor;

// 「日付だけ」を扱う LocalDate をインポートする（新規フォームの初期日付に使う）
import java.time.LocalDate;
// 年月を扱う YearMonth をインポートする
import java.time.YearMonth;

// Spring に Controller として登録する
@Controller
// 依存先（final フィールド）を引数に取るコンストラクタを Lombok に生成させる
@RequiredArgsConstructor
// 収支の記録（履歴一覧・新規登録・削除）を担当する Controller
public class TransactionController {

    // 記録の集計・登録・削除を担う Service
    private final TransactionService transactionService;
    // カテゴリー選択肢を取得する Service
    private final CategoryService categoryService;
    // ログイン中ユーザーをemailから引き直す Service
    private final UserService userService;

    // HTTP GET "/transactions" にマッピング：指定月の履歴一覧を表示する
    @GetMapping("/transactions")
    public String list(
            // ?month=2026-06 を受け取る（無ければ当月）
            @RequestParam(name = "month", required = false) String month,
            // ログイン中ユーザー（認証情報から取得）
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        // email から本人の User を取り直す
        User user = userService.findByEmail(principal.getUsername());
        // 文字列の月を YearMonth に変換（不正・未指定なら当月）
        YearMonth selected = transactionService.parseMonthOrCurrent(month);

        // 表示用の利用者名を渡す
        model.addAttribute("userName", user.getName());
        // 選択中の月を渡す
        model.addAttribute("selectedMonth", selected.toString());
        // 月選択ドロップダウンの選択肢を渡す
        model.addAttribute("availableMonths", transactionService.availableMonths(YearMonth.now()));
        // その月の記録一覧を渡す
        model.addAttribute("transactions", transactionService.findByMonth(user, selected));
        // テンプレート templates/transaction/list.html を返す
        return "transaction/list";
    }

    // HTTP GET "/transactions/new" にマッピング：新規登録フォームを表示する
    @GetMapping("/transactions/new")
    public String showForm(
            // ?type=income のように初期タイプを受け取る（無ければ支出）
            @RequestParam(name = "type", required = false, defaultValue = "EXPENSE") TransactionType type,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        // 本人の User を取得する
        User user = userService.findByEmail(principal.getUsername());
        // 空の Form を生成し、初期値（種類・今日の日付）を入れておく
        TransactionForm form = new TransactionForm();
        // 画面で選ばれたトグル（支出/収入）を初期値にする
        form.setType(type);
        // 取引日の初期値を「今日」にする（React版と同じ挙動）
        form.setTransactionDate(LocalDate.now());
        // フォームをモデルに入れる
        model.addAttribute("transactionForm", form);
        // 選んだ種類に対応するカテゴリー選択肢を渡す（支出なら支出カテゴリーだけ）
        model.addAttribute("categories", categoryService.findByUserAndType(user, type));
        // テンプレート templates/transaction/form.html を返す
        return "transaction/form";
    }

    // HTTP POST "/transactions" にマッピング：フォーム送信内容で新規登録する
    @PostMapping("/transactions")
    public String register(
            // @Valid で Form のバリデーションを発動させる
            @Valid TransactionForm transactionForm,
            // ★バリデーション結果は @Valid の直後に置く★
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        // 本人の User を取得する
        User user = userService.findByEmail(principal.getUsername());

        // 入力チェックでエラーがあれば、フォームを再描画する
        if (bindingResult.hasErrors()) {
            // ★再描画時はカテゴリー選択肢を詰め直す★（GET時のモデルは引き継がれないため）
            model.addAttribute("categories",
                    categoryService.findByUserAndType(user, transactionForm.getType()));
            return "transaction/form";
        }
        try {
            // 業務ロジック（categoryId→Category変換・整合チェック・保存）を Service に委譲する
            transactionService.register(user, transactionForm);
        } catch (IllegalArgumentException e) {
            // 業務エラー（種類不一致など）はフォーム全体のエラーとして再描画する
            bindingResult.reject("global.error", e.getMessage());
            model.addAttribute("categories",
                    categoryService.findByUserAndType(user, transactionForm.getType()));
            return "transaction/form";
        }
        // 成功メッセージをフラッシュ属性に詰める
        redirectAttributes.addFlashAttribute("message", "記録を登録しました");
        // 登録した記録の月の履歴へリダイレクトする（PRGパターン）
        return "redirect:/transactions?month=" + YearMonth.from(transactionForm.getTransactionDate());
    }

    // HTTP POST "/transactions/{id}/delete" にマッピング：1件削除する
    // ★削除は必ず POST★（GETにするとリンク踏みやプリフェッチで消える事故が起きる）
    @PostMapping("/transactions/{id}/delete")
    public String delete(
            // URL の {id} を Long で受け取る
            @PathVariable Long id,
            // 削除後に元の月の一覧へ戻れるよう、月パラメータを受け取る
            @RequestParam(name = "month", required = false) String month,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {

        // 本人の User を取得する
        User user = userService.findByEmail(principal.getUsername());
        // Service に削除を委譲する（他人の記録なら例外で弾かれる）
        transactionService.delete(user, id);
        // 成功メッセージをフラッシュ属性に詰める
        redirectAttributes.addFlashAttribute("message", "記録を削除しました");
        // 元の月の履歴一覧へリダイレクトする
        return "redirect:/transactions" + (month != null ? "?month=" + month : "");
    }
}
