# 📘 第3章

## Entity を作る③（Transaction）
― 2つの外部キーを持つ「収支の記録」。金額・日付の型を選ぶ ―

---

## 🎯 3-1. この章のゴール

✔ **2つの `@ManyToOne`**（User と Category）を1クラスに書ける
✔ 金額に `Integer`、取引日に `LocalDate` を選ぶ理由を説明できる
✔ `LocalDate`（日付だけ）と `LocalDateTime`（日付＋時刻）の使い分けができる
✔ Transaction を **最後に作る** 理由を説明できる

---

## 🧩 3-2. Transaction とは

家計簿の中心。「いつ・いくら・どのカテゴリーで・支出か収入か」を表す **記録1件** です。
React 版の `transactions` 配列の1要素に相当します。

---

## 🔄 3-3. なぜ Transaction を最後に作るのか

```text
User ─┐
      ├─ Transaction が両方を参照する
Category ─┘
```

- Transaction は **User と Category の両方** を参照する
- → 両方が揃ってからでないと書けない（import エラーになる）
- → だから Entity の中で **一番最後**

👉 「2つに依存するものは、2つが揃ってから作る」。

---

## 🧱 3-4. ブロック解説

完成ファイル：[Transaction.java](../src/main/java/com/example/expensetracker/entity/Transaction.java)

### 2つの外部キー

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;          // 持ち主

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private Category category;  // どのカテゴリーか
```

- 第2章で1つ書いた `@ManyToOne` を、ここでは **2つ** 書くだけ
- `user_id`（誰の記録か）と `category_id`（どのカテゴリーか）の2本の外部キー

### 金額：なぜ `Integer` なのか

```java
@Column(name = "amount", nullable = false)
private Integer amount;
```

- 日本円は **小数を扱わない**（1.5円は無い）→ `BigDecimal` や `double` は不要
- → 整数で十分なので `Integer`
- なぜ `int` でなく `Integer`？ → Form でも使い、未入力(null)を区別したいから（ラッパー型）

### 取引日：`LocalDate`（日付だけ）

```java
@Column(name = "transaction_date", nullable = false)
private LocalDate transactionDate;
```

| 型              | 持つ情報         | このアプリでの用途              |
| --------------- | ---------------- | ------------------------------- |
| `LocalDate`     | 日付だけ         | **取引日**（時刻は不要）        |
| `LocalDateTime` | 日付＋時刻       | created_at / updated_at（記録時刻）|

- 「2026-06-01に使った」のように **取引は日付単位** → `LocalDate`
- 一方 `created_at` は「いつ登録操作したか」なので時刻付き → `LocalDateTime`

👉 **意味に合わせて型を選ぶ**。日付だけなら `LocalDate`、時刻も要るなら `LocalDateTime`。

### メモ（任意項目）

```java
@Column(name = "memo", length = 255)   // ← nullable を書かない＝NULL許可
private String memo;
```

- メモは任意 → **NOT NULL を付けない**（`nullable` を省略すると既定で NULL 許可）

そして共通の `created_at` / `updated_at` ＋ `@PrePersist` / `@PreUpdate`。

---

## ❌ 3-5. 初心者がやりがちなNG

| NG                                  | 何が起きる？                          |
| ----------------------------------- | ------------------------------------- |
| 金額を `double` にする              | 小数誤差／円なのに無駄に複雑          |
| 取引日を `LocalDateTime` にする     | 不要な時刻が混じり、月の集計がブレやすい |
| 任意のメモに `nullable=false`       | メモ無しで保存できず、毎回エラー       |
| `@ManyToOne` を1つ書き忘れる        | category または user が保存されない    |

---

## 📝 3-6. ソースを書くときの順番

1. 空クラス → `@Entity` / `@Table(name="transactions")` / Lombok3点セット
2. 主キー `id`
3. **外部キー2本** `user` / `category`（`@ManyToOne` ＋ `@JoinColumn`）
4. 列挙型カラム `type`（`@Enumerated(EnumType.STRING)`）
5. `transactionDate`（`LocalDate`）
6. `amount`（`Integer`）／ `memo`（任意）
7. 共通カラム `createdAt` / `updatedAt` ＋ 日時フック

---

## ✨ 3-7. ソースを書くときのポイント

- **2つ以上に依存するEntityは最後に作る**
- **金額は `Integer`（円は整数）**、未入力を区別したいのでラッパー型
- **日付だけなら `LocalDate`、時刻も要るなら `LocalDateTime`**
- **任意項目は NOT NULL を付けない**

---

## ✅ 3-8. 第3章まとめ

✔ Transaction は User と Category の **2つの外部キー** を持つ「記録」
✔ 金額は `Integer`、取引日は `LocalDate`、登録時刻は `LocalDateTime`
✔ 「**2つに依存するものは最後**」の順番感覚を再確認
✔ これで Entity 4つ（型・User・Category・Transaction）が完成 → 次は Repository

---

## 🔜 次の章

**第4章：Repository を作る**
― DBと会話する「専門窓口」。SQLを書かずに検索メソッドを生やす ―
