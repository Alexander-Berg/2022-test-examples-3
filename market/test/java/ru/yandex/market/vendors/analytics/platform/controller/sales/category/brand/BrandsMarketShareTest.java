package ru.yandex.market.vendors.analytics.platform.controller.sales.category.brand;

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
@DbUnitDataSet(before = "BrandsMarketShareTest.before.csv")
@ClickhouseDbUnitDataSet(before = "BrandsMarketShareTest.ch.before.csv")
public class BrandsMarketShareTest extends CalculateFunctionalTest {

    private static final long DASHBOARD_ID = 100L;
    private static final String BRAND_MARKET_SHARE_PATH = "/salesCategory/getBrandsMarketShare";

    @Test
    @DisplayName("Продажи в брендах")
    @ClickhouseDbUnitDataSet(before = "BrandsMarketShareTest.getBrandsMarketShare.before.csv")
    void getBrandsMarketShare() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2018-01-01\",\n"
                + "    \"endDate\": \"2018-03-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"topBrandsCount\": 2,\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\""
                + "}";

        String actual = calcBrandMarketShare(body);
        String expected = loadFromFile("BrandsMarketShareTest.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Поисковые запросы в брендах")
    @ClickhouseDbUnitDataSet(before = "BrandsMarketShareTest.search.before.csv")
    void getBrandsSearchShare() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2020-01-01\",\n"
                + "    \"endDate\": \"2020-03-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"topBrandsCount\": 2,\n"
                + "  \"topSelectionStrategy\": \"SEARCH_QUERIES\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\""
                + "}";

        String actual = calcBrandMarketShare(body);
        String expected = loadFromFile("BrandsMarketShareTest.getBrandsSearchShare.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Продажи в брендах, урезанные скрытиями по регионам и категориям")
    @ClickhouseDbUnitDataSet(before = "BrandsMarketShareTest.getAllBrandsMarketShare.before.csv")
    void getAllBrandsMarketShare() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [\n"
                + "      213\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-07-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"QUARTER\",\n"
                + "  \"topBrandsCount\": 5,\n"
                + "  \"topSelectionStrategy\": \"COUNT\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\""
                + "}";

        String actual = calcBrandMarketShare(body);
        String expected = loadFromFile("BrandsMarketShareTest.all.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Продажи в брендах, урезанные новыми скрытиями")
    @DbUnitDataSet(before = "GetNewHidingBrandsMarketShare.before.csv")
    @ClickhouseDbUnitDataSet(before = "GetNewHidingBrandsMarketShare.ch.before.csv")
    void getNewHidingBrandsMarketShare() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [\n"
                + "      213\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-07-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"QUARTER\",\n"
                + "  \"topBrandsCount\": 5,\n"
                + "  \"topSelectionStrategy\": \"COUNT\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\""
                + "}";

        String actual = calcBrandMarketShare(body);
            String expected = loadFromFile("BrandsMarketShareTest.new.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Поисковые запросы по всем брендам")
    @ClickhouseDbUnitDataSet(before = "BrandsMarketShareTest.search.before.csv")
    void getAllBrandsSearchShare() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2020-01-01\",\n"
                + "    \"endDate\": \"2020-03-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"topBrandsCount\": 100,\n"
                + "  \"topSelectionStrategy\": \"SEARCH_QUERIES\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\""
                + "}";

        String actual = calcBrandMarketShare(body);
        String expected = loadFromFile("BrandsMarketShareTest.getAllBrandsSearchShare.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Всё скрыто :-(")
    @ClickhouseDbUnitDataSet(before = "BrandsMarketShareTest.noneDetailing.before.csv")
    void noneDetailing() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [\n"
                + "      213\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-02-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"topBrandsCount\": 5,\n"
                + "  \"topSelectionStrategy\": \"COUNT\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\""
                + "}";
        var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> calcBrandMarketShare(body)
        );
        var expectedResponse = ""
                + "{  \n"
                + "   \"code\":\"HIDDEN_DATA\",\n"
                + "   \"message\":\"Hiding exception with type CATEGORY\",\n"
                + "   \"hidingType\":\"CATEGORY\",\n"
                + "   \"hiddenIntervals\":[  \n"
                + "      {  \n"
                + "         \"startDate\":\"2018-01-01\",\n"
                + "         \"endDate\":\"2018-02-28\"\n"
                + "      }\n"
                + "   ],\n"
                + "   \"hidingMap\":{  \n"
                + "      \"31\":[  \n"
                + "         {  \n"
                + "            \"startDate\":\"2018-01-01\",\n"
                + "            \"endDate\":\"2018-02-28\"\n"
                + "         }\n"
                + "      ]\n"
                + "   }\n"
                + "}";
        JsonTestUtil.assertEquals(
                expectedResponse,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Продажи в брендах, для случая, когда vendor_id отрицательный")
    @DbUnitDataSet(before = "BrandsMarketShareTest.vendorIdNegative.before.csv")
    @ClickhouseDbUnitDataSet(before = "BrandsMarketShareTest.vendorIdNegative.ch.before.csv")
    void getBrandsMarketShareIfVendorIdNegative() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2018-01-01\",\n"
                + "    \"endDate\": \"2018-03-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"topBrandsCount\": 3,\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\""
                + "}";

        String actual = calcBrandMarketShare(body);
        String expected = loadFromFile("BrandsMarketShareTest.vendorIdNegative.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    private String calcBrandMarketShare(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(BRAND_MARKET_SHARE_PATH, DASHBOARD_ID), body);
    }
}
