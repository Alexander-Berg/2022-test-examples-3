package ru.yandex.market.indexer.yt.category;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.category.model.ShopCategory;

/**
 * Функциональные тесты для {@link IndexerFeedCategoryDao}.
 *
 * @author avetokhin 17/10/17.
 */
class IndexerFeedCategoryDaoTest extends FunctionalTest {

    private static final List<ShopCategory> CATEGORIES = Arrays.asList(
            category(1, "10", "my_cat_1", "1234"),
            category(2, "20", "my_cat_2", "098")
    );

    @Autowired
    private IndexerFeedCategoryDao indexerFeedCategoryDao;

    private static ShopCategory category(final long feedId, final String categoryId, final String name,
                                         final String parentCategoryId) {
        final ShopCategory category = new ShopCategory();
        category.setFeedId(feedId);
        category.setName(name);
        category.setCategoryId(categoryId);
        category.setParentCategoryId(parentCategoryId);

        return category;
    }

    /**
     * Проверить вставку в таблицу.
     */
    @Test
    @DbUnitDataSet(
            before = "IndexerFeedCategoryDao.save.before.csv",
            after = "IndexerFeedCategoryDao.save.after.csv")
    void testInsert() {
        indexerFeedCategoryDao.insertCategories(CATEGORIES, "shops_web.feed_categories_1", true);
    }

}
