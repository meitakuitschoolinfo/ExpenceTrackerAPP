// このテストクラスが属するパッケージを宣言する（テスト対象と同じ controller パッケージに置く）
package com.example.expensetracker.controller;

// 本物の認可ルール（"/login" はログイン不要、など）で検証するため SecurityConfig をインポートする
import com.example.expensetracker.config.SecurityConfig;
// SecurityConfig（formLogin）が必要とする UserDetailsService 役としてモック化するためインポートする
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

// HTTP GET リクエストを組み立てる static メソッド get(...) を直接呼べるように import する
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// 検証メソッド（status()/view()/model()）を直接呼べるように import する
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// LoginController だけを対象に、ログイン画面の表示を検証する軽量テスト
@WebMvcTest(LoginController.class)
// 本物の認可ルール（"/login" は permitAll）で検証したいので SecurityConfig を取り込む
@Import(SecurityConfig.class)
// このテストクラスの説明（テスト結果一覧に表示される）
@DisplayName("LoginController の単体テスト（ログイン画面の表示）")
class LoginControllerTest {

    // 擬似HTTPリクエストを送るための MockMvc を Spring から注入してもらう
    @Autowired
    private MockMvc mockMvc;

    // LoginController 自身は UserService を使わないが、SecurityConfig の formLogin が
    // UserDetailsService（=UserService）を必要とするため、モックとして1つ登録しておく
    @MockitoBean
    private UserService userService;

    // テスト1：未ログインでも "/login" が表示でき、画面・フォームが正しく渡されることを検証する
    @Test
    @DisplayName("GET \"/login\" を実行 → ステータス200・ビュー \"auth/login\"・model に loginForm がある")
    // @WithMockUser を付けない＝未認証。"/login" は permitAll なのでアクセスできるはず
    void showLoginForm_returnsLoginView() throws Exception {
        // GET "/login" を実行する
        mockMvc.perform(get("/login"))
                // ステータスが 200 OK であることを検証する（permitAll が効いている証拠）
                .andExpect(status().isOk())
                // Controller が返したビュー名が "auth/login" であることを検証する
                .andExpect(view().name("auth/login"))
                // テンプレートが th:object で使う "loginForm" がモデルに入っていることを検証する
                .andExpect(model().attributeExists("loginForm"));
    }
}
