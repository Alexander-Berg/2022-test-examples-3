package ru.yandex.market.vendors.analytics.platform.controller.sales.table.growth;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.TestDatasource;
import ru.yandex.market.vendors.analytics.core.calculate.growth.request.model.TopModelsGrowthRequest;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.growth.GrowthDriversController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.WidgetTableViewController;

/**
 * Functional tests for
 * {@link GrowthDriversController#topModelsGrowth(TopModelsGrowthRequest, long, LanguageDTO)}
 * в представлени "Таблица".
 *
 * @author ogonek
 * @see WidgetTableViewController
 */
@DbUnitDataSet(before = "GrowthTest.before.csv")
@ClickhouseDbUnitDataSet(before = "GrowthTest.ch.before.csv")
public class TableViewControllerTopModelsGrowthTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "TableViewControllerTopModelsGrowthTest.data.csv"
    )
    void topModelsGrowth() {
        String body =
                //language=json
                "{\n"
                        + "  \"categoryFilter\": {\n"
                        + "    \"hid\": 91491\n"
                        + "  },\n"
                        + "  \"interval\": {\n"
                        + "    \"startDate\": \"2019-02-01\",\n"
                        + "    \"endDate\": \"2019-02-28\"\n"
                        + "  },\n"
                        + "  \"timeDetailing\": \"MONTH\",\n"
                        + "  \"sortBy\": \"MONEY\",\n"
                        + "  \"selectionInfo\": {\n"
                        + "    \"limit\": 10,\n"
                        + "    \"strategy\": \"MONEY\"\n"
                        + "  }\n"
                        + "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.MODELS_CONTRIBUTION, body, baseUrl());

        var expectedResponse = loadFromFile("TableViewControllerTopModelsGrowthTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }
}
