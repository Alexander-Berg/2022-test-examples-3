package ru.yandex.market.vendors.analytics.platform.controller.sales.table.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.model.request.ModelSalesRequest;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.model.ModelFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.model.ModelSalesController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;

/**
 * Functional tests for
 * {@link ModelSalesController#getCategoryTopRegions(ModelSalesRequest, long, LanguageDTO)}.
 * в представлении "Таблица".
 *
 * @author fbokovikov
 */
@ClickhouseDbUnitDataSet(before = "TableViewControllerTopRegionsTest.csv")
public class TableViewControllerTopRegionsTest extends ModelFunctionalTest {

    @Test
    @ClickhouseDbUnitDataSet(before = "ref_panel_aggregated.csv")
    void tableView() {
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
                + "  \"timeDetailing\": \"MONTH\"\n"
                + "}";

        var actualResponse =
                TableViewTestHelper.getTableView(WidgetType.CATEGORY_MARKET_SHARE_BY_REGIONS, request, baseUrl());
        var expectedResponse = loadFromFile("TableViewControllerTopRegionsTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }
}
