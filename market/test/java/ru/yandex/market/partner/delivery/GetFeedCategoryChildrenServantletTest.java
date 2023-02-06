package ru.yandex.market.partner.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link GetFeedCategoryChildrenServantlet}.
 */
@DbUnitDataSet(before = "testGetFeedRootCategories.before.csv")
public class GetFeedCategoryChildrenServantletTest extends FunctionalTest {

    /**
     * Проверяем получение рутовых категорий если нет с null parent_category_id.
     */
    @Test
    public void testGetFeedRootCategories() {
        ResponseEntity<String> res = FunctionalTestHelper.get(baseUrl + buildFeedUrl(2326, ""));
        JsonTestUtil.assertEquals(res, this.getClass(), "testGetFeedRootCategories.json");
    }

    /**
     * Проверяем получение рутовых категорий если есть с null parent_category_id.
     */
    @Test
    public void testGetNullFeedRootCategories() {
        ResponseEntity<String> res = FunctionalTestHelper.get(baseUrl + buildFeedUrl(2327, ""));
        JsonTestUtil.assertEquals(res, this.getClass(), "testGetFeedNullRootCategories.json");
    }

    /**
     * Проверяем получение дочерних категорий по родительской.
     */
    @Test
    public void testGetFeedChildCategories() {
        ResponseEntity<String> res = FunctionalTestHelper.get(baseUrl + buildFeedUrl(2326, "1013"));
        JsonTestUtil.assertEquals(res, this.getClass(), "testGetFeedChildCategories.json");
    }

    private String buildFeedUrl(int feedId, String categoryId) {
        return "/getFeedCategoryChildren?format=json&feedId=" + feedId + "&parentCategoryId=" + categoryId;
    }

}
