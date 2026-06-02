// このクラスが属するパッケージを宣言する（form パッケージ＝画面の入力1件を表すクラスを置く場所）
package com.example.expensetracker.form;

// 収支の種類（EXPENSE/INCOME）を保持するため TransactionType をインポートする
import com.example.expensetracker.entity.TransactionType;
// 「null」を弾く入力チェック用アノテーションをインポートする（参照型・enumに使う）
import jakarta.validation.constraints.NotNull;
// 「空文字・null・空白のみ」をすべて弾く入力チェック用アノテーションをインポートする（文字列に使う）
import jakarta.validation.constraints.NotBlank;
// 最大文字数をチェックするためのアノテーションをインポートする
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
// カテゴリーの「登録／編集」両方を兼ねるフォームクラス
// （フィールドが少なく、登録と編集でチェック内容が同じなので1クラスで兼用する）
public class CategoryForm {

    // 編集時だけ値が入るID（新規登録時は null）。Controller側で id の有無を見て登録/更新を分岐する
    // ※バリデーションは付けない（新規では null が正常なため）
    private Long id;

    // enum（参照型）なので @NotBlank ではなく @NotNull で「未選択」を弾く
    @NotNull(message = "種類（支出/収入）は必須です")
    // このカテゴリーが「支出用」か「収入用」かを表す（画面のラジオ/hiddenから渡る）
    private TransactionType type;

    // 文字列の必須チェックは @NotBlank を使う
    @NotBlank(message = "カテゴリー名は必須です")
    // 最大100文字までに制限する（Entity の @Column(length = 100) と揃える）
    @Size(max = 100, message = "カテゴリー名は100文字以内で入力してください")
    // 画面に表示するカテゴリー名（例：食費）
    private String label;

    // 色も必須（パレットから必ず1つ選ばせる）
    @NotBlank(message = "色は必須です")
    // 最大20文字（"#f87171" のようなカラーコードを想定）
    @Size(max = 20, message = "色の指定が不正です")
    // 表示色のカラーコード（例："#f87171"）
    private String color;
}
