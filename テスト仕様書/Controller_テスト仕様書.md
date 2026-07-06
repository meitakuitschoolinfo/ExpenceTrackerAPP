# 単体テスト仕様書（Controller 層）

| 項目 | 内容 | 項目 | 内容 |
| --- | --- | --- | --- |
| システム名 | スマート家計簿（ExpenseTrackerApp） | 作成者 | （未記入） |
| サブシステム名 | 画面入口（Controller）全体 | 作成日 | 2026-06-23 |
| 対象パッケージ | com.example.expensetracker.controller | 実施者 | （未記入） |
| テスト方式 | Spring Boot Test（`@WebMvcTest`）＋ MockMvc ＋ Mockito（Service を `@MockitoBean` でモック化）＋ Spring Security Test | - | - |

## 共通方針

- 各テストは `@WebMvcTest(対象Controller.class)` で **Controller 層だけ** を起動する（Service / DB は読み込まない）。
- 本物の認可ルールで検証するため `@Import(SecurityConfig.class)` で SecurityConfig を取り込む。
- ログインが必要な画面は `@WithMockUser(username = "ake@test.com")` で「ログイン済み」を擬似的に作る。
- POST は CSRF 保護が有効なので `.with(csrf())` でトークンを付ける。
- 依存 Service は `@MockitoBean` でモック化し、`when(...).thenReturn(...)` で戻り値を仕込む。
- 検証は「HTTPステータス」「ビュー名」「model 属性」「リダイレクト先」「フラッシュ属性」「Service 呼び出し（`verify`）」で行う。

> 補足：Spring Security 7（Spring Boot 4.x）では、未認証時のリダイレクト先が相対パス `"/login"` になる（旧バージョンの `http://localhost/login` から変更）。本仕様書もそれに合わせている。

---

## 1. HomeController（対応ソース：`HomeControllerTest`）

| 項目 | 内容 |
| --- | --- |
| 対象クラス | com.example.expensetracker.controller.HomeController |
| 概要 | トップページ `"/"` のリダイレクト |

| No | 分類 | テスト項目 | 検証内容 | 結果 | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | 正常系 | ログイン済みで GET `/` | ステータス 3xx／リダイレクト先が `"/dashboard"` | - | `redirect:/dashboard` |
| 2 | 認可 | 未ログインで GET `/` | ステータス 3xx／リダイレクト先が `"/login"` | - | `"/"` は認証必須 |

---

## 2. LoginController（対応ソース：`LoginControllerTest`）

| 項目 | 内容 |
| --- | --- |
| 対象クラス | com.example.expensetracker.controller.LoginController |
| 概要 | ログイン画面の表示（POST /login は Spring Security が処理するので対象外） |

| No | 分類 | テスト項目 | 検証内容 | 結果 | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | 表示／認可 | 未ログインで GET `/login` | ステータス 200／ビュー `"auth/login"`／model に `loginForm` がある | - | `/login` は permitAll |

---

## 3. UserRegisterController（対応ソース：`UserRegisterControllerTest`）

| 項目 | 内容 |
| --- | --- |
| 対象クラス | com.example.expensetracker.controller.UserRegisterController |
| 概要 | 利用者の新規登録（表示・成功・入力エラー・重複エラー） |

| No | 分類 | テスト項目 | 検証内容 | 結果 | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | 表示 | GET `/register` | 200／ビュー `"auth/register"`／model に `userRegisterForm` | - | フォーム初期表示 |
| 2 | 正常系 | 正しい入力で POST `/register` | 3xx／`"/login"` へリダイレクト／フラッシュ `message` あり／`userService.register` が1回呼ばれる | - | PRGパターン |
| 3 | 異常系（入力） | name を空で POST `/register` | 200／再描画 `"auth/register"`／`userRegisterForm.name` に項目エラー／`register` は呼ばれない | - | `@NotBlank` 違反 |
| 4 | 異常系（業務） | Service が重複例外を投げる POST `/register` | 200／再描画 `"auth/register"`／`userRegisterForm` にフォーム全体エラー | - | `bindingResult.reject` |

### モック設定（前提条件）

| No | モック対象メソッド | 引数 | 戻り値 |
| --- | --- | --- | --- |
| 1 | userService.register | 正常な UserRegisterForm | （スタブ不要。null を返す） |
| 2 | userService.register | 重複ケース | `IllegalArgumentException` を throw |

---

## 4. TransactionController（対応ソース：`TransactionControllerTest`）

| 項目 | 内容 |
| --- | --- |
| 対象クラス | com.example.expensetracker.controller.TransactionController |
| 概要 | 収支記録の履歴一覧・新規登録・削除 |
| 前提 | 全テスト `@WithMockUser("ake@test.com")` でログイン済み |

| No | 分類 | テスト項目 | 検証内容 | 結果 | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | 表示 | GET `/transactions` | 200／ビュー `"transaction/list"`／model に `userName,selectedMonth,availableMonths,transactions` | - | 履歴一覧 |
| 2 | 表示 | GET `/transactions/new` | 200／ビュー `"transaction/form"`／model に `transactionForm,categories` | - | 登録フォーム |
| 3 | 正常系 | 正しい入力で POST `/transactions` | 3xx／`"/transactions?month=2026-06"`／フラッシュ `message`／`register(user,_)` が1回 | - | PRG |
| 4 | 異常系（入力） | amount なしで POST `/transactions` | 200／再描画 `"transaction/form"`／model に `categories` 再設定／`register` 未呼び出し | - | `@NotNull` 違反 |
| 5 | 正常系 | POST `/transactions/5/delete` | 3xx／`"/transactions?month=2026-06"`／フラッシュ `message`／`delete(user,5L)` が1回 | - | 削除はPOST |

### モック設定（前提条件）

| No | モック対象メソッド | 引数 | 戻り値 |
| --- | --- | --- | --- |
| 1 | userService.findByEmail | "ake@test.com" | テスト用 User（id=1, name=Akemi） |
| 2 | transactionService.parseMonthOrCurrent | 任意 | YearMonth 2026-06 |
| 3 | transactionService.availableMonths | 任意 | ["2026-06","2026-05"] |
| 4 | categoryService.findByUserAndType | (user, 任意) | [食費カテゴリー] |
| 5 | transactionService.findByMonth | (user, 任意) | 空リスト（テスト1） |

---

## 5. CategoryController（対応ソース：`CategoryControllerTest`）

| 項目 | 内容 |
| --- | --- |
| 対象クラス | com.example.expensetracker.controller.CategoryController |
| 概要 | カテゴリーの一覧・登録・編集・削除（`@RequestMapping("/categories")`） |
| 前提 | 全テスト `@WithMockUser("ake@test.com")` でログイン済み |

| No | 分類 | テスト項目 | 検証内容 | 結果 | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | 表示 | GET `/categories` | 200／ビュー `"category/list"`／model に `userName,expenseCategories,incomeCategories` | - | 一覧 |
| 2 | 表示 | GET `/categories/new` | 200／ビュー `"category/form"`／`isEdit=false` | - | 登録フォーム |
| 3 | 正常系 | 正しい入力で POST `/categories` | 3xx／`"/categories"`／フラッシュ `message`／`register(user,_)` が1回 | - | 登録 |
| 4 | 異常系（入力） | label を空で POST `/categories` | 200／再描画 `"category/form"`／`isEdit=false`／`register` 未呼び出し | - | `@NotBlank` 違反 |
| 5 | 表示 | GET `/categories/3/edit` | 200／ビュー `"category/form"`／`isEdit=true`／`categoryForm` あり | - | `toEditForm` 利用 |
| 6 | 正常系 | 正しい入力で POST `/categories/3/edit` | 3xx／`"/categories"`／フラッシュ `message`／`update(user,_)` が1回 | - | 更新 |
| 7 | 正常系 | POST `/categories/4/delete`（未使用） | 3xx／`"/categories"`／フラッシュ `message`／`delete(user,4L)` | - | 削除成功 |
| 8 | 異常系（業務） | 使用中で POST `/categories/5/delete` | 3xx／`"/categories"`／フラッシュ `errorMessage` | - | `IllegalStateException` を握る |

### モック設定（前提条件）

| No | モック対象メソッド | 引数 | 戻り値 |
| --- | --- | --- | --- |
| 1 | userService.findByEmail | "ake@test.com" | テスト用 User |
| 2 | categoryService.findByUserAndType | (user, 任意) | 空リスト（テスト1） |
| 3 | categoryService.toEditForm | (user, 3L) | CategoryForm（食費） |
| 4 | categoryService.delete | (user, 5L) | `IllegalStateException` を throw（テスト8） |

---

## 6. DashboardController（対応ソース：`DashboardControllerTest`）

| 項目 | 内容 |
| --- | --- |
| 対象クラス | com.example.expensetracker.controller.DashboardController |
| 概要 | ダッシュボード（残高サマリー・支出内訳・6ヶ月推移）の表示 |

| No | 分類 | テスト項目 | 検証内容 | 結果 | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | 表示 | ログイン済みで GET `/dashboard` | 200／ビュー `"dashboard"`／model に `userName,selectedMonth,availableMonths,summary,breakdown,trend` | - | テンプレート最後まで描画できることの確認も兼ねる |
| 2 | 認可 | 未ログインで GET `/dashboard` | 3xx／`"/login"` へリダイレクト | - | 認証必須 |

### モック設定（前提条件）

| No | モック対象メソッド | 引数 | 戻り値 |
| --- | --- | --- | --- |
| 1 | userService.findByEmail | "ake@test.com" | テスト用 User |
| 2 | transactionService.parseMonthOrCurrent | 任意 | YearMonth 2026-06 |
| 3 | transactionService.availableMonths | 任意 | ["2026-06","2026-05"] |
| 4 | transactionService.summarize | (任意, 任意) | MonthlySummary(1000, 500, 500) |
| 5 | transactionService.expenseBreakdown | (任意, 任意) | [CategorySlice("食費","#f87171",500,100)] |
| 6 | transactionService.recentTrend | (任意, 任意) | [MonthlyTrendPoint("6月",1000,500)] |

---

## テストデータ（Controller 層 共通）

| No | 項目 | 値 | 説明 |
| --- | --- | --- | --- |
| 1 | user.id | 1L | モック User のID |
| 2 | user.name | Akemi | 表示名 |
| 3 | user.email | ake@test.com | ログインID（`@WithMockUser` の username と一致） |
| 4 | category | 食費 / #f87171 / EXPENSE | 記録フォームの選択肢用 |
| 5 | 月 | 2026-06 | テストの基準月 |

## 実行結果（最終）

| 区分 | テスト数 | 結果 |
| --- | --- | --- |
| HomeControllerTest | 2 | OK |
| LoginControllerTest | 1 | OK |
| UserRegisterControllerTest | 4 | OK |
| TransactionControllerTest | 5 | OK |
| CategoryControllerTest | 8 | OK |
| DashboardControllerTest | 2 | OK |
| **合計** | **22** | **全件成功** |
