package ru.yandex.market.vendors.analytics.platform.controller.sales.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.model.request.ModelSalesRequest;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Functional tests for {@link ModelSalesController#getCategoryTopRegions(ModelSalesRequest, long, LanguageDTO)}.
 *
 * @author fbokovikov
 */

@ClickhouseDbUnitDataSet(before = "CategoryRegionsShareTest.ch.csv")
@DbUnitDataSet(before = "CategoryRegionsShareTest.csv")
class CategoryRegionsShareTest extends ModelFunctionalTest {

    /**
     * Всего есть продажи по 5 регионам:
     * 1,Москва и Московская область,Moscow and Moscow State
     * 10174,Санкт-Петербург и Ленинградская область,Saint Petersburg and Leningrad State
     * 10995,Краснодарский край,Krasnodar Area
     * 11131,Самарская область,Samara State
     * 11162,Свердловская область,Sverdlovsk State
     * <p>
     * 10995,Краснодарский край,Krasnodar Area - скрыт на уровне категории
     * 10174,Санкт-Петербург и Ленинградская область,Saint Petersburg and Leningrad State скрыт на уровне вайтилиста моделей
     * 11131,Самарская область,Samara State скрыт на уровни % продаж моделей
     */
    @Test
    @ClickhouseDbUnitDataSet(before = "Region.clickhouse.before.csv")
    @DisplayName("Доля продаж категории по регионам")
    void regionsShare() {
        String reportResponse = loadFromFile("reportCategoryModelSalesResponse.json");
        var requestUrl = BASE_REPORT_URL + "&hid=91491"
                + "&modelid=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19"
                + "&glfilter=201:1,2&glfilter=202:100";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        var request = "{\n"
                + "  \"hid\": 91491,\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [\n"
                + "      3\n"
                + "    ]\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-02-28\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"limit\": 5,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {\n"
                + "      \"201\": [1, 2],\n"
                + "      \"202\": [100]\n"
                + "    }\n"
                + "  },\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        var response = regionsShare(DASHBOARD_ID, request);
        var expectedResponse = loadFromFile("CategoryRegionsShareTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                response
        );
    }

    @Test
    @DisplayName("Ошибка 400, если переданы и модель и репорт фильтры")
    void modelsAndReportFiltersRestricted() {
        var request = ""
                + "{\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-01-31\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"limit\": 10,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"models\": 1,\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {\n"
                + "      \"201\": [1, 2],\n"
                + "      \"202\": [100]\n"
                + "    }\n"
                + "  }\n"
                + "}";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> regionsShare(DASHBOARD_ID, request)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    @DisplayName("Ошибка 400, если все данные скрыты")
    void noDataTest() {
        var request = "{\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-01-31\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"limit\": 10,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"models\": [1],\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> regionsShare(DASHBOARD_ID, request)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        String expected = ""
                + "{\n"
                + "  \"code\": \"HIDDEN_DATA\",\n"
                + "  \"message\": \"Hiding exception with type REGION\",\n"
                + "  \"hidingType\": \"REGION\",\n"
                + "  \"hiddenIntervals\": [\n"
                + "    {\n"
                + "      \"startDate\": \"2019-01-01\",\n"
                + "      \"endDate\": \"2019-01-31\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"hidingMap\": {}\n"
                + "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    private String regionsShareUrl(long dashboardId) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/calculate/salesModels/regions/top")
                .queryParam("dashboardId", dashboardId)
                .toUriString();
    }

    private String regionsShare(long dashboardId, String body) {
        var regionsUrl = regionsShareUrl(dashboardId);
        return FunctionalTestHelper.postForJson(regionsUrl, body);
    }
}
