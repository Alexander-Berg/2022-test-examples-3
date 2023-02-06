package ru.yandex.market.vendors.analytics.platform.controller.sales.category.segment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * @author ogonek
 */
@DbUnitDataSet(before = "PriceSegments.before.csv")
@ClickhouseDbUnitDataSet(before = "GetPriceSegmentsMarketShareTest.ch.before.csv")
public class GetPriceSegmentsMarketShareTest extends CalculateFunctionalTest {

    private static final String PRICE_MARKET_SHARE_PATH = "/salesCategory/getPricesMarketShare";

    @Test
    @DisplayName("Ценовые сегмены категории")
    void getPriceSegmentsMarketShare() {
        String body = ""
                + "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [\n"
                + "      213\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-02-28\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getPriceSegments(body);

        String expected = loadFromFile("GetPriceSegmentsMarketShareTest.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Кастомные ценовые сегмены категории")
    void getPriceSegmentsMarketShareCustom() {
        String body = ""
                //language=json
                + "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [\n"
                + "      213\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-02-28\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"priceSegmentBounds\": [10, 28],\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getPriceSegments(body);

        String expected = loadFromFile("GetPriceSegmentsMarketShareCustomTest.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Категория скрыта в 5 регионе во все даты")
    void getPriceSegmentsMarketShareBadRegion() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [\n"
                + "      213,5\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-02-28\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"WEEK\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getPriceSegments(body)
        );

        var expected = ""
                + "{\n"
                + "  \"code\": \"HIDDEN_DATA\",\n"
                + "  \"message\": \"Hiding exception with type CATEGORY\",\n"
                + "  \"hidingType\": \"CATEGORY\",\n"
                + "  \"hiddenIntervals\": [\n"
                + "    {\n"
                + "      \"startDate\": \"2018-01-01\",\n"
                + "      \"endDate\": \"2018-03-04\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"hidingMap\": {\n"
                + "    \"31\": [\n"
                + "      {\n"
                + "        \"startDate\": \"2018-01-01\",\n"
                + "        \"endDate\": \"2018-03-04\"\n"
                + "      }\n"
                + "    ]\n"
                + "  }\n"
                + "}";

        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    private String getPriceSegments(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(PRICE_MARKET_SHARE_PATH), body);
    }
}
