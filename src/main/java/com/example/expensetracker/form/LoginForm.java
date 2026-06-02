// このクラスが属するパッケージを宣言する（form パッケージ＝画面の入力1件を表すクラスを置く場所）
package com.example.expensetracker.form;

// メールアドレスの形式（〇〇@〇〇.〇〇）をチェックするアノテーションをインポートする
import jakarta.validation.constraints.Email;
// 「空文字・null・空白のみ」をすべて弾く入力チェック用アノテーションをインポートする
import jakarta.validation.constraints.NotBlank;

// 全フィールドの getter を自動生成する Lombok アノテーションをインポートする
import lombok.Getter;
// 引数なしコンストラクタを自動生成する Lombok アノテーションをインポートする
import lombok.NoArgsConstructor;
// 全フィールドの setter を自動生成する Lombok アノテーションをインポートする
import lombok.Setter;

// 全フィールドの getter を自動生成する
@Getter
// 全フィールドの setter を自動生成する
@Setter
// Springがリフレクションで画面入力を詰めるために必要な「引数なしコンストラクタ」を自動生成する
@NoArgsConstructor
// ログイン画面の入力を表すフォームクラス
// ※実際の認証（パスワード照合）は Spring Security が行うので、ここは「空欄ガード」と画面バインドが主目的
public class LoginForm {

    // 入力必須（空文字・null・空白だけはNG）に指定する
    @NotBlank(message = "メールアドレスは必須です")
    // メールアドレスの形式（@を含む等）をチェックする
    @Email(message = "メールアドレスの形式が不正です")
    // 画面の input[type="email"][name="email"] と紐づくフィールド
    private String email;

    // 入力必須に指定する
    @NotBlank(message = "パスワードは必須です")
    // 画面の input[type="password"][name="password"] と紐づくフィールド
    private String password;
}
