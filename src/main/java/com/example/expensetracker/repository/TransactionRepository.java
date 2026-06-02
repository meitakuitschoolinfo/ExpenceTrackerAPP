// このインターフェースが属するパッケージを宣言する（repository パッケージ＝DB操作を行う層）
package com.example.expensetracker.repository;

// このRepositoryが扱うEntity（Transaction）をインポートする
import com.example.expensetracker.entity.Transaction;
// 記録の持ち主を表す User をインポートする（ユーザー単位での絞り込みに使う）
import com.example.expensetracker.entity.User;
// 削除前チェック（このカテゴリーは何件使われているか）で使う Category をインポートする
import com.example.expensetracker.entity.Category;
// Spring Data JPA が提供する標準Repositoryインターフェース
import org.springframework.data.jpa.repository.JpaRepository;
// このインターフェースを「Repository役のBean」として明示するためのアノテーション
import org.springframework.stereotype.Repository;

// 「日付だけ」を扱う LocalDate をインポートする（月初〜月末の範囲検索で使う）
import java.time.LocalDate;
// 複数件をまとめて返すための List をインポートする
import java.util.List;

// このインターフェースが「transactions テーブル専用のDB操作窓口」であることを明示する
@Repository
// JpaRepository<Transaction, Long> → 扱うEntityは Transaction、主キーの型は Long
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 「指定ユーザーの、指定期間（start〜end）の記録を、取引日の新しい順→ID新しい順で取得する」検索メソッド
    //   SELECT * FROM transactions
    //   WHERE user_id = ? AND transaction_date BETWEEN ? AND ?
    //   ORDER BY transaction_date DESC, id DESC
    // 「今月の履歴」「今月の集計」「直近6ヶ月の推移」など、月で区切る処理すべてに使える
    // ※ Between は「以上・以下（両端を含む）」なので、月初と月末を渡せばその月だけが取れる
    List<Transaction> findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(
            User user, LocalDate start, LocalDate end);

    // 「このカテゴリーは何件の記録で使われているか？」を数える存在件数メソッド
    //   SELECT count(*) FROM transactions WHERE category_id = ?
    // カテゴリー削除前に「使われていたら消させない」判定（業務判定は Service）で使う
    long countByCategory(Category category);
}
