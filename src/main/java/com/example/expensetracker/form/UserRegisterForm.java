// このクラスが属するパッケージを宣言する（form パッケージ＝画面の入力1件を表すクラスを置く場所）
package com.example.expensetracker.form;

// メールアドレスの形式をチェックするアノテーションをインポートする
import jakarta.validation.constraints.Email;
// 「空文字・null・空白のみ」をすべて弾く入力チェック用アノテーションをインポートする
import jakarta.validation.constraints.NotBlank;
// 最大／最小文字数をチェックするためのアノテーションをインポートする
import jakarta.validation.constraints.Size;

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
// 利用者「新規登録（サインアップ）画面」の入力を表すフォームクラス
public class UserRegisterForm {

    // 入力必須（空文字・null・空白だけはNG）に指定する
    @NotBlank(message = "ユーザー名は必須です")
    // 最大100文字までに制限する（Entity の @Column(length = 100) と揃える）
    @Size(max = 100, message = "ユーザー名は100文字以内で入力してください")
    // 画面の input[name="name"] と紐づくフィールド（React版の displayName に相当）
    private String name;

    // 入力必須に指定する
    @NotBlank(message = "メールアドレスは必須です")
    // メールアドレスの形式（@を含む等）をチェックする
    @Email(message = "メールアドレスの形式が不正です")
    // 最大255文字までに制限する（Entity の @Column(length = 255) と揃える）
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    // 画面の input[type="email"][name="email"] と紐づくフィールド
    private String email;

    // 入力必須に指定する
    @NotBlank(message = "パスワードは必須です")
    // 安全のため最低6文字を要求し、上限はDBサイズ(255)に合わせる
    // ※「6文字以上」はDB制約ではなく業務ルールなので Form 側に書く（Entityには書かない）
    @Size(min = 6, max = 255, message = "パスワードは6文字以上で入力してください")
    // 画面の input[type="password"][name="password"] と紐づくフィールド（※生パスワード。Service側でハッシュ化する）
    private String password;
}
