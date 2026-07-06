# 単体テスト仕様書

| 項目 | 内容 | 項目 | 内容 |
| --- | --- | --- | --- |
| システム名 | Blog アプリケーション | 作成者 | （未記入） |
| サブシステム名 | ログイン機能 | 作成日 | （未記入） |
| 対象クラス | blog.ex.controller.UserLoginController | 実施者 | （未記入） |
| テスト方式 | Spring Boot Test + MockMvc + Mockito（Service をモック化） | - | - |

## テストケース一覧

| No | 分類 | テスト項目 | 検証内容 | 実施日 | 結果 | 備考・特記事項 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 表示テスト | "/user/login" へのGETリクエストを実行 | ビュー名が "login.html" であることを検証 |  | - | ログイン画面表示 |
| 2 | 正常系 | 正しい email("ake@test.com") と password("1234abcd") で "/user/login/process" へPOST | リダイレクト先URLが "/user/blog/list" であることを検証。ログイン成功時にセッションへ user 属性が格納されること |  | - | loginAccount が UserEntity を返す |
| 3 | 異常系 | 誤った email("test@test.com") と正しい password("1234abcd") で POST | status が 3xx リダイレクトであり、リダイレクト先が "/user/login" であることを検証。セッションの user 属性が null であること |  | - | loginAccount が null を返す |
| 4 | 異常系 | 正しい email("ake@test.com") と誤った password("12345678") で POST | リダイレクト先が "/user/login" であることを検証。セッションの user 属性が null であること |  | - | loginAccount が null を返す |
| 5 | 異常系 | 誤った email("test@test.com") と誤った password("12345678") で POST | status が 3xx リダイレクトであり、リダイレクト先が "/user/login" であることを検証。セッションの user 属性が null であること |  | - | email・password 両方誤り |
| 6 | 表示テスト | 初期表示の "/user/login" を実行し、セッションを確認 | ログイン画面の初期表示でセッションに user 属性が存在しない（入力欄が空白状態）ことを検証 |  | - | 初期表示の入力欄空白 |

## テストデータ

| No | 項目 | 値 | 説明 |
| --- | --- | --- | --- |
| 1 | userId | 1L | モック UserEntity のユーザーID |
| 2 | userName | Akemi | モック UserEntity のユーザー名 |
| 3 | email（正） | ake@test.com | 登録済みの正しいメールアドレス |
| 4 | password（正） | 1234abcd | 正しいパスワード |
| 5 | email（誤） | test@test.com | 未登録の誤ったメールアドレス |
| 6 | password（誤） | 12345678 | 誤ったパスワード |
| 7 | registerDate | LocalDateTime.now() | モック UserEntity の登録日時 |

### モック設定（前提条件）

| No | モック対象メソッド | 引数 | 戻り値 |
| --- | --- | --- | --- |
| 1 | userService.loginAccount | ("ake@test.com", "1234abcd") | userEntity（成功） |
| 2 | userService.loginAccount | ("ake@test.com", "12345678") | null（失敗） |
| 3 | userService.loginAccount | ("test@test.com", "1234abcd") | null（失敗） |
| 4 | userService.loginAccount | ("test@test.com", "12345678") | null（失敗） |
