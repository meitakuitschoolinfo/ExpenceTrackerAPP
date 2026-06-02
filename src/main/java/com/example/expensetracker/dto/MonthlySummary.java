// このクラスが属するパッケージを宣言する（dto パッケージ＝層をまたいで値を運ぶ「箱」を置く場所）
package com.example.expensetracker.dto;

// 全フィールドの getter を自動生成する Lombok アノテーションをインポートする
import lombok.Getter;
// 全フィールドを引数に取るコンストラクタを自動生成する Lombok アノテーションをインポートする
import lombok.AllArgsConstructor;

// 全フィールドの getter を自動生成する（画面で ${summary.income} のように使える）
@Getter
// 全フィールド (income, expense, balance) を引数に取るコンストラクタを自動生成する
@AllArgsConstructor
// 「ある月の合計」を画面に渡すためのDTO（Data Transfer Object＝値を運ぶだけの箱）
// ※Entityではない（DBテーブルと結びつかない）。集計結果を Service → Controller → 画面へ運ぶ専用
public class MonthlySummary {

    // その月の収入合計
    private int income;
    // その月の支出合計
    private int expense;
    // 残高（収入 − 支出）。マイナスにもなり得る
    private int balance;
}
