// このクラスが属するパッケージを宣言する（entity パッケージ＝DBテーブルに対応するクラスを置く場所）
package com.example.expensetracker.entity;

// DBカラムとフィールドの対応を細かく指定するためのアノテーションをインポートする
import jakarta.persistence.Column;
// このクラスが「JPAエンティティ（テーブル1行を表すクラス）」であることを示すアノテーションをインポートする
import jakarta.persistence.Entity;
// 主キーを「どう採番するか」を指定するためのアノテーションをインポートする
import jakarta.persistence.GeneratedValue;
// 主キー採番戦略の種類（IDENTITY/SEQUENCEなど）を表す enum をインポートする
import jakarta.persistence.GenerationType;
// 主キー（PRIMARY KEY）であることを示すアノテーションをインポートする
import jakarta.persistence.Id;
// INSERT直前に自動で呼ばれるメソッドを指定するアノテーションをインポートする
import jakarta.persistence.PrePersist;
// UPDATE直前に自動で呼ばれるメソッドを指定するアノテーションをインポートする
import jakarta.persistence.PreUpdate;
// マッピング先のテーブル名を指定するためのアノテーションをインポートする
import jakarta.persistence.Table;

// 日付・時刻を扱う標準クラス LocalDateTime をインポートする（created_at / updated_at で使用）
import java.time.LocalDateTime;

// 全フィールドの getter を自動生成する Lombok アノテーションをインポートする
import lombok.Getter;
// 引数なしコンストラクタを自動生成する Lombok アノテーションをインポートする（JPA仕様で必須）
import lombok.NoArgsConstructor;
// 全フィールドの setter を自動生成する Lombok アノテーションをインポートする
import lombok.Setter;

// このクラスが JPA エンティティであることを Spring に伝える（=DBテーブルと結びつくクラス）
@Entity
// マッピング先テーブル名を "users" に指定する（"user" は予約語になりがちなので複数形 users にする）
@Table(name = "users")
// 全フィールドの getter を自動生成する
@Getter
// 全フィールドの setter を自動生成する
@Setter
// JPA仕様で必要な「引数なしコンストラクタ」を自動生成する（JPAがリフレクションで生成するため必須）
@NoArgsConstructor
// users テーブルの1行（=利用者1名）を表すクラスを宣言する
public class User {

    // このフィールドが主キーであることを JPA に伝える
    @Id
    // 主キーをDB側で自動採番させる（PostgreSQL の BIGSERIAL に対応）
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // DBカラム "id" に対応付ける
    @Column(name = "id")
    // 利用者ID（自動採番のため、新規作成時は null のままでよい。だから long ではなく Long にする）
    private Long id;

    // DBカラム "name" に対応付け、NOT NULL かつ最大100文字を指定する
    @Column(name = "name", nullable = false, length = 100)
    // 画面に表示する利用者の表示名（React版の displayName に相当）
    private String name;

    // DBカラム "email" に対応付け、NOT NULL・UNIQUE・最大255文字を指定する（ログインIDとして使う）
    @Column(name = "email", nullable = false, unique = true, length = 255)
    // ログインID兼連絡先となるメールアドレス
    private String email;

    // DBカラム "password" に対応付け、NOT NULL かつ最大255文字を指定する（BCryptハッシュは約60文字）
    @Column(name = "password", nullable = false, length = 255)
    // 「BCryptで暗号化済みのハッシュ文字列」を入れる欄（★生のパスワードは絶対に入れない★）
    private String password;

    // DBカラム "created_at" に対応付け、NOT NULL かつ「更新不可」にする（登録時に一度だけ書き込む）
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
        // 更新日時にも現在時刻をセットする（INSERT 直後は作成日時と同じ値になる）
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
