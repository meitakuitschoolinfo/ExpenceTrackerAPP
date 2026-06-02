# 📘 第4章

## Repository を作る
― DBと会話する「専門窓口」。SQLを書かずに検索メソッドを生やす ―

---

## 🎯 4-1. この章のゴール

✔ Repository の役割を **一文で説明できる**
✔ `JpaRepository<Entity, ID>` の **2つの型引数** を分解して説明できる
✔ `findByEmail` のような **メソッド名規約** がなぜ動くか説明できる
✔ `Optional<User>` を **null チェックの代わり** に使える
✔ `findByUserAndTransactionDateBetween...` のような **複合条件＋並び替え** が書ける
✔ Repository に **書いてはいけないコード** が分かる

---

## 🧩 4-2. Repository の役割

> Repository ＝ **データベースと直接会話するための「専門窓口」**

```text
[ Service ] → [ Repository ] ← ★この章。DBとのやり取りだけ → [ DB ]
```

👉 **Repository は「DB操作以外をしてはいけない」層**。判断（if/for/業務ルール）は書かない。

---

## 🔄 4-3. なぜ Entity を全部作った後、まとめて作るのか

```java
public interface CategoryRepository extends JpaRepository<Category, Long> { ... }
//                                                        ↑ Entityがここで登場
```

- Repository は **必ず Entity に依存** する
- → Entity が全部揃ってから書くと、`repository/` フォルダ内で **連続して書ける**

### この章で書く順番（簡単 → 複雑）

| 順 | Repository              | 学ぶこと                                 |
| -- | ----------------------- | ---------------------------------------- |
| 1  | `UserRepository`        | メソッド名規約（`findByEmail`）＋ `Optional` |
| 2  | `CategoryRepository`    | 複数条件（`findByUserAndType...`）        |
| 3  | `TransactionRepository` | 範囲検索＋並び替え（`Between` ＋ `OrderBy`）|

---

## 🧠 4-4. 共通の型構造

```java
public interface XxxRepository extends JpaRepository<Entity, ID> { }
```

| 部分                     | 意味                            |
| ------------------------ | ------------------------------- |
| `interface`              | **クラスではなく** インターフェース |
| `extends JpaRepository`  | 標準CRUDを全部引き継ぐ          |
| 第1型引数 `Entity`       | 扱うEntity                      |
| 第2型引数 `ID`           | そのEntityの主キー(@Id)の型     |

このアプリでは全部 `Long`（`id BIGSERIAL`）なので：

| Repository              | 書き方                                          |
| ----------------------- | ----------------------------------------------- |
| `UserRepository`        | `extends JpaRepository<User, Long>`             |
| `CategoryRepository`    | `extends JpaRepository<Category, Long>`         |
| `TransactionRepository` | `extends JpaRepository<Transaction, Long>`      |

`extends` 1行だけで `save` / `findById` / `findAll` / `deleteById` / `count` が **SQLを書かずに** 使えます。

---

## 🧱 4-5. ステップ① UserRepository（メソッド名規約）

完成ファイル：[UserRepository.java](../src/main/java/com/example/expensetracker/repository/UserRepository.java)

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### `findByEmail` の分解（メソッド名からSQLが作られる）

| 部分    | 意味                          |
| ------- | ----------------------------- |
| `find`  | 検索（SELECT）                |
| `By`    | 条件指定                      |
| `Email` | **Entityのフィールド名** `email` |

→ 自動生成SQL：`SELECT * FROM users WHERE email = ?`

> 📌 基準は **DBカラム名ではなく Entityのフィールド名**。今回は偶然同じだが、
> 　 `transactionDate`（DB: `transaction_date`）のような場合は **Java側の名前** を使う。

### `Optional<User>` を返す理由

```java
Optional<User> findByEmail(String email);
```

- 「該当 email のユーザーが居ない」可能性がある
- `User` を直接返すと、呼び出し側で **null チェック忘れ** が起きる
- `Optional` なら「中身を取り出す処理」をコンパイラが促す

```java
// Service での使い方（第6章）
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new UsernameNotFoundException("..."));
```

### `existsByEmail` の使い分け

| やりたいこと          | 使うメソッド    |
| --------------------- | --------------- |
| ユーザーの**情報**が欲しい | `findByEmail`   |
| **存在の有無**だけ知りたい | `existsByEmail` |

存在チェックに `findByEmail` を使うと不要な列まで取るので非効率。**目的でメソッドを選ぶ**。

---

## 🧱 4-6. ステップ② CategoryRepository（複数条件）

完成ファイル：[CategoryRepository.java](../src/main/java/com/example/expensetracker/repository/CategoryRepository.java)

```java
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserOrderByIdAsc(User user);
    List<Category> findByUserAndTypeOrderByIdAsc(User user, TransactionType type);
    boolean existsByUserAndTypeAndLabel(User user, TransactionType type, String label);
}
```

### `findByUserAndTypeOrderByIdAsc` の分解

| 部分          | 意味                         |
| ------------- | ---------------------------- |
| `findBy`      | 検索                         |
| `User`        | 条件①：user で絞る           |
| `And`         | 条件をAND結合                |
| `Type`        | 条件②：type で絞る           |
| `OrderById`   | id で並び替え                |
| `Asc`         | 昇順                         |

→ `SELECT * FROM categories WHERE user_id = ? AND type = ? ORDER BY id ASC`

- 引数 `User user` を渡すと、JPAが内部で `user.getId()` を取り出して `user_id = ?` にしてくれる
- 記録画面で「支出カテゴリーだけ／収入カテゴリーだけ」を出すのに使う

### `existsByUserAndTypeAndLabel`

- 「同じユーザー内に、同じ種類・同じ名前のカテゴリーが既にあるか？」の重複チェック専用
- 判定（登録させる/させない）は **Service の仕事**。Repository は「あるか無いか」を返すだけ

---

## 🧱 4-7. ステップ③ TransactionRepository（範囲検索＋並び替え）

完成ファイル：[TransactionRepository.java](../src/main/java/com/example/expensetracker/repository/TransactionRepository.java)

```java
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(
            User user, LocalDate start, LocalDate end);
    long countByCategory(Category category);
}
```

### 長いメソッド名の分解

| 部分                          | 意味                                  |
| ----------------------------- | ------------------------------------- |
| `findByUser`                  | user で絞る                           |
| `And`                         | AND結合                               |
| `TransactionDateBetween`      | transaction_date が start〜end の範囲 |
| `OrderByTransactionDateDesc`  | 取引日の降順で並べる                  |
| `IdDesc`                      | （同日なら）id の降順で並べる         |

→ ```sql
SELECT * FROM transactions
WHERE user_id = ? AND transaction_date BETWEEN ? AND ?
ORDER BY transaction_date DESC, id DESC
```

- `Between` は **両端を含む** → 月初(1日)と月末を渡せば「その月だけ」が取れる
- この1メソッドで「今月の履歴」「今月の集計」「6ヶ月推移」すべてをまかなえる（呼ぶ期間を変えるだけ）

> 💡 メソッド名が長すぎる…と感じたら、`@Query` で自分でJPQLを書く方法もある（発展）。
> 　 研修ではまず「名前規約だけでここまで書ける」ことを体感する。

### `countByCategory`

- 「このカテゴリーは何件の記録で使われているか？」を数える
- カテゴリー削除前に「使われていたら消させない」判定（**判定は Service**）で使う

---

## ⚠️ 4-8. Repository でやってはいけないこと

| ❌ NG                                  | なぜダメ？                      |
| -------------------------------------- | ------------------------------- |
| `if`/`for` で業務分岐を書く            | 業務判定は Service の仕事        |
| 「使われてなければ消す」等のルールを書く | ビジネスルールは Service へ      |
| 1つの Repository で複数 Entity を扱う  | **1 Entity = 1 Repository** が原則 |

---

## 📝 4-9. ソースを書くときの順番

1. **UserRepository**：`extends` の最小形 → `findByEmail`（Optional）→ `existsByEmail`
2. **CategoryRepository**：`findByUserOrderByIdAsc` → 複数条件 `findByUserAndType...` → 重複チェック
3. **TransactionRepository**：範囲検索＋並び替え → `countByCategory`

👉 **「最小形 → 名前規約 → 複合条件」** の階段で進む。

---

## ✨ 4-10. ソースを書くときのポイント

- **`interface` であって `class` ではない**（実装はSpringが自動生成）
- **型引数は `<Entity, 主キーの型>`**
- **「無いかも」な単件取得は `Optional`**（null を返さない）
- **メソッド名の単語は Entity の「フィールド名」と一致** させる（DBカラム名ではない）
- **Repository に if/for/業務判定を書かない**
- **1 Entity ＝ 1 Repository**

---

## ✅ 4-11. 第4章まとめ

✔ Repository は **DB操作専用** の窓口
✔ 型引数は `<Entity, 主キー型>`、`extends` だけで基本CRUDが使える
✔ メソッド名規約：`find / exists / count + By + フィールド名 (+ And + OrderBy...)`
✔ 単件取得は `Optional`、範囲は `Between`、並び替えは `OrderBy...Desc`
✔ 判断は Service、入出力は Repository

---

## 🔜 次の章

**第5章：Form を作る**
― 画面の入力を受け取る箱と入力チェック。**Entity と Form を分ける理由** の本番 ―
