// このテストクラスが属するパッケージを宣言する（テスト対象と同じ controller パッケージに置く）
package com.example.expensetracker.controller;

// 本物の認可ルールで検証するため SecurityConfig をインポートする
import com.example.expensetracker.config.SecurityConfig;
// 収支の種類（EXPENSE/INCOME）をインポートする
import com.example.expensetracker.entity.TransactionType;
// テスト用の User Entity を組み立てるためインポートする
import com.example.expensetracker.entity.User;
// 編集表示テストで Service の戻り値として使う CategoryForm をインポートする
import com.example.expensetracker.form.CategoryForm;
// モック化する対象（カテゴリーのCRUD）をインポートする
import com.example.expensetracker.service.CategoryService;
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

// テスト用のリストを作るため List をインポートする
import java.util.List;

// Mockito のマッチャ／検証メソッドを直接呼べるように import する
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

// CategoryController だけを対象に、カテゴリーの一覧・登録・編集・削除を検証する軽量テスト
@WebMvcTest(CategoryController.class)
// 本物の認可ルール（カテゴリー画面はログイン必須）で検証したいので SecurityConfig を取り込む
@Import(SecurityConfig.class)
// 全テストを「ake@test.com でログイン済み」の状態で実行する
@WithMockUser(username = "ake@test.com")
// このテストクラスの説明
@DisplayName("CategoryController の単体テスト（一覧・登録・編集・削除）")
class CategoryControllerTest {

    // 擬似HTTPリクエストを送るための MockMvc を注入してもらう
    @Autowired
    private MockMvc mockMvc;

    // Controller が依存する2つの Service をモックに差し替える
    @MockitoBean
    private CategoryService categoryService;
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
        // 認証情報の email から本人 User を引き直す処理は、常にこの user を返す
        when(userService.findByEmail("ake@test.com")).thenReturn(user);
    }

    // テスト1：カテゴリー一覧（支出＋収入）の表示を検証する
    @Test
    @DisplayName("GET \"/categories\" → 200・ビュー \"category/list\"・支出/収入の一覧がある")
    void list_returnsListView() throws Exception {
        // 支出・収入それぞれの一覧は空リストを返すようにする（テンプレートは「なし」表示になる）
        when(categoryService.findByUserAndType(eq(user), any())).thenReturn(List.of());
        // GET "/categories" を実行する
        mockMvc.perform(get("/categories"))
                // 200 OK を検証する
                .andExpect(status().isOk())
                // ビュー名が "category/list" であることを検証する
                .andExpect(view().name("category/list"))
                // 一覧画面に必要な属性が渡っていることを検証する
                .andExpect(model().attributeExists("userName", "expenseCategories", "incomeCategories"));
    }

    // テスト2：新規登録フォームの表示を検証する
    @Test
    @DisplayName("GET \"/categories/new\" → 200・ビュー \"category/form\"・isEdit=false")
    void showRegisterForm_returnsFormView() throws Exception {
        // GET "/categories/new" を実行する
        mockMvc.perform(get("/categories/new"))
                // 200 OK を検証する
                .andExpect(status().isOk())
                // ビュー名が "category/form" であることを検証する
                .andExpect(view().name("category/form"))
                // フォームが渡っていることを検証する
                .andExpect(model().attributeExists("categoryForm"))
                // 「編集ではない（新規）」フラグが false であることを検証する
                .andExpect(model().attribute("isEdit", false));
    }

    // テスト3：正常な入力で登録すると、一覧へリダイレクトすることを検証する
    @Test
    @DisplayName("正常入力で POST \"/categories\" → \"/categories\" へリダイレクト＋成功メッセージ")
    void register_valid_redirectsToList() throws Exception {
        // POST "/categories" を実行する。カテゴリーの項目を渡し、CSRFを付ける
        mockMvc.perform(post("/categories")
                        .param("type", "EXPENSE")   // 種類（支出）
                        .param("label", "交際費")    // カテゴリー名（必須）
                        .param("color", "#a78bfa")  // 色（必須）
                        .with(csrf()))
                // 3xx リダイレクト
                .andExpect(status().is3xxRedirection())
                // 転送先が一覧であることを検証する（PRG）
                .andExpect(redirectedUrl("/categories"))
                // 成功メッセージが付いていることを検証する
                .andExpect(flash().attributeExists("message"));
        // Service の register が user とともに1回呼ばれたことを検証する
        verify(categoryService).register(eq(user), any());
    }

    // テスト4：入力エラー（カテゴリー名が空）のときはフォームを再描画することを検証する
    @Test
    @DisplayName("名前が空で POST \"/categories\" → 再描画 \"category/form\"・登録は呼ばれない")
    void register_invalid_rerendersForm() throws Exception {
        // POST "/categories" を実行する。label を空にして @NotBlank に違反させる
        mockMvc.perform(post("/categories")
                        .param("type", "EXPENSE")
                        .param("label", "")          // ★空＝バリデーションエラー★
                        .param("color", "#a78bfa")
                        .with(csrf()))
                // 再描画なので 200
                .andExpect(status().isOk())
                // ビューはフォームのまま
                .andExpect(view().name("category/form"))
                // 編集ではない（新規）フラグが付いていることを検証する
                .andExpect(model().attribute("isEdit", false));
        // 登録処理（register）は呼ばれていないことを検証する
        verify(categoryService, never()).register(any(), any());
    }

    // テスト5：編集フォームの表示を検証する（Service が詰め替えた Form が渡る）
    @Test
    @DisplayName("GET \"/categories/3/edit\" → 200・ビュー \"category/form\"・isEdit=true")
    void showEditForm_returnsFormView() throws Exception {
        // Service が「DBの Category → CategoryForm」へ詰め替えて返す処理をモックする
        CategoryForm editForm = new CategoryForm();      // 空の Form を用意する
        editForm.setId(3L);                              // 編集対象のID
        editForm.setType(TransactionType.EXPENSE);       // 種類
        editForm.setLabel("食費");                       // 名前
        editForm.setColor("#f87171");                    // 色
        // toEditForm(user, 3) が呼ばれたら、上の editForm を返すようにする
        when(categoryService.toEditForm(user, 3L)).thenReturn(editForm);
        // GET "/categories/3/edit" を実行する
        mockMvc.perform(get("/categories/3/edit"))
                // 200 OK を検証する
                .andExpect(status().isOk())
                // ビュー名が "category/form" であることを検証する
                .andExpect(view().name("category/form"))
                // フォームが渡っていることを検証する
                .andExpect(model().attributeExists("categoryForm"))
                // 「編集モード」フラグが true であることを検証する
                .andExpect(model().attribute("isEdit", true));
    }

    // テスト6：正常な入力で更新すると、一覧へリダイレクトすることを検証する
    @Test
    @DisplayName("正常入力で POST \"/categories/3/edit\" → \"/categories\" へリダイレクト")
    void update_valid_redirectsToList() throws Exception {
        // POST "/categories/3/edit" を実行する
        mockMvc.perform(post("/categories/3/edit")
                        .param("id", "3")
                        .param("type", "EXPENSE")
                        .param("label", "食料品")
                        .param("color", "#f87171")
                        .with(csrf()))
                // 3xx リダイレクト
                .andExpect(status().is3xxRedirection())
                // 転送先が一覧であることを検証する
                .andExpect(redirectedUrl("/categories"))
                // 成功メッセージが付いていることを検証する
                .andExpect(flash().attributeExists("message"));
        // Service の update が user とともに1回呼ばれたことを検証する
        verify(categoryService).update(eq(user), any());
    }

    // テスト7：削除に成功すると、一覧へリダイレクトし成功メッセージが付くことを検証する
    @Test
    @DisplayName("POST \"/categories/4/delete\"（未使用）→ \"/categories\" へリダイレクト＋成功メッセージ")
    void delete_success_redirectsToList() throws Exception {
        // POST "/categories/4/delete" を実行する（delete は void。例外を出さなければ成功扱い）
        mockMvc.perform(post("/categories/4/delete").with(csrf()))
                // 3xx リダイレクト
                .andExpect(status().is3xxRedirection())
                // 転送先が一覧
                .andExpect(redirectedUrl("/categories"))
                // 成功メッセージ message が付いていることを検証する
                .andExpect(flash().attributeExists("message"));
        // Service の delete が「user と id=4」で呼ばれたことを検証する
        verify(categoryService).delete(user, 4L);
    }

    // テスト8：使用中カテゴリーの削除（Service が IllegalStateException）のときの挙動を検証する
    @Test
    @DisplayName("使用中の POST \"/categories/5/delete\" → \"/categories\"・errorMessage が付く")
    void delete_inUse_redirectsWithErrorMessage() throws Exception {
        // delete(user, 5) が呼ばれたら「使用中で消せない」例外を投げるようにモックする
        // ※戻り値が void のメソッドは doThrow(...).when(mock).method(...) の順で書く
        doThrow(new IllegalStateException("このカテゴリーは 3 件の記録で使われているため削除できません"))
                .when(categoryService).delete(user, 5L);
        // POST "/categories/5/delete" を実行する
        mockMvc.perform(post("/categories/5/delete").with(csrf()))
                // 例外は Controller が受け止めるので、画面は落ちず一覧へリダイレクトする
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"))
                // 成功ではなくエラーメッセージ errorMessage が付いていることを検証する
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
