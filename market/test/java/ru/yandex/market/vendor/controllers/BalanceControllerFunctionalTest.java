package ru.yandex.market.vendor.controllers;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.err.BalanceServiceException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/BalanceControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/BalanceControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class BalanceControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private static final String BALANCE_ERROR_MESSAGE = "" +
            "Error returned from Balance: (4008) " +
            "Passport seliverstovamegaten (1346276814) is already " +
            "linked to OTHER client 92332110";

    @Autowired
    private WireMockServer blackboxMock;

    @Autowired
    private BalanceService balanceService;

    /**
     * Проверяет добавление логина с точкой
     */
    @Test
    void testLoginWithCommaAddToClient() {
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/testLoginWithCommaAddToClient/blackbox_response.json"))));

        Mockito.doNothing()
                .when(balanceService)
                .bindPassportToClient(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());

        String request = getStringResource("/testLoginWithCommaAddToClient/request.json");
        String response = FunctionalTestHelper.put(
                baseUrl + "/vendors/1/recommended/balance/clientLogins?uid=100500",
                request
        );
        String expected = getStringResource("/testLoginWithCommaAddToClient/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Disabled
    @Test
    @DisplayName("Проверка невозможности привязки сервисных клиентов")
    void testServiceClients() {

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/testLoginWithCommaAddToClient/blackbox_response.json"))));

        Mockito.doNothing()
                .when(balanceService)
                .bindPassportToClient(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());

        Mockito.when(balanceService.hasAnyServiceClient(Mockito.anyList(), Mockito.anyLong()))
                .thenReturn(true);

        String request = getStringResource("/testLoginWithCommaAddToClient/request.json");

        final HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(baseUrl + "/vendors/1/recommended/balance/clientLogins?uid=100500",
                        request)
        );
        final String response = ex.getResponseBodyAsString();
        final String expected = getStringResource(
                "/testLoginWithCommaAddToClient/service_clients_exception_response.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Проверка невозможности добавления балансового пользователя, когда он уже связан с другим клиентом")
    void testBindAlreadyLinkedClient() {

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/testLoginWithCommaAddToClient/blackbox_response.json"))));

        Mockito.doThrow(new BalanceServiceException(BALANCE_ERROR_MESSAGE))
                .when(balanceService)
                .bindPassportToClient(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());

        String request = getStringResource("/testLoginWithCommaAddToClient/request.json");
        String response = FunctionalTestHelper.put(
                baseUrl + "/vendors/1/recommended/balance/clientLogins?uid=100500",
                request
        );

        final String expected = getStringResource(
                "/testBindAlreadyLinkedClient/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

}
