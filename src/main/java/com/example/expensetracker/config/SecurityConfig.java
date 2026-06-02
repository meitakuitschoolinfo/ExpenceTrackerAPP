// このクラスが属するパッケージを宣言する（config パッケージ＝Spring の設定クラスを置く場所）
package com.example.expensetracker.config;

// このクラスが「Bean定義の設定クラス」であることを示すアノテーションをインポートする
import org.springframework.context.annotation.Bean;
// このクラスが「設定クラス」であることを示すアノテーションをインポートする
import org.springframework.context.annotation.Configuration;
// Spring Security の設定 DSL（authorizeHttpRequests など）を持つビルダークラス
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// Spring Security の Web 機能を有効化するアノテーション
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// パスワードのハッシュ化／検証を担う Spring Security のインターフェース
import org.springframework.security.crypto.password.PasswordEncoder;
// BCrypt 方式の PasswordEncoder 実装クラス
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// 認証・認可フィルタの集合を表す Spring Security の型
import org.springframework.security.web.SecurityFilterChain;

// このクラスが Spring の設定クラスである（中の @Bean メソッドが起動時に呼ばれる）と宣言する
@Configuration
// Spring Security の Web 機能を ON にする（必須）
@EnableWebSecurity
// Spring Security の設定クラス
// 役割は2つ：① パスワードのハッシュ化器(PasswordEncoder)の提供、② URLごとの認可・ログイン設定
public class SecurityConfig {

    // Spring に PasswordEncoder Bean を1つ登録するメソッド
    // → UserService がこれを注入して「パスワードのハッシュ化」に使う
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 方式の PasswordEncoder を返す（salt 自動付与 ＆ 一方向ハッシュ＝元に戻せない）
        return new BCryptPasswordEncoder();
    }

    // Spring に SecurityFilterChain Bean を1つ登録するメソッド
    // → どのURLに認証が必要か／ログイン画面のURL／ログアウトURL などを定義する
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 各 URL ごとに「誰がアクセスできるか」を定義する
            .authorizeHttpRequests(auth -> auth
                // 公開ページ：ログイン無しでも誰でもアクセスできる
                // ★順番が重要★ permitAll を先に、authenticated を後に書く
                .requestMatchers(
                        "/login",        // ログイン画面（GET）
                        "/register",     // 新規登録（サインアップ）画面と登録処理
                        "/css/**",       // CSS（あれば）
                        "/js/**",        // JS（あれば）
                        "/images/**"     // 画像（あれば）
                ).permitAll()
                // 上記以外のすべてのURL（ダッシュボード・記録・カテゴリー等）はログイン必須にする
                .anyRequest().authenticated()
            )
            // フォーム認証（HTMLフォームで email + password を送るログイン方式）の設定
            .formLogin(form -> form
                // 自作のログイン画面を使う
                .loginPage("/login")
                // POST /login を Spring Security が直接受け取って認証処理する（Controllerに POST /login は書かない）
                .loginProcessingUrl("/login")
                // フォームの input[name="email"] をユーザー名として扱う（デフォルトの "username" を上書き）
                .usernameParameter("email")
                // フォームの input[name="password"] をパスワードとして扱う（デフォルトと同じ）
                .passwordParameter("password")
                // ログイン成功時に必ずダッシュボードへ飛ばす（true＝以前見ていた画面ではなく常にここ）
                .defaultSuccessUrl("/dashboard", true)
                // ログイン失敗時のリダイレクト先（?error が付くのを login.html が検知してメッセージ表示）
                .failureUrl("/login?error")
                // ログイン関連URLは未認証でもアクセスできるようにする
                .permitAll()
            )
            // ログアウト設定
            .logout(logout -> logout
                // POST /logout でログアウトする（GETにしない＝CSRF対策に揃える）
                .logoutUrl("/logout")
                // ログアウト成功後の遷移先（?logout が付くのを login.html が検知してメッセージ表示）
                .logoutSuccessUrl("/login?logout")
                // ログアウトURLは誰でもアクセスOK
                .permitAll()
            );
        // 上記の設定を SecurityFilterChain として組み立てて返す
        return http.build();
    }
}
