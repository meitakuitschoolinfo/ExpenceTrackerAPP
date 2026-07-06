// このテストクラスが属するパッケージを宣言する（テスト対象と同じ controller パッケージに置く）
package com.example.expensetracker.controller;

// 本物の認可ルールで検証するため SecurityConfig をインポートする
import com.example.expensetracker.config.SecurityConfig;
// テスト用の Category Entity を組み立てるためインポートする
import com.example.expensetracker.entity.Category;
// 収支の種類（EXPENSE/INCOME）をインポートする
import com.example.expensetracker.entity.TransactionType;
// テスト用の User Entity を組み立てるためインポートする
import com.example.expensetracker.entity.User;
// モック化する対象（カテゴリー選択肢）をインポートする
import com.example.expensetracker.service.CategoryService;
// モック化する対象（集計・登録・削除）をインポートする
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

// Mockito のマッチャ／検証メソッドを直接呼べるように import する
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
// CSRFトークンを擬似リクエストに付与する csrf() を直接呼べるように import する
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// HTTP GET / POST を組み立てる get(...) / post(...) を直接呼べるように import する
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// 検証メソッドを直接呼べるように import する
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// TransactionController だけを対象に、履歴一覧・新規登録・削除を検証する軽量テスト
@WebMvcTest(TransactionController.class)
// 本物の認可ルール（記録画面はログイン必須）で検証したいので SecurityConfig を取り込む
@Import(SecurityConfig.class)
// 全テストを「ake@test.com でログイン済み」の状態で実行する（@AuthenticationPrincipal に効く）
@WithMockUser(username = "ake@test.com")
// このテストクラスの説明
@DisplayName("TransactionController の単体テスト（履歴一覧・新規登録・削除）")
class TransactionControllerTest {

    // 擬似HTTPリクエストを送るための MockMvc を注入してもらう
    @Autowired
    private MockMvc mockMvc;

    // Controller が依存する3つの Service をモックに差し替える
    @MockitoBean
    private TransactionService transactionService;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private UserService userService;

    // テスト全体で使い回す「ログイン中ユーザー」のテストデータ
    private User user;
    // テスト全体で使い回す「支出カテゴリー1件」のテストデータ
    private Category category;

    // 各テストの前に共通のモック設定（前提条件）をまとめて仕込む
    @BeforeEach
    void setUp() {
        // テスト用 User を組み立てる（@Setter があるので setter で値を入れられる）
        user = new User();
        user.setId(1L);                 // ユーザーID
        user.setName("Akemi");          // 表示名
        user.setEmail("ake@test.com");  // ログインID（@WithMockUser の username と一致させる）

        // テスト用 Category を組み立てる（記録フォームの選択肢用）
        category = new Category();
        category.setId(10L);                       // カテゴリーID
        category.setUser(user);                    // 持ち主
        category.setType(TransactionType.EXPENSE); // 支出カテゴリー
        category.setLabel("食費");                 // 表示名
        category.setColor("#f87171");              // 表示色

        // 認証情報の email から本人 User を引き直す処理は、常にこの user を返すようにする
        when(userService.findByEmail("ake@test.com")).thenReturn(user);
        // 月文字列→YearMonth 変換は、常に 2026年6月 を返すようにする
        when(transactionService.parseMonthOrCurrent(any())).thenReturn(YearMonth.of(2026, 6));
        // ドロップダウンの選択肢は固定の2ヶ月を返すようにする
        when(transactionService.availableMonths(any())).thenReturn(List.of("2026-06", "2026-05"));
        // カテゴリー選択肢は、常に上で作った支出カテゴリー1件を返すようにする
        when(categoryService.findByUserAndType(eq(user), any())).thenReturn(List.of(category));
    }

    // テスト1：履歴一覧の表示を検証する
    @Test
    @DisplayName("GET \"/transactions\" → 200・ビュー \"transaction/list\"・必要な model 属性がある")
    void list_returnsListView() throws Exception {
        // その月の記録一覧は空（0件）を返すようにする（一覧テンプレートは「データなし」表示になる）
        when(transactionService.findByMonth(eq(user), any())).thenReturn(List.of());
        // GET "/transactions" を実行する
        mockMvc.perform(get("/transactions"))
                // 200 OK を検証する
                .andExpect(status().isOk())
                // ビュー名が "transaction/list" であることを検証する
                .andExpect(view().name("transaction/list"))
                // 画面に必要な属性が一通り渡っていることを検証する
                .andExpect(model().attributeExists("userName", "selectedMonth", "availableMonths", "transactions"));
    }

    // テスト2：新規登録フォームの表示を検証する
    @Test
    @DisplayName("GET \"/transactions/new\" → 200・ビュー \"transaction/form\"・form と categories がある")
    void showForm_returnsFormView() throws Exception {
        // GET "/transactions/new" を実行する
        mockMvc.perform(get("/transactions/new"))
                // 200 OK を検証する
                .andExpect(status().isOk())
                // ビュー名が "transaction/form" であることを検証する
                .andExpect(view().name("transaction/form"))
                // フォームと、選んだ種類のカテゴリー選択肢が渡っていることを検証する
                .andExpect(model().attributeExists("transactionForm", "categories"));
    }

    // テスト3：正常な入力で登録すると、その月の履歴へリダイレクトすることを検証する
    @Test
    @DisplayName("正常入力で POST \"/transactions\" → \"/transactions?month=2026-06\" へリダイレクト")
    void register_valid_redirectsToMonthList() throws Exception {
        // POST "/transactions" を実行する。記録フォームの項目を渡し、CSRFを付ける
        mockMvc.perform(post("/transactions")
                        .param("type", "EXPENSE")               // 種類（支出）
                        .param("transactionDate", "2026-06-15")  // 取引日
                        .param("categoryId", "10")               // カテゴリーID
                        .param("amount", "1000")                 // 金額（1円以上）
                        .with(csrf()))
                // 入力が正常なので 3xx リダイレクト
                .andExpect(status().is3xxRedirection())
                // 転送先が登録した記録の月の履歴であることを検証する
                .andExpect(redirectedUrl("/transactions?month=2026-06"))
                // 成功メッセージが付いていることを検証する
                .andExpect(flash().attributeExists("message"));
        // Service の register が user とともに1回呼ばれたことを検証する
        verify(transactionService).register(eq(user), any());
    }

    // テスト4：入力エラー（金額なし）のときはフォームを再描画し、選択肢を詰め直すことを検証する
    @Test
    @DisplayName("金額なしで POST \"/transactions\" → 再描画 \"transaction/form\"・categories を再設定")
    void register_invalid_rerendersFormWithCategories() throws Exception {
        // POST "/transactions" を実行する。amount を渡さず @NotNull に違反させる
        mockMvc.perform(post("/transactions")
                        .param("type", "EXPENSE")
                        .param("transactionDate", "2026-06-15")
                        .param("categoryId", "10")
                        // amount は意図的に送らない（バリデーションエラーを起こす）
                        .with(csrf()))
                // 再描画なので 200
                .andExpect(status().isOk())
                // ビューはフォームのまま
                .andExpect(view().name("transaction/form"))
                // ★再描画時もカテゴリー選択肢が詰め直されている★ことを検証する
                .andExpect(model().attributeExists("categories"));
        // 登録処理（register）は呼ばれていないことを検証する
        verify(transactionService, never()).register(any(), any());
    }

    // テスト5：削除すると、元の月の履歴へリダイレクトすることを検証する
    @Test
    @DisplayName("POST \"/transactions/5/delete\" → \"/transactions?month=2026-06\" へリダイレクト")
    void delete_redirectsBackToList() throws Exception {
        // POST "/transactions/5/delete" を実行する。元の月を month で渡し、CSRFを付ける
        mockMvc.perform(post("/transactions/5/delete")
                        .param("month", "2026-06")
                        .with(csrf()))
                // 3xx リダイレクト
                .andExpect(status().is3xxRedirection())
                // 元の月の履歴へ戻ることを検証する
                .andExpect(redirectedUrl("/transactions?month=2026-06"))
                // 成功メッセージが付いていることを検証する
                .andExpect(flash().attributeExists("message"));
        // Service の delete が「user と id=5」で1回呼ばれたことを検証する
        verify(transactionService).delete(user, 5L);
    }
}
