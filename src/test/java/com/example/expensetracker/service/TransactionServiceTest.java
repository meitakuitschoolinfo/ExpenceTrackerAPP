// このテストクラスが属するパッケージを宣言する（テスト対象と同じ service パッケージに置く）
package com.example.expensetracker.service;

// 集計結果を運ぶ DTO（月合計）をインポートする
import com.example.expensetracker.dto.MonthlySummary;
// 集計結果を運ぶ DTO（カテゴリー内訳の1切れ）をインポートする
import com.example.expensetracker.dto.CategorySlice;
// 集計結果を運ぶ DTO（推移グラフの1ヶ月分）をインポートする
import com.example.expensetracker.dto.MonthlyTrendPoint;
// テストデータに使う Entity 群をインポートする
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.Transaction;
import com.example.expensetracker.entity.TransactionType;
import com.example.expensetracker.entity.User;
// 「見つからない／持ち主違い」のときに飛ぶ例外をインポートする
import com.example.expensetracker.exception.ResourceNotFoundException;
// 登録テストで使う入力フォームをインポートする
import com.example.expensetracker.form.TransactionForm;
// DB操作をモック化する対象の Repository をインポートする
import com.example.expensetracker.repository.TransactionRepository;

// 各テストの前に共通準備を走らせるためのアノテーションをインポートする
import org.junit.jupiter.api.BeforeEach;
// テストの表示名を日本語で付けるためのアノテーションをインポートする
import org.junit.jupiter.api.DisplayName;
// 1つのテストメソッドであることを示すアノテーションをインポートする
import org.junit.jupiter.api.Test;
// JUnit5 に「Mockito の機能（@Mock等）を使う」と伝える拡張をインポートする
import org.junit.jupiter.api.extension.ExtendWith;
// save() に渡された Transaction を「捕まえて」中身を検証するための道具をインポートする
import org.mockito.ArgumentCaptor;
// 「この依存はモックにする」印をインポートする
import org.mockito.InjectMocks;
import org.mockito.Mock;
// JUnit5 と Mockito を連携させる拡張クラスをインポートする
import org.mockito.junit.jupiter.MockitoExtension;

// 「日付だけ」を扱う LocalDate をインポートする
import java.time.LocalDate;
// 年月を扱う YearMonth をインポートする
import java.time.YearMonth;
// テスト用のリストを作るため List をインポートする
import java.util.List;
// 「該当なし」を表す Optional をインポートする（findById の戻り値で使う）
import java.util.Optional;

// JUnit5 の検証メソッド（assertEquals 等）を直接呼べるように import する
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
// Mockito のマッチャ／設定／検証メソッドを直接呼べるように import する
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// このテストクラスで Mockito の @Mock / @InjectMocks を有効にする
@ExtendWith(MockitoExtension.class)
// このテストクラスの説明
@DisplayName("TransactionService の単体テスト（集計ロジックと登録・削除）")
class TransactionServiceTest {

    // DB操作はモックに差し替える（本物のDBにはつながない）
    @Mock
    private TransactionRepository transactionRepository;
    // register が使う CategoryService もモックにする
    @Mock
    private CategoryService categoryService;

    // 上の2つのモックを「コンストラクタ経由で」差し込んだ、検証対象の本物の Service
    @InjectMocks
    private TransactionService transactionService;

    // テストで使い回す「持ち主」ユーザー
    private User user;

    // 各テストの前に共通のテストデータを用意する
    @BeforeEach
    void setUp() {
        // 持ち主ユーザーを組み立てる
        user = new User();
        user.setId(1L);
        user.setName("Akemi");
        user.setEmail("ake@test.com");
    }

    // ---- テスト用の小さな組み立てヘルパー（同じ生成処理を1か所にまとめる）----

    // 指定の種類・金額・カテゴリーを持つ Transaction を1件作るヘルパー
    private Transaction tx(TransactionType type, int amount, Category category) {
        Transaction t = new Transaction();        // 空の記録を作る
        t.setUser(user);                          // 持ち主を紐づける
        t.setType(type);                          // 支出/収入
        t.setAmount(amount);                      // 金額
        t.setCategory(category);                  // カテゴリー
        t.setTransactionDate(LocalDate.of(2026, 6, 10)); // 取引日（当月内）
        return t;                                 // 完成した記録を返す
    }

    // 指定の名前・色・種類を持つ Category を1件作るヘルパー
    private Category category(Long id, String label, TransactionType type) {
        Category c = new Category();   // 空のカテゴリーを作る
        c.setId(id);                   // ID
        c.setUser(user);               // 持ち主
        c.setLabel(label);             // 名前
        c.setColor("#000000");         // 色（テストでは中身は何でもよい）
        c.setType(type);               // 支出/収入
        return c;                      // 完成したカテゴリーを返す
    }

    // ============================================================
    //  集計：summarize（収入・支出・残高）
    // ============================================================
    @Test
    @DisplayName("summarize：収入と支出を仕分けして合計し、残高（収入−支出）を返す")
    void summarize_calculatesIncomeExpenseBalance() {
        // 食費カテゴリー（支出）を用意する
        Category food = category(10L, "食費", TransactionType.EXPENSE);
        // 給与カテゴリー（収入）を用意する
        Category salary = category(20L, "給与", TransactionType.INCOME);
        // その月の記録として「収入1000・支出300・支出200」を返すようにモックを仕込む
        when(transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(any(), any(), any()))
                .thenReturn(List.of(
                        tx(TransactionType.INCOME, 1000, salary),
                        tx(TransactionType.EXPENSE, 300, food),
                        tx(TransactionType.EXPENSE, 200, food)));

        // 集計を実行する
        MonthlySummary summary = transactionService.summarize(user, YearMonth.of(2026, 6));

        // 収入合計は 1000 のはず
        assertEquals(1000, summary.getIncome());
        // 支出合計は 300+200=500 のはず
        assertEquals(500, summary.getExpense());
        // 残高は 1000−500=500 のはず
        assertEquals(500, summary.getBalance());
    }

    // ============================================================
    //  集計：expenseBreakdown（カテゴリー別の内訳）
    // ============================================================
    @Test
    @DisplayName("expenseBreakdown：支出だけをカテゴリー別に合計し、割合付き・金額の多い順で返す")
    void expenseBreakdown_groupsByCategorySortedDesc() {
        // 食費（支出）と交通費（支出）のカテゴリーを用意する
        Category food = category(10L, "食費", TransactionType.EXPENSE);
        Category transport = category(11L, "交通費", TransactionType.EXPENSE);
        // 給与（収入）も混ぜる（内訳には含まれないことを確認するため）
        Category salary = category(20L, "給与", TransactionType.INCOME);
        // 食費は同じカテゴリーで2件（100+200=300）、交通費は1件（700）、収入1000は無視される想定
        when(transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(any(), any(), any()))
                .thenReturn(List.of(
                        tx(TransactionType.EXPENSE, 100, food),
                        tx(TransactionType.EXPENSE, 200, food),
                        tx(TransactionType.EXPENSE, 700, transport),
                        tx(TransactionType.INCOME, 1000, salary)));

        // 内訳集計を実行する
        List<CategorySlice> slices = transactionService.expenseBreakdown(user, YearMonth.of(2026, 6));

        // 支出カテゴリーは2種類（食費・交通費）なので2件のはず（収入は除外）
        assertEquals(2, slices.size());
        // 金額の多い順なので先頭は交通費（700円・70%）
        assertEquals("交通費", slices.get(0).getLabel());
        assertEquals(700, slices.get(0).getAmount());
        assertEquals(70, slices.get(0).getPercentage());
        // 2番目は食費（合計300円・30%）
        assertEquals("食費", slices.get(1).getLabel());
        assertEquals(300, slices.get(1).getAmount());
        assertEquals(30, slices.get(1).getPercentage());
    }

    // ============================================================
    //  集計：recentTrend（直近6ヶ月の推移）
    // ============================================================
    @Test
    @DisplayName("recentTrend：当月を含む直近6ヶ月分（古い順）を返し、月ラベルが正しい")
    void recentTrend_returnsSixMonthsOldestFirst() {
        // どの月の問い合わせでも空リスト（=その月は0円）を返すようにする（件数・ラベルの検証が目的）
        when(transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(any(), any(), any()))
                .thenReturn(List.of());

        // 2026年6月を基準に直近6ヶ月を集計する
        List<MonthlyTrendPoint> points = transactionService.recentTrend(user, YearMonth.of(2026, 6));

        // ちょうど6ヶ月分あるはず
        assertEquals(6, points.size());
        // 古い順なので先頭は5ヶ月前＝2026年1月＝"1月"
        assertEquals("1月", points.get(0).getLabel());
        // 末尾は当月＝2026年6月＝"6月"
        assertEquals("6月", points.get(5).getLabel());
        // 記録0件なので収入・支出はどちらも0
        assertEquals(0, points.get(5).getIncome());
        assertEquals(0, points.get(5).getExpense());
    }

    // ============================================================
    //  変換：parseMonthOrCurrent（文字列→YearMonth）
    // ============================================================
    @Test
    @DisplayName("parseMonthOrCurrent：正しい文字列は変換し、null や不正値は当月にフォールバックする")
    void parseMonthOrCurrent_handlesValidNullAndInvalid() {
        // "2026-06" はそのまま 2026年6月に変換される
        assertEquals(YearMonth.of(2026, 6), transactionService.parseMonthOrCurrent("2026-06"));
        // null は当月にフォールバックする
        assertEquals(YearMonth.now(), transactionService.parseMonthOrCurrent(null));
        // 不正な文字列も当月にフォールバックする（アプリが落ちない）
        assertEquals(YearMonth.now(), transactionService.parseMonthOrCurrent("not-a-month"));
    }

    // ============================================================
    //  登録：register（正常系）
    // ============================================================
    @Test
    @DisplayName("register：正常時は Form を Entity に詰めて保存し、保存結果を返す")
    void register_valid_savesTransaction() {
        // 入力フォームを組み立てる（支出・食費・1000円）
        TransactionForm form = new TransactionForm();
        form.setType(TransactionType.EXPENSE);
        form.setCategoryId(10L);
        form.setAmount(1000);
        form.setTransactionDate(LocalDate.of(2026, 6, 15));
        form.setMemo("スーパー");
        // categoryId=10 を Category に変換する処理は、種類が一致する食費カテゴリーを返すようにする
        Category food = category(10L, "食費", TransactionType.EXPENSE);
        when(categoryService.findOwnedById(user, 10L)).thenReturn(food);
        // save は「渡されたものをそのまま返す」ようにする（戻り値の検証用）
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // 登録を実行する
        Transaction saved = transactionService.register(user, form);

        // save に渡された Transaction を捕まえて、中身が Form どおりかを検証する
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction passed = captor.getValue();
        // 持ち主はログイン中ユーザーになっているはず（★Formの値ではなく必ずuser★）
        assertSame(user, passed.getUser());
        // 種類・カテゴリー・金額・メモが Form どおりに詰められているはず
        assertEquals(TransactionType.EXPENSE, passed.getType());
        assertSame(food, passed.getCategory());
        assertEquals(1000, passed.getAmount());
        assertEquals("スーパー", passed.getMemo());
        // 戻り値は save の結果（=詰めた Transaction）であるはず
        assertSame(passed, saved);
    }

    // ============================================================
    //  登録：register（異常系：種類の不一致）
    // ============================================================
    @Test
    @DisplayName("register：記録の種類とカテゴリーの種類が違うと例外を投げ、保存しない")
    void register_typeMismatch_throwsAndDoesNotSave() {
        // フォームは「支出」を選んでいる
        TransactionForm form = new TransactionForm();
        form.setType(TransactionType.EXPENSE);
        form.setCategoryId(20L);
        form.setAmount(1000);
        form.setTransactionDate(LocalDate.of(2026, 6, 15));
        // ところが categoryId=20 は「収入」カテゴリー（給与）だった、という食い違いを作る
        Category salary = category(20L, "給与", TransactionType.INCOME);
        when(categoryService.findOwnedById(user, 20L)).thenReturn(salary);

        // 種類が食い違うので IllegalArgumentException が飛ぶことを検証する
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.register(user, form));
        // 例外で弾かれたので save は1度も呼ばれていないことを検証する
        verify(transactionRepository, never()).save(any());
    }

    // ============================================================
    //  削除：delete（正常系）
    // ============================================================
    @Test
    @DisplayName("delete：自分の記録なら削除を実行する")
    void delete_ownRecord_deletes() {
        // 自分（user）の記録を用意する
        Transaction own = tx(TransactionType.EXPENSE, 500, category(10L, "食費", TransactionType.EXPENSE));
        own.setId(5L);
        // findById(5) でその記録が返るようにする
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(own));

        // 削除を実行する
        transactionService.delete(user, 5L);

        // Repository の delete がその記録に対して呼ばれたことを検証する
        verify(transactionRepository).delete(own);
    }

    // ============================================================
    //  削除：delete（異常系：他人の記録）
    // ============================================================
    @Test
    @DisplayName("delete：他人の記録は見つからない扱いで例外を投げ、削除しない")
    void delete_othersRecord_throwsAndDoesNotDelete() {
        // 別のユーザー（id=2）を持ち主にした記録を用意する
        User other = new User();
        other.setId(2L);
        Transaction othersTx = new Transaction();
        othersTx.setId(5L);
        othersTx.setUser(other);
        // findById(5) でその「他人の記録」が返るようにする
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(othersTx));

        // 持ち主が違うので ResourceNotFoundException が飛ぶことを検証する
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.delete(user, 5L));
        // 削除（delete）は1度も呼ばれていないことを検証する
        verify(transactionRepository, never()).delete(any());
    }

    // ============================================================
    //  削除：delete（異常系：そもそも存在しない）
    // ============================================================
    @Test
    @DisplayName("delete：存在しないIDは例外を投げる")
    void delete_notFound_throws() {
        // findById(99) は空（該当なし）を返すようにする
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        // 見つからないので ResourceNotFoundException が飛ぶことを検証する
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.delete(user, 99L));
    }
}
