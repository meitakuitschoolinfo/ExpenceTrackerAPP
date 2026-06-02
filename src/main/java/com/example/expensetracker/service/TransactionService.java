// このクラスが属するパッケージを宣言する（service パッケージ＝業務ロジック層）
package com.example.expensetracker.service;

// 集計結果を運ぶDTO（月合計）をインポートする
import com.example.expensetracker.dto.MonthlySummary;
// 集計結果を運ぶDTO（カテゴリー内訳の1切れ）をインポートする
import com.example.expensetracker.dto.CategorySlice;
// 集計結果を運ぶDTO（推移グラフの1ヶ月分）をインポートする
import com.example.expensetracker.dto.MonthlyTrendPoint;
// このサービスが扱う Entity（Transaction）をインポートする
import com.example.expensetracker.entity.Transaction;
// 記録の持ち主・カテゴリーの持ち主を表す User をインポートする
import com.example.expensetracker.entity.User;
// 記録が属する Category をインポートする
import com.example.expensetracker.entity.Category;
// 収支の種類（EXPENSE/INCOME）をインポートする
import com.example.expensetracker.entity.TransactionType;
// 「見つからなかった」場合に投げるカスタム例外をインポートする
import com.example.expensetracker.exception.ResourceNotFoundException;
// 記録登録画面の入力を表す Form クラスをインポートする
import com.example.expensetracker.form.TransactionForm;
// Transaction の CRUD を担う Repository をインポートする
import com.example.expensetracker.repository.TransactionRepository;

// Spring の Service Bean として認識させるためのアノテーション
import org.springframework.stereotype.Service;
// トランザクションを張るためのアノテーション
import org.springframework.transaction.annotation.Transactional;

// final フィールドを引数に取るコンストラクタを Lombok に自動生成させる
import lombok.RequiredArgsConstructor;

// 「日付だけ」を扱う LocalDate をインポートする
import java.time.LocalDate;
// 「年月（YYYY-MM）」を型として扱う YearMonth をインポートする（月初・月末計算が楽になる）
import java.time.YearMonth;
// "yyyy-MM" のような日付フォーマットを定義する DateTimeFormatter をインポートする
import java.time.format.DateTimeFormatter;
// 連結された順序付きMap（カテゴリー集計の挿入順を保つ）をインポートする
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// このクラスを Spring の DI コンテナに登録し、Service 層として扱わせる
@Service
// クラスレベルで「読み取り専用トランザクション」を既定値にする
@Transactional(readOnly = true)
// 依存先（final フィールド）を引数に取るコンストラクタを Lombok に生成させる
@RequiredArgsConstructor
// 収支の記録と、その集計（ダッシュボード）に関する業務ロジックを担当するサービスクラス
public class TransactionService {

    // Transaction の CRUD を行うリポジトリ
    private final TransactionRepository transactionRepository;
    // 登録時に categoryId → Category へ変換し、持ち主チェックも行うため CategoryService を使う
    private final CategoryService categoryService;

    // 画面の "yyyy-MM" 文字列と YearMonth を相互変換するためのフォーマッタ（使い回すので定数化）
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    // 指定ユーザーの「指定月」の記録一覧を、新しい順で取得する（履歴画面で使う）
    public List<Transaction> findByMonth(User user, YearMonth month) {
        // その月の1日（月初）を計算する
        LocalDate start = month.atDay(1);
        // その月の末日（月末）を計算する
        LocalDate end = month.atEndOfMonth();
        // 月初〜月末の範囲で取得する（Between は両端を含む）
        return transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(user, start, end);
    }

    // 指定月の「収入合計・支出合計・残高」を集計して返す
    public MonthlySummary summarize(User user, YearMonth month) {
        // その月の記録を全部取ってくる
        List<Transaction> list = findByMonth(user, month);
        // 収入の合計を入れる変数
        int income = 0;
        // 支出の合計を入れる変数
        int expense = 0;
        // 1件ずつ見て、種類に応じて足し込む
        for (Transaction tx : list) {
            // 収入なら income に加算する
            if (tx.getType() == TransactionType.INCOME) {
                income += tx.getAmount();
            // 支出なら expense に加算する
            } else if (tx.getType() == TransactionType.EXPENSE) {
                expense += tx.getAmount();
            }
        }
        // 収入・支出・残高（収入−支出）をDTOに詰めて返す
        return new MonthlySummary(income, expense, income - expense);
    }

    // 指定月の「支出の内訳（カテゴリー別合計）」を、金額の多い順で返す（円グラフ用データ）
    public List<CategorySlice> expenseBreakdown(User user, YearMonth month) {
        // その月の記録を取得する
        List<Transaction> list = findByMonth(user, month);
        // カテゴリーごとの合計を貯める入れ物（挿入順を保つ LinkedHashMap）
        Map<Category, Integer> grouped = new LinkedHashMap<>();
        // 支出合計（割合計算の分母に使う）
        int totalExpense = 0;
        // 1件ずつ見て、支出だけをカテゴリー別に合計する
        for (Transaction tx : list) {
            // 収入は内訳に含めないのでスキップする
            if (tx.getType() != TransactionType.EXPENSE) {
                continue;
            }
            // このカテゴリーの現在の合計を取り出す（まだ無ければ0）
            int current = grouped.getOrDefault(tx.getCategory(), 0);
            // 金額を足して入れ直す
            grouped.put(tx.getCategory(), current + tx.getAmount());
            // 全体の支出合計にも足す
            totalExpense += tx.getAmount();
        }
        // 集計結果を CategorySlice のリストに変換する
        List<CategorySlice> slices = new ArrayList<>();
        // Map の各エントリ（カテゴリー → 合計）を1切れずつ詰める
        for (Map.Entry<Category, Integer> entry : grouped.entrySet()) {
            // カテゴリーEntity
            Category category = entry.getKey();
            // そのカテゴリーの合計金額
            int amount = entry.getValue();
            // 全体に占める割合（％）。分母が0のときは0にする（ゼロ除算を避ける）
            int percentage = totalExpense == 0 ? 0 : (int) Math.round(amount * 100.0 / totalExpense);
            // ラベル・色・金額・割合を詰めてリストに追加する
            slices.add(new CategorySlice(category.getLabel(), category.getColor(), amount, percentage));
        }
        // 金額の多い順（降順）に並び替える（比較は b と a を入れ替えて降順にする）
        slices.sort((a, b) -> Integer.compare(b.getAmount(), a.getAmount()));
        // 並び替えたリストを返す
        return slices;
    }

    // 「指定月を含む直近6ヶ月」の収入・支出推移を返す（棒グラフ用データ）
    public List<MonthlyTrendPoint> recentTrend(User user, YearMonth current) {
        // 結果を入れるリスト
        List<MonthlyTrendPoint> points = new ArrayList<>();
        // 5ヶ月前 → 当月、の順に6回ループする（古い月から新しい月へ）
        for (int i = 5; i >= 0; i--) {
            // i ヶ月前の YearMonth を求める
            YearMonth ym = current.minusMonths(i);
            // その月の合計を集計する（既存メソッドを再利用する）
            MonthlySummary s = summarize(user, ym);
            // 月ラベル（例："6月"）を作る（getMonthValue は 1〜12 を返す）
            String label = ym.getMonthValue() + "月";
            // ラベル・収入・支出を詰めてリストに追加する
            points.add(new MonthlyTrendPoint(label, s.getIncome(), s.getExpense()));
        }
        // 6ヶ月分の推移を返す
        return points;
    }

    // 画面のドロップダウン用に「選べる月の一覧（直近12ヶ月）」を新しい順で返す
    public List<String> availableMonths(YearMonth current) {
        // 結果リスト
        List<String> months = new ArrayList<>();
        // 当月 → 11ヶ月前、の順に12個作る
        for (int i = 0; i < 12; i++) {
            // i ヶ月前を "yyyy-MM" の文字列にして追加する
            months.add(current.minusMonths(i).format(MONTH_FORMAT));
        }
        // 新しい月が先頭のリストを返す
        return months;
    }

    // 画面から来た "yyyy-MM" 文字列を YearMonth に変換する（null や不正値なら当月にフォールバックする）
    public YearMonth parseMonthOrCurrent(String monthStr) {
        // パラメータが無ければ当月を返す
        if (monthStr == null || monthStr.isBlank()) {
            return YearMonth.now();
        }
        // "2026-06" のような文字列を YearMonth に変換する。失敗したら当月にする
        try {
            return YearMonth.parse(monthStr, MONTH_FORMAT);
        } catch (Exception e) {
            // 不正な値（手で URL を書き換えた等）でもアプリが落ちないように当月へフォールバック
            return YearMonth.now();
        }
    }

    // 新規登録：書き込み系なのでデフォルトの readOnly を上書きする
    @Transactional
    public Transaction register(User user, TransactionForm form) {
        // ★最重要★ Form は categoryId(Long) を持つ → ここで Category Entity に変換する
        //   同時に「自分のカテゴリーか？」も CategoryService がチェックしてくれる
        Category category = categoryService.findOwnedById(user, form.getCategoryId());
        // 念のため「記録の種類」と「カテゴリーの種類」が一致しているか確認する
        //   （支出の記録に収入カテゴリーが紐づくような不整合を防ぐ）
        if (category.getType() != form.getType()) {
            throw new IllegalArgumentException("カテゴリーの種類が記録の種類と一致しません");
        }
        // 空の Entity を生成する
        Transaction tx = new Transaction();
        // 持ち主を紐づける（★必ずログイン中ユーザー★）
        tx.setUser(user);
        // 種類（支出/収入）をセットする
        tx.setType(form.getType());
        // 取引日をセットする
        tx.setTransactionDate(form.getTransactionDate());
        // Long → Category に変換した結果をセットする
        tx.setCategory(category);
        // 金額をセットする
        tx.setAmount(form.getAmount());
        // メモをセットする（任意なので null でもよい）
        tx.setMemo(form.getMemo());
        // INSERT 文が発行される
        return transactionRepository.save(tx);
    }

    // 削除：書き込み系なので @Transactional を明示する
    @Transactional
    public void delete(User user, Long id) {
        // 対象の記録を取得する（無ければ例外）
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("記録が見つかりません: id=" + id));
        // ★セキュリティ★ 自分の記録でなければ削除させない（他人のデータを守る）
        if (!tx.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("記録が見つかりません: id=" + id);
        }
        // DELETE 文を発行する
        transactionRepository.delete(tx);
    }
}
