package ru.yandex.market.vendors.analytics.platform.controller.sales.category.simple;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.model.ModelFunctionalTest;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * @author ogonek
 */
@DbUnitDataSet(before = "GetParentMarketShareTest.before.csv")
public class CategorySalesTest extends ModelFunctionalTest {

    private static final String CATEGORY_SALES_PATH = "/salesCategory/getCategorySales";

    @Test
    @DisplayName("Продажи в категории")
    @ClickhouseDbUnitDataSet(before = {"CategorySalesTest.dailyCategorySales.before.csv",
            "GetParentMarketShareTest.ch.before.csv"})
    void dailyCategorySales() {
        String reportResponse = loadFromFile("reportCategorySalesResponse.json");
        var requestUrl = BASE_REPORT_URL + "&hid=31"
                + "&modelid=100"
                + "&glfilter=201:1,2&glfilter=202:100";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-04-06\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
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
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {\n"
                + "      \"423423\": [],"
                + "      \"22342342\": null,"
                + "      \"201\": [1, 2],\n"
                + "      \"202\": [100]\n"
                + "    }\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getCategorySales(body);

        String expected = loadFromFile("CategorySalesTest.dailyCategorySales.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Продажи в категории с новыми скрытиями")
    @DbUnitDataSet(before = "NewHiding.before.csv")
    @ClickhouseDbUnitDataSet(before = "CategorySalesTest.newHiding.before.csv")
    void dailyCategorySalesNewHiding() {
        String reportResponse = loadFromFile("reportCategorySalesResponse.json");
        var requestUrl = BASE_REPORT_URL + "&hid=31"
                + "&modelid=100"
                + "&glfilter=201:1,2&glfilter=202:100";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-04-06\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {\n"
                + "      \"423423\": [],"
                + "      \"22342342\": null,"
                + "      \"201\": [1, 2],\n"
                + "      \"202\": [100]\n"
                + "    }\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getCategorySales(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\n"
                        + "  \"code\": \"BAD_FILTERS\",\n"
                        + "  \"message\": \"Hiding exception with type MODEL\",\n"
                        + "  "
                        + "\"hidingType\": \"MODEL\"\n" +
                        "}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Продажи в категории. Только с фильтром цены.")
    @ClickhouseDbUnitDataSet(before = {
            "CategorySalesTest.dailyModelSales.before.csv",
            "CategorySalesTest.dailyModelSales.ch.before.csv"
    })
    void dailyModelSalesWithPrice() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-04-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {},\n"
                + "      \"price\": {\n"
                + "         \"from\": 80.99,\n"
                + "         \"to\": 90.99\n"
                + "      }"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getCategorySales(body);

        String expected = loadFromFile("CategorySalesTest.dailyModelSales.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Продажи товара")
    @ClickhouseDbUnitDataSet(before = {"CategorySalesTest.dailyModelSales.before.csv",
            "CategorySalesTest.dailyModelSales.ch.before.csv"})
    void dailyModelSales() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"modelIds\": [100],\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-04-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getCategorySales(body);

        String expected = loadFromFile("CategorySalesTest.dailyModelSales.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Продажи партнера пользователя в категории. Бенчмарк.")
    @DbUnitDataSet(before = "CategorySalesTest.userPartnerSales.before.csv")
    @ClickhouseDbUnitDataSet(before = {"CategorySalesTest.userPartnerSales.ch.before.csv"})
    void userPartnerSales() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-02-01\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {},\n"
                + "      \"price\": {\n"
                + "         \"from\": 1,\n"
                + "         \"to\": 100\n"
                + "      }"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\",\n"
                + "  \"domains\": [\"domain.ru\", \"csv-domain.ru\", \"ga-domain.ru\"]\n"
                + "}";
        String actual = getCategorySales(body);

        String expected = loadFromFile("CategorySalesTest.userPartnerSales.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Начальная дата больше конечной")
    @ClickhouseDbUnitDataSet(before = "GetParentMarketShareTest.ch.before.csv")
    void getCategorySalesInvertedDates() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2017-01-01\",\n"
                + "    \"startDate\": \"2018-03-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getCategorySales(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonAssert.assertJsonEquals(
                "{\n" +
                        "   \"message\" : \"startDate should be before endDate\",\n" +
                        "   \"code\" : \"BAD_REQUEST\"\n" +
                        "}",
                clientException.getResponseBodyAsString()
        );
    }

    private String getCategorySales(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(CATEGORY_SALES_PATH) + "&uid=1", body);
    }
}
