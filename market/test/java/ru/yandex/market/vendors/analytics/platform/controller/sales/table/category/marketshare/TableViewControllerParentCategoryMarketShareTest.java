package ru.yandex.market.vendors.analytics.platform.controller.sales.table.category.marketshare;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.TestDatasource;
import ru.yandex.market.vendors.analytics.core.calculate.category.request.CategoriesMarketShareRequest;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.category.CategorySalesController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.WidgetTableViewController;

/**
 * Functional tests for
 * {@link CategorySalesController#getParentMarketShare(CategoriesMarketShareRequest, long, Long, LanguageDTO)}
 * в представлении "Таблица".
 *
 * @author ogonek
 * @see WidgetTableViewController
 */
@DbUnitDataSet(before = "ParentCategoryMarketShareTest.before.csv")
public class TableViewControllerParentCategoryMarketShareTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "TableViewControllerParentCategoryMarketShareTest.data.csv"
    )
    void calculateTable() {
        String body =
                //language=json
                "{\n" +
                        "   \"hid\":91491,\n" +
                        "   \"measure\":\"MONEY\",\n" +
                        "   \"interval\":{\n" +
                        "      \"endDate\":\"2019-02-28\",\n" +
                        "      \"startDate\":\"2019-01-01\"\n" +
                        "   },\n" +
                        "   \"timeDetailing\":\"MONTH\",\n" +
                        "   \"visualization\":\"PIE\",\n" +
                        "   \"subCategoryHids\":null,\n" +
                        "   \"topCategoriesCount\":null,\n" +
                        "   \"topSelectionStrategy\":null\n" +
                        "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.PARENT_CATEGORY_MARKET_SHARE, body, baseUrl());

        var expectedResponse = loadFromFile("TableViewControllerParentCategoryMarketShareTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }
}
