// このクラスが属するパッケージを宣言する（exception パッケージ＝カスタム例外を置く場所）
package com.example.expensetracker.exception;

// HTTPステータスコードを表す enum をインポートする
import org.springframework.http.HttpStatus;
// 例外クラスに「HTTPレスポンスのステータス」を紐づけるアノテーションをインポートする
import org.springframework.web.bind.annotation.ResponseStatus;

// この例外がControllerの外まで到達したら、Springが自動でHTTP 404 (Not Found) を返すように指定する
@ResponseStatus(HttpStatus.NOT_FOUND)
// 「探したリソース（DBの1件）が見つからなかった」ことを表すカスタム実行時例外
// RuntimeException を継承する理由：チェック例外（Exception）だと呼び出し側に throws が伝染して汚れるため
public class ResourceNotFoundException extends RuntimeException {

    // メッセージ付きでスローできるようにするコンストラクタ
    public ResourceNotFoundException(String message) {
        // 親クラス（RuntimeException）にメッセージを渡す（ログやエラー画面に表示される）
        super(message);
    }
}
