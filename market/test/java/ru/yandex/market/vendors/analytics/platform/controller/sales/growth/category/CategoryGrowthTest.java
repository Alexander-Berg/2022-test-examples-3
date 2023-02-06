package ru.yandex.market.vendors.analytics.platform.controller.sales.growth.category;

import org.intellij.lang.annotations.Language;
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
import ru.yandex.market.vendors.analytics.core.TestDatasource;
import ru.yandex.market.vendors.analytics.core.calculate.growth.request.GrowthRequest;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.growth.GrowthDriversController;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Functional tests for {@link GrowthDriversController#categoryGrowthWaterfall(GrowthRequest, LanguageDTO)} and
 * {@link GrowthDriversController#categoryGrowthExtended(GrowthRequest, LanguageDTO)}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "CategoryGrowthTest.before.csv")
@ClickhouseDbUnitDataSet(before = "CategoryGrowthTest.ch.before.csv")
public class CategoryGrowthTest extends CalculateFunctionalTest {

    private static final String WATERFALL_PATH = "/growthDrivers/category/waterfall";
    private static final String EXTENDED_INFO_PATH = "/growthDrivers/category/extended";

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "CategoryWaterfall.csv"
    )
    @DisplayName("Рост категории: водопад, бренды не скрыты")
    void categoryWaterfall() {
        String expected = loadFromFile("CategoryGrowthTestWaterfall.response.json");

        String body = ""
                + "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491,\n"
                + "    \"priceSegments\": [\n"
                + "      7,\n"
                + "      8\n"
                + "    ]\n"
                + "  },\n"
                + "  \"brands\": [11],\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-02-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"WATERFALL\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getWaterfall(body);
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "CategoryWaterfallSeveralMonths.csv"
    )
    @DisplayName("Рост категории: водопад, несколько месяцев")
    void categoryWaterfallSeveralMonth() {
        String expected = loadFromFile("CategoryGrowthTestWaterfallSeveralMonths.response.json");

        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491,\n"
                + "    \"priceSegments\": [\n"
                + "      7,\n"
                + "      8\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-03-01\",\n"
                + "    \"endDate\": \"2019-03-03\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"WATERFALL\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getWaterfall(body);
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Рост категории: водопад, нет данных")
    void categoryWaterfallNoSales() {
        String body = ""
                + "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2018-01-01\",\n"
                + "    \"endDate\": \"2018-01-31\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"WATERFALL\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getWaterfall(body)
        );

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\n"
                        + "  \"hidingType\": \"CATEGORY\",\n"
                        + "  \"hiddenIntervals\": [\n"
                        + "    {\n"
                        + "      \"startDate\": \"2017-12-01\",\n"
                        + "      \"endDate\": \"2017-12-31\"\n"
                        + "    }\n"
                        + "  ],\n"
                        + "  \"hidingMap\": {"
                        + "    \"91491\":["
                        + "      {"
                        + "         \"startDate\":\"2017-12-01\","
                        + "         \"endDate\":\"2017-12-31\""
                        + "      }"
                        + "    ]"
                        + "  },"
                        + "  \"code\": \"HIDDEN_DATA\",\n"
                        + "  \"message\": \"Hiding exception with type CATEGORY\"\n"
                        + "}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Рост категории: водопад, скрыт бренд")
    void categoryWaterfallHiddenBrands() {
        String body = ""
                + "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491\n"
                + "  },\n"
                + "  \"brands\": [10, 11],"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-02-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"WATERFALL\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getWaterfall(body)
        );

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\n"
                        + "  \"code\": \"HIDDEN_DATA\",\n"
                        + "  \"message\": \"Hiding exception with type BRAND\",\n"
                        + "  \"hidingType\": \"BRAND\",\n"
                        + "  \"hiddenIntervals\": [\n"
                        + "    {\n"
                        + "      \"startDate\": \"2019-01-01\",\n"
                        + "      \"endDate\": \"2019-01-31\"\n"
                        + "    }\n"
                        + "  ],\n"
                        + "  \"hidingMap\": {\n"
                        + "    \"10\": [\n"
                        + "      {\n"
                        + "        \"startDate\": \"2019-01-01\",\n"
                        + "        \"endDate\": \"2019-01-31\"\n"
                        + "      }\n"
                        + "    ]\n"
                        + "  }\n"
                        + "}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "CategoryExtended.csv"
    )
    @DisplayName("Рост категории: дополнительная информация")
    void categoryExtended() {
        String expected = loadFromFile("CategoryGrowthTestExtended.response.json");

        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491,\n"
                + "    \"priceSegments\": [\n"
                + "      7,\n"
                + "      8\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-02-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"WATERFALL\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getExtendedGrowth(body);
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @ClickhouseDbUnitDataSet(before = "CategoryExtended.csv")
    @DisplayName("Рост категории: дополнительная информация (c gl-фильтром).")
    void categoryExtendedWithGlFilter() {
        String reportResponse = loadFromFile("CategoryExtendedReport.response.json");
        var requestUrl = BASE_REPORT_URL + "&hid=91491"
                + "&modelid=1,2,3,4,5,6"
                + "&glfilter=201:1,2";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        String expected = loadFromFile("CategoryExtendedWithGlFilter.response.json");

        @Language("JSON")
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491,\n"
                + "    \"priceSegments\": [\n"
                + "      7,\n"
                + "      8\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-02-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {"
                + "      \"201\": [1, 2]\n"
                + "    }\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"WATERFALL\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getExtendedGrowth(body);
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "CategoryExtendedNoSales.csv"
    )
    @DisplayName("Рост категории: дополнительная информация, нет данных")
    void categoryExtendedNoSales() {
        String body = "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-03-01\",\n"
                + "    \"endDate\": \"2019-03-31\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"WATERFALL\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getExtendedGrowth(body)
        );

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\"code\":\"NO_DATA\",\"message\":\"There are no sales for this periods\"}",
                clientException.getResponseBodyAsString()
        );
    }

    private String getWaterfall(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(WATERFALL_PATH), body);
    }

    private String getExtendedGrowth(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(EXTENDED_INFO_PATH), body);
    }
}
