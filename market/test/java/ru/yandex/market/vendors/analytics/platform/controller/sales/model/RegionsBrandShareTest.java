package ru.yandex.market.vendors.analytics.platform.controller.sales.model;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;

/**
 * @author fbokovikov
 */
@DbUnitDataSet(before = "RegionsBrandShareTest.csv")
@ClickhouseDbUnitDataSet(before = "RegionsBrandShareTest.clickhouse.csv")
class RegionsBrandShareTest extends ModelFunctionalTest {

    /**
     * Открыт только один бренд из 3.
     */
    @Test
    @DisplayName("Получение доли брендов по регионам без подсчета региона Others")
    void withoutOtherRegions() {
        var requestBody = "{\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalSubjectIds\": [\n"
                + "      1\n"
                + "    ]\n"
                + "  },\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-01-31\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"brandLimit\": 8,\n"
                + "    \"regionLimit\": 6,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                + "  \"socdemFilters\": [\n"
                + "    {\n"
                + "      \"ageSegment\": \"AGE_25_34\",\n"
                + "      \"gender\": \"FEMALE\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        var response = regionBrandShare(DASHBOARD_ID, requestBody);
        var expectedResponse = loadFromFile("RegionsBrandShareTest.json");

        JsonTestUtil.assertEquals(expectedResponse, response);
    }

    /**
     * Выбранные в фильтре бренды должны отобразиться в отчёте.
     */
    @Test
    @DisplayName("Получение доли брендов по регионам с одним выбранным брендом")
    void filteredByOneBrand() {
        var requestBody = "{\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalSubjectIds\": [\n"
                + "    ]\n"
                + "  },\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-01-31\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"brandLimit\": 2,\n"
                + "    \"regionLimit\": 6,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                +"   \"brands\": [\n"
                +"     3\n"
                +"   ],\n"
                + "  \"socdemFilters\": [\n"
                + "  ],\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"visualization\": \"TABLE\",\n"
                + "  \"measure\": \"MONEY_PART\"\n"
                + "}";

        var response = regionBrandShare(DASHBOARD_ID, requestBody);
        var expectedResponse = loadFromFile("RegionsBrandShareTest.json");

        JsonTestUtil.assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Ошибка 400, если все данные скрыты")
    void noDataTest() {
        var requestBody = "{\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalSubjectIds\": [\n"
                + "      1\n"
                + "    ]\n"
                + "  },\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2018-03-31\",\n"
                + "    \"startDate\": \"2018-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"brandLimit\": 8,\n"
                + "    \"regionLimit\": 6,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                + "  \"socdemFilters\": [\n"
                + "    {\n"
                + "      \"ageSegment\": \"AGE_25_34\",\n"
                + "      \"gender\": \"FEMALE\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";


        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> regionBrandShare(DASHBOARD_ID, requestBody)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = ""
                + "{\n"
                + "  \"code\": \"HIDDEN_DATA\",\n"
                + "  \"message\": \"Hiding exception with type REGION\",\n"
                + "  \"hidingType\": \"REGION\",\n"
                + "  \"hiddenIntervals\": [\n"
                + "    {\n"
                + "      \"startDate\": \"2018-01-01\",\n"
                + "      \"endDate\": \"2018-03-31\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"hidingMap\": {}\n"
                + "}";
        JsonTestUtil.assertEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    void monthTimeDetailing() {
        var request = ""
                + "{\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-01-31\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"brandLimit\": 8,\n"
                + "    \"regionLimit\": 6,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> regionBrandShare(DASHBOARD_ID, request)
        );

        var expected = ""
                + "{\"code\":\"BAD_REQUEST\",\"message\":\"Time detailing NONE is expected\"}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    private String regionBrandShareUrl(long dashboardId) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/calculate/salesModels/regions/brands/share")
                .queryParam("dashboardId", dashboardId)
                .toUriString();
    }

    private String regionBrandShare(long dashboardId, String body) {
        var regionBrandUrl = regionBrandShareUrl(dashboardId);
        return FunctionalTestHelper.postForJson(regionBrandUrl, body);
    }
}
