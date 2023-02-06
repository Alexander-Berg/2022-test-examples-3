package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;

class BillingControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final WireMockServer csBillingApiMock;
    private final Clock clock;

    @Autowired
    BillingControllerFunctionalTest(WireMockServer csBillingApiMock, Clock clock) {
        this.csBillingApiMock = csBillingApiMock;
        this.clock = clock;
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.when(clock.instant()).thenReturn(
                TimeUtil.toInstant(LocalDateTime.of(2020, Month.MAY, 20, 0, 0))
        );
    }

    @Disabled
    @DisplayName("Успешно изменяем лимит расходов для рек. магов")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testUpdateVendorProductLimitSuccess/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testUpdateVendorProductLimitSuccess/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testUpdateVendorProductLimitSuccess() {
        csBillingApiMock.stubFor(WireMock.put("/service/132/datasource/1/chargeLimit?uid=100500")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testUpdateVendorProductLimitSuccess/csBillingApiRequest.json"), true, false))
                .willReturn(okJson(getStringResource("/testUpdateVendorProductLimitSuccess/csBillingApiResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get("/service/132/campaign/1/balance/forecast")
                .willReturn(okJson(getStringResource("/testUpdateVendorProductLimitSuccess/csBillingApiBalanceForecastResponse.json"))));

        final String request = getStringResource("/testUpdateVendorProductLimitSuccess/request.json");
        final String response = FunctionalTestHelper.put(baseUrl + "/vendors/1/recommended/billing/chargeLimit?uid=100500", request);
        final String expected = getStringResource("/testUpdateVendorProductLimitSuccess/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @Disabled
    @DisplayName("Успешно закрываем лимит расходов для рек. магов")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testCloseVendorProductLimitSuccess/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testCloseVendorProductLimitSuccess/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCloseVendorProductLimitSuccess() {
        csBillingApiMock.stubFor(WireMock.put("/service/132/datasource/1/chargeLimit?uid=100500")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testCloseVendorProductLimitSuccess/csBillingApiRequest.json"), true, false))
                .willReturn(okJson(getStringResource("/testCloseVendorProductLimitSuccess/csBillingApiResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get("/service/132/campaign/1/balance/forecast")
                .willReturn(okJson(getStringResource("/testCloseVendorProductLimitSuccess/csBillingApiBalanceForecastResponse.json"))));

        final String request = getStringResource("/testCloseVendorProductLimitSuccess/request.json");
        final String response = FunctionalTestHelper.put(baseUrl + "/vendors/1/recommended/billing/chargeLimit?uid=100500", request);
        final String expected = getStringResource("/testCloseVendorProductLimitSuccess/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @Disabled
    @DisplayName("Недостаточно лимита расходов для рек. магов для периода")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testUpdateVendorProductLimitFailure/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testUpdateVendorProductLimitFailure/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testUpdateVendorProductLimitFailure() {
        csBillingApiMock.stubFor(WireMock.put("/service/132/datasource/1/chargeLimit?uid=100500")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testUpdateVendorProductLimitFailure/csBillingApiRequest.json"), true, false))
                .willReturn(okJson(getStringResource("/testUpdateVendorProductLimitFailure/csBillingApiResponse.json"))));

        final String request = getStringResource("/testUpdateVendorProductLimitFailure/request.json");
        final HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(baseUrl + "/vendors/1/recommended/billing/chargeLimit?uid=100500", request)
        );
        final String response = ex.getResponseBodyAsString();
        final String expected = getStringResource("/testUpdateVendorProductLimitFailure/expected.json");
        Assertions.assertEquals(ex.getStatusCode(), HttpStatus.BAD_REQUEST);
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Получение доступной суммы перевода маркетинговой услуги")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testGetAvailableTransferForMarketingService/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testGetAvailableTransferForMarketingService/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetAvailableTransferForMarketingService() {
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1322/balance")
                .willReturn(okJson(getStringResource("/testGetAvailableTransferForMarketingService/serviceDatasourceBalanceResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/marketingLandings/billing/availableTransfer?uid=100500");
        String expected = getStringResource("/testGetAvailableTransferForMarketingService/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Перевод суммы с баланса маркетинговой услуги (ошибка при валидации суммы)")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testGetAvailableTransferForMarketingService/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BillingControllerFunctionalTest/testGetAvailableTransferForMarketingService/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMakeTransferFromMarketingServiceValidationFail() {
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1322/balance")
                .willReturn(okJson(getStringResource("/testMakeTransferFromMarketingServiceValidationFail/serviceDatasourceBalanceResponse.json"))));

        HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/321/marketingLandings/billing/makeTransfer/321/marketingBanners?uid=100500&sumInCents=100500")
                );
        String response = ex.getResponseBodyAsString();
        String expected = getStringResource("/testMakeTransferFromMarketingServiceValidationFail/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }
}
