package ru.yandex.market.vendors.analytics.platform.controller.sales.category.segment;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * s @author ogonek
 */
@DbUnitDataSet(before = "PriceSegments.before.csv")
public class GetBrandsPriceSegmentsTest extends CalculateFunctionalTest {

    private static final long DASHBOARD_ID = 100L;
    private static final String BRAND_PRICE_SEGMENTS_PATH = "/salesCategory/getBrandsPriceSegments";

    @Test
    @DisplayName("Бренды в ценовых сегментах")
    @ClickhouseDbUnitDataSet(before = "GetBrandsPriceSegmentsTest.ch.before.csv")
    void getBrandsPriceSegments() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-03-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"topBrandsCount\": 2,\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getBrandsPriceSegments(body);
        String expected = loadFromFile("GetBrandsPriceSegmentsTest.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Бренды в ценовых сегментах: все попали в ответ")
    @ClickhouseDbUnitDataSet(before = "GetBrandsPriceSegmentsTest.ch.before.csv")
    void getAllBrandsPriceSegments() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-03-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"topBrandsCount\": 4,\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getBrandsPriceSegments(body);
        String expected = loadFromFile("GetBrandsPriceSegmentsTest.all.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Категория скрыта в некоторых датах")
    void getBrandsPriceSegmentsBadDates() {
        @Language("JSON") String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-10-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getBrandsPriceSegments(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\n"
                        + "  \"code\": \"HIDDEN_DATA\",\n"
                        + "  \"message\": \"Hiding exception with type CATEGORY\",\n"
                        + "  \"hidingType\": \"CATEGORY\",\n"
                        + "  \"hiddenIntervals\": [\n"
                        + "    {\n"
                        + "      \"startDate\": \"2018-04-01\",\n"
                        + "      \"endDate\": \"2018-04-30\"\n"
                        + "    },\n"
                        + "    {\n"
                        + "      \"startDate\": \"2018-08-01\",\n"
                        + "      \"endDate\": \"2018-10-31\"\n"
                        + "    }\n"
                        + "  ],\n"
                        + "  \"hidingMap\": {\n"
                        + "    \"31\": [\n"
                        + "      {\n"
                        + "        \"startDate\": \"2018-04-01\",\n"
                        + "        \"endDate\": \"2018-04-30\"\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"startDate\": \"2018-08-01\",\n"
                        + "        \"endDate\": \"2018-10-31\"\n"
                        + "      }\n"
                        + "    ]\n"
                        + "  }\n"
                        + "}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Регионы скрыты в некоторых датах")
    void getBrandsPriceSegmentsBadRegion() {
        String body = "{\n"
                + "    \"categoryFilter\": {\n"
                + "        \"hid\": 31\n"
                + "    },\n"
                + "    \"interval\": {\n"
                + "        \"endDate\": \"2018-03-01\",\n"
                + "        \"startDate\": \"2018-01-01\"\n"
                + "    },\n"
                + "    \"geoFilters\": {\n"
                + "        \"federalDistrictIds\": [\n"
                + "            213, 3\n"
                + "        ],\n"
                + "        \"cityTypes\": [\n"
                + "          \"MILLION\"\n"
                + "        ]\n"
                + "    },\n"
                + "    \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getBrandsPriceSegments(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals("{\n"
                        + "  \"code\": \"HIDDEN_DATA\",\n"
                        + "  \"message\": \"Hiding exception with type CATEGORY\",\n"
                        + "  \"hidingType\": \"CATEGORY\",\n"
                        + "  \"hiddenIntervals\": [\n"
                        + "    {\n"
                        + "      \"startDate\": \"2018-01-01\",\n"
                        + "      \"endDate\": \"2018-03-31\"\n"
                        + "    }\n"
                        + "  ],\n"
                        + "  \"hidingMap\": {\n"
                        + "    \"31\": [\n"
                        + "      {\n"
                        + "        \"startDate\": \"2018-01-01\",\n"
                        + "        \"endDate\": \"2018-03-31\"\n"
                        + "      }\n"
                        + "    ]\n"
                        + "  }\n"
                        + "}",
                clientException.getResponseBodyAsString()
        );
    }


    private String getBrandsPriceSegments(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(BRAND_PRICE_SEGMENTS_PATH, DASHBOARD_ID), body);
    }
}
