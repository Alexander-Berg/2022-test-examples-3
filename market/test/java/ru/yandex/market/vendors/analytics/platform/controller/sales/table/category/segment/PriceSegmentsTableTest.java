package ru.yandex.market.vendors.analytics.platform.controller.sales.table.category.segment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "PriceSegmentsTableTest.postgres.csv")
@ClickhouseDbUnitDataSet(before = "PriceSegmentsTableTest.clickhouse.csv")
public class PriceSegmentsTableTest extends FunctionalTest {

    @Test
    void generateTable() {
        String request = ""
                + "{\n"
                + "  \"categoryFilter\": {\n"
                + "    \"hid\": 31\n"
                + "  },\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-02-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"MONTH\"\n"
                + "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.CATEGORY_PRICE_SEGMENTS, request, baseUrl());
        var expectedResponse = loadFromFile("PriceSegmentsTableTest.response.json");

        JsonTestUtil.assertEquals(expectedResponse, actualResponse);
    }
}
