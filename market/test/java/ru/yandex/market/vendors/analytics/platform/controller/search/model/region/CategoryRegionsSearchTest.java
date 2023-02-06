package ru.yandex.market.vendors.analytics.platform.controller.search.model.region;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * @author antipov93
 */
@DbUnitDataSet(before = "CategoryRegionsSearchTest.csv")
class CategoryRegionsSearchTest extends CalculateFunctionalTest {

    @Test
    @ClickhouseDbUnitDataSet(before = "CategoryRegionsSearchTest.ch.csv")
    @DisplayName("Доля поисковых интересов по регионам")
    void regionsShare() {
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
                + "    \"strategy\": \"SEARCH_QUERIES\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";

        var response = regionsShare(100, request);
        var expectedResponse = loadFromFile("CategoryRegionsSearchTest.response.json");

        JsonTestUtil.assertEquals(expectedResponse, response);
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
