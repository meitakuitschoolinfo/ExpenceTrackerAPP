// このクラスが属するパッケージを宣言する（entity パッケージ＝DBテーブルに対応するクラスを置く場所）
package com.example.expensetracker.entity;

// DBカラムとフィールドの対応を細かく指定するためのアノテーションをインポートする
import jakarta.persistence.Column;
// 文字列をDBに保存するときの形式（序数 or 文字列）を指定するアノテーションをインポートする
import jakarta.persistence.Enumerated;
// enum を「文字列として保存する／序数で保存する」を選ぶための enum をインポートする
import jakarta.persistence.EnumType;
// このクラスが「JPAエンティティ」であることを示すアノテーションをインポートする
import jakarta.persistence.Entity;
// 関連先（User）を「いつ取りに行くか（即時/遅延）」を指定する enum をインポートする
import jakarta.persistence.FetchType;
// 主キーを「どう採番するか」を指定するためのアノテーションをインポートする
import jakarta.persistence.GeneratedValue;
// 主キー採番戦略の種類を表す enum をインポートする
import jakarta.persistence.GenerationType;
// 主キー（PRIMARY KEY）であることを示すアノテーションをインポートする
import jakarta.persistence.Id;
// 外部キー（FOREIGN KEY）のカラム名を指定するためのアノテーションをインポートする
import jakarta.persistence.JoinColumn;
// 「多対1」の関連を表すアノテーションをインポートする（Category 多 ─ 1 User）
import jakarta.persistence.ManyToOne;
// INSERT直前に自動で呼ばれるメソッドを指定するアノテーションをインポートする
import jakarta.persistence.PrePersist;
// UPDATE直前に自動で呼ばれるメソッドを指定するアノテーションをインポートする
import jakarta.persistence.PreUpdate;
// マッピング先のテーブル名を指定するためのアノテーションをインポートする
import jakarta.persistence.Table;

// 日付・時刻を扱う標準クラス LocalDateTime をインポートする
import java.time.LocalDateTime;

// 全フィールドの getter を自動生成する Lombok アノテーションをインポートする
import lombok.Getter;
// 引数なしコンストラクタを自動生成する Lombok アノテーションをインポートする（JPA仕様で必須）
import lombok.NoArgsConstructor;
// 全フィールドの setter を自動生成する Lombok アノテーションをインポートする
import lombok.Setter;

// このクラスが JPA エンティティであることを Spring に伝える
@Entity
// マッピング先テーブル名を "categories" に指定する（クラス名 Category と異なるので明示する）
@Table(name = "categories")
// 全フィールドの getter を自動生成する
@Getter
// 全フィールドの setter を自動生成する
@Setter
// JPA仕様で必要な「引数なしコンストラクタ」を自動生成する
@NoArgsConstructor
// categories テーブルの1行（=収支カテゴリー1件。例：食費・給与）を表すクラスを宣言する
public class Category {

    // このフィールドが主キーであることを JPA に伝える
    @Id
    // 主キーをDB側で自動採番させる（PostgreSQL の BIGSERIAL に対応）
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // DBカラム "id" に対応付ける
    @Column(name = "id")
    // カテゴリーID（自動採番のため、新規作成時は null のままでよい）
    private Long id;

    // 「Category 多 ─ 1 User」の関連を宣言し、必要になったときだけ User を取得する（LAZY）
    @ManyToOne(fetch = FetchType.LAZY)
    // 外部キーのカラム名を "user_id" に指定し、NOT NULL を強制する（カテゴリーは必ず誰かのもの）
    @JoinColumn(name = "user_id", nullable = false)
    // このカテゴリーの持ち主（users テーブルへの参照）。利用者ごとにカテゴリーを分けるために必要
    private User user;

    // この列挙型カラムを「序数(0,1)ではなく文字列('EXPENSE','INCOME')」でDBに保存させる
    // ※序数だと後で enum の並び順を変えた瞬間にデータが壊れるので、必ず STRING を選ぶ
    @Enumerated(EnumType.STRING)
    // DBカラム "type" に対応付け、NOT NULL かつ最大20文字を指定する
    @Column(name = "type", nullable = false, length = 20)
    // このカテゴリーが「支出用」か「収入用」かを表す（TransactionType.EXPENSE / INCOME）
    private TransactionType type;

    // DBカラム "label" に対応付け、NOT NULL かつ最大100文字を指定する
    @Column(name = "label", nullable = false, length = 100)
    // 画面に表示するカテゴリー名（例：食費・住居費・給与）
    private String label;

    // DBカラム "color" に対応付け、NOT NULL かつ最大20文字を指定する
    @Column(name = "color", nullable = false, length = 20)
    // 円グラフ等で使う表示色（例："#f87171"）。React版のカテゴリー color に相当
    private String color;

    // DBカラム "created_at" に対応付け、NOT NULL かつ「更新不可」にする
    @Column(name = "created_at", nullable = false, updatable = false)
    // レコード作成日時
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
