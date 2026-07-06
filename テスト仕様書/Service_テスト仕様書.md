# 単体テスト仕様書（Service 層）

| 項目 | 内容 | 項目 | 内容 |
| --- | --- | --- | --- |
| システム名 | スマート家計簿（ExpenseTrackerApp） | 作成者 | （未記入） |
| サブシステム名 | 集計・記録の業務ロジック | 作成日 | 2026-06-23 |
| 対象クラス | com.example.expensetracker.service.TransactionService | 実施者 | （未記入） |
| テスト方式 | 純粋な単体テスト（`@ExtendWith(MockitoExtension.class)` ＋ `@Mock` ＋ `@InjectMocks`）。Spring も DB も起動しない | - | - |

## 共通方針

- `@Mock` で `TransactionRepository` と `CategoryService` をモック化し、`@InjectMocks` で本物の `TransactionService` に差し込む。
- DB にはつながず、Repository の戻り値を `when(...).thenReturn(...)` で仕込んで **集計ロジックだけ** を検証する。
- 保存内容の検証には `ArgumentCaptor<Transaction>` を使い、`save()` に渡された Entity の中身を確認する。
- 例外系は `assertThrows(...)`、未呼び出しは `verify(mock, never())` で検証する。

## テストケース一覧

| No | 分類 | テスト項目 | 検証内容 | 結果 | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | 集計 | summarize | 収入1000・支出(300+200)から income=1000／expense=500／balance=500 | - | 残高＝収入−支出 |
| 2 | 集計 | expenseBreakdown | 支出のみをカテゴリー別合計。交通費700(70%)→食費300(30%) の降順。収入は除外 | - | 同一カテゴリーは合算 |
| 3 | 集計 | recentTrend | 当月含む6ヶ月分・古い順。先頭"1月"・末尾"6月"、0件月は収入支出0 | - | summarize を6回内部呼び出し |
| 4 | 変換 | parseMonthOrCurrent | "2026-06"→2026-06／null→当月／不正値→当月 | - | 例外で落とさない |
| 5 | 登録（正常） | register | Form を Entity へ詰めて save。持ち主は必ず user、種類/カテゴリー/金額/メモが一致。戻り値は save 結果 | - | ArgumentCaptor で検証 |
| 6 | 登録（異常） | register（種類不一致） | 記録の種類とカテゴリーの種類が違うと `IllegalArgumentException`／save 未呼び出し | - | 整合性チェック |
| 7 | 削除（正常） | delete | 自分の記録なら `repository.delete` を呼ぶ | - | - |
| 8 | 削除（異常） | delete（他人の記録） | 持ち主違いは `ResourceNotFoundException`／delete 未呼び出し | - | 情報を漏らさない |
| 9 | 削除（異常） | delete（存在しない） | 該当なしは `ResourceNotFoundException` | - | - |

## テストデータ

| No | 項目 | 値 | 説明 |
| --- | --- | --- | --- |
| 1 | user | id=1, Akemi, ake@test.com | 持ち主 |
| 2 | 食費 | id=10, EXPENSE | 支出カテゴリー |
| 3 | 交通費 | id=11, EXPENSE | 支出カテゴリー |
| 4 | 給与 | id=20, INCOME | 収入カテゴリー（内訳除外の確認用） |
| 5 | 基準月 | 2026-06 | 集計対象月 |

## モック設定（前提条件）

| No | モック対象メソッド | 引数 | 戻り値 |
| --- | --- | --- | --- |
| 1 | transactionRepository.findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc | (any, any, any) | テストごとの記録リスト |
| 2 | categoryService.findOwnedById | (user, 10L) | 食費カテゴリー（種類一致／テスト5） |
| 3 | categoryService.findOwnedById | (user, 20L) | 給与カテゴリー（種類不一致／テスト6） |
| 4 | transactionRepository.save | any(Transaction) | 渡された引数をそのまま返す |
| 5 | transactionRepository.findById | 5L / 99L | Optional.of(記録) / Optional.empty() |

## 実行結果（最終）

| 区分 | テスト数 | 結果 |
| --- | --- | --- |
| TransactionServiceTest | 9 | 全件成功 |
