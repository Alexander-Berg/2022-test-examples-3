package ru.yandex.market.vendors.analytics.platform.controller.sales.table.category.brand;

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
 * @author sergeymironov
 * @see WidgetTableViewController
 */
@DbUnitDataSet(before = "BrandsMarketShareTest.before.csv")
@ClickhouseDbUnitDataSet(before = "BrandsMarketShareTest.ch.before.csv")
public class TableViewControllerBrandsMarketShareTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "TableViewControllerBrandsMarketShareTest.data.csv"
    )
    void calculateTable() {
        String body =
                //language=json
                "{\n" +
                        "   \"brands\":null,\n" +
                        "   \"measure\":\"MONEY_PART\",\n" +
                        "   \"interval\":{\n" +
                        "    \"startDate\": \"2019-01-01\",\n" +
                        "    \"endDate\": \"2019-02-28\"\n" +
                        "   },\n" +
                        "   \"modelIds\":null,\n" +
                        "   \"geoFilters\":null,\n" +
                        "   \"reportFilters\":null,\n" +
                        "   \"socdemFilters\":null,\n" +
                        "   \"timeDetailing\":\"MONTH\",\n" +
                        "   \"visualization\":\"PIE\",\n" +
                        "   \"categoryFilter\":{\n" +
                        "      \"hid\":91491,\n" +
                        "      \"priceSegments\":null\n" +
                        "   },\n" +
                        "   \"topBrandsCount\":7,\n" +
                        "   \"topSelectionStrategy\":null\n" +
                        "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.CATEGORY_BRANDS_MARKET_SHARE, body, baseUrl());

        var expectedResponse = loadFromFile("TableViewControllerBrandsMarketShareTest.json");

        JsonTestUtil.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DbUnitDataSet(
            dataSource = TestDatasource.CLICKHOUSE,
            before = "TableViewControllerBrandsMarketShareTest.data.search.csv"
    )
    void calculateSearchTable() {
        String body =
                //language=json
                "{\n" +
                        "   \"brands\":null,\n" +
                        "   \"measure\":\"SEARCH_COUNT\",\n" +
                        "   \"interval\":{\n" +
                        "    \"startDate\": \"2019-01-01\",\n" +
                        "    \"endDate\": \"2019-02-28\"\n" +
                        "   },\n" +
                        "   \"modelIds\":null,\n" +
                        "   \"geoFilters\":null,\n" +
                        "   \"reportFilters\":null,\n" +
                        "   \"socdemFilters\":null,\n" +
                        "   \"timeDetailing\":\"MONTH\",\n" +
                        "   \"visualization\":\"PIE\",\n" +
                        "   \"categoryFilter\":{\n" +
                        "      \"hid\":91491,\n" +
                        "      \"priceSegments\":null\n" +
                        "   },\n" +
                        "   \"topBrandsCount\":7,\n" +
                        "   \"topSelectionStrategy\":null\n" +
                        "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.CATEGORY_BRANDS_MARKET_SHARE, body, baseUrl());

        var expectedResponse = loadFromFile("TableViewControllerBrandsMarketShareTest.search.json");

        JsonTestUtil.assertEquals(expectedResponse, actualResponse);
    }

}
