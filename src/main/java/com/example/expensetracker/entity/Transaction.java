// このクラスが属するパッケージを宣言する（entity パッケージ＝DBテーブルに対応するクラスを置く場所）
package com.example.expensetracker.entity;

// DBカラムとフィールドの対応を細かく指定するためのアノテーションをインポートする
import jakarta.persistence.Column;
// enum をDBに保存するときの形式を指定するアノテーションをインポートする
import jakarta.persistence.Enumerated;
// enum を「文字列として保存する」を選ぶための enum をインポートする
import jakarta.persistence.EnumType;
// このクラスが「JPAエンティティ」であることを示すアノテーションをインポートする
import jakarta.persistence.Entity;
// 関連先を「いつ取りに行くか（即時/遅延）」を指定する enum をインポートする
import jakarta.persistence.FetchType;
// 主キーを「どう採番するか」を指定するためのアノテーションをインポートする
import jakarta.persistence.GeneratedValue;
// 主キー採番戦略の種類を表す enum をインポートする
import jakarta.persistence.GenerationType;
// 主キー（PRIMARY KEY）であることを示すアノテーションをインポートする
import jakarta.persistence.Id;
// 外部キー（FOREIGN KEY）のカラム名を指定するためのアノテーションをインポートする
import jakarta.persistence.JoinColumn;
// 「多対1」の関連を表すアノテーションをインポートする
import jakarta.persistence.ManyToOne;
// INSERT直前に自動で呼ばれるメソッドを指定するアノテーションをインポートする
import jakarta.persistence.PrePersist;
// UPDATE直前に自動で呼ばれるメソッドを指定するアノテーションをインポートする
import jakarta.persistence.PreUpdate;
// マッピング先のテーブル名を指定するためのアノテーションをインポートする
import jakarta.persistence.Table;

// 「日付だけ（時刻なし）」を扱う標準クラス LocalDate をインポートする（取引日に使用）
import java.time.LocalDate;
// 日付＋時刻を扱う標準クラス LocalDateTime をインポートする（created_at / updated_at で使用）
import java.time.LocalDateTime;

// 全フィールドの getter を自動生成する Lombok アノテーションをインポートする
import lombok.Getter;
// 引数なしコンストラクタを自動生成する Lombok アノテーションをインポートする（JPA仕様で必須）
import lombok.NoArgsConstructor;
// 全フィールドの setter を自動生成する Lombok アノテーションをインポートする
import lombok.Setter;

// このクラスが JPA エンティティであることを Spring に伝える
@Entity
// マッピング先テーブル名を "transactions" に指定する
@Table(name = "transactions")
// 全フィールドの getter を自動生成する
@Getter
// 全フィールドの setter を自動生成する
@Setter
// JPA仕様で必要な「引数なしコンストラクタ」を自動生成する
@NoArgsConstructor
// transactions テーブルの1行（=収支の記録1件）を表すクラスを宣言する
public class Transaction {

    // このフィールドが主キーであることを JPA に伝える
    @Id
    // 主キーをDB側で自動採番させる（PostgreSQL の BIGSERIAL に対応）
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // DBカラム "id" に対応付ける
    @Column(name = "id")
    // 取引ID（自動採番のため、新規作成時は null のままでよい）
    private Long id;

    // 「Transaction 多 ─ 1 User」の関連を宣言する（LAZY＝必要時だけ取得）
    @ManyToOne(fetch = FetchType.LAZY)
    // 外部キーのカラム名を "user_id" に指定し、NOT NULL を強制する（記録は必ず誰かのもの）
    @JoinColumn(name = "user_id", nullable = false)
    // この記録の持ち主（users テーブルへの参照）
    private User user;

    // enum を文字列でDBに保存させる（'EXPENSE' / 'INCOME'）
    @Enumerated(EnumType.STRING)
    // DBカラム "type" に対応付け、NOT NULL かつ最大20文字を指定する
    @Column(name = "type", nullable = false, length = 20)
    // この記録が「支出」か「収入」かを表す
    private TransactionType type;

    // DBカラム "transaction_date" に対応付け、NOT NULL を指定する
    @Column(name = "transaction_date", nullable = false)
    // 取引が発生した日付（時刻は持たないので LocalDate を使う）
    private LocalDate transactionDate;

    // 「Transaction 多 ─ 1 Category」の関連を宣言する（LAZY）
    @ManyToOne(fetch = FetchType.LAZY)
    // 外部キーのカラム名を "category_id" に指定し、NOT NULL を強制する
    @JoinColumn(name = "category_id", nullable = false)
    // この記録が属するカテゴリー（categories テーブルへの参照）
    private Category category;

    // DBカラム "amount" に対応付け、NOT NULL を指定する
    @Column(name = "amount", nullable = false)
    // 金額（円。日本円は小数を扱わないので Integer を使う。null可否のため int ではなく Integer）
    private Integer amount;

    // DBカラム "memo" に対応付け、NULL許可かつ最大255文字を指定する
    @Column(name = "memo", length = 255)
    // メモ（任意項目。例：「スーパーでの買い物」）。NOT NULL にしない
    private String memo;

    // DBカラム "created_at" に対応付け、NOT NULL かつ「更新不可」にする
    @Column(name = "created_at", nullable = false, updatable = false)
    // レコード作成日時（システムが自動で入れる。一覧の並び順にも使う）
    private LocalDateTime createdAt;

    // DBカラム "updated_at" に対応付け、NOT NULL を指定する
    @Column(name = "updated_at", nullable = false)
    // レコード最終更新日時
    private LocalDateTime updatedAt;

    // INSERT 直前に JPA から自動的に呼び出されるメソッドであることを宣言する
    @PrePersist
    // 新規保存（INSERT）の直前に created_at / updated_at をセットするメソッド
    protected void onCreate() {
        // 現在時刻を1回だけ取得して、両カラムで同じ値を使うようにする
        LocalDateTime now = LocalDateTime.now();
        // 作成日時に現在時刻をセットする
        this.createdAt = now;
        // 更新日時にも現在時刻をセットする
        this.updatedAt = now;
    }

    // UPDATE 直前に JPA から自動的に呼び出されるメソッドであることを宣言する
    @PreUpdate
    // 更新（UPDATE）の直前に updated_at だけを現在時刻で上書きするメソッド
    protected void onUpdate() {
        // 更新日時を「いま」に書き換える（作成日時は触らない）
        this.updatedAt = LocalDateTime.now();
    }
}
