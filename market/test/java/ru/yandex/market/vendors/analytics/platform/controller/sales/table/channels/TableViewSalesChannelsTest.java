package ru.yandex.market.vendors.analytics.platform.controller.sales.table.channels;

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
@DbUnitDataSet(before = "SalesChannelsTest.before.csv")
@ClickhouseDbUnitDataSet(before = "SalesChannelsTest.ch.before.csv")
public class TableViewSalesChannelsTest extends FunctionalTest {

    @Test
    void calculateTable() {
        String body = "{"
                + "  \"hid\": 100,"
                + "  \"interval\": {"
                + "    \"startDate\": \"2020-01-01\","
                + "    \"endDate\": \"2020-03-15\""
                + "  },"
                + "  \"timeDetailing\": \"MONTH\","
                + "  \"visualization\": \"LINE\","
                + "  \"measure\": \"MONEY_PART\","
                + "  \"fullData\": true"
                + "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.CATEGORY_SALES_CHANNELS, body, baseUrl());

        var expectedResponse = loadFromFile("TableViewSalesChannelsTest.response.json");

        JsonTestUtil.assertEquals(expectedResponse, actualResponse);
    }

}
