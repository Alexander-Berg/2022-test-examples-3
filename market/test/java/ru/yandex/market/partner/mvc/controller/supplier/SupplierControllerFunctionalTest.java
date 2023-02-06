package ru.yandex.market.partner.mvc.controller.supplier;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.cpa.yam.service.PrepayRequestValidatorService;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.cache.MemCachedCampaignService;
import ru.yandex.market.core.supplier.registration.SupplierRegistrationService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Тесты на {@link SupplierController}.
 *
 * @author sviperll
 */
@DbUnitDataSet(before = "SupplierControllerFunctionalTest.csv")
class SupplierControllerFunctionalTest extends AbstractSupplierFunctionalTest {

    private static final List<String> SUPPLIERS_LIST = List.of(
            "  {\n" +
                    "    \"id\":4, \n" +
                    "    \"name\":\"no client shop\"\n" +
                    "  }",
            "  {\n" +
                    "    \"id\":5,\n" +
                    "    \"name\":\"my shop\"\n" +
                    "  }",
            "  {\n" +
                    "    \"id\":6, \n" +
                    "    \"name\":\"my shop\"\n" +
                    "  }"
    );

    @Autowired
    private MemCachedCampaignService campaignService; // проверим работу с кешом
    @Autowired
    private PrepayRequestValidatorService prepayRequestValidatorService;
    @Autowired
    private BalanceContactService balanceContactService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private EnvironmentService environmentService;

    private static String createSupplierRequest(boolean isDropship, boolean isCrossdock) {
        //language=json
        return
                "{" +
                        "    \"name\": \"my super shop\"," +
                        "    \"internalShopName\": \"my super shop\"," +
                        "    \"domain\": \"super.shop.ru\"," +
                        "    \"dropship\":" + isDropship + "," +
                        "    \"crossdock\":" + isCrossdock + "," +
                        "    \"notificationContact\": {" +
                        "        \"firstName\": \"Vasia\"," +
                        "        \"lastName\": \"Pupkin\"," +
                        "        \"email\": \"vasia.pupkin@yandex.ru\"," +
                        "        \"phone\": \"+7-916-123-4455\"" +
                        "    }" +
                        "}";
    }

    private static Function<Long, String> registerSupplierResponse() {
        return campaignId -> "{" +
                "    \"campaignId\":" + campaignId + "," +
                "    \"datasourceId\":1," +
                "    \"businessId\":2," +
                "    \"status\":\"7\"," +
                "    \"domain\":\"super.shop.ru\"," +
                "    \"name\":\"my super shop\"" +
                "}";
    }

    @BeforeEach
    void setUp() {
        environmentService.setValue(SupplierRegistrationService.ENV_CAMPAIGNS_LIMIT, "300");
        when(balanceService.getClient(eq(110774L))).thenReturn(new ClientInfo(110774L, ClientType.PHYSICAL));
        when(balanceService.getClient(eq(210774L))).thenReturn(new ClientInfo(210774L, ClientType.PHYSICAL));
        when(balanceService.getClient(eq(210775L))).thenReturn(new ClientInfo(210775L, ClientType.PHYSICAL));
    }

    @Test
    @DisplayName("Получение 1P и 3P поставщиков, а также магазинов с фичей Fulfillment")
    @DbUnitDataSet(before = "fulfillmentSuppliers.before.csv")
    void fulfillmentSuppliers() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/fulfillment");
        JsonTestUtil.assertEquals(
                response,
                "[\n" +
                        String.join(",\n", SUPPLIERS_LIST) +
                        "\n]"
        );
    }

    @Test
    @DisplayName("Получение 1P и 3P поставщиков, а также магазинов с фичей Fulfillment и пейджированной выдачей и " +
            "без параметров пейджирования")
    @DbUnitDataSet(before = "fulfillmentSuppliers.before.csv")
    void fulfillmentSuppliersPaged() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/fulfillment/paged");
        JsonTestUtil.assertEquals(
                response,
                "{\n" +
                        "  \"paging\": {\n" +
                        "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjYsInNraXAiOjB9\"\n" +
                        "  },\n" +
                        "  \"searchResult\": [\n" +
                        String.join(",\n", SUPPLIERS_LIST) +
                        "\n  ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Получение 1P и 3P поставщиков, а также магазинов с фичей Fulfillment. " +
            "Параметр пейджирования limit=2, токен не передается.")
    @DbUnitDataSet(before = "fulfillmentSuppliers.before.csv")
    void fulfillmentSuppliersPageIteration() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/fulfillment/paged?limit=2");
        JsonTestUtil.assertEquals(
                response,
                "{\n" +
                        "  \"paging\": {\n" +
                        "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjUsInNraXAiOjB9\"\n" +
                        "  },\n" +
                        "  \"searchResult\": [\n" +
                        SUPPLIERS_LIST.stream()
                                .limit(2)
                                .collect(Collectors.joining(",\n")) +
                        "\n  ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Получение 1P и 3P поставщиков, а также магазинов с фичей Fulfillment. " +
            "Параметр пейджирования limit=2, значение в токене id > 5.")
    @DbUnitDataSet(before = "fulfillmentSuppliers.before.csv")
    void fulfillmentSuppliersPageIterationPage2() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/suppliers/fulfillment/paged?limit=2&page_token=eyJvcCI6Ij4iLCJrZXkiOjUsInNraXAiOjB9"
        );
        JsonTestUtil.assertEquals(
                response,
                //language=json
                "{\n" +
                        "  \"paging\": {\n" +
                        "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjYsInNraXAiOjB9\"\n" +
                        "  },\n" +
                        "  \"searchResult\": [\n" +
                        SUPPLIERS_LIST.stream()
                                .skip(2)
                                .limit(2)
                                .collect(Collectors.joining(",\n")) +
                        "  ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Получение 1P и 3P поставщиков, а также магазинов с фичей Fulfillment. " +
            "Запрос за пустой страницей.")
    @DbUnitDataSet(before = "fulfillmentSuppliers.before.csv")
    void fulfillmentSuppliersPageIterationEmptyPage() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/suppliers/fulfillment/paged?limit=2&page_token=eyJvcCI6Ij4iLCJrZXkiOjYsInNraXAiOjB9"
        );
        JsonTestUtil.assertEquals(
                response,
                //language=json
                "{\n" +
                        "  \"paging\": {},\n" +
                        "  \"searchResult\": []\n" +
                        "}"
        );
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerFunctionalTestUpdateRequest.before.csv")
    void testSupplierUpdateRegistrationRequestStatus() {
        doNothing().when(prepayRequestValidatorService).checkRequestIsFullFilled(any());
        reset(campaignService);

        String requestBody = "{\n" +
                "  \"status\": \"0\"\n" +
                "}";

        String url = supplierApplicationEditStatusUrl(10101L, 100504L);
        ResponseEntity<String> response = FunctionalTestHelper.put(url, requestBody);

        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerAgencyAccessTest.before.csv")
    void severalAgencySuppliersToOneClientTest() {
        when(balanceContactService.getClientIdByUid(200)).thenReturn(123L);
        when(balanceService.getClient(123L)).thenReturn(new ClientInfo(123, ClientType.PHYSICAL, true, 123));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/full-info?euid={euid}", 200);
        JsonTestUtil.assertEquals("SupplierControllerSeveralAgencySuppliersToOneClientTest.json", getClass(), response);
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerAgencyAccessTest.before.csv")
    void severalAgencySuppliersToOneClientWithPagingTest() {
        when(balanceContactService.getClientIdByUid(200)).thenReturn(123L);
        when(balanceService.getClient(123L)).thenReturn(new ClientInfo(123, ClientType.PHYSICAL, true, 123));
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/suppliers/full-info?euid={euid}&page=1&perpageNumber=1", 200);
        JsonTestUtil.assertEquals(response, getClass(),
                "SupplierControllerSeveralAgencySuppliersToOneClientWithPagingTest.json");
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerAgencyAccessTest.before.csv")
    void filterByBusinessTest() {
        when(balanceContactService.getClientIdByUid(200)).thenReturn(123L);
        when(balanceService.getClient(123L)).thenReturn(new ClientInfo(123, ClientType.PHYSICAL, true, 123));
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/suppliers/full-info?euid={euid}&business_id=100", 200);
        JsonTestUtil.assertEquals(response, getClass(), "SupplierControllerFilerByBusinessTest.json");
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerAgencyAccessTest.before.csv")
    void agencyWithoutClientsTest() {
        when(balanceContactService.getClientIdByUid(101)).thenReturn(124L);
        when(balanceService.getClient(124L)).thenReturn(new ClientInfo(124, ClientType.PHYSICAL, true, 124));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/full-info?euid={euid}", 101);
        JsonTestUtil.assertEquals(response, getClass(), "SupplierControllerAgencyWithoutClientsTest.json");
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerAgencyAccessTest.before.csv")
    void severalAgencySuppliersToEachClientTest() {
        when(balanceContactService.getClientIdByUid(102)).thenReturn(125L);
        when(balanceService.getClient(125L)).thenReturn(new ClientInfo(125, ClientType.OOO, true, 125));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/full-info?euid={euid}", 102);
        JsonTestUtil.assertEquals(response, getClass(),
                "SupplierControllerSeveralAgencySuppliersToEachClientTest.json");
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerAgencyAccessTest.before.csv")
    void severalAgencySuppliersToEachClientPagedTest() {
        when(balanceContactService.getClientIdByUid(102)).thenReturn(125L);
        when(balanceService.getClient(125L)).thenReturn(new ClientInfo(125, ClientType.OOO, true, 125));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "/suppliers/full-info?euid={euid}&page=1&perpageNumber=2", 102);
        JsonTestUtil.assertEquals(response, getClass(),
                "SupplierControllerSeveralAgencySuppliersToEachClientPagedTest.json");
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerAgencyAccessTest.before.csv")
    void severalAgencySuppliersToManyClientTest() {
        when(balanceContactService.getClientIdByUid(103)).thenReturn(126L);
        when(balanceService.getClient(126L)).thenReturn(new ClientInfo(126, ClientType.OOO, true, 126));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/full-info?euid={euid}", 103);
        JsonTestUtil.assertEquals("SupplierControllerSeveralAgencySuppliersToManyClientTest.json",
                getClass(), response);
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerAgencyAccessTest.before.csv")
    void testFullInfoWithoutAgency() {
        when(balanceContactService.getClientIdByUid(100)).thenReturn(444L);
        when(balanceService.getClient(444L)).thenReturn(new ClientInfo(444, ClientType.PHYSICAL));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/full-info?euid={euid}", 100);
        JsonTestUtil.assertEquals(response, getClass(), "SupplierControllerFullInfoWithoutAgency.json");
    }

    @Test
    void clientNotFoundTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/suppliers/full-info?euid={euid}", 127);
        JsonTestUtil.assertEquals(
                response,
                "{\"suppliers\":[]}"
        );
    }
}
