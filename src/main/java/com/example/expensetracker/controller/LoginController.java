// このクラスが属するパッケージを宣言する（controller パッケージ＝HTTPの入口を置く場所）
package com.example.expensetracker.controller;

// ログイン画面にバインドする Form をインポートする
import com.example.expensetracker.form.LoginForm;

// このクラスを Controller として認識させるアノテーション
import org.springframework.stereotype.Controller;
// テンプレートに値を渡すための入れ物
import org.springframework.ui.Model;
// HTTP GET 用アノテーション
import org.springframework.web.bind.annotation.GetMapping;

// Spring に Controller として登録する
@Controller
// ログイン画面の表示を担当する Controller
public class LoginController {

    // HTTP GET "/login" にマッピング：ログイン画面を表示する
    // ★POST /login は書かない★（認証処理は SecurityConfig 経由で Spring Security が直接受け取る）
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        // 空の LoginForm をモデルに入れる（テンプレートの th:object="${loginForm}" と紐づける）
        model.addAttribute("loginForm", new LoginForm());
        // テンプレート templates/auth/login.html を返す
        return "auth/login";
    }
}
