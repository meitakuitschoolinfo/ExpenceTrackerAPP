// このクラスが属するパッケージを宣言する（service パッケージ＝業務ロジック層）
package com.example.expensetracker.service;

// このサービスが扱う Entity（Category）をインポートする
import com.example.expensetracker.entity.Category;
// カテゴリーの持ち主を表す User をインポートする
import com.example.expensetracker.entity.User;
// 収支の種類（EXPENSE/INCOME）をインポートする
import com.example.expensetracker.entity.TransactionType;
// 「見つからなかった」場合に投げるカスタム例外をインポートする
import com.example.expensetracker.exception.ResourceNotFoundException;
// 画面の入力1件を表す Form クラスをインポートする
import com.example.expensetracker.form.CategoryForm;
// Category の CRUD を担う Repository をインポートする
import com.example.expensetracker.repository.CategoryRepository;
// 削除前の使用件数チェックに使う TransactionRepository をインポートする
import com.example.expensetracker.repository.TransactionRepository;

// Spring の Service Bean として認識させるためのアノテーション
import org.springframework.stereotype.Service;
// トランザクションを張るためのアノテーション
import org.springframework.transaction.annotation.Transactional;

// final フィールドを引数に取るコンストラクタを Lombok に自動生成させる
import lombok.RequiredArgsConstructor;

// 複数件を返すための List をインポートする
import java.util.List;

// このクラスを Spring の DI コンテナに登録し、Service 層として扱わせる
@Service
// クラスレベルで「読み取り専用トランザクション」を既定値にする
@Transactional(readOnly = true)
// 依存先（final フィールド）を引数に取るコンストラクタを Lombok に生成させる
@RequiredArgsConstructor
// カテゴリーに関する業務ロジックを担当するサービスクラス
public class CategoryService {

    // Category の CRUD を行うリポジトリ
    private final CategoryRepository categoryRepository;
    // 削除前に「使用件数」を数えるためのリポジトリ
    private final TransactionRepository transactionRepository;

    // 指定ユーザーのカテゴリーを全件（ID昇順）で取得する（カテゴリー管理画面の一覧で使う）
    public List<Category> findAllByUser(User user) {
        // Repository に委譲する（DB操作は Repository の仕事）
        return categoryRepository.findByUserOrderByIdAsc(user);
    }

    // 指定ユーザーの、指定タイプ（支出 or 収入）のカテゴリーを取得する（記録画面の選択肢で使う）
    public List<Category> findByUserAndType(User user, TransactionType type) {
        return categoryRepository.findByUserAndTypeOrderByIdAsc(user, type);
    }

    // ID指定で1件取得する。ただし「他人のカテゴリー」を触れないよう持ち主チェックも行う
    public Category findOwnedById(User user, Long id) {
        // まず存在チェック（無ければ 404 相当の例外）
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("カテゴリーが見つかりません: id=" + id));
        // ★セキュリティ★ ログイン中ユーザーのものでなければ拒否する（他人のデータを編集させない）
        if (!category.getUser().getId().equals(user.getId())) {
            // 「あなたのものではない」ので、存在しないのと同じ扱い（情報を漏らさないため）にする
            throw new ResourceNotFoundException("カテゴリーが見つかりません: id=" + id);
        }
        // 自分のカテゴリーであることを確認できたら返す
        return category;
    }

    // 編集画面の初期表示用に「DBの Category」を「画面の CategoryForm」に詰め替えるユーティリティ
    public CategoryForm toEditForm(User user, Long id) {
        // 自分のカテゴリーを取得する（無ければ例外）
        Category category = findOwnedById(user, id);
        // 空の Form を生成する
        CategoryForm form = new CategoryForm();
        // 編集対象を区別するために id を入れる（hidden で画面に保持される）
        form.setId(category.getId());
        // 種類（支出/収入）を埋める
        form.setType(category.getType());
        // 名前を埋める
        form.setLabel(category.getLabel());
        // 色を埋める
        form.setColor(category.getColor());
        // 完成した Form を返す
        return form;
    }

    // 新規登録：書き込み系なのでデフォルトの readOnly を上書きする
    @Transactional
    public Category register(User user, CategoryForm form) {
        // 同じユーザー内で「同じ種類・同じ名前」が既にあれば登録させない（重複チェック）
        if (categoryRepository.existsByUserAndTypeAndLabel(user, form.getType(), form.getLabel())) {
            // 業務ルール違反は IllegalArgumentException で表現する
            throw new IllegalArgumentException("同じ名前のカテゴリーが既に存在します: " + form.getLabel());
        }
        // 空の Entity を生成する
        Category category = new Category();
        // 持ち主を紐づける（★必ずログイン中ユーザーにする。Formの値は信用しない★）
        category.setUser(user);
        // Form の値を Entity に詰め替える
        category.setType(form.getType());
        category.setLabel(form.getLabel());
        category.setColor(form.getColor());
        // save() で INSERT 文が発行される
        return categoryRepository.save(category);
    }

    // 更新：書き込み系なので @Transactional を明示する
    @Transactional
    public Category update(User user, CategoryForm form) {
        // 自分の既存カテゴリーを取得する（無ければ例外）
        Category category = findOwnedById(user, form.getId());
        // 名前を変更する場合のみ、新しい名前の重複をチェックする
        if (!category.getLabel().equals(form.getLabel())
                && categoryRepository.existsByUserAndTypeAndLabel(user, category.getType(), form.getLabel())) {
            // 業務ルール違反は IllegalArgumentException
            throw new IllegalArgumentException("同じ名前のカテゴリーが既に存在します: " + form.getLabel());
        }
        // 名前を上書きする（種類は変更させない＝既存の記録との整合を保つ）
        category.setLabel(form.getLabel());
        // 色を上書きする
        category.setColor(form.getColor());
        // save() で UPDATE 文が発行される（@PreUpdate で updated_at も自動更新）
        return categoryRepository.save(category);
    }

    // 削除：書き込み系なので @Transactional を明示する
    @Transactional
    public void delete(User user, Long id) {
        // 自分のカテゴリーを取得する（無ければ例外）
        Category category = findOwnedById(user, id);
        // このカテゴリーを使っている記録の件数を数える（数えるのは Repository の仕事）
        long usageCount = transactionRepository.countByCategory(category);
        // 1件でも使われていれば削除を拒否する（業務ルール）
        if (usageCount > 0) {
            // 「状態が原因で実行不可」なので IllegalStateException を使う
            throw new IllegalStateException(
                    "このカテゴリーは " + usageCount + " 件の記録で使われているため削除できません");
        }
        // 上記チェックを通過したら削除を実行する（DELETE 文が発行される）
        categoryRepository.delete(category);
    }
}
