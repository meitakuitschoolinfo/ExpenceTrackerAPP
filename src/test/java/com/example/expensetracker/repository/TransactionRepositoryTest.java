// このテストクラスが属するパッケージを宣言する（テスト対象と同じ repository パッケージに置く）
package com.example.expensetracker.repository;

// テストデータに使う Entity 群をインポートする
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.Transaction;
import com.example.expensetracker.entity.TransactionType;
import com.example.expensetracker.entity.User;

// 各テストの前に共通準備を走らせるためのアノテーションをインポートする
import org.junit.jupiter.api.BeforeEach;
// テストの表示名を日本語で付けるためのアノテーションをインポートする
import org.junit.jupiter.api.DisplayName;
// 1つのテストメソッドであることを示すアノテーションをインポートする
import org.junit.jupiter.api.Test;
// Springが用意したBean（Repository / TestEntityManager）を注入してもらうためのアノテーションをインポートする
import org.springframework.beans.factory.annotation.Autowired;
// 「JPA（DB操作層）だけ」を対象にした軽量テストを起動するアノテーション（Boot 4.0の新パッケージ）をインポートする
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
// テスト用DB（H2）の差し替え方を制御するアノテーション（Boot 4.0の新パッケージ）をインポートする
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
// テスト内で「DBに直接データを入れる」ための補助クラス（Boot 4.0の新パッケージ）をインポートする
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

// 「日付だけ」を扱う LocalDate をインポートする
import java.time.LocalDate;
// 検索結果を受け取る List をインポートする
import java.util.List;

// JUnit5 の検証メソッドを直接呼べるように import する
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TransactionRepository を本物のDB（テスト用H2）相手に検証する軽量テスト
@DataJpaTest
// @DataJpaTest は標準だと組み込みDBへ勝手に差し替えるが、ここでは application.properties に書いた
// H2 設定をそのまま使いたいので NONE（=差し替えない）にする
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// このテストクラスの説明
@DisplayName("TransactionRepository の単体テスト（月範囲検索とカテゴリー使用件数）")
class TransactionRepositoryTest {

    // テスト対象の Repository を注入してもらう（@DataJpaTest が H2 につないだ本物のBean）
    @Autowired
    private TransactionRepository transactionRepository;

    // テストデータをDBへ入れたり、IDを採番させたりするための補助
    @Autowired
    private TestEntityManager entityManager;

    // テストで使い回す「持ち主」ユーザーとカテゴリー
    private User user;
    private Category food;
    private Category transport;

    // 各テストの前に、ユーザー・カテゴリー・記録を実際にH2へ保存する
    @BeforeEach
    void setUp() {
        // 持ち主ユーザーを作って保存する（FKに使うので先に永続化して採番させる）
        user = new User();
        user.setName("Akemi");
        user.setEmail("ake@test.com");
        user.setPassword("hashed"); // NOT NULL カラムなので何か入れておく
        entityManager.persist(user);

        // 食費カテゴリー（支出）を保存する
        food = newCategory("食費", TransactionType.EXPENSE);
        entityManager.persist(food);
        // 交通費カテゴリー（支出）を保存する
        transport = newCategory("交通費", TransactionType.EXPENSE);
        entityManager.persist(transport);

        // ★6月の記録3件★（6/10 食費300／6/20 交通費700／6/20 食費200）を保存する
        // ※6/20 を2件入れているのは「同じ日のときID降順になるか」を確かめるため
        entityManager.persist(newTx(LocalDate.of(2026, 6, 10), 300, food));
        entityManager.persist(newTx(LocalDate.of(2026, 6, 20), 700, transport));
        entityManager.persist(newTx(LocalDate.of(2026, 6, 20), 200, food));
        // ★5月の記録1件★（5/15 食費999）。6月検索では除外されるべきデータ
        entityManager.persist(newTx(LocalDate.of(2026, 5, 15), 999, food));

        // ここまでの INSERT をDBへ反映し、永続化コンテキストを空にする（次の検索が本物のSELECTになる）
        entityManager.flush();
        entityManager.clear();
    }

    // カテゴリーを1件組み立てるヘルパー
    private Category newCategory(String label, TransactionType type) {
        Category c = new Category();
        c.setUser(user);       // 持ち主
        c.setLabel(label);     // 名前
        c.setColor("#000000"); // 色
        c.setType(type);       // 支出/収入
        return c;
    }

    // 記録を1件組み立てるヘルパー
    private Transaction newTx(LocalDate date, int amount, Category category) {
        Transaction t = new Transaction();
        t.setUser(user);              // 持ち主
        t.setType(category.getType()); // カテゴリーの種類に合わせる
        t.setTransactionDate(date);   // 取引日
        t.setAmount(amount);          // 金額
        t.setCategory(category);      // カテゴリー
        return t;
    }

    // テスト1：月範囲検索が「その月だけ」を「日付降順→ID降順」で返すことを検証する
    @Test
    @DisplayName("findBy...Between...：6月の記録だけを日付降順・同日はID降順で返す")
    void findByMonth_returnsOnlyThatMonthOrdered() {
        // 2026年6月の月初〜月末で検索する
        List<Transaction> result = transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(
                        user, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        // 5月分(999)は除外され、6月の3件だけが返るはず
        assertEquals(3, result.size());
        // 日付降順なので、先頭2件は新しい日付の 6/20 のはず
        assertEquals(LocalDate.of(2026, 6, 20), result.get(0).getTransactionDate());
        assertEquals(LocalDate.of(2026, 6, 20), result.get(1).getTransactionDate());
        // 最後は古い日付の 6/10 のはず
        assertEquals(LocalDate.of(2026, 6, 10), result.get(2).getTransactionDate());
        // 同じ 6/20 の2件は「ID降順」なので、先頭のIDの方が大きいはず（後から入れた方が先頭）
        assertTrue(result.get(0).getId() > result.get(1).getId());
        // 念のため、5月分(999円)が混ざっていないことを確認する
        assertTrue(result.stream().noneMatch(t -> t.getAmount() == 999));
    }

    // テスト2：カテゴリー使用件数を正しく数えることを検証する（カテゴリー削除前チェックで使う）
    @Test
    @DisplayName("countByCategory：そのカテゴリーを使う記録の件数を（月に関係なく）数える")
    void countByCategory_countsAllUsages() {
        // 食費は 6/10・6/20・5/15 の計3件で使われている
        assertEquals(3, transactionRepository.countByCategory(food));
        // 交通費は 6/20 の1件だけで使われている
        assertEquals(1, transactionRepository.countByCategory(transport));
    }
}
