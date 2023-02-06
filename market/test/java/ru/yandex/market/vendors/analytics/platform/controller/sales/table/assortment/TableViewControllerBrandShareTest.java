package ru.yandex.market.vendors.analytics.platform.controller.sales.table.assortment;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.assortment.request.share.BrandMarketShareByShopsCategoryRequest;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.sales.shops.ShopSalesDAO;
import ru.yandex.market.vendors.analytics.core.model.common.GeoFilters;
import ru.yandex.market.vendors.analytics.core.model.common.StartEndDate;
import ru.yandex.market.vendors.analytics.core.model.common.socdem.SocdemFilter;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.model.sales.common.CategoryPriceSegmentsFilter;
import ru.yandex.market.vendors.analytics.core.service.sales.common.DBDatesInterval;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.shop.ShopsAssortmentController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.WidgetTableViewController;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.market.vendors.analytics.core.service.SalesTestUtils.createRawMarketShare;
import static ru.yandex.market.vendors.analytics.core.service.sales.shop.ShopAssortmentTestUtils.createCategoryPriceSegmentsFilter;

/**
 * Functional tests for
 * {@link ShopsAssortmentController#brandMarketShareByCategory(BrandMarketShareByShopsCategoryRequest, LanguageDTO)}
 * в представлении "Таблица".
 *
 * @author fbokovikov
 * @see WidgetTableViewController
 */
@DbUnitDataSet(before = "TableViewControllerAssortmentTest.csv")
public class TableViewControllerBrandShareTest extends FunctionalTest {

    @Autowired
    private ShopSalesDAO shopSalesDAO;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(shopSalesDAO);
    }

    @Test
    void brandShare() {
        long brandId = 153043L;
        StartEndDate interval = new StartEndDate("2019-01-01", "2019-02-28");

        reset(shopSalesDAO);

        DBDatesInterval dbInterval = new DBDatesInterval(interval, TimeDetailing.MONTH);
        GeoFilters geoFilters = GeoFilters.empty();
        SocdemFilter socdemFilter = SocdemFilter.empty();
        Set<CategoryPriceSegmentsFilter> categoriesFilter = Set.of(
                createCategoryPriceSegmentsFilter(91491, Set.of(8)),
                createCategoryPriceSegmentsFilter(91013, Collections.emptySet())
        );
        Set<Long> modelIds = Collections.emptySet();

        when(shopSalesDAO.loadBrandRawMarketSharesByCategory(
                eq(brandId),
                eq(modelIds),
                eq(dbInterval),
                eq(geoFilters),
                eq(categoriesFilter),
                eq(socdemFilter))
        ).thenReturn(
                List.of(
                        createRawMarketShare(600, 1000, "2019-01-01", 91491),
                        createRawMarketShare(150, 1000, "2019-01-01", 91013),

                        createRawMarketShare(550, 1100, "2019-02-01", 91491)
                )
        );

        var request = ""
                + "{\n"
                + "  \"brandId\": 153043,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"shareType\": \"PERCENT\",\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"categoriesFilter\": [\n"
                + "    {\n"
                + "      \"hid\": 91491,\n"
                + "      \"priceSegments\": [8]\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 91013\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.BRAND_PERCENT_BY_SHOPS, request, baseUrl());
        var expectedResponse = loadFromFile("TableViewControllerBrandShareTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }
}
