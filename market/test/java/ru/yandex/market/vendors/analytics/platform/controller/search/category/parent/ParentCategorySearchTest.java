package ru.yandex.market.vendors.analytics.platform.controller.search.category.parent;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * @author antipov93
 */
@DbUnitDataSet(before = "ParentCategorySearchTest.before.csv")
@ClickhouseDbUnitDataSet(before = "ParentCategorySearchTest.ch.before.csv")
public class ParentCategorySearchTest extends CalculateFunctionalTest {

    private static final String PARENT_CATEGORY_SEARCH_PATH = "/salesCategory/getParentMarketShare";

    @Test
    @DisplayName("Поисковые интересы в категории")
    void parentCategorySearch() {
        String body = "{\n"
                + "  \"hid\": 31,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2020-02-29\",\n"
                + "    \"startDate\": \"2020-01-01\"\n"
                + "  },\n"
                + "  \"subCategoryHids\": [\n"
                + "    31\n"
                + "  ],\n"
                + "  \"topCategoriesCount\": 2,\n"
                + "  \"topSelectionStrategy\": \"SEARCH_QUERIES\",\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";
        String actual = getParentCategorySearch(body);

        String expected = loadFromFile("ParentCategorySearchTest.parentCategorySearch.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }


    private String getParentCategorySearch(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(PARENT_CATEGORY_SEARCH_PATH, 100), body);
    }
}
