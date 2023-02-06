package ru.yandex.market.vendors.analytics.platform.controller.sales.table.assortment;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.assortment.request.share.ModelMarketShareByShopsRequest;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.sales.shops.ShopSalesDAO;
import ru.yandex.market.vendors.analytics.core.model.common.GeoFilters;
import ru.yandex.market.vendors.analytics.core.model.common.StartEndDate;
import ru.yandex.market.vendors.analytics.core.model.common.socdem.SocdemFilter;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.service.sales.common.DBDatesInterval;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.shop.ShopsAssortmentController;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.TableViewTestHelper;
import ru.yandex.market.vendors.analytics.platform.controller.sales.table.WidgetTableViewController;

import static org.mockito.Mockito.when;
import static ru.yandex.market.vendors.analytics.core.model.enums.RegionType.FEDERAL_DISTRICT;
import static ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType.MOSCOW;
import static ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType.POPULATION_500K_1M;
import static ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType.POPULATION_MORE_1M;
import static ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType.SAINT_PETERSBURG;
import static ru.yandex.market.vendors.analytics.core.service.SalesTestUtils.createRawMarketShare;


/**
 * Functional tests for
 * {@link ShopsAssortmentController#modelMarketShareByRegion(ModelMarketShareByShopsRequest, LanguageDTO)}
 * в представлени "Таблица".
 *
 * @author fbokovikov
 * @see WidgetTableViewController
 */
@DbUnitDataSet(before = "TableViewControllerAssortmentTest.csv")
public class TableViewControllerModelShareTest extends FunctionalTest {

    @Autowired
    private ShopSalesDAO shopSalesDAO;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(shopSalesDAO);
    }

    @Test
    void shopModelShare() {
        var expectedGeoFilters = GeoFilters.builder()
                .federalDistrictIds(Set.of())
                .federalSubjectIds(Set.of())
                .clickhouseCityTypes(Set.of(MOSCOW, SAINT_PETERSBURG, POPULATION_MORE_1M, POPULATION_500K_1M))
                .build();

        when(shopSalesDAO.loadRawModelMarketShare(
                14206636L,
                91491L,
                FEDERAL_DISTRICT,
                new DBDatesInterval(new StartEndDate("2019-01-01", "2019-01-02"), TimeDetailing.DAY),
                expectedGeoFilters,
                SocdemFilter.empty()))
                .thenReturn(
                        List.of(
                                createRawMarketShare(15, 50, "2019-01-01", 3),
                                createRawMarketShare(10, 100, "2019-01-02", 3)
                        )
                );

        var request = ""
                + "{\n"
                + "  \"modelId\": 14206636,\n"
                + "  \"hid\": 91491,\n"
                + "  \"regionType\": \"FEDERAL_DISTRICT\",\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-01-02\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": []\n"
                + ",\n"
                + "    \"cityTypes\": [\"MILLION\",\"HALF_MILLION\"]\n"
                + "  },\n"
                + "  \"shareType\": \"COUNT\"\n"
                + "}";

        var actualResponse = TableViewTestHelper.getTableView(WidgetType.MODEL_SHOP_PERCENT, request, baseUrl());

        var expectedResponse = loadFromFile("TableViewControllerModelShareTest.json");

        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }
}
