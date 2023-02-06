package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.balance.model.BalanceClientUser;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.balance.xmlrpc.model.ClientUserStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.ArgumentMatchers.eq;

class VendorModelbidsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private WireMockServer csBillingApiMock;
    @Autowired
    private WireMockServer mbiBiddingMock;
    @Autowired
    private Clock clock;

    @DisplayName("Ставки на модель. Подключение по предоплатному договору")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/VendorModelbidsControllerFunctionalTest/testCreatePrepaidModelbidsProduct/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorModelbidsControllerFunctionalTest/testCreatePrepaidModelbidsProduct/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testCreatePrepaidModelbidsProduct() {
        LocalDateTime now = LocalDateTime.of(2020, 5, 18, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));

        Mockito.when(balanceService.getClientUsers(eq(3)))
                .thenReturn(List.of(new BalanceClientUser(
                        new ClientUserStructure(
                                Map.of(
                                        "PASSPORT_ID", 100500,
                                        "GECOS", "Test",
                                        "LOGIN", "Test")
                        )))
                );

        String getAllCutoffsResponse = getStringResource("/testCreatePrepaidModelbidsProduct/getAllCutoffsRequest.json");
        csBillingApiMock.stubFor(WireMock.get("/getAllCutoffs?serviceId=132&datasourceId=1")
                .willReturn(WireMock.okJson(getAllCutoffsResponse)));

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_MODEL_BIDS")
                .willReturn(aResponse().withBody(getStringResource("/testCreatePrepaidModelbidsProduct" +
                        "/retrofit2_response.json"))));

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1/billingPeriod")
                .willReturn(WireMock.okJson(
                        getStringResource("/testCreatePrepaidModelbidsProduct/csBillingBillingPeriodResponse.json")))
        );

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/actions/action?uid=100500")
                .willReturn(WireMock.okJson("100500")));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/1/model-bids")
                .willReturn(WireMock.ok()));

        String request = getStringResource("/testCreatePrepaidModelbidsProduct/request.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/100/modelbids?uid=100500", request);
        String expected = getStringResource("/testCreatePrepaidModelbidsProduct/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Ставки на модель. Подключение по предоплатному договору")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/VendorModelbidsControllerFunctionalTest/testCreatePostpaidModelbidsProduct/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorModelbidsControllerFunctionalTest/testCreatePostpaidModelbidsProduct/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testCreatePostpaidModelbidsProduct() {
        LocalDateTime now = LocalDateTime.of(2020, 5, 18, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));

        Mockito.when(balanceService.getClientUsers(eq(3)))
                .thenReturn(List.of(new BalanceClientUser(
                        new ClientUserStructure(
                                Map.of(
                                        "PASSPORT_ID", 100500,
                                        "GECOS", "Test",
                                        "LOGIN", "Test")
                        )))
                );

        String getAllCutoffsResponse = getStringResource("/testCreatePostpaidModelbidsProduct/getAllCutoffsRequest.json");
        csBillingApiMock.stubFor(WireMock.get("/getAllCutoffs?serviceId=132&datasourceId=1")
                .willReturn(WireMock.okJson(getAllCutoffsResponse)));

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_MODEL_BIDS")
                .willReturn(aResponse().withBody(getStringResource("/testCreatePostpaidModelbidsProduct" +
                        "/retrofit2_response.json"))));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/actions/action?uid=100500")
                .willReturn(WireMock.okJson("100500")));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/1/model-bids")
                .willReturn(WireMock.ok()));

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1/billingPeriod")
                .willReturn(WireMock.okJson(
                        getStringResource("/testCreatePostpaidModelbidsProduct/csBillingBillingPeriodResponse.json")))
        );

        String request = getStringResource("/testCreatePostpaidModelbidsProduct/request.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/100/modelbids?uid=100500", request);
        String expected = getStringResource("/testCreatePostpaidModelbidsProduct/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Ставки на модель. Изменение услуги")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorModelbidsControllerFunctionalTest/testChangeModelBidsProductForVendor/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorModelbidsControllerFunctionalTest/testChangeModelBidsProductForVendor/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorModelbidsControllerFunctionalTest/testChangeModelBidsProductForVendor/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testChangeModelBidsProductForVendor() {
        LocalDateTime now = LocalDateTime.of(2020, 5, 18, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));

        Mockito.when(balanceService.getClientUsers(eq(4)))
                .thenReturn(List.of(new BalanceClientUser(
                        new ClientUserStructure(
                                Map.of(
                                        "PASSPORT_ID", 100500,
                                        "GECOS", "Test",
                                        "LOGIN", "Test")
                        )))
                );

        String getAllCutoffsResponse = getStringResource("/testCreatePostpaidModelbidsProduct/getAllCutoffsRequest.json");
        csBillingApiMock.stubFor(WireMock.get("/getAllCutoffs?serviceId=132&datasourceId=1")
                .willReturn(WireMock.okJson(getAllCutoffsResponse)));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/actions/action?uid=100500")
                .willReturn(WireMock.okJson("100500")));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/1/model-bids")
                .willReturn(WireMock.ok()));

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1/billingPeriod")
                .willReturn(WireMock.okJson(
                        getStringResource("/testChangeModelBidsProductForVendor/csBillingBillingPeriodResponse.json")))
        );

        String request = getStringResource("/testChangeModelBidsProductForVendor/request.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/100/modelbids?uid=100500", request);
        String expected = getStringResource("/testChangeModelBidsProductForVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }
}
