package ru.yandex.market.vendors.analytics.platform.controller.sales.table.growth;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.TestDatasource;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.WidgetTableViewController;

/**
 * @author ogonek
 * @see WidgetTableViewController
 */
@DbUnitDataSet(before = "GrowthTest.before.csv")
@ClickhouseDbUnitDataSet(before = "GrowthTest.ch.before.csv")
public class TableViewControllerBrandGrowthTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "TableViewControllerBrandGrowthTest.data.csv"
    )
    void brandsGrowth() {
        String body = ""
                + "{\n"
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
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.BRANDS_GROWTH, body, baseUrl());

        var expectedResponse = loadFromFile("TableViewControllerBrandGrowthTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }
}
