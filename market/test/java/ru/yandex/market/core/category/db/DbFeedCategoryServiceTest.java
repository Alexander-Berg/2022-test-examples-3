package ru.yandex.market.core.category.db;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.tariff.model.CategoryId;

import static org.assertj.core.api.Assertions.assertThat;

public class DbFeedCategoryServiceTest extends FunctionalTest {
    @Autowired
    DbFeedCategoryService feedCategoryService;

    private final Set<CategoryId> categories = Set.of(
            new CategoryId("100", 4869),
            new CategoryId(null, 1323),
            new CategoryId("1233", 3333)
    );

    @Test
    @DbUnitDataSet(before = "DbFeedCategoriesService.before.csv")
    void datasourceCategoriesSubtreeTest() {
        assertThat(feedCategoryService.getDatasourceCategoriesSubtree(1, categories)).hasSize(4);
    }

    @Test
    void datasourceCategoriesCountTest() {
        feedCategoryService.getDatasourceCategoriesCount(123);
    }

    @Test
    void datasourceCategoriesTest() {
        feedCategoryService.getDatasourceCategories(123, 1, 4);
    }

    @Test
    void getCategoriesWithChildCountTest() {
        feedCategoryService.getCategoriesWithChildCount(categories);
    }

    @Test
    void getFeedCategoriesTest() {
        feedCategoryService.getFeedCategories(123, 2, 3);
    }

    @Test
    void getFeedChildrenCategoriesTest() {
        feedCategoryService.getFeedChildrenCategories(123, "333", 4, 5);
    }

    @Test
    void getFeedChildrenCategoriesEmptyTest() {
        feedCategoryService.getFeedChildrenCategories(123, "", 4, 5);
    }

    @Test
    void getFeedCategoriesCountTest() {
        feedCategoryService.getFeedCategoriesCount(123);
    }

    @Test
    void getFeedCategoriesCount2Test() {
        feedCategoryService.getFeedCategoriesCount(123, Set.of(123L, 111L, 444L));
    }


}
