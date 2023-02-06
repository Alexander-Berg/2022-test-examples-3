package ru.yandex.market.vendors.analytics.platform.controller.search.category.brand;

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
@DbUnitDataSet(before = "TopBrandsSearchTest.before.csv")
@ClickhouseDbUnitDataSet(before = "TopBrandsSearchTest.ch.before.csv")
public class TopBrandsSearchTest extends CalculateFunctionalTest {

    private static final long DASHBOARD_ID = 100L;
    private static final String BRAND_MARKET_SHARE_PATH = "/salesCategory/getBrandsMarketShare";

    @Test
    @DisplayName("Поисковые интересы в категории по брендам")
    void brandsSearchShare() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2020-03-24\",\n"
                + "    \"endDate\": \"2020-04-06\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"WEEK\",\n"
                + "  \"topBrandsCount\": 2,\n"
                + "  \"topSelectionStrategy\": \"SEARCH_QUERIES\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\""
                + "}";
        String actual = brandSearchShare(body);

        String expected = loadFromFile("TopBrandsSearchTest.brandsSearchShare.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    private String brandSearchShare(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(BRAND_MARKET_SHARE_PATH, DASHBOARD_ID), body);
    }
}
