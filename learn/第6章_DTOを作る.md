# 📘 第6章

## DTO を作る
― 画面へ結果を返す「出力の箱」。Entity をそのまま画面に返さない理由 ―

---

## 🎯 6-1. この章のゴール

✔ DTO の役割を **一文で説明できる**
✔ **「集計結果を Entity で返さない理由」** を3つ以上挙げられる
✔ Form（入力箱）と DTO（出力箱）の **違い** を説明できる
✔ DTO に **`@Setter` を付けない理由**（Form との決定的な違い）が分かる
✔ 3つの DTO（`MonthlySummary` / `CategorySlice` / `MonthlyTrendPoint`）が React のどの部品に対応するか言える

---

## 🧩 6-2. DTO とは何か

> DTO ＝ **Data Transfer Object＝「層をまたいで値を運ぶだけの箱」**

このアプリでは、**Service が計算した集計結果を、Controller 経由で画面へ運ぶ** ために使います。

```text
[ Entity ] ─Serviceが集計─▶ [ DTO ] ─Controllerが渡す─▶ [ 画面 ]
```

👉 Form が「画面 → サーバー」の **入力箱** なら、DTO は「サーバー → 画面」の **出力箱**。
　 どちらも **「Entity（DBの都合）と混ぜない」** ための箱です。

---

## 🔄 6-3. なぜ Form の次に DTO なのか

```text
[ Service ]（第7章）
   ├── Repository  ← 第4章で完成
   ├── Form        ← 第5章で完成（入力箱）
   └── DTO         ← ★第6章でここ（出力箱）
```

Service は **Form を受け取り、DTO を返す**。だから Service より先に **両方の箱** を揃えておく。
DTO は中身が単純（フィールドと getter だけ）なので、Service を書く前にサッと用意できます。

---

## 🆚 6-4. ★最重要★ なぜ集計結果を Entity で返さないのか

ダッシュボードに出す「今月の収入合計・支出合計・残高」は、**どのテーブルにも無い値** です。
`Transaction` を全部足して初めて作られる「計算結果」なので、Entity では表せません。

| ＃ | Entity をそのまま画面に返すと…                      | 起きること                         |
| -- | --------------------------------------------------- | ---------------------------------- |
| ①  | 「合計」「割合」などの集計値を入れる場所が無い       | そもそも Entity で表現できない     |
| ②  | `id` / `createdAt` / 関連先まで画面に漏れる         | 不要な情報が露出する               |
| ③  | 遅延ロードの関連を画面で触る                        | `LazyInitializationException` の罠 |
| ④  | 「食費」「#f87171」「45%」など画面向けの形にできない | 画面のコードが複雑になる           |

👉 だから **「画面に必要な形だけを持つ専用の箱（DTO）」** を用意し、Service が集計結果を詰めて返す。

---

## 🔁 6-5. Form と DTO の対比（入力箱と出力箱）

| 観点             | Form（第5章）                      | DTO（この章）                        |
| ---------------- | ---------------------------------- | ------------------------------------ |
| 向き             | 画面 → サーバー（**入力**）        | サーバー → 画面（**出力**）          |
| 中身             | 画面の入力1件                      | 集計・表示のための結果               |
| バリデーション   | `@NotBlank` などを付ける           | 付けない（すでに正しい値を詰めるだけ）|
| 誰が値を入れる？ | **Spring** がリフレクションで詰める | **Service** が `new` して詰める      |
| Lombok           | `@Getter`＋`@Setter`＋`@NoArgsConstructor` | `@Getter`＋`@AllArgsConstructor`（`@Setter` 無し） |

👉 「値を入れるのが誰か」が Lombok の違いに直結します（詳しくは 6-10）。

---

## 📦 6-6. この章で作る3つの DTO

| 順 | DTO                | 役割                     | React 版の対応                       |
| -- | ------------------ | ------------------------ | ------------------------------------ |
| 1  | `MonthlySummary`   | その月の収入・支出・残高 | `summary`（`useMemo` の集計）        |
| 2  | `CategorySlice`    | 支出内訳の1カテゴリー分  | 円グラフの1要素（ラベル・色・割合）  |
| 3  | `MonthlyTrendPoint`| 推移グラフの1ヶ月分      | 棒グラフの1本（その月の収入・支出）  |

すべて `com.example.expensetracker.dto` パッケージに置きます。

---

## 🧱 6-7. ステップ① MonthlySummary（月の合計）

完成ファイル：[MonthlySummary.java](../src/main/java/com/example/expensetracker/dto/MonthlySummary.java)

```java
@Getter
@AllArgsConstructor
public class MonthlySummary {
    private int income;   // その月の収入合計
    private int expense;  // その月の支出合計
    private int balance;  // 残高（収入 − 支出）。マイナスにもなり得る
}
```

- 金額は「円」なので **`int`** で十分（小数は扱わない）
- `balance`（残高）も **フィールドとして持つ**
  - 計算（`income - expense`）は **Service の仕事**、DTO は結果を **運ぶだけ**
  - 画面では `${summary.balance}` と書くだけで残高が出せる（画面側で引き算しない）

---

## 🧱 6-8. ステップ② CategorySlice（支出内訳の1切れ）

完成ファイル：[CategorySlice.java](../src/main/java/com/example/expensetracker/dto/CategorySlice.java)

```java
@Getter
@AllArgsConstructor
public class CategorySlice {
    private String label;      // カテゴリー名（例："食費"）
    private String color;      // 表示色（例："#f87171"）
    private int amount;        // そのカテゴリーの支出合計
    private int percentage;    // 全体に占める割合（％。0〜100）
}
```

- React 版の円グラフ1要素（ラベル・色・金額・割合）に相当
- `percentage` は **Service が計算した結果** を入れるだけ。DTO は割り算をしない
- 「割合」も「色」も **画面が必要とする形** なので、専用の箱にまとめておくと画面が楽

---

## 🧱 6-9. ステップ③ MonthlyTrendPoint（推移の1ヶ月）

完成ファイル：[MonthlyTrendPoint.java](../src/main/java/com/example/expensetracker/dto/MonthlyTrendPoint.java)

```java
@Getter
@AllArgsConstructor
public class MonthlyTrendPoint {
    private String label;   // 月のラベル（例："6月"）
    private int income;     // その月の収入合計
    private int expense;    // その月の支出合計
}
```

- 「直近6ヶ月の推移」は、この箱を **`List<MonthlyTrendPoint>`** にして6個分渡す
- React 版の棒グラフ1本（その月の収入・支出）に相当

---

## 🏷 6-10. Lombok：`@Getter`＋`@AllArgsConstructor`（`@Setter` が無い理由）

DTO に付ける Lombok は **2つだけ**。Form とここが決定的に違います。

| アノテーション         | 役割                                              |
| ---------------------- | ------------------------------------------------- |
| `@Getter`              | 画面で `${summary.income}` のように値を読めるようにする |
| `@AllArgsConstructor`  | 全フィールドを引数に取るコンストラクタを自動生成する |

### なぜ `@Setter` / `@NoArgsConstructor` が要らないのか

- Form は **Spring がリフレクションで1個ずつ詰める** → 空コンストラクタと setter が必須だった
- DTO は **Service が `new MonthlySummary(income, expense, balance)` と一気に作る**
  → 全部まとめて渡せる `@AllArgsConstructor` があれば足りる
- 一度作った集計結果を **後から書き換える必要が無い** → `@Setter` を付けない（**値が変わらない箱＝安全**）

> 💡 補足：Java の `record` でも同じ「不変の箱」を書けます。
> 　 この教材では他クラスと **Lombok でスタイルを統一** するため `class ＋ @Getter/@AllArgsConstructor` にしています。

---

## 🖥 6-11. 画面での使われ方（Thymeleaf と JSON）

DTO は getter があるので、**そのまま画面に渡せます**。

- Thymeleaf：`th:text="${summary.income}"` のように **getter 経由** で表示
- グラフ用：`List<CategorySlice>` / `List<MonthlyTrendPoint>` は **getter 経由で JSON 配列** になり、画面の JavaScript がグラフを描く

👉 「Service が計算した集計値（DTO）を、画面や画面の JavaScript にそのまま渡す」だけ。
　 具体的な受け渡しは **第8章（Controller・テンプレート）** で扱います。

---

## ❌ 6-12. 初心者がやりがちなNG

| NG                                          | 何が起きる？                         |
| ------------------------------------------- | ------------------------------------ |
| 集計結果を Entity に無理やり入れて返す      | どのテーブルにも無い値を表現できない |
| Entity をそのまま画面に返す                 | id/createdAt が漏れる／Lazy例外の罠  |
| DTO に `@Setter` を付ける                   | 不要（値は生成時に確定＝不変でよい） |
| DTO の中で割合や残高を計算する              | 計算は Service の仕事。DTO は運ぶだけ |
| DTO を `entity` パッケージに置く            | 役割が混ざる。`dto` パッケージに置く |

---

## 📝 6-13. ソースを書くときの順番

1. `MonthlySummary`（int 3つ：income / expense / balance）
2. `CategorySlice`（label / color / amount / percentage）
3. `MonthlyTrendPoint`（label / income / expense）

3つとも **`@Getter` ＋ `@AllArgsConstructor`** を付けるだけ。中身はフィールド宣言のみ。

---

## ✨ 6-14. ソースを書くときのポイント

- **DTO ＝ 画面へ結果を運ぶ出力箱**。Entity（DBの都合）と混ぜない
- 集計値（合計・割合・残高）は **DTO でしか表せない**（Entity には無い）
- **計算は Service、DTO は運ぶだけ**（ロジックを持たせない）
- Lombok は **`@Getter` ＋ `@AllArgsConstructor` の2つだけ**（`@Setter` は付けない＝不変）
- 置き場所は **`dto` パッケージ**

---

## ✅ 6-15. 第6章まとめ

✔ DTO ＝「集計結果を画面へ運ぶ出力箱」（Form は入力箱、DTO は出力箱）
✔ 合計・割合・残高は **Entity では表せない** → 専用の DTO を用意する
✔ 値を入れるのは **Service**（`new` で一気に）→ `@Setter` は不要（不変の箱）
✔ 計算は Service、**DTO はロジックを持たず運ぶだけ**
✔ 3つの DTO は React の summary / 円グラフ / 棒グラフに対応する

---

## 🔜 次の章

**第7章：Service を作る**
― 業務ロジックの中心。**パスワードのハッシュ化**・集計・詰め替えがつながる ―
