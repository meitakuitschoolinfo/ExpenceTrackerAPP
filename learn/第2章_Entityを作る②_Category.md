# 📘 第2章

## Entity を作る②（Category）
― 外部キー（`user_id`）と列挙型カラム。`@ManyToOne` が初登場 ―

---

## 🎯 2-1. この章のゴール

✔ `@ManyToOne` / `@JoinColumn` で **「他のテーブルへの参照（外部キー）」** を表現できる
✔ `FetchType.LAZY`（遅延読み込み）の意味を説明できる
✔ enum を `@Enumerated(EnumType.STRING)` で保存する理由を説明できる
✔ なぜ Category が **User の後・Transaction の前** なのか説明できる

---

## 🧩 2-2. Category とは

家計簿の「カテゴリー」（食費・住居費・給与…）1件を表すテーブルです。
React 版の `DEFAULT_CATEGORIES` ＋ ユーザーが追加するカスタムカテゴリーに相当します。

特徴は2つ：

1. **利用者ごとに分かれている**（誰のカテゴリーか＝`user_id`）
2. **支出用か収入用かが決まっている**（`type`）

---

## 🔄 2-3. なぜ User の次、Transaction の前なのか

```text
User              ← 第1章で完成
   ▲
Category          ← ★この章。User を参照する
   ▲
Transaction       ← 次章。Category を参照する
```

- Category は `User` を参照する → User が先に必要（第1章で完成済み）
- Transaction は `Category` を参照する → Category を先に作っておく必要がある

👉 だから **User → Category → Transaction** の順。

---

## 🧱 2-4. ブロック解説

完成ファイル：[Category.java](../src/main/java/com/example/expensetracker/entity/Category.java)

### ★初登場★ 外部キー `@ManyToOne` / `@JoinColumn`

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

| 部分                                  | 意味                                            |
| ------------------------------------- | ----------------------------------------------- |
| `@ManyToOne`                          | 「カテゴリー 多 ─ 1 ユーザー」の関連            |
| `fetch = FetchType.LAZY`              | User は **必要になったとき** に取りに行く（遅延）|
| `@JoinColumn(name = "user_id")`       | 外部キーのカラム名は `user_id`                  |
| `nullable = false`                    | 「持ち主のいないカテゴリー」は許さない          |

#### `Long userId` ではなく `User user` にする理由

- Entity の世界では **「IDの数字」ではなく「相手のオブジェクトそのもの」** を持つのが自然
- こうしておくと `category.getUser().getName()` のように **たどれる**
- JPAが内部で `user_id` カラムと自動でつなげてくれる

> 💡 「画面の都合（Form）」では逆に `Long` を使います（第5章）。
> 　 **Entity は相手のオブジェクト、Form は ID（数字）** ―この使い分けが超重要。

#### `FetchType.LAZY`（遅延読み込み）とは

- カテゴリーを取得した瞬間に **毎回 User まで取りに行くと無駄**
- LAZY にすると「`category.getUser()` を実際に呼んだ瞬間」に初めてDBへ取りに行く
- 無駄なSQLを減らせる（パフォーマンスのため）

### ★初登場★ 列挙型カラム `@Enumerated(EnumType.STRING)`

```java
@Enumerated(EnumType.STRING)
@Column(name = "type", nullable = false, length = 20)
private TransactionType type;
```

| 設定                     | DBに入る値        |
| ------------------------ | ----------------- |
| `EnumType.STRING`        | `'EXPENSE'` `'INCOME'`（文字列） |
| `EnumType.ORDINAL`（既定）| `0` `1`（並び順の番号）           |

#### なぜ必ず `STRING` を選ぶのか？（重要）

- `ORDINAL`（番号）だと、後で enum の並び順を変えた瞬間に **既存データの意味がズレて壊れる**
  （例：`EXPENSE,INCOME` を `INCOME,EXPENSE` に並べ替えると、0と1が入れ替わる）
- `STRING` なら `'EXPENSE'` という **文字で残る** ので、並び順を変えても安全

👉 **enum をDBに保存するときは必ず `@Enumerated(EnumType.STRING)`**。

### 残りのカラム

```java
@Column(name = "label", nullable = false, length = 100)
private String label;   // 画面に出すカテゴリー名（例：食費）

@Column(name = "color", nullable = false, length = 20)
private String color;   // 表示色（例："#f87171"）
```

- `label` … React版の `info.label` に相当
- `color` … 円グラフや一覧の丸アイコンの色（React版の `info.color`）

そして共通の `created_at` / `updated_at` ＋ `@PrePersist` / `@PreUpdate`（第1章と同じ）。

---

## ❌ 2-5. 初心者がやりがちなNG

| NG                                     | 何が起きる？                          |
| -------------------------------------- | ------------------------------------- |
| `@JoinColumn` を書かず `Long userId` にする | 関連がたどれない／JPAの旨味が消える |
| `@Enumerated` を付けない（既定のORDINAL）  | 並び替えでデータが壊れる             |
| `fetch` を全部 EAGER にする            | 無駄なSQLが増える／N+1問題の温床      |
| `nullable=false` を書かず持ち主なしを許す | 誰のカテゴリーか分からないゴミが入る |

---

## 📝 2-6. ソースを書くときの順番

1. 空クラス → `@Entity` / `@Table(name="categories")` / Lombok3点セット
2. 主キー `id`（`Long` ＋ IDENTITY）
3. **外部キー** `user`（`@ManyToOne` ＋ `@JoinColumn`）
4. **列挙型カラム** `type`（`@Enumerated(EnumType.STRING)`）
5. 業務カラム `label` / `color`
6. 共通カラム `createdAt` / `updatedAt` ＋ `@PrePersist`/`@PreUpdate`

---

## ✨ 2-7. ソースを書くときのポイント

- **他テーブルへの参照は `@ManyToOne` ＋ `@JoinColumn`**、相手は ID でなく **オブジェクト**で持つ
- **enum カラムは必ず `@Enumerated(EnumType.STRING)`**
- **`@ManyToOne` の既定は EAGER ではなく LAZY を明示**（無駄なSQLを避ける）
- 参照は `nullable = false` で「持ち主なし」を防ぐ

---

## ✅ 2-8. 第2章まとめ

✔ Category は「利用者ごと・支出/収入の種類を持つ」カテゴリーのテーブル
✔ 外部キーは **`@ManyToOne` ＋ `@JoinColumn(name="user_id")`**、相手はオブジェクトで持つ
✔ enum カラムは **`@Enumerated(EnumType.STRING)`** で文字列保存（並び替え事故を防ぐ）
✔ `FetchType.LAZY` で必要時だけ関連を取りに行く

---

## 🔜 次の章

**第3章：Entity を作る③（Transaction）**
― 2つの外部キー（User と Category）を持つ「収支の記録」。金額・日付の型を学ぶ ―
