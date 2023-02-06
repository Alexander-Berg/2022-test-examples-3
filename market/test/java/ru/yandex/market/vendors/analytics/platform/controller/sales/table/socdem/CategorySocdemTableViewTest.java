package ru.yandex.market.vendors.analytics.platform.controller.sales.table.socdem;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.socdem.request.SocdemDistributionRequest;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.socdem.SocdemSalesController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;

/**
 * Functional tests for
 * {@link SocdemSalesController#socdemDistribution(SocdemDistributionRequest, long)}
 * table view.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "CategorySocdemTableViewTest.csv")
@ClickhouseDbUnitDataSet(before = "CategorySocdemTableViewTest.clickhouse.csv")
public class CategorySocdemTableViewTest extends FunctionalTest {

    @Test
    void tableView() {
        var request = ""
                + "{\n"
                + "  \"brands\": [1, 2, 3],\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 91491\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-02-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"NONE\",\n"
                + "  \"visualization\": \"TABLE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        var response = TableViewTestHelper.getTableView(WidgetType.CATEGORY_SOC_DEM_DISTRIBUTION, request, baseUrl());
        var expected = loadFromFile("CategorySocdemTableViewTest.json");
        JsonTestUtil.assertEquals(expected, response);
    }
}
