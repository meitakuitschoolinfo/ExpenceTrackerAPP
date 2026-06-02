// このクラスが属するパッケージを宣言する（dto パッケージ＝層をまたいで値を運ぶ「箱」を置く場所）
package com.example.expensetracker.dto;

// 全フィールドの getter を自動生成する Lombok アノテーションをインポートする
import lombok.Getter;
// 全フィールドを引数に取るコンストラクタを自動生成する Lombok アノテーションをインポートする
import lombok.AllArgsConstructor;

// 全フィールドの getter を自動生成する
@Getter
// 全フィールド (label, income, expense) を引数に取るコンストラクタを自動生成する
@AllArgsConstructor
// 「直近6ヶ月の推移グラフ」の1ヶ月分を画面に渡すためのDTO
// React版の棒グラフ1本（その月の収入・支出）に相当する
public class MonthlyTrendPoint {

    // 月のラベル（例："6月"）
    private String label;
    // その月の収入合計
    private int income;
    // その月の支出合計
    private int expense;
}
