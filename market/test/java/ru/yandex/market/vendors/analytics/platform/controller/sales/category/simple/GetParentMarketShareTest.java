package ru.yandex.market.vendors.analytics.platform.controller.sales.category.simple;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.model.common.language.Language;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * @author ogonek
 */
@DbUnitDataSet(before = "GetParentMarketShareTest.before.csv")
public class GetParentMarketShareTest extends CalculateFunctionalTest {

    private static final long DASHBOARD_ID = 100L;
    private static final String PARENT_MARKET_SHARE_PATH = "/salesCategory/getParentMarketShare";

    @Test
    @DbUnitDataSet(before = "GetParentMarketMarketShare.before.csv")
    @ClickhouseDbUnitDataSet(before = "GetParentMarketMarketShareTest.ch.before.csv")
    @DisplayName("Распределение продаж в родительской категории при всех нескрытых категории")
    void getParentMarketMarketShare() {
        String body = "{\n"
                + "  \"hid\": 31,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-03-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"subCategoryHids\": [\n"
                + "    1,3\n"
                + "  ],\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"topCategoriesCount\": 3,\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getParentMarketShare(body);

        String expected = loadFromFile("GetParentMarketShare.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Родительская категория разрешена к показу не во всех регионах не во всех датах")
    @DbUnitDataSet
    void getMarketShareBadRegions() {
        String body = "{\n"
                + "  \"purchasedHid\": 31,\n"
                + "  \"topCategoriesCount\": 3,\n"
                + "  \"categoryTopStrategy\": \"MONEY\",\n"
                + "  \"subCategoryHids\": [\n"
                + "    1,\n"
                + "    3\n"
                + "  ],\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [\n"
                + "      213\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2018-01\",\n"
                + "    \"endDate\": \"2018-02\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\"\n"
                + "}";
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getParentMarketShare(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
    }

    @Test
    @DisplayName("Даты не парсятся")
    @DbUnitDataSet
    void getMarketShareBadDatesFormat() {
        String body = ""
                + "{\n"
                + "\"purchasedHid\": 31,\n"
                + "\"topCategoriesCount\": 3,\n"
                + "\"categoryTopStrategy\": \"MONEY\",\n"
                + "\"subCategoryHids\": [\n"
                + "    1,\n"
                + "    3\n"
                + "],\n"
                + "\"interval\": {\n"
                + "    \"startDate\": \"2018-05\",\n"
                + "    \"endDate\": \"2018-06\"\n"
                + "},\n"
                + "\"timeDetailing\": \"DAY\"\n"
                + "}";
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getParentMarketShare(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
    }

    @Test
    @DisplayName("Начальная дата больше конечной")
    @DbUnitDataSet
    void getMarketShareInvertedDates() {
        String body = "{\n"
                + "  \"hid\": 40,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-01-01\",\n"
                + "    \"startDate\": \"2018-03-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"topCategoriesCount\": 3,\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getParentMarketShare(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\n" +
                        "   \"message\" : \"startDate should be before endDate\",\n" +
                        "   \"code\" : \"BAD_REQUEST\"\n" +
                        "}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Между датами больше 366 дней")
    @DbUnitDataSet
    void getMarketShareBadDates() {
        String body = "{\n"
                + "  \"hid\": 40,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-03-01\",\n"
                + "    \"startDate\": \"2016-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"topCategoriesCount\": 3,\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getParentMarketShare(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\n" +
                        "   \"message\" : \"When time detailing = DAYS, "
                        + "the duration between startDate and endDate should be <= 366 days\",\n" +
                        "   \"code\" : \"BAD_REQUEST\"\n" +
                        "}",
                clientException.getResponseBodyAsString()
        );
    }

    private String getParentMarketShare(String body) {
        return FunctionalTestHelper.postForJson(
                getFullWidgetUrl(PARENT_MARKET_SHARE_PATH, DASHBOARD_ID, Language.EN),
                body
        );
    }
}
