// このenum（列挙型）が属するパッケージを宣言する（entity パッケージ＝DBと結びつく型を置く場所）
package com.example.expensetracker.entity;

// 「収支の種類」を表す列挙型（enum）
// 取りうる値は「支出（EXPENSE）」と「収入（INCOME）」の2つだけに限定したいので、
// String の "expense" / "income" を直接使わず、型として固定する（打ち間違い・想定外の値を防げる）
public enum TransactionType {

    // 支出を表す定数。画面に出す日本語ラベルとして "支出" を持たせる
    EXPENSE("支出"),
    // 収入を表す定数。画面に出す日本語ラベルとして "収入" を持たせる
    INCOME("収入");

    // 各定数が持つ「画面表示用ラベル」を保持するフィールド（外から書き換えないので final）
    private final String label;

    // enum のコンストラクタ（EXPENSE("支出") のカッコの中の値がここに渡る）
    // enum のコンストラクタは必ず private 相当（外部から new できない）
    TransactionType(String label) {
        // 渡されたラベルをフィールドに保存する
        this.label = label;
    }

    // 画面（Thymeleaf）から ${type.label} で日本語ラベルを取り出せるようにする getter
    public String getLabel() {
        // 保持しているラベルを返す
        return label;
    }
}
