package ru.yandex.market.vendors.analytics.platform.controller.sales.table.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.model.request.RegionBrandsSalesRequest;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.model.ModelFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.model.ModelSalesController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;

/**
 * Functional tests in {@link ModelSalesController#getRegionBrandShare(RegionBrandsSalesRequest, long, LanguageDTO)}
 * in table view.
 *
 * @author fbokovikov
 */
@ClickhouseDbUnitDataSet(before = "TableViewControllerTopBrandsTest.csv")
public class TableViewControllerTopBrandsTest extends ModelFunctionalTest {

    @Test
    void topBrandsInRegions() {
        var requestBody = "{\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalSubjectIds\": [\n"
                + "      1\n"
                + "    ]\n"
                + "  },\n"
                + "  \"hid\": 91491,\n"
                + "  \"interval\": {\n"
                + "    \"endDate\": \"2019-01-31\",\n"
                + "    \"startDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"selectionInfo\": {\n"
                + "    \"brandLimit\": 8,\n"
                + "    \"regionLimit\": 6,\n"
                + "    \"strategy\": \"MONEY\"\n"
                + "  },\n"
                + "  \"socdemFilters\": [\n"
                + "    {\n"
                + "      \"ageSegment\": \"AGE_25_34\",\n"
                + "      \"gender\": \"FEMALE\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"timeDetailing\": \"NONE\"\n"
                + "}";

        var response = TableViewTestHelper.getTableView(WidgetType.CATEGORY_BRANDS_SHARE_BY_REGIONS, requestBody, baseUrl());
        var expectedResponse = loadFromFile("TableViewControllerTopBrandsTest.json");

        JsonTestUtil.assertEquals(expectedResponse, response);
    }
}
