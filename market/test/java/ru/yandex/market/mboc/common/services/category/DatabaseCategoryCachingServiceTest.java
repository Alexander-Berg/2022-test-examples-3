package ru.yandex.market.mboc.common.services.category;


import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author danfertev
 * @since 07.07.2020
 */
public class DatabaseCategoryCachingServiceTest extends BaseDbTestClass {
    private static final long CHECK_TIMEOUT_IN_MINUTES = 0;

    @Autowired
    private CategoryRepository categoryRepository;
    private CategoryRepository categoryRepositoryMock;
    private StorageKeyValueServiceMock storageKeyValueServiceMock;
    private DatabaseCategoryCachingService loader;

    @Before
    public void setUp() throws Exception {
        storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        categoryRepositoryMock = Mockito.spy(categoryRepository);
        loader = new DatabaseCategoryCachingService(
            categoryRepositoryMock,
            storageKeyValueServiceMock,
            Mockito.mock(ScheduledExecutorService.class),
            CHECK_TIMEOUT_IN_MINUTES
        );
    }

    @Test
    public void testNoCategories() {
        assertThatThrownBy(() -> loader.getAllCategories())
            .hasMessageContaining("Categories doesn't contain root");
    }

    @Test
    public void testNoRoot() {
        categoryRepository.insert(new Category().setCategoryId(1));
        assertThatThrownBy(() -> loader.getAllCategories())
            .hasMessageContaining("Categories doesn't contain root");
    }

    @Test
    public void testLoadAndCache() {
        Category root = new Category().setCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        Category category = new Category().setCategoryId(1L).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        categoryRepository.insert(root);
        categoryRepository.insert(category);

        loader.getAllCategories();
        var tree = loader.getCategoryTree();
        var categories = loader.getAllCategories();

        verify(categoryRepositoryMock, times(1)).findAll();

        assertThat(categories).containsExactlyInAnyOrder(root, category);
        assertThat(tree.getRoot()).isEqualTo(root);
    }

    @Test
    public void testCheckFroUpdate() {
        Category root = new Category().setCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        categoryRepository.insert(root);
        storageKeyValueServiceMock.putValue(DatabaseCategoryCachingService.LAST_IMPORTED_STUFF_KEY, "1");

        var categories = loader.getAllCategories();

        verify(categoryRepositoryMock, times(1)).findAll();
        assertThat(categories).containsExactlyInAnyOrder(root);

        Category category = new Category().setCategoryId(1L).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        categoryRepository.insert(category);

        //Категории изменились, но stuff нет и не проверяли обновление
        categories = loader.getAllCategories();
        verify(categoryRepositoryMock, times(1)).findAll();
        assertThat(categories).containsExactlyInAnyOrder(root);

        storageKeyValueServiceMock.putValue(DatabaseCategoryCachingService.LAST_IMPORTED_STUFF_KEY, "2");

        //Категории и stuff изменились, но не было обновления
        categories = loader.getAllCategories();
        verify(categoryRepositoryMock, times(1)).findAll();
        assertThat(categories).containsExactlyInAnyOrder(root);

        loader.checkForUpdate();

        //Категории и stuff изменились, и проверили на обновление
        categories = loader.getAllCategories();
        verify(categoryRepositoryMock, times(2)).findAll();
        assertThat(categories).containsExactlyInAnyOrder(root, category);
    }

    @Test
    public void testCheckFroUpdateNoLastStuffKey() {
        Category root = new Category().setCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        categoryRepository.insert(root);

        var categories = loader.getAllCategories();

        verify(categoryRepositoryMock, times(1)).findAll();
        assertThat(categories).containsExactlyInAnyOrder(root);

        Category category = new Category().setCategoryId(1L).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        categoryRepository.insert(category);

        loader.checkForUpdate();

        //Категории изменились, stuff ключа нет в БД и проверили обновление
        categories = loader.getAllCategories();
        verify(categoryRepositoryMock, times(2)).findAll();
        assertThat(categories).containsExactlyInAnyOrder(root, category);
    }

    // TODO: until https://st.yandex-team.ru/MBO-35667
    @Test
    public void testRootBookCategoryHasKnowledge() {
        Category root = new Category().setCategoryId(CategoryTree.ROOT_CATEGORY_ID);
        Category rootBookCategory = new Category()
            .setCategoryId(CategoryCachingService.ROOT_BOOK_CATEGORY_ID)
            .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setHasKnowledge(false);
        Category someCategory = new Category()
            .setCategoryId(1)
            .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setHasKnowledge(false);
        categoryRepository.insertBatch(root, rootBookCategory, someCategory);

        // ROOT_BOOK_CATEGORY always has knowledge
        Optional<Category> loaderRootBookCategory = loader.getCategory(CategoryCachingService.ROOT_BOOK_CATEGORY_ID);
        assertThat(loaderRootBookCategory)
            .isPresent()
            .map(Category::isHasKnowledge)
            .contains(true);

        Optional<Category> loaderSomeCategory = loader.getCategory(1);
        assertThat(loaderSomeCategory)
            .isPresent()
            .map(Category::isHasKnowledge)
            .contains(false);
    }
}
