package ru.yandex.market.vendors.analytics.platform.controller.sales.table.growth;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.TestDatasource;
import ru.yandex.market.vendors.analytics.core.calculate.growth.request.GrowthRequest;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.growth.GrowthDriversController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.WidgetTableViewController;

/**
 * Functional tests for
 * {@link GrowthDriversController#categoryGrowthExtended(GrowthRequest, LanguageDTO)}
 * в представлени "Таблица".
 *
 * @author ogonek
 * @see WidgetTableViewController
 */
@DbUnitDataSet(before = "GrowthTest.before.csv")
@ClickhouseDbUnitDataSet(before = "GrowthTest.ch.before.csv")
public class TableViewControllerCategoryGrowthTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "TableViewControllerCategoryGrowthTest.data.csv"
    )
    void categoryGrowth() {
        String body =
                //language=json
                "{\n" +
                        "  \"categoryFilter\": {\n" +
                        "    \"hid\": 91491,\n" +
                        "    \"priceSegments\": [\n" +
                        "      7,\n" +
                        "      8\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"interval\": {\n" +
                        "    \"startDate\": \"2019-02-01\",\n" +
                        "    \"endDate\": \"2019-02-28\"\n" +
                        "  },\n" +
                        "  \"timeDetailing\": \"MONTH\"\n" +
                        "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.CATEGORY_GROWTH_WATERFALL, body, baseUrl());

        var expectedResponse = loadFromFile("TableViewControllerCategoryGrowthTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }

}
