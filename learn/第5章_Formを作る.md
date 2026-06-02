# 📘 第5章

## Form を作る
― 画面とサーバーをつなぐ「入力の箱」。Entity と Form を分ける理由 ―

---

## 🎯 5-1. この章のゴール

✔ Form クラスの役割を **一文で説明できる**
✔ **「Entity じゃダメな理由」** を3つ以上挙げられる
✔ `@NotBlank` / `@NotNull` / `@Size` / `@Email` / `@Min` の **使い分け** ができる
✔ Form の `categoryId: Long` と Entity の `category: Category` の **違い** を説明できる
✔ `@DateTimeFormat` で `<input type="date">` を `LocalDate` に変換できる

---

## 🧩 5-2. Form とは何か

> Form ＝ **「画面の入力1件分」を Java のクラスとして表したもの**

```text
[ ブラウザ ] ─HTMLフォーム─▶ [ Controller ] ─▶ [ Form ] ─Serviceが詰め替え─▶ [ Entity ] ─▶ [ DB ]
```

👉 **Form は「画面側の都合」、Entity は「DB側の都合」**。形は似ていても役割が違う。

---

## 🔄 5-3. なぜ Repository の次に Form なのか

```text
[ Service ]（第6章）
   ├── Repository  ← 第4章で完成
   └── Form        ← ★第5章でここ
```

Service は Repository と Form の **両方** を使う。だから Service より先に両者を揃える。

### この章で書く順番

| 順 | Form               | 学ぶこと                                      |
| -- | ------------------ | --------------------------------------------- |
| 1  | `LoginForm`        | 最小形（Security の入口）                     |
| 2  | `UserRegisterForm` | `@Email` ＋ パスワードの業務ルール（6文字以上）|
| 3  | `CategoryForm`     | 登録/編集の兼用（id持ち）＋ enum を `@NotNull` |
| 4  | `TransactionForm`  | `Long categoryId` ＋ `@DateTimeFormat` ＋ `@Min` |

---

## 🆚 5-4. ★最重要★ Form と Entity を混ぜない理由

| ＃ | Entity を画面で使い回すと…                       | 起きること                       |
| -- | ------------------------------------------------ | -------------------------------- |
| ①  | Entity にバリデーションを書き散らかす            | DB制約と画面制約が混ざって読めない |
| ②  | 書き換えたくない `id` / `createdAt` まで晒す     | 改ざんされる                     |
| ③  | **生パスワード ≠ 保存値(ハッシュ)** に対応できない | 生パスワードがEntityに直入りして事故 |
| ④  | リレーション `Category category` を画面で扱うのは難しい | 画面は `categoryId: Long` が圧倒的に楽 |

### 三段構えで守る

```text
[ Form ]   … @NotBlank / @Size / @Email / @Min（画面寄りのチェック）
[ Entity ] … @Column(nullable=false, length=...)（DB寄りの制約）
[ DB ]     … NOT NULL / VARCHAR(N) / UNIQUE（最後の砦）
```

どこかが抜けても他の層が守る。

---

## 🏷 5-5. バリデーション早見表

| アノテーション   | 何を弾く？                          | 対象型       |
| ---------------- | ----------------------------------- | ------------ |
| `@NotNull`       | null だけ                           | 数値・参照型・enum |
| `@NotBlank`      | null ＋ 空文字 ＋ 空白だけ          | **String専用** |
| `@Size(min,max)` | 文字数                              | String       |
| `@Email`         | メアド形式                          | String       |
| `@Min`           | 数値の最小                          | 数値         |

### 一番ハマる使い分け

```text
@NotNull   ── null だけNG（参照型・enum・数値はこれ）
@NotBlank  ── null も "" も "   " もNG（文字列はこれ）
```

👉 **String には `@NotBlank`、数値・enum・参照型には `@NotNull`**。混ぜると動かない。

---

## 🧱 5-6. ステップ① LoginForm（最小形）

完成ファイル：[LoginForm.java](../src/main/java/com/example/expensetracker/form/LoginForm.java)

```java
public class LoginForm {
    @NotBlank @Email private String email;
    @NotBlank         private String password;
}
```

- ログインは「空欄ガード」程度で十分。**実際の認証は Spring Security の仕事**（第7章）
- POST /login は Security が直接受けるので、この Form は主に **GET時の画面バインド** 用

---

## 🧱 5-7. ステップ② UserRegisterForm（@Email と業務ルール）

完成ファイル：[UserRegisterForm.java](../src/main/java/com/example/expensetracker/form/UserRegisterForm.java)

```java
public class UserRegisterForm {
    @NotBlank @Size(max = 100)          private String name;
    @NotBlank @Email @Size(max = 255)   private String email;
    @NotBlank @Size(min = 6, max = 255) private String password;
}
```

### パスワードの `@Size(min = 6)` は **業務ルール**

- DB制約には「6文字以上」は無い（`VARCHAR(255)` だけ）
- → これは **画面側の品質ルール** なので Form に書く（Entityには書かない）
- React 版の「パスワードは6文字以上」をそのまま再現

### 重複チェックはここでやらない

- `@Email` などは **1フィールド単独で完結するチェック** が得意
- 「DBに同じemailがあるか？」は DB問い合わせが必要 → **Service の仕事**（第6章）

---

## 🧱 5-8. ステップ③ CategoryForm（登録/編集 兼用 ＋ enum）

完成ファイル：[CategoryForm.java](../src/main/java/com/example/expensetracker/form/CategoryForm.java)

```java
public class CategoryForm {
    private Long id;                          // 編集時のみ値が入る（新規は null）
    @NotNull             private TransactionType type;   // enum → @NotNull
    @NotBlank @Size(max=100) private String label;
    @NotBlank @Size(max=20)  private String color;
}
```

### 1クラスで登録/編集を兼ねる

- 新規 → `id == null`、編集 → `id != null`
- Controller 側で id の有無を見て分岐する（第7章）
- フィールドが少なく、登録と編集でチェックが同じなので **1クラスでOK**

### `type` は enum なので `@NotBlank` ではなく `@NotNull`

- 文字列ではないので `@NotBlank` は付けられない
- 「未選択(null)」を弾くのは `@NotNull`

---

## 🧱 5-9. ステップ④ TransactionForm（categoryId と日付変換）

完成ファイル：[TransactionForm.java](../src/main/java/com/example/expensetracker/form/TransactionForm.java)

```java
public class TransactionForm {
    @NotNull                          private TransactionType type;
    @NotNull @DateTimeFormat(iso = ISO.DATE) private LocalDate transactionDate;
    @NotNull                          private Long categoryId;   // ★Category ではなく Long★
    @NotNull @Min(1)                  private Integer amount;
    @Size(max = 255)                  private String memo;       // 任意 → @NotNull なし
}
```

### ★最重要★ なぜ `Long categoryId` で `Category category` ではないのか

| 観点                    | `Long categoryId`（Form）        | `Category category`（Entity） |
| ----------------------- | -------------------------------- | ----------------------------- |
| 画面の `<select value>` | そのまま使える（数値ID）         | できない                      |
| バリデーション          | `@NotNull` で完結                | 複雑                          |
| Service側で詰め替え     | `categoryRepository.findById()`  | 不要                          |

👉 **Form は ID（Long）だけ持つ。Service が Repository を使って Category に変換する**（第6章）。
　 これが Spring Boot の鉄板パターン。

### `@DateTimeFormat(iso = ISO.DATE)`

- `<input type="date">` は `"2026-06-01"` という文字列を送ってくる
- これを `LocalDate` に変換するために付ける
- 付け忘れると日付がうまくバインドされないことがある

### `amount` の `@Min(1)`

- 0円や負数を弾く（React 版の `min="1"` に相当）
- 数値なので必須チェックは `@NotNull`（`@NotBlank` ではない）

### `memo` に `@NotNull` を付けない

- 任意項目なので、入力があったときだけ `@Size` で長さチェック

---

## ❌ 5-10. 初心者がやりがちなNG

| NG                                          | 何が起きる？                       |
| ------------------------------------------- | ---------------------------------- |
| Entity を画面の `th:object` にする          | id/createdAt が改ざんされる        |
| `@NotBlank` を enum/数値に付ける            | 動かない                           |
| 任意の `memo` に `@NotBlank`                | 空で送ると毎回エラー               |
| Form の `categoryId` を `Category` にする   | 画面で扱いづらい／バインド複雑     |
| パスワードの「6文字以上」を Entity に書く   | DBの責務を超える（業務ルールはForm） |
| `<input type=date>` に `@DateTimeFormat` 無し | 日付がバインドされない           |

---

## 📝 5-11. ソースを書くときの順番

1. **LoginForm**（最小形）
2. **UserRegisterForm**（`@Email` ＋ パスワード6文字以上）
3. **CategoryForm**（id持ちの兼用 ＋ enum を `@NotNull`）
4. **TransactionForm**（`Long categoryId` ＋ `@DateTimeFormat` ＋ `@Min`）

---

## ✨ 5-12. ソースを書くときのポイント

- **Form と Entity は別物**。画面の都合とDBの都合を混ぜない
- **Form は ID（Long）で関連を持つ**。Entity（Category）は持たない
- **String は `@NotBlank`、enum/数値/参照型は `@NotNull`**
- **業務ルール（6文字以上）は Form/Service へ、DB制約は Entity/DB へ**
- **日付入力には `@DateTimeFormat`**
- Form も Lombok **`@Getter`/`@Setter`/`@NoArgsConstructor`** が必須（Springがリフレクションで詰める）

---

## ✅ 5-13. 第5章まとめ

✔ Form ＝「画面の入力1件」を表すクラス（Entityと役割が違う）
✔ Form は **`Long categoryId`** で関連を持ち、Service が Category に変換
✔ String→`@NotBlank` / enum・数値→`@NotNull` / 任意→チェック控えめ
✔ 日付入力は `@DateTimeFormat(iso = ISO.DATE)`
✔ 重複チェックは Form ではなく Service の仕事

---

## 🔜 次の章

**第6章：Service を作る**
― 業務ロジックの中心。**パスワードのハッシュ化**・集計・詰め替えが全部つながる ―
