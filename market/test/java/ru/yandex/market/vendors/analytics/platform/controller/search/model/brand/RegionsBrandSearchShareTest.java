package ru.yandex.market.vendors.analytics.platform.controller.search.model.brand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * @author fbokovikov
 */
@DbUnitDataSet(before = "RegionsBrandSearchShareTest.csv")
class RegionsBrandSearchShareTest extends CalculateFunctionalTest {

    @Test
    @ClickhouseDbUnitDataSet(before = "RegionsBrandSearchShareTest.oneRegion.clickhouse.csv")
    @DisplayName("Получение доли брендов по регионам без подсчета региона Others")
    void oneRegion() {
        var requestBody = ""
                + "{\n"
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
                + "    \"strategy\": \"SEARCH_QUERIES\"\n"
                + "  },\n"
                + "  \"socdemFilters\": [\n"
                + "    {\n"
                + "      \"ageSegment\": \"AGE_25_34\",\n"
                + "      \"gender\": \"FEMALE\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";

        var response = regionBrandShare(100, requestBody);
        var expectedResponse = loadFromFile("RegionsBrandSearchShareTest.oneRegion.json");

        JsonTestUtil.assertEquals(expectedResponse, response);
    }

    @Test
    @ClickhouseDbUnitDataSet(before = "RegionsBrandSearchShareTest.severalRegions.clickhouse.csv")
    @DisplayName("Получение доли брендов по нескольким регионам")
    void severalRegions() {
        var requestBody = ""
                + "{\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-01-31\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"brandLimit\": 3,\n"
                + "    \"regionLimit\": 2,\n"
                + "    \"strategy\": \"SEARCH_QUERIES\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";

        var response = regionBrandShare(100, requestBody);
        var expectedResponse = loadFromFile("RegionsBrandSearchShareTest.severalRegions.json");

        JsonTestUtil.assertEquals(expectedResponse, response);
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
