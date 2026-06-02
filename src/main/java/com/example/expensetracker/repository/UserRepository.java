// このインターフェースが属するパッケージを宣言する（repository パッケージ＝DB操作を行う層）
package com.example.expensetracker.repository;

// このRepositoryが扱うEntity（User＝利用者）をインポートする
import com.example.expensetracker.entity.User;
// Spring Data JPA が提供する標準Repositoryインターフェース
import org.springframework.data.jpa.repository.JpaRepository;
// このインターフェースを「Repository役のBean」として明示するためのアノテーション
import org.springframework.stereotype.Repository;

// 「値が存在するかしないか」を型で表現する Optional をインポートする（findByEmail の戻り値で使う）
import java.util.Optional;

// このインターフェースが「users テーブル専用のDB操作窓口」であることを明示する
@Repository
// JpaRepository<User, Long> → 第1型引数=扱うEntity、第2型引数=主キー(@Id)の型
// これだけで save / findById / findAll / deleteById などの基本CRUDが自動で使える
public interface UserRepository extends JpaRepository<User, Long> {

    // 「メールアドレスから利用者を1件取得する」検索メソッド
    // メソッド名 "findByEmail" を Spring Data JPA が解析して
    //   SELECT * FROM users WHERE email = ?
    // 相当のSQLを自動生成する（メソッド名規約）
    // 戻り値が Optional<User> なのは「該当ユーザーが存在しない可能性がある」ため
    Optional<User> findByEmail(String email);

    // 「指定された email の利用者が既に存在するか？」を boolean で返す存在チェック専用メソッド
    // 新規登録時の重複チェック用（DBの UNIQUE 制約と組み合わせて「二段構え」で使う）
    boolean existsByEmail(String email);
}
