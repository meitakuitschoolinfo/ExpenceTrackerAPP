# 📘 第1章

## Entity を作る①（TransactionType と User）
― 「テーブル＝クラス」を最初に体で覚える ―

---

## 🎯 1-1. この章のゴール

✔ Entity の役割を **一文で説明できる**
✔ なぜ最初に作るのが **TransactionType（enum）と User** なのか説明できる
✔ `@Entity` / `@Table` / `@Id` / `@GeneratedValue` / `@Column` の意味を分解して説明できる
✔ `@PrePersist` / `@PreUpdate` で **created_at / updated_at が自動で入る理由** を説明できる
✔ JPA が **「引数なしコンストラクタ」を要求する理由** を説明できる
✔ 列挙型を `@Enumerated(EnumType.STRING)` で保存する理由を説明できる（※第2章で本格使用）

---

## 🧩 1-2. Entity とは何か？

### 一言で言うと

> Entity ＝ **「DBのテーブル1行」を Java のクラスとして表したもの**

```text
[ Service ] → [ Repository ] → [ Entity ] ← ★ここ：テーブル1行＝クラス1個 → [ DB ]
```

👉 **Entity は「形」だけを持つ。動き（業務ロジック）は持たない。**

---

## 🔄 1-3. なぜ「TransactionType と User」から作るのか

第0章の鉄則「**依存される側 → 依存する側**」で考えます。

```text
TransactionType（enum）  ← Category も Transaction も使う（独立）
User                     ← Category も Transaction も参照する（独立）
   ▲
Category（User に依存）
   ▲
Transaction（User と Category に依存）
```

- `TransactionType` は **誰にも依存しない型**。これが無いと Category も Transaction も書けない。
- `User` も **誰にも依存しない**。Category / Transaction から参照される側。

👉 だから **最初に「種類(enum)」と「ユーザー」** を作る。

| 順番 | 作るもの          | 理由                                |
| ---: | ----------------- | ----------------------------------- |
| 1    | `TransactionType` | enum。支出/収入の2値に固定したい    |
| 2    | `User`            | 独立。他テーブルから参照される      |

---

## 🧱 1-4. ステップ① TransactionType（列挙型）

完成ファイル：[TransactionType.java](../src/main/java/com/example/expensetracker/entity/TransactionType.java)

```java
public enum TransactionType {
    EXPENSE("支出"),
    INCOME("収入");

    private final String label;
    TransactionType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

### なぜ String の "expense" / "income" を直接使わないのか？

| 方式                        | 問題点                                       |
| --------------------------- | -------------------------------------------- |
| `String type = "expense"`   | `"Expense"` `"expence"` など打ち間違いに気付けない |
| `enum TransactionType`      | **取りうる値が2つに固定** され、コンパイラが守ってくれる |

👉 「決まった選択肢しかない項目」は **enum にする**。これが型安全の基本。

### `label`（日本語表示名）を持たせる理由

- 画面に出すのは "EXPENSE" ではなく "支出"
- enum 自身に「画面表示名」を持たせておくと、`${type.label}` で取り出せて便利

---

## 🆔 1-5. ステップ② User Entity の4ブロック

完成ファイル：[User.java](../src/main/java/com/example/expensetracker/entity/User.java)

Entity は大きく **4ブロック** でできています。

```text
①  @Entity / @Table         ← 「これはテーブルだよ」宣言
②  @Id / @GeneratedValue    ← 「主キーはこれ」宣言
③  @Column を持つフィールド ← 「カラム定義」
④  @PrePersist / @PreUpdate ← 「日時を自動で入れる」仕掛け
```

### ブロック① `@Entity` と `@Table`

```java
@Entity
@Table(name = "users")
public class User { ... }
```

| 部分                    | 意味                                       |
| ----------------------- | ------------------------------------------ |
| `@Entity`               | このクラスはJPAエンティティ、とSpringに宣言 |
| `@Table(name = "users")`| マッピングするテーブル名を明示             |

> 💡 なぜ "user" でなく "users"？ → `user` は多くのDBで **予約語** になりがち。
> 　 複数形 `users` にして衝突を避けるのが安全。

### ブロック② 主キー

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id")
private Long id;
```

- `@Id` … PRIMARY KEY 宣言（Entityに必ず1つ必要）
- `IDENTITY` … DB側で自動採番（PostgreSQL の BIGSERIAL に対応）
- 型は **`long` ではなく `Long`**

#### なぜ `Long`（ラッパー型）なのか？（超重要）

- 新規作成時は id がまだ無い（**null**）状態
- `long`（基本型）は null を入れられず **自動で 0** になる
- すると JPA が「id=0 のレコード」と勘違いし、INSERTのはずがUPDATEになる事故が起きる

👉 **主キーは必ずラッパー型（`Long`）**。

### ブロック③ カラム定義

```java
@Column(name = "name", nullable = false, length = 100)
private String name;       // 表示名（React版の displayName）

@Column(name = "email", nullable = false, unique = true, length = 255)
private String email;      // ログインID

@Column(name = "password", nullable = false, length = 255)
private String password;   // ★BCryptハッシュ済み文字列を入れる（生は入れない）★
```

| 属性        | 意味                         |
| ----------- | ---------------------------- |
| `nullable=false` | NOT NULL                |
| `unique=true`    | UNIQUE（emailは重複NG）  |
| `length=N`       | VARCHAR(N)               |

> 💡 `password` の `length=255`：BCryptハッシュは約60文字だが、余裕を持って255にしておく。
> 　 **このカラムに生パスワードを入れてはいけない**（ハッシュ化は第6章の Service の仕事）。

### ブロック④ 日時の自動セット

```java
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;

@PrePersist
protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
}

@PreUpdate
protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
}
```

| アノテーション | 呼ばれるタイミング | やること                     |
| -------------- | ------------------ | ---------------------------- |
| `@PrePersist`  | INSERT直前         | created_at と updated_at をセット |
| `@PreUpdate`   | UPDATE直前         | updated_at だけ上書き        |

- `created_at` に `updatable = false` を付けると、後からの UPDATE で書き換わらない（安全）
- これがあるので **Service で `setCreatedAt(...)` を書かなくて済む**

---

## 🧰 1-6. Lombok 3点セット

```java
@Getter
@Setter
@NoArgsConstructor
public class User { ... }
```

| アノテーション       | 自動生成されるもの            |
| -------------------- | ----------------------------- |
| `@Getter`            | 全フィールドの `getXxx()`     |
| `@Setter`            | 全フィールドの `setXxx()`     |
| `@NoArgsConstructor` | `public User() {}`（引数なし）|

### なぜ `@NoArgsConstructor` が **必須** なのか？

> JPA は **リフレクションで Entity を生成** するため、**引数なしコンストラクタが絶対に必要**。

無いと起動時に `No default constructor for entity ...` というエラーになります。
👉 **Entity を作ったら反射的に `@NoArgsConstructor`** と覚える。

---

## ❌ 1-7. 初心者がやりがちなNG

| NG                              | 何が起きる？                       |
| ------------------------------- | ---------------------------------- |
| `@Entity` を忘れる              | Springが認識せず Repository が動かない |
| 主キーを `long` にする          | INSERTのはずがUPDATEになる事故      |
| `@NoArgsConstructor` 付け忘れ   | 起動時エラー                       |
| `password` に生パスワードを入れる | **重大なセキュリティ事故**         |
| Entity に業務ロジックを書く     | テストできない／責務がぼやける     |
| テーブル名を `user`（予約語）にする | DBによっては起動失敗              |

---

## 📝 1-8. ソースを書くときの順番（実演）

1. **TransactionType（enum）** を書く（支出/収入の2値に固定）
2. **User の空クラス** を書く → `public class User {}`
3. **「これはテーブル」** と宣言 → `@Entity` / `@Table(name = "users")`
4. **主キー** を書く → `id` ＋ `@Id` ＋ `@GeneratedValue`
5. **業務カラム** → `name` / `email` / `password`
6. **共通カラム** → `createdAt` / `updatedAt`
7. **日時の自動セット** → `@PrePersist` / `@PreUpdate`
8. **Lombok 3点セット** を付ける

---

## ✨ 1-9. ソースを書くときのポイント

- **決まった選択肢の項目は enum に**（TransactionType）
- **制約（NOT NULL / UNIQUE / 桁数）は Entity の `@Column` に書く**。DB任せにしない
- **主キーは `Long` 型 ＋ `IDENTITY` 戦略**
- **`@NoArgsConstructor` は反射的に付ける**
- **`created_at` / `updated_at` は `@PrePersist`/`@PreUpdate` で自動化**
- **Entity に業務ロジックを書かない**（ハッシュ化・判断は Service）

---

## ✅ 1-10. 第1章まとめ

✔ Entity ＝ テーブル1行を表す Java クラス（形だけ）
✔ **依存される側（型・User）から先に作る**
✔ enum は **取りうる値を固定** して型安全にする道具
✔ 主キーは **`Long` ＋ `IDENTITY`**、`@NoArgsConstructor` は必須
✔ 日時は **`@PrePersist`/`@PreUpdate`** で自動化
✔ password カラムには **ハッシュ済み文字列だけ** を入れる（生は入れない）

---

## 🔜 次の章

**第2章：Entity を作る②（Category）**
― 外部キー（`user_id`）と列挙型カラムをどう表現するか。`@ManyToOne` が初登場 ―
