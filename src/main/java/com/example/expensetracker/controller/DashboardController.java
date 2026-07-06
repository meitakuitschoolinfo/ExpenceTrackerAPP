// このクラスが属するパッケージを宣言する（controller パッケージ＝HTTPの入口を置く場所）
package com.example.expensetracker.controller;

// ログイン中の利用者Entityを取得するため User をインポートする
import com.example.expensetracker.entity.User;
// 各種集計を担う Service をインポートする
import com.example.expensetracker.service.TransactionService;
// ログイン中ユーザーをemailから引き直すため UserService をインポートする
import com.example.expensetracker.service.UserService;

// Spring に Controller として認識させるアノテーション
import org.springframework.stereotype.Controller;
// テンプレートに値を渡す入れ物
import org.springframework.ui.Model;
// ログイン中ユーザーの認証情報を受け取るアノテーション
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// Spring Security が保持する「ログイン中ユーザー」の型
import org.springframework.security.core.userdetails.UserDetails;
// HTTP GET 用アノテーション
import org.springframework.web.bind.annotation.GetMapping;
// URLのクエリパラメータ（?month=...）を受け取るアノテーション
import org.springframework.web.bind.annotation.RequestParam;

// final フィールドを引数に取るコンストラクタを Lombok に生成させる
import lombok.RequiredArgsConstructor;

// 年月（YYYY-MM）を型として扱う YearMonth をインポートする
import java.time.YearMonth;

// Spring に Controller として登録する
@Controller
// 依存先（final フィールド）を引数に取るコンストラクタを Lombok に生成させる
@RequiredArgsConstructor
// ダッシュボード（残高サマリー・支出内訳・6ヶ月推移）を表示する Controller
public class DashboardController {

    // 集計処理を担う Service
    private final TransactionService transactionService;
    // ログイン中ユーザーをemailから引き直すための Service
    private final UserService userService;

    // HTTP GET "/dashboard" にマッピング：ダッシュボードを表示する
    @GetMapping("/dashboard")
    public String dashboard(
            // ?month=2026-06 のようなクエリを受け取る（無ければ null → Service側で当月にする）
            @RequestParam(name = "month", required = false) String month,
            // ★ログイン中ユーザーはURLからではなく認証情報から取る★（改ざん防止）
            @AuthenticationPrincipal UserDetails principal,
            // テンプレートに値を渡す入れ物
            Model model) {

        // 認証情報の email（getUsername）から本人の User Entity を取り直す
        User user = userService.findByEmail(principal.getUsername());
        // "2026-06" などの文字列を YearMonth に変換する（不正・未指定なら当月）
        YearMonth selected = transactionService.parseMonthOrCurrent(month);

        // 表示中の利用者名を渡す（ヘッダーの「○○さん」表示用）
        model.addAttribute("userName", user.getName());
        // 選択中の月（"yyyy-MM"）を渡す（ドロップダウンの選択状態に使う）
        model.addAttribute("selectedMonth", selected.toString());
        // 月選択ドロップダウンの選択肢（直近12ヶ月）を渡す
        model.addAttribute("availableMonths", transactionService.availableMonths(YearMonth.now()));
        // その月の合計（収入・支出・残高）を渡す
        model.addAttribute("summary", transactionService.summarize(user, selected));
        // その月の支出内訳（カテゴリー別・割合付き）を渡す
        model.addAttribute("breakdown", transactionService.expenseBreakdown(user, selected));
        // 直近6ヶ月の推移を渡す（折れ線グラフ用。縦軸のスケーリングは Chart.js が自動で行う）
        model.addAttribute("trend", transactionService.recentTrend(user, selected));
        // テンプレート templates/dashboard.html を返す
        return "dashboard";
    }
}
