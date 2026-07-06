// このテストクラスが属するパッケージを宣言する（本体クラスと同じパッケージに置くのがJavaの慣習）
package com.example.expensetracker.controller;

// 本物のSecurityConfig（認可ルール）をテストでも使うためインポートする
import com.example.expensetracker.config.SecurityConfig;
// SecurityConfig（formLogin）が必要とする UserDetailsService 役としてモック化するためインポートする
import com.example.expensetracker.service.UserService;

// テストの表示名を日本語で付けるためのアノテーションをインポートする
import org.junit.jupiter.api.DisplayName;
// 1つのテストメソッドであることを示すアノテーションをインポートする
import org.junit.jupiter.api.Test;
// Springが用意したBean（MockMvc等）を注入してもらうためのアノテーションをインポートする
import org.springframework.beans.factory.annotation.Autowired;
// 「Controller層だけ」を対象にした軽量テストを起動するアノテーションをインポートする
// ★Spring Boot 4.0 でパッケージが boot.webmvc.test.autoconfigure に変わった（3.x は boot.test.autoconfigure.web.servlet）★
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// 追加の設定クラス（ここではSecurityConfig）をテストの文脈に取り込むためのアノテーションをインポートする
import org.springframework.context.annotation.Import;
// 「ログイン済みユーザー」を擬似的に作り出すためのアノテーションをインポートする
import org.springframework.security.test.context.support.WithMockUser;
// SpringのBeanをMockitoのモックに差し替えるためのアノテーション（Spring Boot 3.4+／4.xの新方式）をインポートする
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// 擬似的にHTTPリクエストを送る道具 MockMvc をインポートする
import org.springframework.test.web.servlet.MockMvc;

// HTTP GET リクエストを組み立てる static メソッド get(...) を直接呼べるように import する
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// 検証メソッド（status()/redirectedUrl()/redirectedUrlPattern() 等）を直接呼べるように import する
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HomeController だけを対象に、MVC（URL→メソッド対応やリダイレクト）を検証する軽量テスト
@WebMvcTest(HomeController.class)
// 本物の認可ルール（"/" はログイン必須、など）で検証したいので SecurityConfig を取り込む
@Import(SecurityConfig.class)
// このテストクラスの説明（テスト結果一覧に表示される）
@DisplayName("HomeController の単体テスト（トップページのリダイレクト）")
class HomeControllerTest {

    // 擬似HTTPリクエストを送るための MockMvc を Spring から注入してもらう
    @Autowired
    private MockMvc mockMvc;

    // SecurityConfig の formLogin が内部で UserDetailsService（=UserService）を必要とするため、
    // 中身は使わないがモックとして1つ登録しておく（無いと文脈の組み立てに失敗することがある）
    @MockitoBean
    private UserService userService;

    // テスト1：ログイン済みユーザーが "/" を開くとダッシュボードへ転送されることを検証する
    @Test
    @DisplayName("ログイン済みで GET \"/\" を実行 → \"/dashboard\" へリダイレクトされる")
    // @WithMockUser を付けると「認証済み」の状態でリクエストが飛ぶ（username はテスト用の値）
    @WithMockUser(username = "ake@test.com")
    void index_authenticated_redirectsToDashboard() throws Exception {
        // GET "/" を実行する
        mockMvc.perform(get("/"))
                // ステータスが 3xx（リダイレクト）であることを検証する
                .andExpect(status().is3xxRedirection())
                // 転送先がちょうど "/dashboard" であることを検証する
                .andExpect(redirectedUrl("/dashboard"));
    }

    // テスト2：未ログインで "/" を開くと、ログイン画面へ追い返されることを検証する（認可ルールの確認）
    @Test
    @DisplayName("未ログインで GET \"/\" を実行 → ログイン画面へリダイレクトされる")
    // @WithMockUser を付けない＝未認証の状態
    void index_unauthenticated_redirectsToLogin() throws Exception {
        // GET "/" を実行する
        mockMvc.perform(get("/"))
                // 未認証なので Spring Security が 3xx リダイレクトで弾く
                .andExpect(status().is3xxRedirection())
                // 転送先がログイン画面 "/login" であることを検証する
                // ※Spring Security 7 では相対パス "/login"（旧バージョンは http://localhost/login）
                .andExpect(redirectedUrl("/login"));
    }
}
