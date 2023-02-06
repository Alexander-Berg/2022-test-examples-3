package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
public class ModelbidsStatsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private WireMockServer reportMock;

    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetMetricsPpGroupFilter/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetMetricsPpGroupFilter() {
        String filter = getStringResource("/testGetMetricsPpGroupFilter/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&metrics=CTR_NEW" +
                        "&metrics=CR" +
                        "&metrics=CPO" +
                        "&platform=DESKTOP" +
                        "&platform=TOUCH" +
                        "&platform=APPLICATION"+
                        "&ppGroup=SEARCH" +
                        "&ppGroup=KKM",
                filter);
        String expected = getStringResource("/testGetMetricsPpGroupFilter/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetSupplierMetricsNoHide/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetSupplierMetricsNoHide() {
        String filter = getStringResource("/testGetSupplierMetricsNoHide/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&metrics=ORDERS" +
                        "&metrics=CPO" +
                        "&platform=DESKTOP" +
                        "&platform=TOUCH" +
                        "&platform=APPLICATION"+
                        "&ppGroup=SEARCH" +
                        "&ppGroup=KKM",
                filter);
        String expected = getStringResource("/testGetSupplierMetricsNoHide/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetSupplierMetricsHide/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetSupplierMetricsHide() {
        String filter = getStringResource("/testGetSupplierMetricsHide/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&metrics=ORDERS" +
                        "&metrics=CPO" +
                        "&platform=DESKTOP" +
                        "&platform=TOUCH" +
                        "&platform=APPLICATION"+
                        "&ppGroup=SEARCH" +
                        "&ppGroup=KKM",
                filter);
        String expected = getStringResource("/testGetSupplierMetricsHide/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetShows/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetShows() {
        String filter = getStringResource("/testGetShows/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                "&from=1522530000000" +
                "&to=1525035600000" +
                "&scale=MONTH" +
                "&platform=DESKTOP&platform=TOUCH&platform=APPLICATION" +
                "&metrics=SHOWS&metrics=CTR&metrics=AVG_POSITION",
                filter);

        String expected = getStringResource("/testGetShows/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetShowsFilterByPromotion/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetShowsFilterByPromotion/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testGetShowsFilterByPromotion() {
        String filter = getStringResource("/testGetShowsFilterByPromotion/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&promotionType=PROMOTED" +
                        "&metrics=SHOWS&metrics=CTR&metrics=AVG_POSITION",
                filter);

        String expected = getStringResource("/testGetShowsFilterByPromotion/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetChargesNoPromotion/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetChargesNoPromotion/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetChargesNoPromotion() {
        String filter = getStringResource("/testGetChargesNoPromotion/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&promotionType=NON_PROMOTED" +
                        "&ppGroup=SEARCH" +
                        "&ppGroup=SUPER_HORIZONTAL_INCUT" +
                        "&ppGroup=KKM" +
                        "&ppGroup=OTHER" +
                        "&metrics=CHARGES",
                filter);

        String expected = getStringResource("/testGetChargesNoPromotion/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetChargesNoPromotionNoPPGroups/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetChargesNoPromotionNoPPGroups/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetChargesNoPromotionNoPPGroups() {
        String filter = getStringResource("/testGetChargesNoPromotionNoPPGroups/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&promotionType=NON_PROMOTED" +
                        "&metrics=CHARGES",
                filter);

        String expected = getStringResource("/testGetChargesNoPromotionNoPPGroups/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetShowsFilterByModel/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetShowsFilterByModel() {
        String filter = getStringResource("/testGetShowsFilterByModel/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&platform=DESKTOP&platform=TOUCH&platform=APPLICATION" +
                        "&metrics=SHOWS&metrics=CTR&metrics=AVG_POSITION",
                filter);

        String expected = getStringResource("/testGetShowsFilterByModel/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetShowsFilterWithEmptyResult/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetShowsFilterWithEmptyResult() {
        String filter = getStringResource("/testGetShowsFilterWithEmptyResult/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&promotionType=PROMOTED" +
                        "&platform=DESKTOP&platform=TOUCH&platform=APPLICATION" +
                        "&metrics=SHOWS&metrics=CTR&metrics=AVG_POSITION" +
                        "&metrics=SHOWS" +
                        "&metrics=TARGET_ACTIONS" +
                        "&metrics=ORDERS" +
                        "&metrics=CPO" +
                        "&metrics=CR" +
                        "&metrics=CTR_NEW",
                filter);

        String expected = getStringResource("/testGetShowsFilterWithEmptyResult/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetStatsByModel/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetStatsByModel() {
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withBody(getStringResource("/testGetStatsByModel/report_response_blue.json"))));

        String filter = getStringResource("/testGetStatsByModel/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics/byModel?uid=100500" +
                "&from=1522530000000" +
                "&to=1525035600000" +
                "&metrics=CLICKS_TOTAL" +
                "&metrics=CLICKS_PAID" +
                "&metrics=CLICKOUTS_RATE" +
                "&metrics=AVG_POSITION" +
                "&metrics=CLICKOUTS_TOTAL" +
                "&metrics=AVG_CLICK_PRICE" +
                "&metrics=AVG_BID" +
                "&metrics=CLICKOUTS_FREE" +
                "&metrics=CTR" +
                "&metrics=CLICKOUTS_PAID" +
                "&metrics=CHARGES" +
                "&metrics=CLICKS_FREE" +
                "&metrics=SHOWS",
                filter);

        String expected = getStringResource("/testGetStatsByModel/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetStatsByModelSorting/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetStatsByModelSorting() {
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withBody(getStringResource("/testGetStatsByModel/report_response_blue.json"))));

        String filter = getStringResource("/testGetStatsByModelSorting/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics/byModel?uid=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&metrics=CLICKS_TOTAL" +
                        "&metrics=CLICKS_PAID" +
                        "&metrics=CLICKOUTS_RATE" +
                        "&metrics=AVG_POSITION" +
                        "&metrics=CLICKOUTS_TOTAL" +
                        "&metrics=AVG_CLICK_PRICE" +
                        "&metrics=AVG_BID" +
                        "&metrics=CLICKOUTS_FREE" +
                        "&metrics=CTR" +
                        "&metrics=CLICKOUTS_PAID" +
                        "&metrics=CHARGES" +
                        "&metrics=CLICKS_FREE" +
                        "&metrics=SHOWS" +
                        "&sortBy=CLICKS_PAID" +
                        "&sortOrder=DESC",
                filter
        );

        String expected = getStringResource("/testGetStatsByModelSorting/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetStatsByModelFilterByPlatform/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetStatsByModelFilterByPlatform() {
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withBody(getStringResource("/testGetStatsByModel/report_response_blue.json"))));

        String filter = getStringResource("/testGetStatsByModelFilterByPlatform/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics/byModel?uid=100500" +
                "&from=1522530000000" +
                "&to=1525035600000" +
                "&platform=DESKTOP" +
                "&metrics=CLICKS_TOTAL" +
                "&metrics=CLICKS_PAID" +
                "&metrics=CLICKOUTS_RATE" +
                "&metrics=AVG_POSITION" +
                "&metrics=CLICKOUTS_TOTAL" +
                "&metrics=AVG_CLICK_PRICE" +
                "&metrics=AVG_BID" +
                "&metrics=CLICKOUTS_FREE" +
                "&metrics=CTR" +
                "&metrics=CLICKOUTS_PAID" +
                "&metrics=CHARGES" +
                "&metrics=CLICKS_FREE" +
                "&metrics=SHOWS",
                filter
        );

        String expected = getStringResource("/testGetStatsByModelFilterByPlatform/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetStatsByModelFilterByText/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetStatsByModelFilterByText() {
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withBody(getStringResource("/testGetStatsByModel/report_response_blue.json"))));

        String filter = getStringResource("/testGetStatsByModelFilterByText/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics/byModel?uid=100500" +
                "&from=1522530000000" +
                "&to=1525035600000" +
                "&text=64GB 13" +
                "&metrics=CLICKS_TOTAL" +
                "&metrics=CLICKS_PAID" +
                "&metrics=CLICKOUTS_RATE" +
                "&metrics=AVG_POSITION" +
                "&metrics=CLICKOUTS_TOTAL" +
                "&metrics=AVG_CLICK_PRICE" +
                "&metrics=AVG_BID" +
                "&metrics=CLICKOUTS_FREE" +
                "&metrics=CTR" +
                "&metrics=CLICKOUTS_PAID" +
                "&metrics=CHARGES" +
                "&metrics=CLICKS_FREE" +
                "&metrics=SHOWS",
                filter
        );

        String expected = getStringResource("/testGetStatsByModelFilterByText/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetStatsByModelFilterByMetrics/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetStatsByModelFilterByMetrics() {
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withBody(getStringResource("/testGetStatsByModel/report_response_blue.json"))));

        String filter = getStringResource("/testGetStatsByModelFilterByMetrics/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics/byModel?uid=100500" +
                "&from=1522530000000" +
                "&to=1525035600000" +
                "&metrics=SHOWS&metrics=CHARGES",
                filter);

        String expected = getStringResource("/testGetStatsByModelFilterByMetrics/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetStatsByModelWithPaging/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetStatsByModelWithPaging() {

        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withBody(getStringResource("/testGetStatsByModel/report_response_blue.json"))));

        String filter = getStringResource("/testGetStatsByModelWithPaging/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics/byModel?uid=100500" +
                "&from=1522530000000" +
                "&to=1525035600000" +
                "&metrics=CLICKS_TOTAL" +
                "&metrics=CLICKS_PAID" +
                "&metrics=CLICKOUTS_RATE" +
                "&metrics=AVG_POSITION" +
                "&metrics=CLICKOUTS_TOTAL" +
                "&metrics=AVG_CLICK_PRICE" +
                "&metrics=AVG_BID" +
                "&metrics=CLICKOUTS_FREE" +
                "&metrics=CTR" +
                "&metrics=CLICKOUTS_PAID" +
                "&metrics=CHARGES" +
                "&metrics=CLICKS_FREE" +
                "&metrics=SHOWS" +
                "&page=1" +
                "&pageSize=1",
                filter
        );

        String expected = getStringResource("/testGetStatsByModelWithPaging/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetReportTaskId/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/ModelbidsStatsControllerFunctionalTest/testGetReportTaskId/after.csv",
            dataSource = "vendorDataSource"
    )
    void testGetReportTaskId() {
        LocalDateTime testCaseNow = LocalDate.of(2018, 2, 28).atStartOfDay();
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();


        String filter = getStringResource("/testGetReportTaskId/filter.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/modelbids/statistics/asyncDownload?uid" +
                        "=100500" +
                        "&from=1522530000000" +
                        "&to=1525035600000" +
                        "&scale=MONTH" +
                        "&platform=DESKTOP&platform=TOUCH&platform=APPLICATION" +
                        "&metrics=SHOWS&metrics=CTR&metrics=AVG_POSITION",
                filter);

        String expected = getStringResource("/testGetReportTaskId/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

}
