// このクラスが属するパッケージを宣言する（dto パッケージ＝層をまたいで値を運ぶ「箱」を置く場所）
package com.example.expensetracker.dto;

// 全フィールドの getter を自動生成する Lombok アノテーションをインポートする
import lombok.Getter;
// 全フィールドを引数に取るコンストラクタを自動生成する Lombok アノテーションをインポートする
import lombok.AllArgsConstructor;

// 全フィールドの getter を自動生成する
@Getter
// 全フィールド (label, color, amount, percentage) を引数に取るコンストラクタを自動生成する
@AllArgsConstructor
// 「支出の内訳（カテゴリーごとの1切れ）」を画面に渡すためのDTO
// React版の円グラフ1要素に相当する（ラベル・色・金額・割合）
public class CategorySlice {

    // カテゴリー名（例：食費）。削除済みなら "不明" を入れる想定
    private String label;
    // 表示色のカラーコード（例："#f87171"）
    private String color;
    // そのカテゴリーの支出合計
    private int amount;
    // 全体に占める割合（％。0〜100）。横棒グラフの長さや表示に使う
    private int percentage;
}
