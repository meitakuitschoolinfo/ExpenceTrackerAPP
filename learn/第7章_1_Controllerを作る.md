# 📘 第7章（前半）

## Controller を作る
― HTTPの入口。今までのピースを「URLから呼べる」状態にする ―

---

## 🎯 7-1-1. この章のゴール

✔ Controller の役割を **一文で説明できる**
✔ `@GetMapping` / `@PostMapping` / `@PathVariable` / `@RequestParam` を使い分けられる
✔ `@Valid` ＋ `BindingResult` で **入力チェック→再描画** が書ける
✔ **PRGパターン**（Post-Redirect-Get）を説明できる
✔ `addFlashAttribute` で **1回限りメッセージ** を渡せる
✔ ★`@AuthenticationPrincipal` で **ログイン中ユーザー** を安全に受け取れる★
✔ Controller に **書いてはいけないコード** が分かる

---

## 🧩 7-1-2. Controller とは

> Controller ＝ **HTTPリクエストを受け取り、Serviceを呼び、画面を返す** クラス

```text
[ ブラウザ ] ─HTTP─▶ [ Controller ] ─▶ [ Service ] ─▶ [ Repository ]
```

### やること：URL紐付け／値の受け取り／`@Valid`発動／Service呼び出し／画面名 or リダイレクト返却
### やらないこと：SQL／業務ルール／ハッシュ化／HTML組み立て

👉 **薄く保つ**。Controller が太ったら大体 Service の仕事を吸っている。

---

## 🔄 7-1-3. なぜ Service の次に Controller なのか

Controller は **最も上の層** で、Service / Form / Entity すべてに依存する。
→ 全部完成してから書くと import で詰まらない。

### この章で作る Controller と URL

| Controller             | URL                                        | 学ぶこと                     |
| ---------------------- | ------------------------------------------ | ---------------------------- |
| `HomeController`       | `/`                                        | 最小形＋リダイレクト         |
| `LoginController`      | `/login`(GET)                              | POSTは書かない（Securityへ） |
| `UserRegisterController` | `/register`(GET/POST)                    | 新規登録＋PRG                |
| `DashboardController`  | `/dashboard`                               | `@AuthenticationPrincipal`／集計表示 |
| `TransactionController`| `/transactions` ほか                        | 一覧／登録／削除（POST）      |
| `CategoryController`   | `/categories/**`                           | `@RequestMapping` で集約     |

---

## 🧱 7-1-4. 共通の書き方

```java
@Controller
@RequiredArgsConstructor
public class XxxController {
    private final XxxService xxxService;

    @GetMapping("/xxx")
    public String show(Model model) {
        model.addAttribute("key", value);   // テンプレートで ${key} として使える
        return "xxx/yyy";                    // → templates/xxx/yyy.html
    }
}
```

- `@Controller`（`@RestController` ではない）… 戻り値の文字列を **テンプレート名** として解釈
- `return "redirect:/dashboard"` … テンプレートでなく **内部リダイレクト指示**

---

## 🆔 7-1-5. URLからの値の受け取り

| 方法              | 例                                              | 用途                 |
| ----------------- | ----------------------------------------------- | -------------------- |
| `@PathVariable`   | `/categories/{id}/edit` → `@PathVariable Long id` | URLの一部から取る   |
| `@RequestParam`   | `/transactions?month=2026-06` → `@RequestParam String month` | クエリから取る |
| Form 自動バインド | `@Valid TransactionForm form`                   | フォーム送信内容     |

> 💡 enum も自動変換される：`@RequestParam TransactionType type` に `?type=EXPENSE` が入る。

---

## ✅ 7-1-6. ★最重要★ バリデーション → 再描画

```java
@PostMapping("/transactions")
public String register(
        @Valid TransactionForm transactionForm,   // ①@Validで発動
        BindingResult bindingResult,               // ②直後に置く（順番厳守）
        @AuthenticationPrincipal UserDetails principal,
        Model model,
        RedirectAttributes redirectAttributes) {

    User user = userService.findByEmail(principal.getUsername());

    if (bindingResult.hasErrors()) {               // ③エラーなら再描画
        model.addAttribute("categories",
            categoryService.findByUserAndType(user, transactionForm.getType()));  // ★詰め直す★
        return "transaction/form";
    }
    try {
        transactionService.register(user, transactionForm);
    } catch (IllegalArgumentException e) {         // ④業務エラーもフォームエラー化
        bindingResult.reject("global.error", e.getMessage());
        model.addAttribute("categories",
            categoryService.findByUserAndType(user, transactionForm.getType()));
        return "transaction/form";
    }
    redirectAttributes.addFlashAttribute("message", "記録を登録しました");
    return "redirect:/transactions?month=" + YearMonth.from(transactionForm.getTransactionDate());  // ★PRG★
}
```

### `BindingResult` は `@Valid` の **直後** に置く（順番厳守）

```java
// ⭕ 正しい
public String register(@Valid TransactionForm form, BindingResult br, ...) { }
// ❌ 間に挟むとエラー情報が取れなくなる
public String register(@Valid TransactionForm form, Model model, BindingResult br) { }
```

### 再描画時にモデルを **詰め直す** 理由

- カテゴリー選択肢は GET 時に詰めたもの
- POST→再描画では **GET時のモデルは引き継がれない** → 自分で詰め直す
- 忘れるとカテゴリー欄が空になるバグ

### `bindingResult.reject(...)`

- Service が投げた業務エラー（重複・種類不一致など）を **フォーム全体のエラー** に昇格
- テンプレートの `globalErrors` フラグメントで赤バナー表示

---

## 🔁 7-1-7. ★超重要★ PRG パターン

> POST で処理した後は、**必ずリダイレクト**してから GET で画面を表示する

```text
POST /transactions → 登録 → return "redirect:/transactions" → GET /transactions（一覧）
```

- POST 直後に HTML を返すと、F5で **二重送信** が起きる
- リダイレクトしておけば F5 は安全（GETの再読み込みになる）
- `addFlashAttribute` … リダイレクト先で **1回だけ** 取れる属性（「保存しました」表示に最適）

---

## 🔐 7-1-8. ★最重要★ ログイン中ユーザーの取得

```java
@GetMapping("/dashboard")
public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
    User user = userService.findByEmail(principal.getUsername());  // emailから本人を引き直す
    ...
}
```

### なぜ URL の id を信用しないのか

- `/dashboard/{userId}` のような設計だと、**他人の id を入れて他人のデータを見られてしまう**
- → 信用するのは「**いま誰がログインしているか**」だけ
- `principal.getUsername()` で email を得て、`userService.findByEmail(...)` で本人を取り直す

👉 **本人特定は URL ではなく認証情報から**。これがセキュリティの基本。
　 さらに Service 側の **持ち主チェック**（第6章）と合わせて二重で守る。

---

## ⛏ 7-1-9. 個別Controllerの見どころ

### HomeController
```java
@GetMapping("/")
public String index() { return "redirect:/dashboard"; }
```
未ログインなら Security が `/login` へ飛ばす。

### LoginController
- **GETだけ書く**。POST /login は Spring Security が直接受ける（第7章後半）
- 自分で POST /login を書くと Security と衝突する

### TransactionController（記録）
- 一覧 `/transactions?month=`、新規 `/transactions/new?type=`、登録 POST `/transactions`、削除 POST `/transactions/{id}/delete`
- **削除は必ず POST**（GETだとリンク踏みやプリフェッチで消える）
- 新規フォームでは「支出/収入」の切り替えで `?type=` を変えて再読込し、カテゴリー選択肢を切り替える

### CategoryController（集約スタイル）
- `@RequestMapping("/categories")` をクラスに付け、URLの共通プレフィックスをまとめる
- 1クラスで一覧・登録・編集・削除の CRUD を担当（記録の分割スタイルと対比）
- 削除の `IllegalStateException`（使用中）はフラッシュの `errorMessage` で表示

---

## ❌ 7-1-10. 初心者がやりがちなNG

| NG                                          | 何が起きる？                  |
| ------------------------------------------- | ----------------------------- |
| Controller から Repository を直接呼ぶ       | Serviceの意義が消える         |
| `BindingResult` を `@Valid` の直後以外に置く | エラーが取れない             |
| POST後にリダイレクトしない                  | F5で二重送信（PRG違反）       |
| 再描画時に `categories` を詰め直し忘れる    | 選択肢が空になる              |
| 削除を GET で受ける                         | 踏むだけで消える              |
| URL から userId を取って本人特定する        | 他人のデータを操作できる      |
| Controllerでハッシュ化や集計をする          | 層が逆転／テストできない      |

---

## 📝 7-1-11. ソースを書くときの順番

1. **HomeController**（最小形）
2. **LoginController**（GETだけ）
3. **UserRegisterController**（`@Valid`＋PRG）
4. **DashboardController**（`@AuthenticationPrincipal`＋集計表示）
5. **TransactionController**（一覧／登録／削除）
6. **CategoryController**（`@RequestMapping`集約）

各Controllerの中：宣言＋3点セット → GET（表示）→ POST（更新）→ 削除（POST）。

---

## ✨ 7-1-12. ソースを書くときのポイント

- **Controllerは薄く**。ロジックはService
- **`BindingResult` は `@Valid` の直後**
- **POST後は必ず `redirect:`（PRG）**
- **再描画時はモデルを詰め直す**
- **削除は必ず POST**
- ★**ログイン中ユーザーは `@AuthenticationPrincipal` から**（URLのidを信用しない）★
- 業務エラーは `bindingResult.reject(...)` でフォームエラーに昇格、成功はフラッシュで通知

---

## ✅ 7-1-13. 第7章（前半）まとめ

✔ Controller ＝ HTTP受付→Service呼び出し→画面返却の薄い層
✔ 値の受け取り：`@PathVariable` / `@RequestParam` / Form自動バインド（enumも自動変換）
✔ 入力チェック：`@Valid` ＋ `BindingResult`（直後）
✔ PRG：POST後は必ず `redirect:`
✔ ★本人特定は `@AuthenticationPrincipal`、削除は POST★

---

## 🔜 次の章

**第7章（後半）：テンプレートと SecurityConfig**
― Thymeleafで画面を作り、認可・ログイン・CSRFを設定して**動くアプリ**にする ―
