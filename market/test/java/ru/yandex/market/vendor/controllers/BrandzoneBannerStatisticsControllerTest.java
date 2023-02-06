package ru.yandex.market.vendor.controllers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;

class BrandzoneBannerStatisticsControllerTest extends AbstractVendorPartnerFunctionalTest {

    private final WireMockServer csBillingApiMock;

    @Autowired
    BrandzoneBannerStatisticsControllerTest(WireMockServer csBillingApiMock) {
        this.csBillingApiMock = csBillingApiMock;
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsNoData/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsNoData/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetBrandzoneBannerStatisticsNoData() {
        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/tariffs\\?datasourceId=[12]&datasourceId=[12]"))
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsNoData/campaignTariffsResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/activity\\?datasourceId=[12]&datasourceId=[12]"))
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsNoData/activityResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1/brandzone/stats/banner?pageSize=10&currentPage=1&uid=100500&monthStart=1593550800000");
        String expected = getStringResource("/testGetBrandzoneBannerStatisticsNoData/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Запрос на январь")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatistics/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatistics/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetBrandzoneBannerStatistics() {
        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/tariffs\\?datasourceId=[0-9]+&datasourceId=[0-9]+"))
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatistics/campaignTariffsResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/activity\\?datasourceId=[0-9]+&datasourceId=[0-9]+"))
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatistics/activityResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/100/brandzone/stats/banner?pageSize=10&currentPage=1&uid=100500" +
                "&monthStart=1609448400000");
        String expected = getStringResource("/testGetBrandzoneBannerStatistics/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsNoPeriods/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsNoPeriods/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetBrandzoneBannerStatisticsNoPeriods() {
        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/tariffs\\?datasourceId=[12]&datasourceId=[12]"))
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsNoPeriods/campaignTariffsResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/activity\\?datasourceId=[12]&datasourceId=[12]"))
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsNoPeriods/activityResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/1/brandzone/stats/banner?pageSize=10&currentPage=1&uid=100500");

        String expected = getStringResource("/testGetBrandzoneBannerStatisticsNoPeriods/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsPager/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsPager/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetBrandzoneBannerStatisticsPager() {
        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/tariffs\\?datasourceId=[0-9]+&datasourceId=[0-9]+"))
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsPager/campaignTariffsResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/activity\\?datasourceId=[0-9]+&datasourceId=[0-9]+"))
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsPager/activityResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/100/brandzone/stats/banner?pageSize=2&currentPage=1&uid=100500" +
                "&monthStart=1614546000000&monthStart=1612126800000&monthStart=1609448400000");

        String expected = getStringResource("/testGetBrandzoneBannerStatisticsPager/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsOneActivityPeriod/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsOneActivityPeriod/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetBrandzoneBannerStatisticsOneActivityPeriod() {
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/tariffs?datasourceId=99")
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsOneActivityPeriod/campaignTariffsResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/activity?datasourceId=99")
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsOneActivityPeriod/activityResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/100/brandzone/stats/banner?pageSize=2&currentPage=1&uid=100500" +
                "&monthStart=1596229200000");

        String expected = getStringResource("/testGetBrandzoneBannerStatisticsOneActivityPeriod/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsNoManualDatePresent/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneBannerStatisticsControllerTest/testGetBrandzoneBannerStatisticsNoManualDatePresent/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetBrandzoneBannerStatisticsNoManualDatePresent() {
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/tariffs?datasourceId=2084")
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsNoManualDatePresent/campaignTariffsResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/activity?datasourceId=2084")
                .willReturn(okJson(getStringResource("/testGetBrandzoneBannerStatisticsNoManualDatePresent/activityResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/4282/brandzone/stats/banner?pageSize=10&currentPage=1&uid=100500" +
                "&monthStart=1609448400000");

        String expected = getStringResource("/testGetBrandzoneBannerStatisticsNoManualDatePresent/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }
}
