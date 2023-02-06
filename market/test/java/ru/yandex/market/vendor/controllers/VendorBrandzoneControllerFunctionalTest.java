package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.balance.model.BalanceClientUser;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.balance.xmlrpc.model.ClientUserStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.model.IdView;
import ru.yandex.market.vendor.model.campaign.CampaignPlacementView;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.security.Role;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.Mockito.doReturn;

/**
 * Тест для {@link VendorBrandzoneController}.
 */
class VendorBrandzoneControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private WireMockServer mbiBiddingMock;
    @Autowired
    private WireMockServer csBillingApiMock;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.now()));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /vendors/{vendorId}/brandzone}
     * ({@link VendorBrandzoneController#getVendorBrandzone(long, long)})
     * для офертного вендора возвращает корректный результат.
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetOfferVendorBrandzone/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetOfferVendorBrandzone/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetOfferVendorBrandzone() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testGetOfferVendorBrandzone/retrofit2_response.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brandzone?uid=100500");
        String expected = getStringResource("/testGetOfferVendorBrandzone/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /vendors/{vendorId}/brandzone}
     * ({@link VendorBrandzoneController#getVendorBrandzone(long, long)})
     * для контрактного вендора возвращает корректный результат.
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetContractVendorBrandzone/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetContractVendorBrandzone/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetContractVendorBrandzone() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testGetContractVendorBrandzone/retrofit2_response.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/654/brandzone?uid=100500");
        String expected = getStringResource("/testGetContractVendorBrandzone/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /vendors/{vendorId}/brandzone}
     * ({@link VendorBrandzoneController#getVendorBrandzone(long, long)})
     * для вендора без услуги БЗ возвращает 404.
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetVendorWithoutBrandzone/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetVendorWithoutBrandzone/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetVendorWithoutBrandzone() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/987/brandzone?uid=100500")
        );
        String expected = getStringResource("/testGetVendorWithoutBrandzone/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /vendors/{vendorId}/brandzone/tariffs}
     * ({@link VendorBrandzoneController#getVendorBrandzoneTariffs(long, long)})
     * для офертного вендора возвращает корректный результат.
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetVendorBrandzoneTariffs/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetVendorBrandzoneTariffs/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetVendorBrandzoneTariffs() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testGetVendorBrandzoneTariffs/retrofit2_response.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brandzone/tariffs?uid=100500");
        String expected = getStringResource("/testGetVendorBrandzoneTariffs/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code POST /vendors/{vendorId}/brandzone/tariffs}
     * ({@link VendorBrandzoneController#setVendorBrandzoneTariffs(long, long, IdView)})
     * для офертного вендора возвращает корректный результат (оба тарифа в ответе).
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneTariffs/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneTariffs/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    public void testPutVendorBrandzoneTariffs() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testPutVendorBrandzoneTariffs/retrofit2_response.json"))));

        String body = getStringResource("/testPutVendorBrandzoneTariffs/request.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/brandzone/tariffs?uid=100500", body);
        String expected = getStringResource("/testPutVendorBrandzoneTariffs/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code POST /vendors/{vendorId}/brandzone/tariffs}
     * ({@link VendorBrandzoneController#setVendorBrandzoneTariffs(long, long, IdView)})
     * для офертного вендора возвращает корректный результат (оба тарифа в ответе).
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneTariffsWithoutBrandzone/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneTariffsWithoutBrandzone/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testPutVendorBrandzoneTariffsWithoutBrandzone() {
        String body = getStringResource("/testPutVendorBrandzoneTariffsWithoutBrandzone/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/987/brandzone/tariffs?uid=100500", body)
        );
        String expected = getStringResource("/testPutVendorBrandzoneTariffsWithoutBrandzone/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code PUT /vendors/{vendorId}/brandzone/placement}
     * ({@link VendorProductPlacementController#putVendorBrandzonePlacement(long, long, CampaignPlacementView)})
     * возвращает корректный результат.
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzonePlacement/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzonePlacement/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testPutVendorBrandzonePlacement() {
        setVendorUserRoles(singleton(Role.manager_user), 100500L);

        String body = getStringResource("/testPutVendorBrandzonePlacement/request.json");
        String response = FunctionalTestHelper.putWithAuth(baseUrl + "/vendors/321/brandzone/placement?uid=100500",
                body);
        String expected = getStringResource("/testPutVendorBrandzonePlacement/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ручке запуска услуги {@code PUT /vendors/{vendorId}/brandzone/placement}
     * ({@link VendorProductPlacementController#putVendorBrandzonePlacement(long, long, CampaignPlacementView)})
     * снимает только один катофф.
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzonePlacementClosesSingleCutoffOnActivate/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzonePlacementClosesSingleCutoffOnActivate/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testPutVendorBrandzonePlacementClosesSingleCutoffOnActivate() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testPutVendorBrandzonePlacementClosesSingleCutoffOnActivate/retrofit2_response.json"))));

        setVendorUserRoles(singleton(Role.manager_user), 100500L);

        String responseBeforeActivation = FunctionalTestHelper.get(baseUrl + "/vendors/123/brandzone?uid=100500");
        String expectedBeforeActivation = getStringResource(
                "/testPutVendorBrandzonePlacementClosesSingleCutoffOnActivate/expected_before_activation.json");
        JsonAssert.assertJsonEquals(expectedBeforeActivation, responseBeforeActivation, when(IGNORING_ARRAY_ORDER));

        String activationBody = getStringResource("/testPutVendorBrandzonePlacementClosesSingleCutoffOnActivate" +
                "/request_activation.json");

        String responseOfFirstActivation = FunctionalTestHelper.put(baseUrl + "/vendors/123/brandzone/placement?uid=100500", activationBody);

        String responseAfterFirstActivation = FunctionalTestHelper.get(baseUrl + "/vendors/123/brandzone?uid=100500");
        String expectedAfterFirstActivation = getStringResource(
                "/testPutVendorBrandzonePlacementClosesSingleCutoffOnActivate/expected_after_first_activation.json");
        JsonAssert.assertJsonEquals(expectedAfterFirstActivation, responseAfterFirstActivation,
                when(IGNORING_ARRAY_ORDER));

        String responseOfSecondActivation = FunctionalTestHelper.put(baseUrl + "/vendors/123/brandzone/placement?uid" +
                "=100500", activationBody);

        String responseAfterSecondActivation = FunctionalTestHelper.get(baseUrl + "/vendors/123/brandzone?uid=100500");
        String expectedAfterSecondActivation = getStringResource(
                "/testPutVendorBrandzonePlacementClosesSingleCutoffOnActivate/expected_after_second_activation.json");
        JsonAssert.assertJsonEquals(expectedAfterSecondActivation, responseAfterSecondActivation,
                when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет возможность менять информацию о контракте для контрактных вендоров
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneContractForContractVendor/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneContractForContractVendor/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneContractForContractVendor/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPutVendorBrandzoneContractForContractVendor() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testPutVendorBrandzoneContractForContractVendor/retrofit2_response.json"))));

        String body = getStringResource("/testPutVendorBrandzoneContractForContractVendor/request.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/654/brandzone?uid=100500", body);
        String expected = getStringResource("/testPutVendorBrandzoneContractForContractVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет возможность менять клиента в Балансе
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneClientId/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneClientId/after.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneClientId/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneClientId/after.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testPutVendorBrandzoneClientId() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testPutVendorBrandzoneClientId/retrofit2_response.json"))));

        ClientUserStructure clientUserStructure = new ClientUserStructure();
        clientUserStructure.setLogin("vasya");
        BalanceClientUser clientUser = new BalanceClientUser(clientUserStructure);
        doReturn(singletonList(clientUser)).when(balanceService).getClientUsers(1006541);

        String body = getStringResource("/testPutVendorBrandzoneClientId/request.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/654/brandzone?uid=100500", body);
        String expected = getStringResource("/testPutVendorBrandzoneClientId/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что при смене pageId, статус публикации для текущей страницы вендора сбрасывается в false
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneWithPageIdChange/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneWithPageIdChange/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneWithPageIdChange/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPutVendorBrandzoneWithPageIdChange() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testPutVendorBrandzoneWithPageIdChange/retrofit2_response.json"))));

        String body = getStringResource("/testPutVendorBrandzoneWithPageIdChange/request.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", body);
        String expected = getStringResource("/testPutVendorBrandzoneWithPageIdChange/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что при изменении услуги "Бренд-зона" без смены pageId, статус публикации для текущей страницы
     * вендора остаётся прежним
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneWithoutPageIdModification/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneWithoutPageIdModification/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testPutVendorBrandzoneWithoutPageIdModification/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPutVendorBrandzoneWithoutPageIdModification() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testPutVendorBrandzoneWithoutPageIdModification/retrofit2_response.json"))));

        ClientUserStructure clientUserStructure = new ClientUserStructure();
        clientUserStructure.setLogin("vasya");
        BalanceClientUser clientUser = new BalanceClientUser(clientUserStructure);
        doReturn(singletonList(clientUser)).when(balanceService).getClientUsers(1003210);

        String body = getStringResource("/testPutVendorBrandzoneWithoutPageIdModification/request.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", body);
        String expected = getStringResource("/testPutVendorBrandzoneWithoutPageIdModification/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что нельзя сменить дату публикации страницы Бренд-зоны (startDate), если услуга активна
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForActivePlacement/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForActivePlacement/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testStartDateValidationForActivePlacement() {
        String requestBody = getStringResource("/testStartDateValidationForActivePlacement/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", requestBody)
        );
        String expected = getStringResource("/testStartDateValidationForActivePlacement/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что можно сменить дату публикации страницы Бренд-зоны (startDate), если услуга не-активна
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForSuspendedPlacement/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForSuspendedPlacement/after.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForSuspendedPlacement/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForSuspendedPlacement/after.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testStartDateValidationForSuspendedPlacement() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2018, Month.MARCH, 23, 0, 0)));
        setVendorUserRoles(singleton(Role.manager_user), 100500L);

        // suspending BZ placement
        String bodySuspend = getStringResource("/testStartDateValidationForSuspendedPlacement/request_suspend.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone/placement?uid=100500", bodySuspend);

        // changing startDate
        String body = getStringResource("/testStartDateValidationForSuspendedPlacement/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", body);
    }

    /**
     * Тест проверяет, что можно повторно сменить дату публикации страницы Бренд-зоны (startDate), если услуга
     * не-активна из-за уже выставленной даты старта
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForPostponedPlacement/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForPostponedPlacement/after.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForPostponedPlacement/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateValidationForPostponedPlacement/after.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testStartDateValidationForPostponedPlacement() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2018, Month.MARCH, 23, 0, 0)));
        setVendorUserRoles(singleton(Role.manager_user), 100500L);

        // changing startDate
        String body = getStringResource("/testStartDateValidationForPostponedPlacement/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", body);
    }

    /**
     * Тест проверяет, что нельзя задать дату публикации страницы Бренд-зоны (startDate) в прошлом
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateBeforeNowValidationForSuspendedPlacement/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateBeforeNowValidationForSuspendedPlacement/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testStartDateBeforeNowValidationForSuspendedPlacement/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testStartDateBeforeNowValidationForSuspendedPlacement() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 0, 0)));
        setVendorUserRoles(singleton(Role.manager_user), 100500L);

        // suspending BZ placement
        String bodySuspend = getStringResource("/testStartDateBeforeNowValidationForSuspendedPlacement" +
                "/request_suspend.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone/placement?uid=100500", bodySuspend);

        // changing startDate
        String body = getStringResource("/testStartDateBeforeNowValidationForSuspendedPlacement/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", body);
    }

    /**
     * Тест проверяет, что нельзя сменить cmsPageId на айдишник, который сейчас используется другим вендором
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testActualPageIdShouldNotBeReusedByAnotherVendor/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testActualPageIdShouldNotBeReusedByAnotherVendor/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testActualPageIdShouldNotBeReusedByAnotherVendor() {
        String body = getStringResource("/testActualPageIdShouldNotBeReusedByAnotherVendor/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(baseUrl + "/vendors/654/brandzone?uid=100500", body)
        );
        String expected = getStringResource("/testActualPageIdShouldNotBeReusedByAnotherVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что можно сменить cmsPageId на айдишник, который использовался этим вендором ранее, и ещё не
     * распубликован
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testObsoletePageIdMayBeReusedBySameVendor/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testObsoletePageIdMayBeReusedBySameVendor/after.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testObsoletePageIdMayBeReusedBySameVendor/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testObsoletePageIdMayBeReusedBySameVendor/after.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testObsoletePageIdMayBeReusedBySameVendor() {
        // assigning new pageId (old one is still published)
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testObsoletePageIdMayBeReusedBySameVendor/retrofit2_response.json"))));

        String requestChangePageIdTo2 = getStringResource("/testObsoletePageIdMayBeReusedBySameVendor" +
                "/requestChangePageIdTo2.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", requestChangePageIdTo2);

        // assigning old pageId (published by this vendor)
        String requestChangePageIdTo1 = getStringResource("/testObsoletePageIdMayBeReusedBySameVendor" +
                "/requestChangePageIdTo1.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500",
                requestChangePageIdTo1);
        String expected = getStringResource("/testObsoletePageIdMayBeReusedBySameVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что нельзя сменить cmsPageId на айдишник, который использовался другим вендором ранее, и ещё
     * не распубликован
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testObsoleteButPublishedPageIdShouldNotBeReusedByAnotherVendor/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testObsoleteButPublishedPageIdShouldNotBeReusedByAnotherVendor/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testObsoleteButPublishedPageIdShouldNotBeReusedByAnotherVendor() {
        // assigning new pageId to the first vendor (old one is still published)
        String requestChangePageIdTo2 = getStringResource(
                "/testObsoleteButPublishedPageIdShouldNotBeReusedByAnotherVendor/requestChangePageIdTo2.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", requestChangePageIdTo2);

        // assigning old pageId (published by the first vendor) to the second vendor
        String requestChangePageIdTo1 = getStringResource(
                "/testObsoleteButPublishedPageIdShouldNotBeReusedByAnotherVendor/requestChangePageIdTo1.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(baseUrl + "/vendors/654/brandzone?uid=100500", requestChangePageIdTo1)
        );
        String expected = getStringResource("/testObsoleteButPublishedPageIdShouldNotBeReusedByAnotherVendor/expected" +
                ".json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что можно сменить cmsPageId на айдишник, который использовался другим вендором ранее, но
     * сейчас распубликован
     */
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testObsoleteAndUnpublishedPageIdMayBeReusedByAnotherVendor/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testObsoleteAndUnpublishedPageIdMayBeReusedByAnotherVendor/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testObsoleteAndUnpublishedPageIdMayBeReusedByAnotherVendor() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testObsoleteAndUnpublishedPageIdMayBeReusedByAnotherVendor/retrofit2_response.json"))));
        // assigning new pageId to the first vendor
        String requestChangePageIdTo2_321 = getStringResource(
                "/testObsoleteAndUnpublishedPageIdMayBeReusedByAnotherVendor/requestChangePageIdTo2_vendor321.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", requestChangePageIdTo2_321);

        // returning the old pageId to the first vendor
        String requestChangePageIdTo1_321 = getStringResource(
                "/testObsoleteAndUnpublishedPageIdMayBeReusedByAnotherVendor/requestChangePageIdTo1_vendor321.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/321/brandzone?uid=100500", requestChangePageIdTo1_321);

        // assigning obsolete pageId of the first vendor (unpublished) to the second vendor
        String requestChangePageIdTo2_654 = getStringResource(
                "/testObsoleteAndUnpublishedPageIdMayBeReusedByAnotherVendor/requestChangePageIdTo2_vendor654.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/654/brandzone?uid=100500",
                requestChangePageIdTo2_654);
        String expected = getStringResource("/testObsoleteAndUnpublishedPageIdMayBeReusedByAnotherVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetBrandzoneTariffs/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testGetBrandzoneTariffs/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetBrandzoneTariffs() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testGetBrandzoneTariffs/retrofit2_response.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/tariffs/brandzone?uid=100500");
        String expected = getStringResource("/testGetBrandzoneTariffs/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Бренд-зона. Подключение по постоплатному договору")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testCreatePostpaidBrandzoneProduct/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testCreatePostpaidBrandzoneProduct/before.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testCreatePostpaidBrandzoneProduct() {
        LocalDateTime now = LocalDateTime.of(2020, 5, 18, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));

        Mockito.when(balanceService.getClientUsers(Mockito.eq(3)))
                .thenReturn(List.of(new BalanceClientUser(
                        new ClientUserStructure(
                                Map.of(
                                        "PASSPORT_ID", 100500,
                                        "GECOS", "Test",
                                        "LOGIN", "Test")
                        )))
                );

        String getAllCutoffsResponse = getStringResource("/testCreatePostpaidBrandzoneProduct/getAllCutoffsRequest.json");
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testCreatePostpaidBrandzoneProduct/retrofit2_response1.json"))));

        csBillingApiMock.stubFor(WireMock.get("/getAllCutoffs?serviceId=132&datasourceId=1")
                .willReturn(WireMock.okJson(getAllCutoffsResponse)));

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_BRAND_ZONE")
                .willReturn(aResponse().withBody(getStringResource("/testCreatePostpaidBrandzoneProduct" +
                        "/retrofit2_response2.json"))));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/actions/action?uid=100500")
                .willReturn(WireMock.okJson("100500")));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/1/model-bids")
                .willReturn(WireMock.ok()));

        String request = getStringResource("/testCreatePostpaidBrandzoneProduct/request.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/100/brandzone?uid=100500", request);
        String expected = getStringResource("/testCreatePostpaidBrandzoneProduct/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Бренд-зона. Подключение по предоплатному договору")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testCreatePrepaidBrandzoneProduct/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorBrandzoneControllerFunctionalTest/testCreatePrepaidBrandzoneProduct/before.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testCreatePrepaidBrandzoneProduct() {
        LocalDateTime now = LocalDateTime.of(2020, 5, 18, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));

        Mockito.when(balanceService.getClientUsers(Mockito.eq(3)))
                .thenReturn(List.of(new BalanceClientUser(
                        new ClientUserStructure(
                                Map.of(
                                        "PASSPORT_ID", 100500,
                                        "GECOS", "Test",
                                        "LOGIN", "Test")
                        )))
                );

        String getAllCutoffsResponse = getStringResource("/testCreatePrepaidBrandzoneProduct/getAllCutoffsRequest.json");
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testCreatePrepaidBrandzoneProduct/retrofit2_response1.json"))));

        csBillingApiMock.stubFor(WireMock.get("/getAllCutoffs?serviceId=132&datasourceId=1")
                .willReturn(WireMock.okJson(getAllCutoffsResponse)));

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_BRAND_ZONE")
                .willReturn(aResponse().withBody(getStringResource("/testCreatePrepaidBrandzoneProduct" +
                        "/retrofit2_response2.json"))));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/actions/action?uid=100500")
                .willReturn(WireMock.okJson("100500")));

        mbiBiddingMock.stubFor(WireMock.put("/market/bidding/vendors/1/model-bids")
                .willReturn(WireMock.ok()));

        String request = getStringResource("/testCreatePrepaidBrandzoneProduct/request.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/100/brandzone?uid=100500", request);
        String expected = getStringResource("/testCreatePrepaidBrandzoneProduct/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }
}
