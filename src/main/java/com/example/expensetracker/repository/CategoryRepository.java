// このインターフェースが属するパッケージを宣言する（repository パッケージ＝DB操作を行う層）
package com.example.expensetracker.repository;

// このRepositoryが扱うEntity（Category）をインポートする
import com.example.expensetracker.entity.Category;
// カテゴリーの持ち主を表す User をインポートする（ユーザー単位での絞り込みに使う）
import com.example.expensetracker.entity.User;
// 収支の種類（EXPENSE/INCOME）でも絞り込むため TransactionType をインポートする
import com.example.expensetracker.entity.TransactionType;
// Spring Data JPA が提供する標準Repositoryインターフェース
import org.springframework.data.jpa.repository.JpaRepository;
// このインターフェースを「Repository役のBean」として明示するためのアノテーション
import org.springframework.stereotype.Repository;

// 複数件をまとめて返すための List をインポートする
import java.util.List;

// このインターフェースが「categories テーブル専用のDB操作窓口」であることを明示する
@Repository
// JpaRepository<Category, Long> → 扱うEntityは Category、主キーの型は Long
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 「指定ユーザーのカテゴリーを全部、ID昇順で取得する」検索メソッド
    //   SELECT * FROM categories WHERE user_id = ? ORDER BY id ASC
    // が自動生成される。ユーザーごとにデータを分けるため、必ず user で絞る
    List<Category> findByUserOrderByIdAsc(User user);

    // 「指定ユーザーの、指定タイプ（支出 or 収入）のカテゴリーを、ID昇順で取得する」検索メソッド
    //   SELECT * FROM categories WHERE user_id = ? AND type = ? ORDER BY id ASC
    // 記録画面のカテゴリー選択肢（支出だけ／収入だけ）を出すために使う
    List<Category> findByUserAndTypeOrderByIdAsc(User user, TransactionType type);

    // 「同じユーザー内に、同じタイプ・同じ名前のカテゴリーが既に存在するか？」の存在チェック
    //   SELECT count(*) > 0 FROM categories WHERE user_id = ? AND type = ? AND label = ?
    // カテゴリー登録時の重複チェック（業務判定は Service で使う）
    boolean existsByUserAndTypeAndLabel(User user, TransactionType type, String label);
}
