# 📘 第8章（後半）

## テンプレートと SecurityConfig
― Thymeleaf で画面を作り、認可・ログイン・CSRF を設定して**動くアプリ**にする ―

---

## 🎯 8-2-1. この章のゴール

✔ Thymeleaf の **`th:each` / `th:if` / `th:field` / `th:replace`** が読める／書ける
✔ 共通部品（head・フラッシュ・下部ナビ）を **フラグメント** にまとめられる
✔ `RedirectAttributes` のフラッシュを **テンプレートで表示** できる
✔ **削除ボタンを POST フォーム** で実装できる（CSRF自動挿入の仕組みが言える）
✔ `SecurityFilterChain` で **公開URL／要認証URL** を振り分けられる
✔ ★`usernameParameter("email")` で **email ログイン** に切り替えられる★
✔ `application.properties` の DB / JPA / ポートを設定できる

---

## 🧩 8-2-2. 今回作るもの

```text
templates/
├── fragments/layout.html     … head / fieldError / globalErrors / flash / bottomNav / appHeader
├── auth/login.html           … ログイン
├── auth/register.html        … 新規登録
├── dashboard.html            … ダッシュボード（残高・内訳・推移）
├── transaction/list.html     … 履歴一覧（削除POST）
├── transaction/form.html     … 記録の登録
├── category/list.html        … カテゴリー一覧
└── category/form.html        … カテゴリー登録/編集

config/SecurityConfig.java    … SecurityFilterChain を本格化
resources/application.properties … DB接続 / JPA / ポート
```

---

## 🧱 8-2-3. ステップ① 共通フラグメント（layout.html）

完成ファイル：[layout.html](../src/main/resources/templates/fragments/layout.html)

「全画面で繰り返す部品」をフラグメントにまとめ、各画面から `th:replace` で差し込みます。

| フラグメント            | 役割                                       |
| ----------------------- | ------------------------------------------ |
| `head(title)`           | 文字コード・viewport・CSRF meta・Tailwind読込・タイトル |
| `fieldError(fieldName)` | 1項目のバリデーションエラーを赤字表示      |
| `globalErrors`          | フォーム全体エラー（`reject`）を赤バナー表示 |
| `flash`                 | `message`（緑）/ `errorMessage`（赤）を表示 |
| `bottomNav(active)`     | 下部ナビ（ホーム/履歴/＋/設定）。現在地を色付け |
| `appHeader(userName)`   | 上部ヘッダー（利用者名＋ログアウトPOST）   |

### 使う側（例）

```html
<head th:replace="~{fragments/layout :: head('ダッシュボード')}"></head>
...
<header th:replace="~{fragments/layout :: appHeader(${userName})}"></header>
<div th:replace="~{fragments/layout :: flash}"></div>
<nav th:replace="~{fragments/layout :: bottomNav('dashboard')}"></nav>
```

> 💡 React の共通レイアウト/ナビを、Thymeleaf では **フラグメント** で再現する。
> 　 1か所直せば全画面に反映される。

---

## 🧱 8-2-4. ステップ② フォーム画面（`th:field` の仕組み）

完成ファイル：[login.html](../src/main/resources/templates/auth/login.html) / [register.html](../src/main/resources/templates/auth/register.html) / [transaction/form.html](../src/main/resources/templates/transaction/form.html)

### `th:field="*{email}"` が name / id / value を自動生成する

```html
<form th:object="${loginForm}" method="post">
    <input type="email" th:field="*{email}">
</form>
```

レンダリング後：

```html
<input type="email" id="email" name="email" value="（loginFormのemail値）">
```

| 属性          | 役割                                         |
| ------------- | -------------------------------------------- |
| `th:object`   | このフォームが扱う Form（Controllerがmodelに入れた名前） |
| `th:field`    | object 内のフィールドへの bind（name/id/value 自動生成） |

👉 **`th:field` の中身と Form のフィールド名が一致** していれば自動でつながる。
　 ズレるとバインドされない（よくあるバグ）。

### enum やラジオの扱い（記録フォーム）

- 種類（支出/収入）の切り替え：`?type=` を変えてリンクで読み込み直し、カテゴリー選択肢も入れ替える
- カテゴリー選択：`<input type="radio" th:field="*{categoryId}" th:value="${c.id}">` を並べ、
  Tailwind の `peer` / `peer-checked:` で「選択中だけ枠を青く」する

---

## 🧱 8-2-5. ステップ③ 一覧画面（`th:each` とLAZY）

完成ファイル：[transaction/list.html](../src/main/resources/templates/transaction/list.html)

### `th:each` でループ

```html
<div th:each="tx : ${transactions}">
    <span th:text="${tx.category.label}">食費</span>
    <span th:text="${#temporals.format(tx.transactionDate, 'yyyy/MM/dd')}">2026/06/01</span>
</div>
```

- `${tx.category.label}` が動くのは、`Category` が LAZY でも **Open Session In View（既定でON）** が
  画面描画中までDBセッションを開いたままにしてくれるため
- `#temporals.format(...)` で `LocalDate` を任意フォーマットに整形
- `th:text` の中身は **自動でHTMLエスケープ** される → XSSの心配がない

### 金額の整形と収入の符号

```html
<span th:text="(${tx.type.name() == 'INCOME'} ? '+' : '') + '¥'
              + ${#numbers.formatInteger(tx.amount, 0, 'COMMA')}">¥0</span>
```

- `#numbers.formatInteger(値, 最小桁, 'COMMA')` で3桁区切り
- 収入なら先頭に `+` を付ける（React版の挙動を再現）

---

## 🧱 8-2-6. ステップ④ ★削除は POST フォーム＋CSRF★

完成ファイル：[transaction/list.html](../src/main/resources/templates/transaction/list.html) / [category/list.html](../src/main/resources/templates/category/list.html)

```html
<form th:action="@{/transactions/{id}/delete(id=${tx.id})}" method="post"
      onsubmit="return confirm('この記録を削除しますか？');">
    <input type="hidden" name="month" th:value="${selectedMonth}">
    <button type="submit">🗑</button>
</form>
```

### なぜ `<a>` リンクではダメ？
- `<a href>` は **GET**。プリフェッチ・URL直打ち・ブックマークで **勝手に削除** が走る
- → **削除は POST 一択**

### CSRFトークンの自動挿入
- `th:action` ＋ `method="post"` で、Thymeleaf＋Spring Security が **CSRFトークンの hidden input を自動挿入**
- 自分で書く必要なし
- ただし `action="..."`（`th:` 無し）で書くと挿入されず **403** になる

---

## 🧱 8-2-7. ステップ⑤ ダッシュボード（集計の表示）

完成ファイル：[dashboard.html](../src/main/resources/templates/dashboard.html)

- 残高カード：`summary.balance` が0以上なら青、マイナスなら赤（`th:classappend`）
- 支出内訳：**円グラフ（ドーナツ）**。`breakdown` を Chart.js に渡して描画（ラベル=label / 値=amount / 色=color）
- 6ヶ月推移：**折れ線グラフ**。`trend` を Chart.js に渡し、収入(緑)・支出(赤)の2系列で描画
- 月の選択：`<select onchange="this.form.submit()">` で、選んだ瞬間に `?month=` 付きで再読込

### ★グラフ描画★ サーバーのデータを JavaScript に渡す（`th:inline="javascript"`）

React 版の Recharts（円グラフ・グラフ）を、Spring Boot 版では **Chart.js（CDN）** で再現します。
ポイントは「**Service が計算した集計値（DTO）を、画面の JavaScript に渡す**」こと。

```html
<!-- Chart.js 本体を読み込む -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
<script th:inline="javascript">
/*<![CDATA[*/
    // /*[[${...}]]*/ の部分が Thymeleaf によって JSON に変換される
    const breakdown = /*[[${breakdown}]]*/ [];   // [{label,color,amount,percentage}, ...]
    const trend     = /*[[${trend}]]*/ [];        // [{label,income,expense}, ...]

    // 円グラフ（ドーナツ）
    new Chart(document.getElementById('breakdownChart'), {
        type: 'doughnut',
        data: {
            labels: breakdown.map(s => s.label),
            datasets: [{ data: breakdown.map(s => s.amount),
                         backgroundColor: breakdown.map(s => s.color) }]
        }
    });
    // 折れ線グラフ（収入・支出の2系列）
    new Chart(document.getElementById('trendChart'), {
        type: 'line',
        data: {
            labels: trend.map(p => p.label),
            datasets: [
                { label: '収入', data: trend.map(p => p.income), borderColor: '#34d399' },
                { label: '支出', data: trend.map(p => p.expense), borderColor: '#f87171' }
            ]
        }
    });
/*]]>*/
</script>
```

| ポイント | 内容 |
| -------- | ---- |
| `th:inline="javascript"` | スクリプト内で `/*[[${...}]]*/` を使うと、modelの値が **JSON** に変換される |
| DTOのリスト | `breakdown`(List&lt;CategorySlice&gt;) / `trend`(List&lt;MonthlyTrendPoint&gt;) が getter 経由でJSON配列になる |
| 役割分担 | **集計は Service、整形JSONはThymeleaf、描画はChart.js**。サーバーは数値を渡すだけ |
| 空データ | 内訳が0件のときは `th:if` で `<canvas>` を出さず「支出データがありません」を表示する（JS側も要素の有無を確認） |

> 💡 縦軸の最大値（スケール）は **Chart.js が自動計算** するので、サーバー側で最大値を求める必要はない。
> 　 以前の「CSS横棒＋手計算の幅」より、グラフ専用ライブラリに任せる方がシンプルで正確。

---

## 🔐 8-2-8. ステップ⑥ ★最重要★ SecurityConfig 本格化

完成ファイル：[SecurityConfig.java](../src/main/java/com/example/expensetracker/config/SecurityConfig.java)

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("email")          // ★email でログイン★
            .passwordParameter("password")
            .defaultSuccessUrl("/dashboard", true)
            .failureUrl("/login?error")
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        );
    return http.build();
}
```

### `authorizeHttpRequests`（URL単位の認可）

| ルール                                    | 意味             |
| ----------------------------------------- | ---------------- |
| `.requestMatchers("/login","/register",...).permitAll()` | 未ログインOK |
| `.anyRequest().authenticated()`           | それ以外はログイン必須 |

**順番が重要**：`permitAll()` を先に書く。後だと公開ページまで `authenticated()` に飲まれる。

### `formLogin`

| 設定                                       | 役割                                |
| ------------------------------------------ | ----------------------------------- |
| `.loginPage("/login")`                     | 未認証時に飛ばす画面（GET）         |
| `.loginProcessingUrl("/login")`            | POST先（Securityが直接処理）        |
| ★`.usernameParameter("email")`             | input[name="email"] をユーザー名に  |
| `.defaultSuccessUrl("/dashboard", true)`   | 成功後は必ずダッシュボードへ        |
| `.failureUrl("/login?error")`              | 失敗時（login.htmlが`?error`を検知） |

### `UserDetailsService` は自動接続

- `UserService implements UserDetailsService`（第7章）を Spring が **自動検出**
- `PasswordEncoder` Bean と合わせて認証の仕組みが自動で組み上がる
- → SecurityConfig に `.userDetailsService(...)` を明示しなくても繋がる

### CSRF はデフォルトON

- フォームPOSTには CSRFトークンが必須 → `th:action` を使えば自動挿入
- ログアウトも POST `/logout`（`appHeader` フラグメントのフォーム）

---

## ⚙ 8-2-9. ステップ⑦ application.properties

完成ファイル：[application.properties](../src/main/resources/application.properties)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/expense_tracker
spring.datasource.username=postgres
spring.datasource.password=123456
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

server.port=8080
```

| `ddl-auto` | 動作                          | いつ使う？        |
| ---------- | ----------------------------- | ----------------- |
| `create`   | 毎回DROP→CREATE（データ消える）| ローカル実験      |
| `update`   | 差分があればALTER             | **学習・開発**    |
| `validate` | 差分があればエラー            | 本番直前          |

👉 業務では `validate` ＋ Flyway/Liquibase が定石。今回は学習のため `update`。

### 事前準備（DB作成）

```sql
CREATE DATABASE expense_tracker;
```

---

## ▶ 8-2-10. 起動と動作確認

```bash
# Windows
mvnw.cmd spring-boot:run
```

| URL                              | 何が見える？                         |
| -------------------------------- | ------------------------------------ |
| `http://localhost:8080/`         | 未ログインなら /login へ             |
| `http://localhost:8080/register` | 新規登録（成功で初期カテゴリーも作成）|
| `http://localhost:8080/login`    | ログイン                             |
| `http://localhost:8080/dashboard`| ログイン後のダッシュボード           |

### 動作の流れ
1. `/register` で登録 → パスワードが **ハッシュ化** されて保存、初期カテゴリー12件が作られる
2. `/login` で email＋パスワードでログイン
3. `/transactions/new` で記録 → `/dashboard` で集計を確認

---

## ❌ 8-2-11. よくあるつまづき

| 症状                                        | 原因と対処                                          |
| ------------------------------------------- | --------------------------------------------------- |
| `/` を開くとログイン画面に飛ぶ              | （正常）未ログインなら飛ぶ仕様                      |
| ログインで **403 Forbidden**                | フォームに `th:action` を使っていない → CSRF未挿入  |
| ログインしても弾かれる                      | `usernameParameter("email")` 漏れ／email未登録       |
| `Connection refused`                        | PostgreSQL未起動／URL・認証情報違い                  |
| `relation "transactions" does not exist`    | `ddl-auto=none` で起動／DBが空                       |
| `LazyInitializationException`               | Service の `@Transactional` 内で関連を触れていない   |
| 一覧の日付が `06月` でなく崩れる            | `T(java.lang.Integer).parseInt(...)` で月を数値化する |

---

## 📝 8-2-12. ソースを書くときの順番

1. **layout.html**（共通フラグメント）を先に
2. **auth/login・register**（入口）
3. **dashboard**（集計表示）
4. **transaction/list・form**（一覧・登録・削除POST）
5. **category/list・form**（管理）
6. **SecurityConfig 本格化**（permit→authenticated／email ログイン）
7. **application.properties** でDB接続
8. 起動 → 一気通貫の動作確認

---

## ✨ 8-2-13. ソースを書くときのポイント

- **モデル属性キーとテンプレートを完全一致** させる
- **削除は POST フォーム＋confirm**、`th:action` で CSRF 自動挿入
- **SecurityConfig は permitAll → authenticated の順**
- ★**`usernameParameter("email")` を忘れない**★
- `UserDetailsService` 実装は **自動で組み込まれる**（明示不要）
- `ddl-auto=update` は **学習中だけ**

---

## ✅ 8-2-14. 全章まとめ（完成）

| 層             | クラス／ファイル                                              | 章   |
| -------------- | ------------------------------------------------------------ | ---- |
| Entity         | `TransactionType` / `User` / `Category` / `Transaction`      | 1〜3 |
| Repository     | `UserRepository` / `CategoryRepository` / `TransactionRepository` | 4 |
| Form           | `LoginForm` / `UserRegisterForm` / `CategoryForm` / `TransactionForm` | 5 |
| DTO            | `MonthlySummary` / `CategorySlice` / `MonthlyTrendPoint`      | 6 |
| Service        | `UserService`（★ハッシュ化★）/ `CategoryService` / `TransactionService` | 7 |
| Exception      | `ResourceNotFoundException`                                  | 7 |
| Controller     | Home / Login / UserRegister / Dashboard / Transaction / Category | 8-1 |
| Template       | layout / auth / dashboard / transaction / category           | 8-2 |
| Config         | `SecurityConfig` / `application.properties`                  | 7・8-2 |

これで「**パスワードをハッシュ化し、層を分けて作った家計簿アプリ**」が動く形になりました。

お疲れさまでした 🎉
