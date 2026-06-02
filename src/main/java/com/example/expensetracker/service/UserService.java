// このクラスが属するパッケージを宣言する（service パッケージ＝業務ロジック層）
package com.example.expensetracker.service;

// このサービスが扱う Entity（User）をインポートする
import com.example.expensetracker.entity.User;
// 新規登録時に「初期カテゴリー」を作るため Category をインポートする
import com.example.expensetracker.entity.Category;
// 収支の種類（EXPENSE/INCOME）をインポートする
import com.example.expensetracker.entity.TransactionType;
// 「探したけど見つからなかった」場合に投げるカスタム例外をインポートする
import com.example.expensetracker.exception.ResourceNotFoundException;
// 新規登録画面の入力を表す Form クラスをインポートする
import com.example.expensetracker.form.UserRegisterForm;
// User の CRUD を担う Repository をインポートする
import com.example.expensetracker.repository.UserRepository;
// 初期カテゴリーを保存するため CategoryRepository をインポートする
import com.example.expensetracker.repository.CategoryRepository;

// Spring Security に「認証時のユーザー情報の型」として返す UserDetails をインポートする
import org.springframework.security.core.userdetails.UserDetails;
// Spring Security の「ユーザー読み込み窓口」インターフェース
import org.springframework.security.core.userdetails.UserDetailsService;
// 「ユーザーが見つからなかった」ことを示す Security 用の標準例外
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// パスワードのハッシュ化／検証を担う Spring Security のインターフェース
import org.springframework.security.crypto.password.PasswordEncoder;
// このクラスを Spring の Service Bean として認識させるためのアノテーション
import org.springframework.stereotype.Service;
// メソッド／クラス単位でトランザクションを張るためのアノテーション
import org.springframework.transaction.annotation.Transactional;

// final フィールドを引数に取るコンストラクタを Lombok に自動生成させる（コンストラクタインジェクション用）
import lombok.RequiredArgsConstructor;

// このクラスを Spring の DI コンテナに登録し、Service 層として扱わせる
@Service
// クラスレベルで「読み取り専用トランザクション」を既定値にする（参照系メソッドの省略コードを減らす）
@Transactional(readOnly = true)
// 依存先（final フィールド）を引数に取るコンストラクタを Lombok に生成させる
@RequiredArgsConstructor
// 利用者に関する業務ロジックを担うサービスクラス
// 加えて Spring Security の UserDetailsService を実装し、ログイン時の認証情報供給も担う
public class UserService implements UserDetailsService {

    // User の CRUD を行うリポジトリ（コンストラクタインジェクションで注入される）
    private final UserRepository userRepository;
    // 新規登録時に初期カテゴリーを作るためのリポジトリ
    private final CategoryRepository categoryRepository;
    // パスワードのハッシュ化に使う PasswordEncoder（SecurityConfig で @Bean 定義済み）
    private final PasswordEncoder passwordEncoder;

    // Spring Security がログイン時に「email から利用者情報を読みに来る」窓口
    // 戻り値を Spring 標準の UserDetails に詰め替えて返す
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // findByEmail は Optional<User> を返す → 無ければ Security 用の例外を投げる
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + email));
        // Spring Security 側の User クラス（我々の Entity と同名のため必ずフルパスで参照する）を組み立てて返す
        return org.springframework.security.core.userdetails.User.builder()
                // ログインIDとして email を使う
                .username(user.getEmail())
                // DBに保存されている「BCryptハッシュ済み」のパスワードをそのまま渡す（★生に戻さない★）
                .password(user.getPassword())
                // この利用者に付与する権限（ロール）。今は全員 USER として扱う
                .authorities("ROLE_USER")
                // 上記設定で UserDetails を組み立てて返す
                .build();
    }

    // メールアドレスから利用者Entityを取得する（@AuthenticationPrincipal で得た email から本人を引き直す用）
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません: " + email));
    }

    // 新規登録：書き込み系なのでクラスの readOnly を @Transactional で上書きする
    @Transactional
    public User register(UserRegisterForm form) {
        // 同一 email が既に登録されていれば登録させない（Service レベルの重複チェック）
        if (userRepository.existsByEmail(form.getEmail())) {
            // 業務ルール違反は IllegalArgumentException で表現する
            throw new IllegalArgumentException("このメールアドレスは既に登録されています: " + form.getEmail());
        }
        // 空の User Entity を生成する
        User user = new User();
        // 表示名を Form から詰め替える
        user.setName(form.getName());
        // メールアドレスを詰め替える
        user.setEmail(form.getEmail());
        // ★最重要★ 生パスワードを BCrypt でハッシュ化してからセットする（生のまま保存しない）
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        // INSERT 文が発行される（id は自動採番）
        User saved = userRepository.save(user);
        // 登録直後の利用者に、すぐ使えるよう「初期カテゴリー（食費・給与など）」を用意する
        createDefaultCategories(saved);
        // 保存済みユーザーを返す
        return saved;
    }

    // 新規ユーザー用の初期カテゴリーをまとめて作成する（React版の DEFAULT_CATEGORIES に相当）
    // ※外部から呼ばれず register からのみ呼ぶ補助メソッドなので private にする
    private void createDefaultCategories(User user) {
        // 支出カテゴリーの初期セットを順番に作る（ラベルと色のペア）
        createCategory(user, TransactionType.EXPENSE, "食費", "#f87171");
        createCategory(user, TransactionType.EXPENSE, "住居費", "#60a5fa");
        createCategory(user, TransactionType.EXPENSE, "水道光熱費", "#fbbf24");
        createCategory(user, TransactionType.EXPENSE, "交通費", "#34d399");
        createCategory(user, TransactionType.EXPENSE, "交際・娯楽", "#a78bfa");
        createCategory(user, TransactionType.EXPENSE, "日用品", "#f472b6");
        createCategory(user, TransactionType.EXPENSE, "医療費", "#2dd4bf");
        createCategory(user, TransactionType.EXPENSE, "その他", "#9ca3af");
        // 収入カテゴリーの初期セットを順番に作る
        createCategory(user, TransactionType.INCOME, "給与", "#3b82f6");
        createCategory(user, TransactionType.INCOME, "お小遣い", "#10b981");
        createCategory(user, TransactionType.INCOME, "ボーナス", "#f59e0b");
        createCategory(user, TransactionType.INCOME, "その他", "#8b5cf6");
    }

    // カテゴリー1件を生成して保存する小さな補助メソッド（同じ処理の繰り返しをまとめる）
    private void createCategory(User user, TransactionType type, String label, String color) {
        // 空の Category を生成する
        Category category = new Category();
        // 持ち主を紐づける
        category.setUser(user);
        // 支出 or 収入の種類をセットする
        category.setType(type);
        // 表示名をセットする
        category.setLabel(label);
        // 表示色をセットする
        category.setColor(color);
        // INSERT 文が発行される
        categoryRepository.save(category);
    }
}
