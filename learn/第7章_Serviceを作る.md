# 📘 第7章

## Service を作る
― 業務ロジックの中心。**パスワードのハッシュ化**・集計・詰め替えがつながる ―

---

## 🎯 7-1. この章のゴール

✔ Service の役割を **一文で説明できる**
✔ なぜ Controller が直接 Repository を呼ばないか説明できる
✔ **`@Transactional(readOnly = true)`** をクラスに付け、書き込みで上書きする理由が分かる
✔ **コンストラクタインジェクション**（`@RequiredArgsConstructor`）が好まれる理由を3つ挙げられる
✔ ★`PasswordEncoder.encode(...)` で **パスワードをハッシュ化** する★
✔ `UserDetailsService.loadUserByUsername(...)` の意味を説明できる
✔ 「他人のデータを触らせない」**持ち主チェック** が書ける
✔ ダッシュボードの **集計ロジック** を Service に置く理由が分かる

---

## 🧩 7-2. Service とは何か

> Service ＝ **業務のルール・流れ・判断** を担当するクラス

```text
[ Controller ] → [ Service ] ← ★ここ → [ Repository ] → [ Entity ] → [ DB ]
```

### Service が **やる** こと
- ✔ 業務ルールの判定（重複していないか／削除できるか／持ち主か）
- ✔ Form → Entity の **詰め替え**（`Long categoryId` → `Category`）
- ✔ **パスワードのハッシュ化**
- ✔ 集計（合計・内訳・推移）
- ✔ トランザクション境界（`@Transactional`）

### Service が **やらない** こと
- ❌ HTTPを直接さわる（→ Controller）
- ❌ SQLを書く（→ Repository）
- ❌ HTMLを組み立てる（→ Thymeleaf）

👉 **「判断するのが Service、動かすのが Repository」**。

---

## 🔄 7-3. なぜ Form・DTO の次に Service なのか

```text
[ Service ]
   ├── Repository      ← 第4章
   ├── Form            ← 第5章（入力箱：受け取る）
   ├── Entity          ← 第1〜3章
   ├── DTO             ← 第6章（出力箱：集計結果を返す）
   └── PasswordEncoder ← SecurityConfig の @Bean
```

下流が全部揃ってから書くと import エラー無くスラスラ書ける。
Service は Form（入力箱）を受け取り、集計結果を DTO（出力箱）に詰めて返す。だから **両方の箱**（第5章・第6章）を先に用意しておく。

### この章で作るもの・書く順番

| 順 | 作るもの                  | 学ぶこと                                   |
| -- | ------------------------- | ------------------------------------------ |
| 1  | `ResourceNotFoundException` | カスタム例外（@ResponseStatus）           |
| 2  | `SecurityConfig`(一部)    | `PasswordEncoder` Bean を用意              |
| 3  | `UserService`             | **ハッシュ化** ＋ UserDetailsService ＋ 初期カテゴリー |
| 4  | `CategoryService`         | 持ち主チェック ＋ 削除前チェック           |
| 5  | `TransactionService`      | 詰め替え ＋ **集計（ダッシュボード）**     |

> SecurityConfig の認可設定は第8章で本格化。ここでは `PasswordEncoder` だけ先に用意する。

---

## 🧱 7-4. ステップ① ResourceNotFoundException

完成ファイル：[ResourceNotFoundException.java](../src/main/java/com/example/expensetracker/exception/ResourceNotFoundException.java)

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
```

- `@ResponseStatus(NOT_FOUND)` … この例外がControllerの外まで届くと **自動でHTTP 404**
- `RuntimeException` を継承 … チェック例外だと `throws` が伝染して汚れるため

---

## 🧱 7-5. ステップ② PasswordEncoder Bean

完成ファイル：[SecurityConfig.java](../src/main/java/com/example/expensetracker/config/SecurityConfig.java)

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### なぜここで用意するのか
- `UserService` でパスワードをハッシュ化したい → `PasswordEncoder` が必要
- DIで注入するには **Bean登録** が要る → `@Bean` で1つ用意する

### なぜ `BCrypt` なのか
- ハッシュ関数の **業界標準**
- 1ハッシュごとに **salt（ランダムな塩）** が自動で混ざる（同じパスワードでも毎回違うハッシュ）
- 計算コストが高め＝総当たり攻撃に強い

---

## 🔐 7-6. ステップ③ UserService ― ★ハッシュ化の本番★

完成ファイル：[UserService.java](../src/main/java/com/example/expensetracker/service/UserService.java)

### 三種の神器（全Service共通）

```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
}
```

| アノテーション                    | 役割                                       |
| --------------------------------- | ------------------------------------------ |
| `@Service`                        | Serviceの Bean として登録                  |
| `@Transactional(readOnly = true)` | クラス既定を「読み取り専用」に             |
| `@RequiredArgsConstructor`        | `final` フィールドのコンストラクタを自動生成 |

#### なぜ `@Autowired` よりコンストラクタインジェクション？

| 観点                       | フィールド `@Autowired` | コンストラクタ（推奨） |
| -------------------------- | ----------------------- | ---------------------- |
| `final` にできる           | ❌                      | ⭕                    |
| テストでMockを渡しやすい   | ❌                      | ⭕                    |
| 循環参照に早く気づく       | ❌（起動後）            | ⭕（起動時）          |

### ★最重要★ register でのハッシュ化

```java
@Transactional
public User register(UserRegisterForm form) {
    if (userRepository.existsByEmail(form.getEmail())) {                 // ①重複チェック
        throw new IllegalArgumentException("このメールアドレスは既に登録されています");
    }
    User user = new User();
    user.setName(form.getName());
    user.setEmail(form.getEmail());
    user.setPassword(passwordEncoder.encode(form.getPassword()));        // ②★ハッシュ化★
    User saved = userRepository.save(user);                             // ③保存
    createDefaultCategories(saved);                                     // ④初期カテゴリー
    return saved;
}
```

- **`passwordEncoder.encode(form.getPassword())`** が今回の核心
  - 画面から来た **生パスワード** を BCrypt ハッシュに変換してから保存する
  - DBには `$2a$10$....`（約60文字のハッシュ）が入り、**生パスワードはどこにも残らない**
- React 版は生のパスワードを Firestore に保存していた（学習用の簡略実装）
  → Spring Boot 版では **必ずハッシュ化** して保存する。これがこの研修の最重要ポイント。

> ⚠️ 絶対NG：`user.setPassword(form.getPassword());`（生のまま保存）
> 　 これは重大なセキュリティ事故。**必ず `encode(...)` を通す**。

### 初期カテゴリーの自動作成

```java
private void createDefaultCategories(User user) {
    createCategory(user, TransactionType.EXPENSE, "食費", "#f87171");
    // … 支出8種・収入4種を登録 …
}
```

- React 版の `DEFAULT_CATEGORIES` を再現
- 登録直後のユーザーが **すぐ記録を始められる** よう、初期カテゴリーを用意する
- ここで `UserRepository` と `CategoryRepository` の **2つを組み合わせて使う** のも Service の役目

### ログイン連携：`loadUserByUsername`

```java
@Override
public UserDetails loadUserByUsername(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + email));
    return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())   // ★ハッシュ済みをそのまま渡す★
            .authorities("ROLE_USER")
            .build();
}
```

- Spring Security が「ログインが来た。この email は誰？」と聞いてくる窓口
- **我々の `User`（Entity）** と **Spring Security の `User`** は別物 → Security 側は **フルパス** で書いて衝突回避
- `.password(...)` には **ハッシュ済み** を渡す
  - 入力された生パスワードと照合（`matches`）するのは **Security の内部処理**
  - 我々は「ハッシュをそのまま渡すだけ」。**生に戻す処理は書かない**（そもそも数学的に不可能）

---

## 🧱 7-7. ステップ④ CategoryService ― 持ち主チェック

完成ファイル：[CategoryService.java](../src/main/java/com/example/expensetracker/service/CategoryService.java)

### ★セキュリティ★ findOwnedById（他人のデータを守る）

```java
public Category findOwnedById(User user, Long id) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("カテゴリーが見つかりません: id=" + id));
    if (!category.getUser().getId().equals(user.getId())) {     // ★持ち主チェック★
        throw new ResourceNotFoundException("カテゴリーが見つかりません: id=" + id);
    }
    return category;
}
```

- ログイン中ユーザーのものでなければ「存在しない」と同じ扱いにする
- これが無いと、URLの id を書き換えて **他人のカテゴリーを編集/削除** できてしまう
- 「見つからない」と返すのは、存在の有無すら漏らさないため

### 削除前チェック（判断はService）

```java
@Transactional
public void delete(User user, Long id) {
    Category category = findOwnedById(user, id);
    long usageCount = transactionRepository.countByCategory(category);   // 数えるのはRepository
    if (usageCount > 0) {                                                // 判断はService
        throw new IllegalStateException(
            "このカテゴリーは " + usageCount + " 件の記録で使われているため削除できません");
    }
    categoryRepository.delete(category);
}
```

- 「使われていたら消させない」は **業務ルール → Service**
- 「数える」「消す」は **Repository**

### 例外の使い分け

| 例外                        | いつ使う？                       |
| --------------------------- | -------------------------------- |
| `IllegalArgumentException`  | 引数が不正（重複など）           |
| `IllegalStateException`     | 状態が不正（使用中で消せない）   |
| `ResourceNotFoundException` | 対象が無い／他人のもの（→ 404）  |

---

## 🧱 7-8. ステップ⑤ TransactionService ― 詰め替え＋集計

完成ファイル：[TransactionService.java](../src/main/java/com/example/expensetracker/service/TransactionService.java)

### ★最重要★ Long categoryId → Category への詰め替え

```java
@Transactional
public Transaction register(User user, TransactionForm form) {
    Category category = categoryService.findOwnedById(user, form.getCategoryId());  // Long→Category
    if (category.getType() != form.getType()) {                 // 種類の整合チェック
        throw new IllegalArgumentException("カテゴリーの種類が記録の種類と一致しません");
    }
    Transaction tx = new Transaction();
    tx.setUser(user);                       // ★持ち主は必ずログインユーザー（Formを信用しない）★
    tx.setType(form.getType());
    tx.setTransactionDate(form.getTransactionDate());
    tx.setCategory(category);               // Long→Category 変換の結果をセット
    tx.setAmount(form.getAmount());
    tx.setMemo(form.getMemo());
    return transactionRepository.save(tx);
}
```

- Form が持つ `categoryId(Long)` を、Service が `Category` Entity に変換する（第5章で予告した本番）
- 同時に「自分のカテゴリーか？」も `findOwnedById` がチェック
- 持ち主は **必ずログイン中ユーザー** にする（Form から来た値は信用しない）

### 集計ロジックを Service に置く理由

React 版は `useMemo` で画面側が集計していました。Spring Boot 版では **集計も業務ロジック → Service** に置きます。

```java
public MonthlySummary summarize(User user, YearMonth month) {
    List<Transaction> list = findByMonth(user, month);
    int income = 0, expense = 0;
    for (Transaction tx : list) {
        if (tx.getType() == TransactionType.INCOME)  income  += tx.getAmount();
        else if (tx.getType() == TransactionType.EXPENSE) expense += tx.getAmount();
    }
    return new MonthlySummary(income, expense, income - expense);
}
```

- 月の合計（`summarize`）、支出内訳（`expenseBreakdown`）、6ヶ月推移（`recentTrend`）を計算
- 結果は **DTO**（[MonthlySummary](../src/main/java/com/example/expensetracker/dto/MonthlySummary.java) など）に詰めて Controller へ返す
- DTO は Entity ではない＝「集計結果を運ぶだけの箱」。画面に必要な形に整える（**DTO の詳しい役割・作り方は第6章**）

#### 月の範囲検索の使い方

```java
public List<Transaction> findByMonth(User user, YearMonth month) {
    LocalDate start = month.atDay(1);          // 月初
    LocalDate end   = month.atEndOfMonth();    // 月末
    return transactionRepository
        .findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(user, start, end);
}
```

- `YearMonth` を使うと「月初」「月末」が簡単に出せる
- 第4章で作った `Between` 検索に月初・月末を渡すだけで「その月」が取れる

---

## 🌀 7-9. `@Transactional` の張り方

```java
@Service
@Transactional(readOnly = true)   // クラス既定＝読み取り専用
public class XxxService {
    public Xxx find(...) { ... }   // 読み取り専用がそのまま効く

    @Transactional                  // 書き込み系で上書き
    public Xxx register(...) { ... }
}
```

- `readOnly = true` を既定にしておくと、参照系で誤って書き込むとエラーで気づける＋最適化が効く
- **書き込み系（register/update/delete）には必ず `@Transactional` を明示** して上書きする

### ハマりどころ：内部呼び出しでは効かない

```java
public void a() { this.b(); }   // ← this 経由だと @Transactional が効かない
@Transactional public void b() { ... }
```

`@Transactional` は **プロキシ機構** で効くため、同クラス内の `this.xxx()` 呼び出しでは効かない。

---

## ❌ 7-10. 初心者がやりがちなNG

| NG                                          | 何が起きる？               |
| ------------------------------------------- | -------------------------- |
| Controller から Repository を直接呼ぶ       | Serviceが空洞化            |
| **生パスワードを setPassword で保存**       | **重大なセキュリティ事故** |
| 持ち主チェックを書かない                    | 他人のデータを操作できる   |
| 書き込み系に `@Transactional` を付け忘れる  | INSERT/UPDATEが動かないことも |
| `@Autowired` フィールドを新たに書く         | 旧スタイル（コンストラクタ推奨） |
| 集計を Controller でやる                    | テストしづらい／層が崩れる |

---

## 📝 7-11. ソースを書くときの順番

1. `ResourceNotFoundException`（後でServiceから投げる）
2. `SecurityConfig` に `PasswordEncoder` Bean
3. `UserService`（ハッシュ化＋UserDetailsService＋初期カテゴリー）
4. `CategoryService`（持ち主チェック＋削除前チェック）
5. `TransactionService`（詰め替え＋集計）

各Service内の共通テンプレ：
1. 三種の神器（`@Service`/`@Transactional(readOnly=true)`/`@RequiredArgsConstructor`）
2. `private final` で依存を宣言
3. 参照系メソッド
4. 書き込み系メソッド（`@Transactional` を忘れず）

---

## ✨ 7-12. ソースを書くときのポイント

- **三種の神器をセットで覚える**
- 依存は **`private final` ＋ コンストラクタインジェクション**
- ★**パスワードは `passwordEncoder.encode(...)` でハッシュ化**。生は保存しない★
- **持ち主チェック**（`findOwnedById`）で他人のデータを守る
- **Form→Entity 詰め替え（Long→Category）は Service の中心仕事**
- **集計は Service ＋ DTO**
- 書き込み系には **`@Transactional` を明示**

---

## ✅ 7-13. 第7章まとめ

✔ Service ＝ 業務の流れ・判断（ハッシュ化・集計・詰め替え・持ち主チェック）
✔ ★パスワードは **BCryptでハッシュ化**して保存。`loadUserByUsername` ではハッシュをそのまま渡す★
✔ 持ち主チェックで他人のデータを守る
✔ `Long categoryId` → `Category` の変換は Service の仕事
✔ ダッシュボードの集計は Service ＋ DTO
✔ 三種の神器 ＋ コンストラクタインジェクション ＋ 書き込み系に `@Transactional`

---

## 🔜 次の章

**第8章（前半）：Controller を作る**
― HTTPの入口。`@AuthenticationPrincipal` でログイン中ユーザーを安全に取得する ―
