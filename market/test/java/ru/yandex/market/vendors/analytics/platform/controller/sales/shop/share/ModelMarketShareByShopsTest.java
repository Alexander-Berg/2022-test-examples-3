package ru.yandex.market.vendors.analytics.platform.controller.sales.shop.share;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.assortment.request.share.ModelMarketShareByShopsRequest;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.sales.shops.ShopSalesDAO;
import ru.yandex.market.vendors.analytics.core.model.common.GeoFilters;
import ru.yandex.market.vendors.analytics.core.model.common.StartEndDate;
import ru.yandex.market.vendors.analytics.core.model.common.socdem.SocdemFilter;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.service.sales.common.DBDatesInterval;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.shop.ShopsAssortmentController;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.market.vendors.analytics.core.model.enums.RegionType.FEDERAL_DISTRICT;
import static ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType.MOSCOW;
import static ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType.POPULATION_500K_1M;
import static ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType.POPULATION_MORE_1M;
import static ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType.SAINT_PETERSBURG;
import static ru.yandex.market.vendors.analytics.core.service.SalesTestUtils.createRawMarketShare;

/**
 * Functional tests for {@link ShopsAssortmentController#modelMarketShareByRegion(
 * ModelMarketShareByShopsRequest, LanguageDTO)}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "MarketShareByShopsTest.before.csv")
public class ModelMarketShareByShopsTest extends CalculateFunctionalTest {
    private static final String MODEL_SHOPS_SHARE_PATH = "/shops/assortment/share/model";

    @Autowired
    private ShopSalesDAO shopSalesDAO;

    @Test
    @DisplayName("Группировка по дням")
    void dailyModelMarketShareByFederalDistrict() {
        reset(shopSalesDAO);

        var expectedGeoFilters = GeoFilters.builder()
                .federalDistrictIds(Set.of(3L, 40L, 52L))
                .federalSubjectIds(Set.of())
                .clickhouseCityTypes(Set.of(MOSCOW, SAINT_PETERSBURG, POPULATION_MORE_1M, POPULATION_500K_1M))
                .build();

        when(shopSalesDAO.loadRawModelMarketShare(
                eq(14206636L),
                eq(91491L),
                eq(FEDERAL_DISTRICT),
                eq(new DBDatesInterval(new StartEndDate("2019-01-01", "2019-01-02"), TimeDetailing.DAY)),
                eq(expectedGeoFilters),
                eq(SocdemFilter.empty())
        )).thenReturn(List.of(
                createRawMarketShare(15, 50, "2019-01-01", 3),
                createRawMarketShare(12, 24, "2019-01-01", 40),
                createRawMarketShare(11, 11, "2019-01-01", 52),

                createRawMarketShare(10, 100, "2019-01-02", 3),
                createRawMarketShare(20, 100, "2019-01-02", 40),
                createRawMarketShare(0, 100, "2019-01-02", 52)
        ));

        String expected = loadFromFile("ModelMarketShareByShops.response.json");

        String body = "{\n"
                + "  \"modelId\": 14206636,\n"
                + "  \"hid\": 91491,\n"
                + "  \"regionType\": \"FEDERAL_DISTRICT\",\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-01-02\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"geoFilters\": {\n"
                + "    \"federalDistrictIds\": [3, 40, 52]\n"
                + ",\n"
                + "    \"cityTypes\": [\"MILLION\",\"HALF_MILLION\"]\n"
                + "  },\n"
                + "  \"shareType\": \"COUNT\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getModelShopsShare(body);
        JsonTestUtil.assertEquals(expected, actual);
    }

    private String getModelShopsShare(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(MODEL_SHOPS_SHARE_PATH), body);
    }
}
