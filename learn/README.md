# 📚 スマート家計簿（Spring Boot版）学習ガイド

React 1ファイルの家計簿アプリを、**Spring Boot のレイヤード・アーキテクチャ**
（Controller / Service / Repository / Entity / Form / DTO）に作り替えながら学ぶ教材です。
**パスワードは BCrypt でハッシュ化**します。各ソースには1行ずつ初心者向けコメントを付けています。

## 読む順番（＝作る順番）

| 章 | ファイル | テーマ |
| -- | -------- | ------ |
| 0 | [第0章_全体像とアーキテクチャ.md](./第0章_全体像とアーキテクチャ.md) | 層構造／作る順番／React対応表 |
| 1 | [第1章_Entityを作る①_TransactionTypeとUser.md](./第1章_Entityを作る①_TransactionTypeとUser.md) | enum と User（テーブル＝クラス） |
| 2 | [第2章_Entityを作る②_Category.md](./第2章_Entityを作る②_Category.md) | `@ManyToOne` と enum カラム |
| 3 | [第3章_Entityを作る③_Transaction.md](./第3章_Entityを作る③_Transaction.md) | 2つの外部キー／金額・日付の型 |
| 4 | [第4章_Repositoryを作る.md](./第4章_Repositoryを作る.md) | メソッド名規約／範囲検索 |
| 5 | [第5章_Formを作る.md](./第5章_Formを作る.md) | 画面の入力箱／Entityと分ける理由 |
| 6 | [第6章_DTOを作る.md](./第6章_DTOを作る.md) | 画面への出力箱／集計結果を運ぶ／Formとの対比 |
| 7 | [第7章_Serviceを作る.md](./第7章_Serviceを作る.md) | ★パスワードのハッシュ化★／集計／持ち主チェック |
| 8-1 | [第8章_1_Controllerを作る.md](./第8章_1_Controllerを作る.md) | HTTPの入口／`@AuthenticationPrincipal` |
| 8-2 | [第8章_2_テンプレートとSecurityConfig.md](./第8章_2_テンプレートとSecurityConfig.md) | Thymeleaf／認可・ログイン・CSRF／起動 |
| 9 | [第9章_テストを書く.md](./第9章_テストを書く.md) | 単体テスト／`@WebMvcTest`・Mockito・`@DataJpaTest`／Boot4.0の注意点 |

## 一貫して流れる3つの原則

1. **依存される側 → 依存する側** の順で作る（Entity→Repository→Form→Service→Controller→画面）
2. **画面の都合（Form）とDBの都合（Entity）を混ぜない**（Form は `Long categoryId`、Entity は `Category`）
3. **判断は Service、入出力は Repository、受付は Controller**（薄く保つ）

## セキュリティの肝

- パスワードは `BCryptPasswordEncoder` で **ハッシュ化**して保存（生は保存しない）
- 記録・カテゴリーは Service で **持ち主チェック**（他人のデータを触らせない）
- ログイン中ユーザーは **`@AuthenticationPrincipal`** から取得（URLのidを信用しない）
- **削除は POST**＋CSRFトークン（`th:action` で自動挿入）
