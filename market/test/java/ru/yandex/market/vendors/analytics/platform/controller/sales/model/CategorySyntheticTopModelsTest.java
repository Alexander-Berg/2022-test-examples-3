package ru.yandex.market.vendors.analytics.platform.controller.sales.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.calculate.model.request.ModelSalesRequest;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;

import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

/**
 * Functional tests for {@link ModelSalesController#getSyntheticCategoryTopModels(ModelSalesRequest, Long)}.
 *
 * @author ogonek
 */
@ClickhouseDbUnitDataSet(before = "CategorySyntheticTopModelsTest.ch.csv")
class CategorySyntheticTopModelsTest extends ModelFunctionalTest {

    /**
     * Синт. хитлист, в репорт не ходим, известны 5/7 моделей.
     */
    @Test
    @DisplayName("Топовые модели во всей категории")
    void categorySyntheticTopModelsWithoutFilters() {
        var request = ""
                + "{\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-01-31\"\n"
                + "  },"
                + "\"selectionInfo\":{  \n"
                + "      \"limit\":6,\n"
                + "      \"strategy\":\"MONEY\"\n"
                + "   },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"TABLE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        var response = categorySyntheticTopModels(DASHBOARD_ID, request);

        var expectedResponse = loadFromFile("CategorySyntheticTopModelsTest.json");
        assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Поисковые запросы для топовых моделей во всей категории")
    void topModelsSearchWithoutFilters() {
        var request = ""
                + "{\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-01-31\"\n"
                + "  },"
                + "\"selectionInfo\":{  \n"
                + "      \"limit\":100,\n"
                + "      \"strategy\":\"SEARCH_QUERIES\"\n"
                + "   },\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"visualization\": \"TABLE\",\n"
                + "  \"measure\": \"SEARCH_COUNT\"\n"
                + "}";
        var response = categorySyntheticTopModels(DASHBOARD_ID, request);

        var expectedResponse = loadFromFile("CategorySyntheticTopModelsTest.search.json");
        assertEquals(expectedResponse, response);
    }

    private String categorySyntheticTopModels(long dashboardId, String body) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/calculate/salesModels/category/synthetic/top")
                .queryParam("dashboardId", dashboardId)
                .toUriString();
        return FunctionalTestHelper.postForJson(url, body);
    }
}
