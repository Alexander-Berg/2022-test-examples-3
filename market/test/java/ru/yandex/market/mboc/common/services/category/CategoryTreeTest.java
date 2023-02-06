package ru.yandex.market.mboc.common.services.category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.services.category.models.Category;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryTreeTest {

    @Test
    public void testGetByCategoryId() {
        CategoryTree tree = createSimpleTree();

        Assertions.assertThat(tree.getByCategoryId(CategoryTree.ROOT_CATEGORY_ID).getName()).isEqualTo("Все товары");
        Assertions.assertThat(tree.getByCategoryId(1).getName()).isEqualTo("Электроника");
        Assertions.assertThat(tree.getByCategoryId(2).getName()).isEqualTo("Продукты");
        Assertions.assertThat(tree.getByCategoryId(3).getName()).isEqualTo("Компьютеры");
        Assertions.assertThat(tree.getByCategoryId(4).getName()).isEqualTo("Сотовые телефоны");

        Assertions.assertThatThrownBy(() -> tree.getByCategoryId(666))
            .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    public void testGetAllCategoryIdsInTree() {
        CategoryTree tree = createSimpleTree();

        Assertions.assertThat(tree.getAllCategoryIdsInTree(CategoryTree.ROOT_CATEGORY_ID))
            .containsExactlyInAnyOrder(CategoryTree.ROOT_CATEGORY_ID, 1L, 2L, 3L, 4L);
        Assertions.assertThat(tree.getAllCategoryIdsInTree(1)).containsExactlyInAnyOrder(1L, 3L, 4L);
        Assertions.assertThat(tree.getAllCategoryIdsInTree(2)).containsExactlyInAnyOrder(2L);
        Assertions.assertThat(tree.getAllCategoryIdsInTree(3)).containsExactlyInAnyOrder(3L);
        Assertions.assertThat(tree.getAllCategoryIdsInTree(4)).containsExactlyInAnyOrder(4L);
        Assertions.assertThat(tree.getAllCategoryIdsInTree(666)).isEmpty();
    }

    @Test
    public void testGetDepartament() {
        CategoryTree tree = createSimpleTree();

        Assertions.assertThat(tree.getDepartment(CategoryTree.ROOT_CATEGORY_ID)).isEqualTo("");
        Assertions.assertThat(tree.getDepartment(1)).isEqualTo("Электроника");
        Assertions.assertThat(tree.getDepartment(2)).isEqualTo("Продукты");
        Assertions.assertThat(tree.getDepartment(3)).isEqualTo("Электроника");
        Assertions.assertThat(tree.getDepartment(4)).isEqualTo("Электроника");
    }

    @Test
    public void testGetFullPath() {
        CategoryTree tree = createSimpleTree();

        Assertions.assertThat(tree.getFullPath(CategoryTree.ROOT_CATEGORY_ID)).isEqualTo("Все товары");
        Assertions.assertThat(tree.getFullPath(1)).isEqualTo("Все товары\\Электроника");
        Assertions.assertThat(tree.getFullPath(2)).isEqualTo("Все товары\\Продукты");
        Assertions.assertThat(tree.getFullPath(3)).isEqualTo("Все товары\\Электроника\\Компьютеры");
        Assertions.assertThat(tree.getFullPath(4)).isEqualTo("Все товары\\Электроника\\Сотовые телефоны");
    }

    @Test
    public void testTraversePreorder() {
        CategoryTree tree = createSimpleTree();

        List<Long> actualOrder = new ArrayList<>();
        tree.traversePreorder(category -> {
            actualOrder.add(category.getCategoryId());
        });

        Assertions.assertThat(actualOrder).containsExactly(CategoryTree.ROOT_CATEGORY_ID, 1L, 3L, 4L, 2L);
    }

    private CategoryTree createSimpleTree() {
        Category root = new Category()
            .setCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setName("Все товары");
        Category category1 = new Category()
            .setCategoryId(1).setParentCategoryId(root.getCategoryId())
            .setName("Электроника");
        Category category2 = new Category()
            .setCategoryId(2).setParentCategoryId(root.getCategoryId())
            .setName("Продукты");
        Category category3 = new Category()
            .setCategoryId(3).setParentCategoryId(category1.getCategoryId())
            .setName("Компьютеры");
        Category category4 = new Category()
            .setCategoryId(4).setParentCategoryId(category1.getCategoryId())
            .setName("Сотовые телефоны");
        return CategoryTree.computeTree(Arrays.asList(root, category1, category2, category3, category4));
    }
}
