package ru.yandex.market.mbi.partner_stat.mvc.category;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;
import ru.yandex.market.mbi.partner_stat.entity.NavigationTreeType;

/**
 * Тесты для {@link MarketCategoryController}
 */
class MarketCategoryControllerTest extends FunctionalTest {

    @DisplayName("Проверка получения корня маркетного дерева")
    @DbUnitDataSet(before = "MarketCategoryController/before.csv")
    @Test
    void testGetRootCategory() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getChildrenCategoriesUrl(NavigationTreeType.FMCG, null));

        JsonTestUtil.assertEquals(responseEntity, getClass(), "MarketCategoryController/testGetRootCategory.expected.json");
    }

    @DisplayName("Проверка получения дочерних нод")
    @DbUnitDataSet(before = "MarketCategoryController/before.csv")
    @Test
    void testGetChildrenCategories() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper
                .get(getChildrenCategoriesUrl(NavigationTreeType.FMCG, 1L));

        JsonTestUtil.assertEquals(responseEntity, getClass(), "MarketCategoryController/testGetChildrenCategories.expected.json");
    }

    @DisplayName("Проверка получения дочерних нод у листовой")
    @DbUnitDataSet(before = "MarketCategoryController/before.csv")
    @Test
    void testGetChildrenCategoriesFromLeaf() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper
                .get(getChildrenCategoriesUrl(NavigationTreeType.FMCG, 23L));

        JsonTestUtil.assertEquals(responseEntity, getClass(), "MarketCategoryController/testGetChildrenCategoriesFromLeaf.expected.json");
    }

    @DisplayName("Проверка ответа для ноды, которой нет в дереве")
    @DbUnitDataSet(before = "MarketCategoryController/before.csv")
    @Test
    void testGetUnknownCategoryChildren() {
        final HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getChildrenCategoriesUrl(NavigationTreeType.FMCG, 999L))
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        JsonTestUtil.assertResponseErrorMessage(exception, getClass(),
                "MarketCategoryController/testGetUnknownCategoryChildren.expected.json");
    }

    private String getChildrenCategoriesUrl(final NavigationTreeType navigationTreeType, final Long hid) {
        return baseUrl() + "/category/tree/" + navigationTreeType.name()  + "/children?hid=" + ObjectUtils.defaultIfNull(hid, "");
    }
}
