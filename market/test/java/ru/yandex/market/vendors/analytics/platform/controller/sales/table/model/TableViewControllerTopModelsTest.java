package ru.yandex.market.vendors.analytics.platform.controller.sales.table.model;


import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.model.request.ModelSalesRequest;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.model.ModelFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.model.ModelSalesController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Functional tests for {@link ModelSalesController#getSyntheticCategoryTopModels(ModelSalesRequest, Long)}
 * в представлении "Таблица".
 * !!! ModelFunctionalTest.csv приезжает сюда через аннотацию в ModelFunctionalTest !!!
 *
 * @author ogonek
 */
@ClickhouseDbUnitDataSet(before = "TableViewControllerTopModelsTest.ch.csv")
public class TableViewControllerTopModelsTest extends ModelFunctionalTest {

    @Test
    @ClickhouseDbUnitDataSet(before = "TopModels.before.csv")
    void topModels() {
        String reportResponse = loadFromFile("../../model/reportCategoryModelSalesResponse.json");

        var requestUrl = BASE_REPORT_URL + "&hid=91491"
                + "&modelid=1,3,5,7,9";

        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportResponse)
                );

        var request = "{\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-02-28\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"limit\": 100,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.CATEGORY_TOP_MODELS, request, baseUrl());
        var expectedResponse = loadFromFile("TableViewControllerTopModelsTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }
}
