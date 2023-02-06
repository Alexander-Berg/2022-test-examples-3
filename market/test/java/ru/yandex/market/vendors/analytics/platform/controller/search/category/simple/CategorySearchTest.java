package ru.yandex.market.vendors.analytics.platform.controller.search.category.simple;

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
@DbUnitDataSet(before = "CategorySearchTest.before.csv")
@ClickhouseDbUnitDataSet(before = "CategorySearchTest.ch.before.csv")
public class CategorySearchTest extends CalculateFunctionalTest {

    private static final String CATEGORY_SEARCH_PATH = "/salesCategory/getCategorySales";

    @Test
    @DisplayName("Поисковые интересы в категории")
    void dailyCategorySearch() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2020-01-05\",\n"
                + "    \"startDate\": \"2020-01-01\"\n"
                + "  },\n"
                + "  \"socdemFilters\": [\n"
                + "    {\n"
                + "      \"gender\": \"MALE\",\n"
                + "      \"ageSegment\": \"AGE_25_34\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"gender\": \"FEMALE\",\n"
                + "      \"ageSegment\": \"AGE_25_34\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"gender\": \"MALE\",\n"
                + "      \"ageSegment\": \"AGE_0_17\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"UNIQUE_USER_SEARCH\"\n"
                + "}";
        String actual = getCategorySearch(body);

        String expected = loadFromFile("CategorySearchTest.dailyCategorySearch.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Поисковые запросы товара")
    void monthlyModelSearches() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"modelIds\": [900],\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2020-03-01\",\n"
                + "    \"startDate\": \"2020-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"UNIQUE_USER_SEARCH\"\n"
                + "}";
        String actual = getCategorySearch(body);

        String expected = loadFromFile("CategorySearchTest.monthlyModelSearches.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    private String getCategorySearch(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(CATEGORY_SEARCH_PATH), body);
    }
}
