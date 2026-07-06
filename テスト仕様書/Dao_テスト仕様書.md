# 単体テスト仕様書（Dao／Repository 層）

| 項目 | 内容 | 項目 | 内容 |
| --- | --- | --- | --- |
| システム名 | スマート家計簿（ExpenseTrackerApp） | 作成者 | （未記入） |
| サブシステム名 | DB操作（Spring Data JPA Repository） | 作成日 | 2026-06-23 |
| 対象クラス | com.example.expensetracker.repository.TransactionRepository | 実施者 | （未記入） |
| テスト方式 | `@DataJpaTest`（JPA層スライス）＋ テスト用 H2（インメモリDB）＋ `TestEntityManager` | - | - |

## 共通方針

- `@DataJpaTest` で **JPA（Repository）層だけ** を起動する（Controller / Service は読み込まない）。
- 本物の PostgreSQL ではなく **テスト用 H2** を使う（`src/test/resources/application.properties` の設定）。
  - そのため `@AutoConfigureTestDatabase(replace = NONE)` を付け、組み込みDBへの自動差し替えを止めて H2 設定を使う。
  - H2 は `ddl-auto=create-drop` で Entity からテーブルを自動生成し、テスト後に破棄する。
- テストデータは `TestEntityManager` で実際に INSERT する。`flush()`＋`clear()` 後に検索すると本物の SELECT が走る。
- `@DataJpaTest` は既定でトランザクションを張り、各テスト後に **ロールバック** するのでデータは残らない。

## テストケース一覧

| No | 分類 | テスト項目 | 検証内容 | 結果 | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | 検索 | findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc（6/1〜6/30） | 6月の3件だけを返す（5月分は除外）。日付降順、同日(6/20)はID降順 | - | 月の境界（Between は両端含む） |
| 2 | 集計 | countByCategory | 食費=3件（月に関係なく全件）／交通費=1件 | - | カテゴリー削除前チェック用 |

## テストデータ（@BeforeEach で H2 に投入）

| No | 種別 | 内容 | 用途 |
| --- | --- | --- | --- |
| 1 | User | Akemi / ake@test.com | 全データの持ち主 |
| 2 | Category | 食費(EXPENSE)、交通費(EXPENSE) | 記録の分類 |
| 3 | Transaction | 2026-06-10 食費 300円 | 6月分（検索対象） |
| 4 | Transaction | 2026-06-20 交通費 700円 | 6月分（同日・ID降順確認用） |
| 5 | Transaction | 2026-06-20 食費 200円 | 6月分（同日・ID降順確認用、後入れ＝IDが大きい） |
| 6 | Transaction | 2026-05-15 食費 999円 | 5月分（検索では除外されるべきデータ） |

### 期待される並び（テスト1）

```
1番目: 2026-06-20（後から入れた食費200。IDが大きいので先頭）
2番目: 2026-06-20（交通費700）
3番目: 2026-06-10（食費300）
※ 2026-05-15（999円）は範囲外なので含まれない
```

## 実行結果（最終）

| 区分 | テスト数 | 結果 |
| --- | --- | --- |
| TransactionRepositoryTest | 2 | 全件成功 |

## 補足：なぜ H2 を使うのか

| 観点 | 本番(PostgreSQL)で直接テスト | テスト用H2（本仕様） |
| --- | --- | --- |
| 事前準備 | DBの起動・接続情報が必要 | 不要（メモリ上に自動生成） |
| 再現性 | 前のデータが残ることがある | 毎回まっさら（create-drop＋ロールバック） |
| 速度 | ネットワーク往復で遅い | メモリ上で高速 |
| CI/CD | DBコンテナが要る | そのまま動く |
