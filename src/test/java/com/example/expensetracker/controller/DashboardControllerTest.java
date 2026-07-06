// このテストクラスが属するパッケージを宣言する（テスト対象と同じ controller パッケージに置く）
package com.example.expensetracker.controller;

// 本物の認可ルールで検証するため SecurityConfig をインポートする
import com.example.expensetracker.config.SecurityConfig;
// 円グラフ1切れを表す DTO（モックの戻り値用）をインポートする
import com.example.expensetracker.dto.CategorySlice;
// 月合計を表す DTO（モックの戻り値用）をインポートする
import com.example.expensetracker.dto.MonthlySummary;
// 推移グラフ1ヶ月分を表す DTO（モックの戻り値用）をインポートする
import com.example.expensetracker.dto.MonthlyTrendPoint;
// テスト用の User Entity を組み立てるためインポートする
import com.example.expensetracker.entity.User;
// モック化する対象（集計）をインポートする
import com.example.expensetracker.service.TransactionService;
// モック化する対象（email→User 引き直し）をインポートする
import com.example.expensetracker.service.UserService;

// 各テストの前に共通準備を走らせるためのアノテーションをインポートする
import org.junit.jupiter.api.BeforeEach;
// テストの表示名を日本語で付けるためのアノテーションをインポートする
import org.junit.jupiter.api.DisplayName;
// 1つのテストメソッドであることを示すアノテーションをインポートする
import org.junit.jupiter.api.Test;
// Springが用意したBean（MockMvc）を注入してもらうためのアノテーションをインポートする
import org.springframework.beans.factory.annotation.Autowired;
// 「Controller層だけ」を対象にした軽量テストを起動するアノテーション（Boot 4.0の新パッケージ）をインポートする
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// 追加の設定クラス（SecurityConfig）をテストの文脈に取り込むためのアノテーションをインポートする
import org.springframework.context.annotation.Import;
// 「ログイン済みユーザー」を擬似的に作り出すためのアノテーションをインポートする
import org.springframework.security.test.context.support.WithMockUser;
// SpringのBeanをMockitoのモックに差し替えるためのアノテーション（Boot 4.xの新方式）をインポートする
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// 擬似的にHTTPリクエストを送る道具 MockMvc をインポートする
import org.springframework.test.web.servlet.MockMvc;

// 年月を扱う YearMonth をインポートする（モックの戻り値用）
import java.time.YearMonth;
// テスト用のリストを作るため List をインポートする
import java.util.List;

// Mockito のマッチャ／設定メソッドを直接呼べるように import する
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
// HTTP GET を組み立てる get(...) を直接呼べるように import する
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// 検証メソッドを直接呼べるように import する
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// DashboardController だけを対象に、ダッシュボード表示を検証する軽量テスト
@WebMvcTest(DashboardController.class)
// 本物の認可ルール（ダッシュボードはログイン必須）で検証したいので SecurityConfig を取り込む
@Import(SecurityConfig.class)
// このテストクラスの説明
@DisplayName("DashboardController の単体テスト（ダッシュボード表示）")
class DashboardControllerTest {

    // 擬似HTTPリクエストを送るための MockMvc を注入してもらう
    @Autowired
    private MockMvc mockMvc;

    // Controller が依存する2つの Service をモックに差し替える
    @MockitoBean
    private TransactionService transactionService;
    @MockitoBean
    private UserService userService;

    // テスト全体で使い回す「ログイン中ユーザー」のテストデータ
    private User user;

    // 各テストの前に共通のモック設定（前提条件）を仕込む
    @BeforeEach
    void setUp() {
        // テスト用 User を組み立てる
        user = new User();
        user.setId(1L);
        user.setName("Akemi");
        user.setEmail("ake@test.com");
        // email から本人 User を引き直す処理は、常にこの user を返す
        when(userService.findByEmail("ake@test.com")).thenReturn(user);
        // 月文字列→YearMonth 変換は、常に 2026年6月 を返す
        when(transactionService.parseMonthOrCurrent(any())).thenReturn(YearMonth.of(2026, 6));
        // ドロップダウンの選択肢は固定の2ヶ月を返す
        when(transactionService.availableMonths(any())).thenReturn(List.of("2026-06", "2026-05"));
        // 月合計は「収入1000・支出500・残高500」を返す（テンプレートの金額表示用）
        when(transactionService.summarize(any(), any())).thenReturn(new MonthlySummary(1000, 500, 500));
        // 支出内訳は「食費 500円・100%」の1件を返す（円グラフ用。空でないので canvas を出す分岐に入る）
        when(transactionService.expenseBreakdown(any(), any()))
                .thenReturn(List.of(new CategorySlice("食費", "#f87171", 500, 100)));
        // 直近6ヶ月の推移は「6月：収入1000・支出500」の1件を返す（折れ線グラフ用）
        when(transactionService.recentTrend(any(), any()))
                .thenReturn(List.of(new MonthlyTrendPoint("6月", 1000, 500)));
    }

    // テスト1：ログイン済みでダッシュボードが表示され、必要な集計データが全て渡ることを検証する
    @Test
    @DisplayName("ログイン済みで GET \"/dashboard\" → 200・ビュー \"dashboard\"・集計データが揃う")
    @WithMockUser(username = "ake@test.com")
    void dashboard_authenticated_returnsDashboardView() throws Exception {
        // GET "/dashboard" を実行する
        mockMvc.perform(get("/dashboard"))
                // 200 OK を検証する（テンプレートも最後まで描画できる＝以前の描画エラーが無いことの確認にもなる）
                .andExpect(status().isOk())
                // ビュー名が "dashboard" であることを検証する
                .andExpect(view().name("dashboard"))
                // 残高・収入・支出カードや各グラフが使う属性が一通り揃っていることを検証する
                .andExpect(model().attributeExists(
                        "userName", "selectedMonth", "availableMonths", "summary", "breakdown", "trend"));
    }

    // テスト2：未ログインでダッシュボードを開くと、ログイン画面へ追い返されることを検証する（認可ルールの確認）
    @Test
    @DisplayName("未ログインで GET \"/dashboard\" → \"/login\" へリダイレクト")
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        // @WithMockUser を付けない＝未認証で GET "/dashboard" を実行する
        mockMvc.perform(get("/dashboard"))
                // 未認証なので 3xx リダイレクト
                .andExpect(status().is3xxRedirection())
                // 転送先がログイン画面であることを検証する
                .andExpect(redirectedUrl("/login"));
    }
}
