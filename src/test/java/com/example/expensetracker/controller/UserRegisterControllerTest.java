// このテストクラスが属するパッケージを宣言する（テスト対象と同じ controller パッケージに置く）
package com.example.expensetracker.controller;

// 本物の認可ルール（"/register" は permitAll）で検証するため SecurityConfig をインポートする
import com.example.expensetracker.config.SecurityConfig;
// モック化する対象（登録処理を担う Service）をインポートする
import com.example.expensetracker.service.UserService;

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
// SpringのBeanをMockitoのモックに差し替えるためのアノテーション（Boot 4.xの新方式）をインポートする
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// 擬似的にHTTPリクエストを送る道具 MockMvc をインポートする
import org.springframework.test.web.servlet.MockMvc;

// 「引数が何でもOK」を表すマッチャ any() を直接呼べるように import する
import static org.mockito.ArgumentMatchers.any;
// モックの振る舞いを定義する when(...) を直接呼べるように import する
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
// CSRFトークンを擬似リクエストに付与する csrf() を直接呼べるように import する
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// HTTP GET / POST リクエストを組み立てる get(...) / post(...) を直接呼べるように import する
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// 検証メソッド（status()/view()/model()/redirectedUrl()/flash()）を直接呼べるように import する
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// UserRegisterController だけを対象に、新規登録（表示・成功・失敗）を検証する軽量テスト
@WebMvcTest(UserRegisterController.class)
// 本物の認可ルール（"/register" は permitAll）で検証したいので SecurityConfig を取り込む
@Import(SecurityConfig.class)
// このテストクラスの説明
@DisplayName("UserRegisterController の単体テスト（新規登録の表示・成功・失敗）")
class UserRegisterControllerTest {

    // 擬似HTTPリクエストを送るための MockMvc を注入してもらう
    @Autowired
    private MockMvc mockMvc;

    // Controller が依存する UserService をモックに差し替える（登録処理は実際には走らせない）
    @MockitoBean
    private UserService userService;

    // テスト1：登録フォームの初期表示を検証する
    @Test
    @DisplayName("GET \"/register\" → 200・ビュー \"auth/register\"・model に userRegisterForm がある")
    void showForm_returnsRegisterView() throws Exception {
        // GET "/register" を実行する（permitAll なので未認証でOK）
        mockMvc.perform(get("/register"))
                // ステータス 200 を検証する
                .andExpect(status().isOk())
                // ビュー名が "auth/register" であることを検証する
                .andExpect(view().name("auth/register"))
                // テンプレートが th:object で使う "userRegisterForm" がモデルにあることを検証する
                .andExpect(model().attributeExists("userRegisterForm"));
    }

    // テスト2：正常な入力で登録すると、ログイン画面へリダイレクトし成功メッセージが渡ることを検証する
    @Test
    @DisplayName("正常な入力で POST \"/register\" → \"/login\" へリダイレクト＋フラッシュ message が付く")
    void register_valid_redirectsToLogin() throws Exception {
        // POST "/register" を実行する。フォーム項目を param で渡し、CSRFトークンを付ける
        mockMvc.perform(post("/register")
                        .param("name", "Akemi")               // ユーザー名（必須）
                        .param("email", "ake@test.com")        // メール（必須・形式OK）
                        .param("password", "1234abcd")         // パスワード（6文字以上）
                        .with(csrf()))                          // CSRFトークンを付与（POSTでは必須）
                // 入力が正常なので 3xx リダイレクトになる
                .andExpect(status().is3xxRedirection())
                // 転送先が "/login" であることを検証する（PRGパターン）
                .andExpect(redirectedUrl("/login"))
                // 次画面で1回だけ取れる成功メッセージ message が付いていることを検証する
                .andExpect(flash().attributeExists("message"));
        // Service の register が「ちょうど1回」呼ばれたことを検証する（業務処理が委譲された証拠）
        verify(userService).register(any());
    }

    // テスト3：入力エラー（ユーザー名が空）のときは登録画面を再描画することを検証する
    @Test
    @DisplayName("ユーザー名が空で POST \"/register\" → 再描画 \"auth/register\"・name に項目エラー")
    void register_invalid_rerendersForm() throws Exception {
        // POST "/register" を実行する。name を空にしてバリデーションエラーを起こす
        mockMvc.perform(post("/register")
                        .param("name", "")                      // ★空＝@NotBlank に違反させる★
                        .param("email", "ake@test.com")
                        .param("password", "1234abcd")
                        .with(csrf()))
                // 再描画なのでリダイレクトせず 200 で画面を返す
                .andExpect(status().isOk())
                // ビューは登録画面のまま
                .andExpect(view().name("auth/register"))
                // userRegisterForm の name フィールドにエラーが付いていることを検証する
                .andExpect(model().attributeHasFieldErrors("userRegisterForm", "name"));
        // バリデーションで弾かれたので、Service の register は1度も呼ばれていないことを検証する
        verify(userService, never()).register(any());
    }

    // テスト4：メール重複（Service が IllegalArgumentException を投げる）のときの再描画を検証する
    @Test
    @DisplayName("メール重複で POST \"/register\" → 再描画 \"auth/register\"・フォーム全体エラーが付く")
    void register_duplicateEmail_rerendersWithGlobalError() throws Exception {
        // Service の register が呼ばれたら「重複」例外を投げるようにモックを仕込む
        when(userService.register(any()))
                .thenThrow(new IllegalArgumentException("このメールアドレスは既に登録されています"));
        // 入力自体は正常な値で POST する（重複は入力チェックでは分からず、Serviceで判明する）
        mockMvc.perform(post("/register")
                        .param("name", "Akemi")
                        .param("email", "ake@test.com")
                        .param("password", "1234abcd")
                        .with(csrf()))
                // 再描画なので 200
                .andExpect(status().isOk())
                // ビューは登録画面のまま
                .andExpect(view().name("auth/register"))
                // bindingResult.reject(...) で付けたフォーム全体エラーがあることを検証する
                .andExpect(model().attributeHasErrors("userRegisterForm"));
    }
}
