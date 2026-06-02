// このクラスが属するパッケージを宣言する（form パッケージ＝画面の入力1件を表すクラスを置く場所）
package com.example.expensetracker.form;

// 収支の種類（EXPENSE/INCOME）を保持するため TransactionType をインポートする
import com.example.expensetracker.entity.TransactionType;
// 数値の最小値をチェックするアノテーションをインポートする
import jakarta.validation.constraints.Min;
// 「null」を弾く入力チェック用アノテーションをインポートする（数値・参照型に使う）
import jakarta.validation.constraints.NotNull;
// 最大文字数をチェックするためのアノテーションをインポートする
import jakarta.validation.constraints.Size;
// 画面の "yyyy-MM-dd" 文字列を LocalDate に変換する形式を指定するアノテーション
import org.springframework.format.annotation.DateTimeFormat;

// 「日付だけ」を扱う LocalDate をインポートする
import java.time.LocalDate;

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
// 収支の記録「新規登録画面」の入力を表すフォームクラス
public class TransactionForm {

    // enum（参照型）なので @NotNull で「未選択」を弾く
    @NotNull(message = "種類（支出/収入）は必須です")
    // この記録が「支出」か「収入」かを表す（画面のトグルから渡る）
    private TransactionType type;

    // 日付も必須（未入力を弾く）
    @NotNull(message = "日付は必須です")
    // <input type="date"> から来る "2026-06-01" を ISO 形式の日付として解釈させる
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    // 取引日（時刻なし）
    private LocalDate transactionDate;

    // ★最重要★ Form は Category そのものではなく「カテゴリーのID(Long)」だけを持つ
    //   画面の <select value="...">（数値ID）と直結でき、Service が後で Category に変換する
    @NotNull(message = "カテゴリーは必須です")
    // 選択されたカテゴリーのID
    private Long categoryId;

    // 金額も必須（数値・参照型なので @NotNull）
    @NotNull(message = "金額は必須です")
    // 1円以上を要求する（0円や負数を弾く）
    @Min(value = 1, message = "金額は1円以上で入力してください")
    // 金額（円）。null可否のため int ではなく Integer を使う
    private Integer amount;

    // メモは任意項目なので @NotNull/@NotBlank は付けない（入力があったときだけ長さチェックする）
    @Size(max = 255, message = "メモは255文字以内で入力してください")
    // メモ（任意。例：「スーパーでの買い物」）
    private String memo;
}
