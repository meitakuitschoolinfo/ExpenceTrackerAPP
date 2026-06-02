// このクラスが属するパッケージを宣言する（controller パッケージ＝HTTPの入口を置く場所）
package com.example.expensetracker.controller;

// このクラスを「画面を返す Controller」として Spring に認識させるためのアノテーション
import org.springframework.stereotype.Controller;
// HTTP GET リクエスト用アノテーション
import org.springframework.web.bind.annotation.GetMapping;

// Spring に Controller として登録する
@Controller
// トップページ（"/"）へのアクセスを担当する最小の Controller
public class HomeController {

    // HTTP GET "/" にマッピングする
    @GetMapping("/")
    public String index() {
        // "redirect:" はテンプレート名ではなく「内部リダイレクト指示」
        // ログイン済みならダッシュボードへ、未ログインなら Security が /login に飛ばしてくれる
        return "redirect:/dashboard";
    }
}
