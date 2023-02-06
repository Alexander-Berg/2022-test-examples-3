package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.balance.model.BalanceClient;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.balance.xmlrpc.model.ClientPassportStructure;
import ru.yandex.market.common.balance.xmlrpc.model.RelationsStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ServiceClientStructure;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.security.Role;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link OfferController}.
 */
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/OfferControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/OfferControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
class OfferControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private Clock clock;

    @Autowired
    private WireMockServer csBillingApiMock;

    @Test
    @DisplayName("Нельзя принять оферту Маркет.Аналитики дважды")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/OfferControllerFunctionalTest/canNotAcceptOfferTwice/after.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/OfferControllerFunctionalTest/canNotAcceptOfferTwice/after.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void canNotAcceptOfferTwice() {
        long uid = 124;
        setVendorUserRoles(singletonList(Role.admin_user), uid, 1L);
        HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.postWithAuth(baseUrl + "/vendors/1/offer/accept/analytics?uid=" + uid)
        );
        String response = ex.getResponseBodyAsString();
        String expected = getStringResource("/canNotAcceptOfferTwice/expected.json");
        assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Принятие оферты Вендоров")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/OfferControllerFunctionalTest/acceptVendorsOffer/after.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/OfferControllerFunctionalTest/acceptVendorsOffer/after.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void acceptVendorsOffer() {
        long uid = 100500;
        setVendorUserRoles(singletonList(Role.admin_user), uid, 3L);

        var relationsStructure = new RelationsStructure();
        relationsStructure.setFieldAllClientIds(true);
        var clientPassport = buildClientPassportStructure();
        when(balanceService.getPassportByUid(eq(uid), eq(uid), eq(relationsStructure)))
                .thenReturn(clientPassport);

        int clientId = 1000002;
        var balanceClient = buildBalanceClient("Тест", "+71234567892", "test3@email.ru");
        when(balanceService.createClient(eq(uid), eq(balanceClient))).thenReturn(clientId);

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 5, 15, 51);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_MODEL_BIDS")
                .willReturn(aResponse().withBody(getStringResource("/acceptVendorsOffer/retrofit2_response1.json"))));

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=DEFAULT_DUMMY")
                .willReturn(aResponse().withBody(getStringResource("/acceptVendorsOffer/retrofit2_response2.json"))));

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_BRAND_ZONE")
                .willReturn(aResponse().withBody(getStringResource("/acceptVendorsOffer/retrofit2_response3.json"))));

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_MARKETING")
                .willReturn(aResponse().withBody(getStringResource("/acceptVendorsOffer/retrofit2_response4.json"))));

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_PAID_OPINIONS")
                .willReturn(aResponse().withBody(getStringResource("/acceptVendorsOffer/retrofit2_response5.json"))));

        String response = FunctionalTestHelper.postWithAuth(baseUrl + "/vendors/3/offer/accept?uid=" + uid);
        String expected = getStringResource("/acceptVendorsOffer/expected.json");
        assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Принятие оферты Маркет.Аналитики")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/OfferControllerFunctionalTest/acceptAnalyticsOffer/after.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/OfferControllerFunctionalTest/acceptAnalyticsOffer/after.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void acceptAnalyticsOffer() {
        long uid = 100500;
        setVendorUserRoles(singletonList(Role.admin_user), uid, 2L);

        var relationsStructure = new RelationsStructure();
        relationsStructure.setFieldAllClientIds(true);
        var clientPassport = buildClientPassportStructure();
        when(balanceService.getPassportByUid(eq(uid), eq(uid), eq(relationsStructure)))
                .thenReturn(clientPassport);

        int clientId = 1000002;
        var balanceClient = buildBalanceClient("Тест", "+71234567892", "test2@email.ru");
        when(balanceService.createClient(eq(uid), eq(balanceClient))).thenReturn(clientId);

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 5, 15, 51);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames" +
                        "=IS_DEFAULT_ANALYTICS_FOR_OFFER")
                .willReturn(aResponse().withBody(getStringResource("/acceptAnalyticsOffer/retrofit2_response.json"))));

        String response = FunctionalTestHelper.postWithAuth(baseUrl + "/vendors/2/offer/accept/analytics?uid=" + uid);
        String expected = getStringResource("/acceptAnalyticsOffer/expected.json");
        assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    private ClientPassportStructure buildClientPassportStructure() {
        var clientPassport = mock(ClientPassportStructure.class);
        when(clientPassport.getRepresentedClientIds()).thenReturn(new Integer[0]);
        when(clientPassport.getClientId()).thenReturn(0L);
        when(clientPassport.getServiceClientIds()).thenReturn(new ServiceClientStructure[0]);
        when(clientPassport.getLimitedClientIds()).thenReturn(new Integer[0]);
        return clientPassport;
    }

    private BalanceClient buildBalanceClient(String name, String phone, String email) {
        var result = new BalanceClient();
        result.setName(name);
        result.setPhone(phone);
        result.setEmail(email);
        result.setClientType(ClientType.OOO);
        result.setAgency(false);
        return result;
    }

}
