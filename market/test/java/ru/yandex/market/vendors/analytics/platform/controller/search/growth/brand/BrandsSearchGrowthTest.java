package ru.yandex.market.vendors.analytics.platform.controller.search.growth.brand;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "BrandsSearchGrowthTest.before.csv")
@ClickhouseDbUnitDataSet(before = "BrandsSearchGrowthTest.ch.before.csv")
public class BrandsSearchGrowthTest extends CalculateFunctionalTest {

    private static final long DASHBOARD_ID = 100;
    private static final String BRAND_GROWTH_DRIVERS_PATH = "/growthDrivers/brands";

    @Test
    @DisplayName("Рост поисковых интересов по брендам")
    void brandsGrowth() {
        String expected = loadFromFile("BrandSearchGrowthTest.response.json");

        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-02-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";
        String actual = getBrandGrowth(body);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    private String getBrandGrowth(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(BRAND_GROWTH_DRIVERS_PATH, DASHBOARD_ID), body);
    }
}
