package ru.yandex.market.pricelabs.integration.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.api.PublicPartnerApi;
import ru.yandex.market.pricelabs.api.spring.csv.OpenCsvHttpMessageConverter;
import ru.yandex.market.pricelabs.api.spring.excel.ExcelHttpMessageConverter;
import ru.yandex.market.pricelabs.generated.server.pub.api.PartnerApi;
import ru.yandex.market.pricelabs.generated.server.pub.model.ExportFileParams;
import ru.yandex.market.pricelabs.generated.server.pub.model.ExportQueryResultType;
import ru.yandex.market.pricelabs.generated.server.pub.model.OffersStatsResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.ScheduledExportsResponse;
import ru.yandex.market.pricelabs.integration.AbstractIntegrationSpringConfiguration;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.tms.processing.TasksController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTest.checkResponse;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.REGION_ID;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.REGION_ID_2;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.REGION_ID_KZT;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_2;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.UNKNOWN_SHOP;

class PublicPartnerApiTest extends AbstractIntegrationSpringConfiguration {

    public static final Filter RELATIVE_MIN_PRICE_FILTER = Utils.fromJsonString(
            "{\"relativePrices\":{\"price_type_to\": \"MIN\"}}", Filter.class);
    public static final Filter RELATIVE_BELOW_AVG_PRICE_FILTER = Utils.fromJsonString(
            "{\"relativePrices\":{\"price_type_to\": \"AVG\"}}", Filter.class);
    public static final Filter RELATIVE_ABOVE_AVG_PRICE_FILTER = Utils.fromJsonString(
            "{\"relativePrices\":{\"price_type_from\":\"AVG\",\"price_from_strict_inequality\":true}}", Filter.class);

    @Autowired
    private PublicPartnerApi partnerApiBean;
    private PartnerApi partnerApi;

    @Autowired
    private TasksController controller;

    @Autowired
    private PublicApiTestInitializer initializer;

    static Object[][] testGetOffersStatsData() {
        return new Object[][]{
                {SHOP_ID, REGION_ID, new OffersStatsResponse()
                        .minPriceRatio(37.5).belowAvgPriceRatio(37.5).aboveAvgPriceRatio(62.5)},
                {SHOP_ID, REGION_ID_2, new OffersStatsResponse()
                        .minPriceRatio(0.0).belowAvgPriceRatio(0.0).aboveAvgPriceRatio(0.0)},
                {UNKNOWN_SHOP, REGION_ID, new OffersStatsResponse()
                        .minPriceRatio(0.0).belowAvgPriceRatio(0.0).aboveAvgPriceRatio(0.0)}
        };
    }

    static Object[][] exportPricesToPartner() {
        return new Object[][]{
                {SHOP_ID, REGION_ID, new Filter(),
                        "integration/api/exportPricesToPartner-full.csv",
                        "integration/api/exportPricesToPartner-full.xlsx"},
                {SHOP_ID, REGION_ID_2, new Filter(),
                        "integration/api/exportPricesToPartner-no-models.csv",
                        "integration/api/exportPricesToPartner-no-models.xlsx"},
                {SHOP_ID_2, REGION_ID, new Filter(),
                        "integration/api/exportPricesToPartner-no-offers.csv",
                        "integration/api/exportPricesToPartner-no-offers.xlsx"},
                {UNKNOWN_SHOP, REGION_ID, new Filter(),
                        "integration/api/exportPricesToPartner-no-offers.csv",
                        "integration/api/exportPricesToPartner-no-offers.xlsx"},
                {SHOP_ID, REGION_ID, RELATIVE_MIN_PRICE_FILTER,
                        "integration/api/exportPricesToPartner-min.csv",
                        "integration/api/exportPricesToPartner-min.xlsx"},
                {SHOP_ID, REGION_ID, RELATIVE_BELOW_AVG_PRICE_FILTER,
                        "integration/api/exportPricesToPartner-below-avg.csv",
                        "integration/api/exportPricesToPartner-below-avg.xlsx"},
                {SHOP_ID, REGION_ID, RELATIVE_ABOVE_AVG_PRICE_FILTER,
                        "integration/api/exportPricesToPartner-above-avg.csv",
                        "integration/api/exportPricesToPartner-above-avg.xlsx"},
                {SHOP_ID, REGION_ID_KZT, new Filter(),
                        "integration/api/exportPricesToPartner-full-with-currency-conversion.csv",
                        "integration/api/exportPricesToPartner-full-with-currency-conversion.xlsx"}
        };
    }

    static Object[][] partnerListExportsDifferentTypes() {
        return new Object[][]{
                {null, 1},
                {List.of(ExportQueryResultType.EXPORT_PRICES_TO_PARTNER_RESPONSE), 1},
                {List.of(ExportQueryResultType.ANALYTICS_PER_OFFER_RESPONSE), 0},
                {List.of(ExportQueryResultType.ANALYTICS_PER_OFFER_RESPONSE,
                        ExportQueryResultType.EXPORT_PRICES_RESPONSE), 0},
                {List.of(ExportQueryResultType.ANALYTICS_PER_OFFER_RESPONSE,
                        ExportQueryResultType.EXPORT_PRICES_RESPONSE,
                        ExportQueryResultType.EXPORT_PRICES_TO_PARTNER_RESPONSE), 1},
        };
    }

    @BeforeEach
    void init() {
        partnerApi = MockMvcProxy.buildProxy(PartnerApi.class, partnerApiBean,
                () -> List.of(new ExcelHttpMessageConverter(), new OpenCsvHttpMessageConverter()));
        TimingUtils.setTime(Utils.parseDateTimeAsInstant("2019-12-01T01:02:03"));
        testControls.initOnce(this.getClass(), () -> initializer.init());
        testControls.executeInParallel(
                () -> testControls.cleanupTasksService(),
                () -> testControls.resetExportService()
        );
    }

    @Test
    void checkShopForExistence() {
        var ret = checkResponse(partnerApi.partnerCheckShopExistGet(SHOP_ID));
        assertTrue(ret.getExists());
    }

    @Test
    void checkNonExistsShop() {
        var ret = checkResponse(partnerApi.partnerCheckShopExistGet(77777));
        assertFalse(ret.getExists());
    }

    private void assertScheduledExportsResponseEquals(ScheduledExportsResponse response, Long exportId,
                                                      ScheduledExportsResponse.StatusEnum status,
                                                      Integer shopId, Integer regionId) {
        assertEquals(exportId, response.getExportId());
        assertEquals(status, response.getStatus());
        assertEquals(ExportQueryResultType.EXPORT_PRICES_TO_PARTNER_RESPONSE, response.getType());
        assertNotNull(response.getOriginalRequest());
        if (response.getOriginalRequest() instanceof Map) {
            Map<?, ?> originalRequest = (Map<?, ?>) response.getOriginalRequest();
            assertEquals(shopId, originalRequest.get("shopId"));
            assertEquals(regionId, originalRequest.get("regionId"));
        }
    }

    private long sendPartnerScheduleExportPrices(Instant time, Integer shopId, Integer regionId) {
        TimingUtils.setTime(time);
        return checkResponse(partnerApi.partnerScheduleExportPricesPost(shopId, regionId,
                ExportFileParams.FileTypeEnum.csv.name(),
                null, null)).getExportId();
    }

}
