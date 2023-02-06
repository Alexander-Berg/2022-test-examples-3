package ru.yandex.market.partner.mvc.controller.warehouses;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.delivery.DeliveryServiceMarketStatus;
import ru.yandex.market.core.delivery.DeliveryServiceShipmentType;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.program.partner.status.PartnerStatusService;
import ru.yandex.market.core.warehouse.BusinessWarehouseChangeData;
import ru.yandex.market.core.warehouse.model.WarehouseType;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversResponse;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static ru.yandex.common.util.collections.CollectionUtils.emptyIfNull;
import static ru.yandex.market.core.delivery.DeliveryServiceShipmentType.EXPRESS;

/**
 * Тесты для {@link BusinessWarehouseController}
 */
@DbUnitDataSet(before = "BusinessWarehouseControllerFunctionalTest.before.csv")
class BusinessWarehouseControllerFunctionalTest extends FunctionalTest {

    private static final int UID_BUSINESS_ADMIN = 100500;
    private static final int UID_SHOP_OPERATOR = 500100;
    private static final int AGENCY_UID = 10;
    private static final long AGENCY_CLIENT = 123;

    @Autowired
    private NesuClient nesuClient;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private BalanceContactService balanceContactService;
    @Autowired
    private PartnerStatusService partnerStatusService;

    @BeforeEach
    void initMocks() {
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(new StatusResolversResponse()));
    }

    @Test
    @DisplayName("BadRequest если партнер не DBS/Dropship/ Crossdock")
    void getBusinessWarehousesFailNotCorrectPartnerType() {
        assertThrows(HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.get(
                        baseUrl + "warehouses/" +
                                getAuthorizationParams(UID_BUSINESS_ADMIN, 210) +
                                getPagingParams(1, null)));
    }

    @DisplayName("Тесты фильтров")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("testDbsWarehousesArgs")
    public void DbsTests(String testName, String authParams, String pagingParams, String searchParams,
                         String expected) {
        environmentService.setValue("fbs.display.warehouses.by.business.enabled", "true");
        environmentService.setValue("display.warehouses.by.business.enabled", "true");
        when(balanceContactService.getClientIdByUid(AGENCY_UID)).thenReturn(AGENCY_CLIENT);

        var response = FunctionalTestHelper.get(
                baseUrl + "warehouses/" + authParams + pagingParams + searchParams);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expected, actualJson, false);
    }

    @DisplayName("Тесты ручки поиска складов по бизнесу")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getArgsSearchByBusiness")
    public void testSearchWhByBusiness(String testName, String uid, String pagingParams, String searchParams,
                                       String expected) {
        var response = FunctionalTestHelper.get(baseUrl + "business/104/warehouses/" +
                uid + pagingParams + searchParams);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expected, actualJson, false);
    }

    @Test
    @DisplayName("Тест кросдока")
    public void crossdockTest() {
        var expected = "{\"paging\":{\"nextPageToken\":\"eyJvcCI6Ij4iLCJrZXkiOjQwOSwic2tpcCI6MH0" +
                "\"},\"warehouses\":[{\"address\":\"warehouse7\",\"campaignId\":215," +
                "\"marketStatus\":\"on\", \"warehouseType\":\"crossdock\"," +
                "\"id\":409,\"name\":\"fby\",\"programStatus\":{\"status\":\"failed\"," +
                "\"subStatuses\":[{\"code\":\"feature_cutoff_types\"}]}}]}";
        var response = FunctionalTestHelper.get(
                baseUrl + "warehouses/" + getAuthorizationParams(UID_BUSINESS_ADMIN, 215) +
                        getPagingParams(1, null) +
                        getSearchStringParams(null, null, null, null));
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expected, actualJson, false);
    }

    @ParameterizedTest
    @CsvSource({
            "211, bs, 3",
            "211, house, 3",
            "212, 1, 1",
            "212, nothing, 0"
    })
    @DisplayName("Тест ручки /count-warehouses")
    public void testCountWarehousesByName(int id, String nameOrAddress, String expected) {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "count-warehouses/" +
                getAuthorizationParams(UID_BUSINESS_ADMIN, id) +
                getSearchStringParams(nameOrAddress, null, null, null));
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Тест ручки /count-warehouses")
    public void testCountWarehousesFullFilters() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "count-warehouses/" +
                getAuthorizationParams(UID_BUSINESS_ADMIN, 212) +
                getSearchStringParams("ware", EXPRESS, "Москва",
                        DeliveryServiceMarketStatus.ON));
        JsonTestUtil.assertEquals(response, "1");
    }


    @ParameterizedTest
    @CsvSource({
            "dbs, 2",
            "fbs, 3",
            "fby, 1",
            "house, 6",
            "warehouse13, 1",
            "513,1",
            "nothing, 0"
    })
    @DisplayName("Тест ручки business/{businessId}/count-warehouses")
    public void testCountWarehousesByName(String nameOrAddress, String expected) {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "business/104/count-warehouses/" +
                getAuthorizationParams(UID_BUSINESS_ADMIN, null) +
                getSearchStringParamsForBusinessHandler(nameOrAddress, null, null, null, null, null));
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Тест ручки business/{businessId}/count-warehouses")
    public void testCountWarehousesAllFilters() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "business/104/count-warehouses/" +
                getAuthorizationParams(UID_BUSINESS_ADMIN, null) +
                getSearchStringParamsForBusinessHandler("ware", 213L, DeliveryServiceMarketStatus.ON,
                        Set.of(DeliveryServiceType.DROPSHIP), null, null));
        JsonTestUtil.assertEquals(response, "1");
    }

    @Test
    @DisplayName("Тест ручки business/{businessId}/count-warehouses")
    public void testCountWarehousesShopAdmin() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "business/104/count-warehouses/" +
                getAuthorizationParams(UID_SHOP_OPERATOR, null));
        JsonTestUtil.assertEquals(response, "1");
    }

    @Test
    @DisplayName("Тест c ролью SHOP_OPERATOR")
    public void testSearchWithShopAdmin() {
        var expected = "{\"paging\":{},\"warehouses\":[" +
                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}]}";
        var response = FunctionalTestHelper.get(baseUrl + "business/104/warehouses/" +
                getAuthorizationParams(UID_SHOP_OPERATOR, null));
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expected, actualJson, false);
    }

    @Test
    @DisplayName("Тест получения списка доноров для репликации. BusinessAdmin")
    public void testSearchDonorBusinessAdmin() {
        var expected = "{\n" +
                "    \"paging\": {},\n" +
                "    \"warehouses\": [\n" +
                "        {\n" +
                "            \"warehouseType\": \"dropship\",\n" +
                "            \"address\": \"warehouse11\",\n" +
                "            \"shipmentType\": \"EXPRESS\",\n" +
                "            \"settlement\": \"Москва\",\n" +
                "            \"campaignId\": 218,\n" +
                "            \"marketStatus\": \"on\",\n" +
                "            \"id\": 510,\n" +
                "            \"status\": \"disabled\",\n" +
                "            \"name\": \"fbs1\",\n" +
                "            \"programStatus\": {\n" +
                "                \"status\": \"enabling\",\n" +
                "                \"subStatuses\": []\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"warehouseType\": \"dropship_by_seller\",\n" +
                "            \"address\": \"warehouse13\",\n" +
                "            \"shipmentType\": \"IMPORT\",\n" +
                "            \"settlement\": \"Санкт-Петербург\",\n" +
                "            \"campaignId\": 220,\n" +
                "            \"marketStatus\": \"on\",\n" +
                "            \"id\": 512,\n" +
                "            \"status\": \"disabled\",\n" +
                "            \"name\": \"dbs\",\n" +
                "            \"programStatus\": {\n" +
                "                \"status\": \"enabling\",\n" +
                "                \"subStatuses\": []\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"warehouseType\": \"dropship_by_seller\",\n" +
                "            \"address\": \"warehouse14\",\n" +
                "            \"shipmentType\": \"IMPORT\",\n" +
                "            \"settlement\": \"Санкт-Петербург\",\n" +
                "            \"campaignId\": 221,\n" +
                "            \"marketStatus\": \"on\",\n" +
                "            \"id\": 513,\n" +
                "            \"status\": \"disabled\",\n" +
                "            \"name\": \"dbs\",\n" +
                "            \"programStatus\": {\n" +
                "                \"status\": \"enabling\",\n" +
                "                \"subStatuses\": []\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "                \"partnerId\": 506,\n" +
                "                \"warehouseType\": \"dropship\",\n" +
                "                \"address\": \"warehouse12\",\n" +
                "                \"shipmentType\": \"IMPORT\",\n" +
                "                \"campaignId\": 225,\n" +
                "                \"marketStatus\": \"on\",\n" +
                "                \"id\": 517,\n" +
                "                \"name\": \"fbs\",\n" +
                "                \"region\": {\n" +
                "                    \"id\": 217\n" +
                "                },\n" +
                "                \"orgType\": \"0\",\n" +
                "                \"programStatus\": {\n" +
                "                    \"program\": \"marketplace\",\n" +
                "                    \"status\": \"empty\",\n" +
                "                    \"isEnabled\": false,\n" +
                "                    \"subStatuses\": [],\n" +
                "                    \"needTestingState\": \"not_required\",\n" +
                "                    \"newbie\": false\n" +
                "                },\n" +
                "                \"status\": \"disabled\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        var response = FunctionalTestHelper.get(baseUrl + "business/104/warehouses/" +
                getAuthorizationParams(UID_BUSINESS_ADMIN, null) + "&need_donor=true");
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expected, actualJson, false);
    }

    @Test
    @DisplayName("Тест получения списка доноров для репликации. ShopOperator")
    public void testSearchDonorShopAdmin() {
        var expected = "{\"paging\":{},\"warehouses\":[" +
                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}]}";
        var response = FunctionalTestHelper.get(baseUrl + "business/104/warehouses/" +
                getAuthorizationParams(UID_SHOP_OPERATOR, null) + "&need_donor=true");
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expected, actualJson, false);
    }

    @Test
    @DisplayName("Тест пагинации")
    public void pagingTest() {
        //1
        var response = FunctionalTestHelper.get(
                baseUrl + "warehouses/" + getAuthorizationParams(UID_BUSINESS_ADMIN, 211) +
                        getPagingParams(1, null) +
                        getSearchStringParams(null, null, null, null));
        var expected = "{\"paging\":{\"nextPageToken\":\"eyJvcCI6Ij4iLCJrZXkiOjQwMSwic2tpcCI6MH0\"}," +
                "\"warehouses\":[{\"id\":401,\"status\":\"incorrect\",\"partnerId\":201," +
                "\"warehouseType\":\"dropship_by_seller\"," +
                "\"address\":\"warehouse1\",\"marketStatus\":\"on\"," +
                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"name\":\"dbs\",\"campaignId\":211," +
                "\"programStatus\":{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
                "\"isEnabled\":false," +
                "\"subStatuses\":[{\"code\":\"work_mode\"}],\"needTestingState\":\"not_required\"," +
                "\"newbie\":false}, \"region\":{\"id\":213,\"name\":\"Москва\"},\"legalName\":\"ООО ЛАБОРАТОРИЯ " +
                "ИННОВАЦИЙ\",\"orgType\":\"1\"}]}";
        JsonTestUtil.assertEquals(response, expected);
        //2
        response = FunctionalTestHelper.get(
                baseUrl + "warehouses/" + getAuthorizationParams(UID_BUSINESS_ADMIN, 211) +
                        getPagingParams(1, "eyJvcCI6Ij4iLCJrZXkiOjQwMSwic2tpcCI6MH0") +
                        getSearchStringParams(null, null, null, null));
        expected = "{\"paging\":{\"prevPageToken\":\"eyJvcCI6IjwiLCJrZXkiOjQwMiwic2tpcCI6MH0\"," +
                "\"nextPageToken\":\"eyJvcCI6Ij4iLCJrZXkiOjQwMiwic2tpcCI6MH0\"}," +
                "\"warehouses\":[{\"id\":402,\"address\":\"warehouse3\",\"marketStatus\":\"on\"," +
                "\"shipmentType\":\"IMPORT\",\"partnerId\":202," +
                "\"warehouseType\":\"dropship_by_seller\",\"settlement\":\"Санкт-Петербург\"," +
                "\"name\":\"dbs\"," +
                "\"campaignId\":213,\"programStatus\":{\"program\":\"dropship_by_seller\"," +
                "\"status\":\"full\"," +
                "\"isEnabled\":true,\"subStatuses\":[],\"needTestingState\":\"not_required\"," +
                "\"newbie\":false},\"orgType\":\"0\"," +
                "\"status\":\"enabled\",\"region\":{\"id\":214,\"name\":\"Санкт-Петербург\"}}]}";
        JsonTestUtil.assertEquals(response, expected);
        //3
        response = FunctionalTestHelper.get(
                baseUrl + "warehouses/" + getAuthorizationParams(UID_BUSINESS_ADMIN, 211) +
                        getPagingParams(1, "eyJvcCI6Ij4iLCJrZXkiOjQwMiwic2tpcCI6MH0") +
                        getSearchStringParams(null, null, null, null));
        expected = "{\"paging\":{\"prevPageToken\":\"eyJvcCI6IjwiLCJrZXkiOjQwMywic2tpcCI6MH0\"," +
                "\"nextPageToken\":\"eyJvcCI6Ij4iLCJrZXkiOjQwMywic2tpcCI6MH0\"},\"warehouses\":[{\"id\":403," +
                "\"address\":\"warehouse4\",\"marketStatus\":\"off\",\"shipmentType\":\"EXPRESS\",\"warehouseType" +
                "\":\"dropship_by_seller\",\"settlement\":\"Москва\",\"name\":\"dbs\",\"campaignId\":214," +
                "\"programStatus\":{\"program\":\"dropship_by_seller\",\"status\":\"full\",\"isEnabled\":true," +
                "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false},\"partnerId\":203," +
                "\"status\":\"enabled\", \"region\":{\"id\":213,\"name\":\"Москва\"},\"orgType\":\"0\"}]}";
        JsonTestUtil.assertEquals(response, expected);
        //2
        response = FunctionalTestHelper.get(
                baseUrl + "warehouses/" + getAuthorizationParams(UID_BUSINESS_ADMIN, 211) +
                        getPagingParams(1, "eyJvcCI6IjwiLCJrZXkiOjQwMywic2tpcCI6MH0") +
                        getSearchStringParams(null, null, null, null));
        expected = "{\"paging\":{\"prevPageToken\":\"eyJvcCI6IjwiLCJrZXkiOjQwMiwic2tpcCI6MH0\"," +
                "\"nextPageToken\":\"eyJvcCI6Ij4iLCJrZXkiOjQwMiwic2tpcCI6MH0\"}," +
                "\"warehouses\":[{\"id\":402,\"address\":\"warehouse3\",\"marketStatus\":\"on\"," +
                "\"shipmentType\":\"IMPORT\"," +
                "\"warehouseType\":\"dropship_by_seller\",\"settlement\":\"Санкт-Петербург\",\"name\":\"dbs\"," +
                "\"campaignId\":213,\"programStatus\":{\"program\":\"dropship_by_seller\",\"status\":\"full\"," +
                "\"isEnabled\":true,\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}," +
                "\"status\":\"enabled\",\"region\":{\"id\":214,\"name\":\"Санкт-Петербург\"},\"partnerId\":202," +
                "\"orgType\":\"0\"}]}";
        JsonTestUtil.assertEquals(response, expected);
        //1
        response = FunctionalTestHelper.get(
                baseUrl + "warehouses/" + getAuthorizationParams(UID_BUSINESS_ADMIN, 211) +
                        getPagingParams(1, "eyJvcCI6IjwiLCJrZXkiOjQwMiwic2tpcCI6MH0") +
                        getSearchStringParams(null, null, null, null));
        expected = "{\"paging\":{\"prevPageToken\":\"eyJvcCI6IjwiLCJrZXkiOjQwMSwic2tpcCI6MH0\"," +
                "\"nextPageToken\":\"eyJvcCI6Ij4iLCJrZXkiOjQwMSwic2tpcCI6MH0\"}," +
                "\"warehouses\":[{\"id\":401,\"address\":\"warehouse1\",\"marketStatus\":\"on\"," +
                "\"shipmentType\":\"EXPRESS\"," +
                "\"settlement\":\"Москва\",\"name\":\"dbs\",\"campaignId\":211,\"orgType\":\"1\"," +
                "\"warehouseType\":\"dropship_by_seller\",\"partnerId\":201,\"legalName\":\"ООО ЛАБОРАТОРИЯ " +
                "ИННОВАЦИЙ\"," +
                "\"programStatus\":{\"program\":\"dropship_by_seller\",\"status\":\"failed\",\"isEnabled\":false," +
                "\"subStatuses\":[{\"code\":\"work_mode\"}],\"needTestingState\":\"not_required\"," +
                "\"newbie\":false},\"status\":\"incorrect\", \"region\":{\"id\":213,\"name\":\"Москва\"}}]}";
        JsonTestUtil.assertEquals(response, expected);
        //2,3
        response = FunctionalTestHelper.get(
                baseUrl + "warehouses/" + getAuthorizationParams(UID_BUSINESS_ADMIN, 211) +
                        getPagingParams(2, "eyJvcCI6Ij4iLCJrZXkiOjQwMSwic2tpcCI6MH0") +
                        getSearchStringParams(null, null, null, null));
        JsonTestUtil.assertEquals(response,
                "{\"paging\":{\"prevPageToken\":\"eyJvcCI6IjwiLCJrZXkiOjQwMiwic2tpcCI6MH0\"," +
                        "\"nextPageToken\":\"eyJvcCI6Ij4iLCJrZXkiOjQwMywic2tpcCI6MH0\"},\"warehouses\":[" +
                        "{\"id\":402,\"status\":\"enabled\",\"address\":\"warehouse3\",\"marketStatus\":\"on\"," +
                        "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"name\":\"dbs\"," +
                        "\"campaignId\":213,\"partnerId\":202,\"orgType\":\"0\"," +
                        " \"warehouseType\":\"dropship_by_seller\"," +
                        "\"programStatus\":{\"program\":\"dropship_by_seller\",\"status\":\"full\"," +
                        "\"isEnabled\":true," +
                        "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}," +
                        "\"status\":\"enabled\", \"region\":{\"id\":214,\"name\":\"Санкт-Петербург\"} }," +
                        "{\"id\":403,\"status\":\"enabled\",\"address\":\"warehouse4\",\"marketStatus\":\"off\"," +
                        "\"warehouseType\":\"dropship_by_seller\",\"partnerId\":203,\"orgType\":\"0\"," +
                        "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"name\":\"dbs\",\"campaignId\":214," +
                        "\"programStatus\":{\"program\":\"dropship_by_seller\",\"status\":\"full\"," +
                        "\"isEnabled\":true," +
                        "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}," +
                        "\"status\":\"enabled\", \"region\":{\"id\":213,\"name\":\"Москва\"}}" +
                        "]}");
    }

    @Test
    @DisplayName("Редактирование склада. Успешное")
    @DbUnitDataSet(after = "BusinessWarehouseControllerFunctionalTest.after.csv")
    public void updateWarehouse() {
        var dataToSend = new BusinessWarehouseChangeData("test", "id");
        var response = FunctionalTestHelper.put(
                baseUrl + "/warehouses" + getAuthorizationParams(UID_BUSINESS_ADMIN, 211),
                dataToSend);
        JsonTestUtil.assertEquals(response, "{\"warehouseName\":\"test\",\"externalWarehouseId\":\"id\"}");
    }

    @Test
    @DisplayName("Редактирование склада. лмс упал")
    public void updateWarehouseFail() {
        var dataToSend = new BusinessWarehouseChangeData("test", "id");
        doThrow(HttpClientErrorException.NotFound.class).when(nesuClient).updateShop(anyInt(), any());
        assertThrows(HttpClientErrorException.class, () -> FunctionalTestHelper.put(
                baseUrl + "/warehouses" + getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                dataToSend));
    }

    @Test
    void testGetBusinessWarehousesForAgency() {
        when(balanceContactService.getClientIdByUid(AGENCY_UID)).thenReturn(AGENCY_CLIENT);

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "business/104/warehouses" +
                "?_user_id=" + AGENCY_UID);
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals("{\"warehouses\":[{\"warehouseType\":\"dropship_by_seller\"," +
                "\"address\":\"warehouse13\"," +
                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"campaignId\":220," +
                "\"marketStatus\":\"on\",\"id\":512,\"status\":\"disabled\",\"name\":\"dbs\"," +
                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}," +
                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse14\"," +
                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"campaignId\":221," +
                "\"marketStatus\":\"on\",\"id\":513,\"status\":\"disabled\",\"name\":\"dbs\"," +
                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}], \"paging\":{}}", actualJson, false);
    }

    @Test
    void testGetBusinessWarehousesCountForAgency() {
        when(balanceContactService.getClientIdByUid(AGENCY_UID)).thenReturn(AGENCY_CLIENT);

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "business/104/count-warehouses" +
                "?_user_id=" + AGENCY_UID);
        JsonTestUtil.assertEquals(response, "2");
    }

    @Test
    @DisplayName("Тест получения списка городов")
    void testGetRegions() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "business/104/warehouses/regions");
        JsonTestUtil.assertEquals(response, "[{\"id\":216,\"name\":\"Воронеж\"},{\"id\":213,\"name\":\"Москва\"}," +
                "{\"id\":214,\"name\":\"Санкт-Петербург\"}]");
    }

    @Test
    void testGetWarehouseStatus() {
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(baseUrl + "warehouses/status?id=211"),
                "{\"partnerId\":201,\"programStatus\":{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
                        "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"work_mode\"}]," +
                        "\"needTestingState\":\"not_required\",\"newbie\":false},\"status\":\"incorrect\"}");
    }

    @Test
    void testGetExtraParams() {
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(baseUrl + "/business/104/warehouses/extra-params?" +
                        "partner_id=500&partner_id=501"),
                "{\"extraParamsList\":[{\"partnerId\":500,\"programStatus\":{\"program\":\"marketplace\"," +
                        "\"status\":\"enabling\",\"isEnabled\":false,\"subStatuses\":[]," +
                        "\"needTestingState\":\"not_required\",\"newbie\":false},\"status\":\"disabled\"," +
                        "\"legalName\":\"ООО ЛАБОРАТОРИЯ ИННОВАЦИЙ\",\"orgType\":\"1\", \"isPi\":false}," +
                        "{\"partnerId\":501," +
                        "\"programStatus\":{\"program\":\"marketplace\",\"status\":\"empty\",\"isEnabled\":false," +
                        "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}," +
                        "\"status\":\"disabled\", \"isPi\":true}]}");
    }

    @DbUnitDataSet(before = "BusinessWarehouseGroup.before.csv")
    @DisplayName("Список складов по бизнесу с флагом для объеденения")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getArgsSearchByBusinessWithMerge")
    void getWarehouseList(String testName, String uid, String pagingParams, String searchParams,
                          String expected) {
        var response = FunctionalTestHelper.get(baseUrl + "business/104/warehouses/for-grouping" +
                uid + pagingParams + searchParams);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expected, actualJson, false);
    }

    private static Stream<Arguments> getArgsSearchByBusinessWithMerge() {
        return Stream.of(
                Arguments.of("Все типы складов на бизнесе.",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, null, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"name\":\"fbs1\", \"groupable\": false," +
                                "\"pi\":false}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Воронеж\",\"campaignId\":219," +
                                "\"marketStatus\":\"on\",\"id\":511,\"name\":\"fbs1\", \"groupable\": true," +
                                "\"pi\":true}," +
                                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse13\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"campaignId\":220," +
                                "\"marketStatus\":\"on\",\"id\":512,\"name\":\"dbs\", \"groupable\": false," +
                                "\"pi\":true}," +
                                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse14\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"campaignId\":221," +
                                "\"marketStatus\":\"on\",\"id\":513,\"name\":\"dbs\", \"groupable\": true," +
                                "\"pi\":true}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"campaignId\":225," +
                                "\"marketStatus\":\"on\",\"id\":517,\"name\":\"fbs\", \"groupable\": false," +
                                "\"pi\":false}," +
                                "{\"warehouseType\":\"dropship\"," +
                                "\"address\":\"warehouse-dropship-selfemployed-2\",\"shipmentType\":\"EXPRESS\"," +
                                "\"campaignId\":416,\"marketStatus\":\"on\",\"id\":527," +
                                "\"name\":\"fbs\", \"groupable\": true, \"pi\":true}," +
                                "{\"warehouseType\":\"dropship\"," +
                                "\"address\":\"warehouse-dropship-selfemployed-2\",\"shipmentType\":\"EXPRESS\"," +
                                "\"campaignId\":413,\"marketStatus\":\"on\",\"id\":525," +
                                "\"name\":\"fbs\", \"groupable\": false, \"pi\":true}," +
                                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse-dsbs-selfemployed-2\"," +
                                "\"campaignId\":414,\"marketStatus\":\"on\",\"id\":526," +
                                "\"name\":\"dbs\", \"groupable\": true, \"pi\":true}," +
                                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse-dsbs-selfemployed-2\"," +
                                "\"campaignId\":415,\"marketStatus\":\"on\",\"id\":528," +
                                "\"name\":\"dbs\", \"groupable\": true, \"pi\":true}]}"),
                Arguments.of("Все склады с фильтром по ид.",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler("510", null, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"name\":\"fbs1\", \"groupable\": false }]}"),
                Arguments.of("Фильтр по partnerId",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler("500", null, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\", " +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218, " +
                                "\"marketStatus\":\"on\",\"id\":510,\"name\":\"fbs1\", \"groupable\": false }]}"),
                Arguments.of("Все склады с фильтром по имени",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler("fbs1", null, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"name\":\"fbs1\", \"groupable\": false }," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Воронеж\",\"campaignId\":219," +
                                "\"marketStatus\":\"on\",\"id\":511,\"name\":\"fbs1\", \"groupable\": true }]}")
        );
    }

    private static Stream<Arguments> testDbsWarehousesArgs() {
        return Stream.of(
                Arguments.of("Пустой ответ если нет подходящего под фильтры DBS склада",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 211),
                        getPagingParams(1, null),
                        getSearchStringParams("такого слада нет совсем олололло",
                                null, null, null),
                        "{\"paging\":{},\"warehouses\":[]}"),
                Arguments.of("Все склады DBS бизнеса",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 211),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":401,\"status\":\"incorrect\",\"warehouseType\":\"dropship_by_seller\"," +
                                "\"address\":\"warehouse1\"," +
                                "\"marketStatus\":\"on\",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"dbs\",\"campaignId\":211," +
                                "\"programStatus\":{\"status\":\"failed\"," +
                                "\"subStatuses\":[{\"code\":\"work_mode\"}]}}," +
                                "{\"id\":402,\"status\":\"enabled\",\"warehouseType\":\"dropship_by_seller\"," +
                                "\"address\":\"warehouse3\"," +
                                "\"marketStatus\":\"on\",\"shipmentType\":\"IMPORT\"," +
                                "\"settlement\":\"Санкт-Петербург\"," +
                                "\"name\":\"dbs\",\"campaignId\":213," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":403,\"status\":\"enabled\",\"warehouseType\":\"dropship_by_seller\"," +
                                "\"address\":\"warehouse4\"," +
                                "\"marketStatus\":\"off\",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"dbs\",\"campaignId\":214," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}" +
                                "]}"
                ),
                Arguments.of("Склады DBS бизнеса с фильтром по типу отгрузки",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 211),
                        getPagingParams(10, null),
                        getSearchStringParams(null, EXPRESS, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":401,\"status\":\"incorrect\",\"address\":\"warehouse1\"," +
                                "\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"dbs\",\"campaignId\":211,\"warehouseType\":\"dropship_by_seller\"," +
                                "\"programStatus\":{\"status\":\"failed\"," +
                                "\"subStatuses\":[{\"code\":\"work_mode\"}]}}," +
                                "{\"id\":403,\"status\":\"enabled\",\"address\":\"warehouse4\"," +
                                "\"marketStatus\":\"off\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"dbs\",\"campaignId\":214,\"warehouseType\":\"dropship_by_seller\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}" +
                                "]}"
                ),
                Arguments.of("Склады DBS бизнеса с фильтром по статусу",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 211),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, DeliveryServiceMarketStatus.OFF),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":403,\"status\":\"enabled\",\"address\":\"warehouse4\"," +
                                "\"marketStatus\":\"off\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"dbs\",\"campaignId\":214,\"warehouseType\":\"dropship_by_seller\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}" +
                                "]}"
                ),
                Arguments.of("Склады DBS бизнеса с фильтром адресу частичное совпадение",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 211),
                        getPagingParams(10, null),
                        getSearchStringParams("wareh", null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":401,\"status\":\"incorrect\",\"address\":\"warehouse1\"," +
                                "\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"dbs\",\"campaignId\":211,\"warehouseType\":\"dropship_by_seller\"," +
                                "\"programStatus\":{\"status\":\"failed\"," +
                                "\"subStatuses\":[{\"code\":\"work_mode\"}]}}," +
                                "{\"id\":402,\"status\":\"enabled\",\"address\":\"warehouse3\"," +
                                "\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\"," +
                                "\"name\":\"dbs\",\"campaignId\":213,\"warehouseType\":\"dropship_by_seller\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":403,\"status\":\"enabled\",\"address\":\"warehouse4\"," +
                                "\"marketStatus\":\"off\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"dbs\",\"campaignId\":214,\"warehouseType\":\"dropship_by_seller\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}" +
                                "]}"
                ),
                Arguments.of("Склады DBS бизнеса с фильтром адресу полное совпадение совпадение",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 211),
                        getPagingParams(10, null),
                        getSearchStringParams("warehouse3", null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":402,\"status\":\"enabled\",\"address\":\"warehouse3\"," +
                                "\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\"," +
                                "\"name\":\"dbs\",\"campaignId\":213,\"warehouseType\":\"dropship_by_seller\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}" +
                                "]}"
                ),
                Arguments.of("Пустой ответ если нет подходящего под фильтры FBS склада",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                        getPagingParams(1, null),
                        getSearchStringParams("такого слада нет совсем олололло",
                                null, null, null),
                        "{\"paging\":{},\"warehouses\":[]}"),
                Arguments.of("DBS - самозанятые. Подстатус npd_unavailable из-за отсутствия доступа в Мой Налог",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 412),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, null),
                        "{\"warehouses\":[{\"address\":\"warehouse-dsbs-selfemployed-1\",\"campaignId\":412," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"legalName\":\"ООО " +
                                "ЛАБОРАТОРИЯ ИННОВАЦИЙ\",\"orgType\":\"1\",\"marketStatus\":\"on\",\"name\":\"dbs\"," +
                                "\"partnerId\":512,\"warehouseType\":\"dropship_by_seller\",\"id\":524," +
                                "\"region\":{\"name\":\"Санкт-Петербург\",\"id\":214}," +
                                "\"programStatus\":{\"needTestingState\":\"not_required\"," +
                                "\"subStatuses\":[{\"code\":\"npd_unavailable\"}],\"newbie\":false," +
                                "\"isEnabled\":false,\"program\":\"dropship_by_seller\",\"status\":\"failed\"}," +
                                "\"status\":\"disabled\"}],\"paging\":{}}"),
                Arguments.of("DBS - самозанятые. Доступ в Мой Налог есть",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 410),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, null),
                        "{\"warehouses\":[{\"address\":\"warehouse-dsbs-selfemployed-2\",\"campaignId\":410," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"legalName\":\"ООО " +
                                "ЛАБОРАТОРИЯ ИННОВАЦИЙ\",\"orgType\":\"1\",\"marketStatus\":\"on\",\"name\":\"dbs\"," +
                                "\"partnerId\":510,\"warehouseType\":\"dropship_by_seller\",\"id\":522," +
                                "\"region\":{\"name\":\"Санкт-Петербург\",\"id\":214}," +
                                "\"programStatus\":{\"needTestingState\":\"not_required\",\"subStatuses\":[]," +
                                "\"newbie\":false,\"isEnabled\":true,\"program\":\"dropship_by_seller\"," +
                                "\"status\":\"full\"},\"status\":\"enabled\"}],\"paging\":{}}"),
                Arguments.of("FBS - самозанятые. Подстатус npd_unavailable из-за отсутствия доступа в Мой Налог",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 409),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, null),
                        "{\"warehouses\":[{\"address\":\"warehouse-dropship-selfemployed-1\",\"campaignId\":409," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"legalName\":\"ООО " +
                                "ЛАБОРАТОРИЯ ИННОВАЦИЙ\",\"orgType\":\"1\",\"marketStatus\":\"on\",\"name\":\"fbs\"," +
                                "\"partnerId\":509,\"warehouseType\":\"dropship\",\"id\":521," +
                                "\"region\":{\"name\":\"Москва\",\"id\":213}," +
                                "\"programStatus\":{\"needTestingState\":\"not_required\"," +
                                "\"subStatuses\":[{\"code\":\"npd_unavailable\"}],\"newbie\":false," +
                                "\"isEnabled\":false,\"program\":\"marketplace\",\"status\":\"failed\"}," +
                                "\"status\":\"disabled\"}],\"paging\":{}}"),
                Arguments.of("FBS - самозанятые. Доступ в Мой Налог есть",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 411),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, null),
                        "{\"warehouses\":[{\"address\":\"warehouse-dropship-selfemployed-2\",\"campaignId\":411," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"legalName\":\"ООО " +
                                "ЛАБОРАТОРИЯ ИННОВАЦИЙ\",\"orgType\":\"1\",\"marketStatus\":\"on\",\"name\":\"fbs\"," +
                                "\"partnerId\":511,\"warehouseType\":\"dropship\",\"id\":523," +
                                "\"region\":{\"name\":\"Москва\",\"id\":213}," +
                                "\"programStatus\":{\"needTestingState\":\"not_required\"," +
                                "\"subStatuses\":[{\"code\":\"no_loaded_offers\"}],\"newbie\":false," +
                                "\"isEnabled\":false,\"program\":\"marketplace\",\"status\":\"failed\"}," +
                                "\"status\":\"disabled\"}],\"paging\":{}}"),
                Arguments.of("Все склады FBS бизнеса",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":404,\"address\":\"warehouse1\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":405,\"address\":\"warehouse3\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":406,\"address\":\"warehouse4\",\"marketStatus\":\"off\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":407,\"address\":\"warehouse5\",\"marketStatus\":\"on\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Казань\"," +
                                "\"name\":\"fbs\",\"campaignId\":216,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"failed\",\"subStatuses\":[{\"code\":\"work_mode\"}, " +
                                "{\"code\":\"feature_cutoff_types\"}]}}" +
                                "]}"
                ),
                Arguments.of("Все склады FBS смешанного бизнеса",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 217),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":410,\"address\":\"warehouse10\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Воронеж\"," +
                                "\"name\":\"fbs\",\"campaignId\":217,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}" +
                                "]}"
                ),
                Arguments.of("Склады FBS бизнеса с фильтром по типу отгрузки",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                        getPagingParams(10, null),
                        getSearchStringParams(null, EXPRESS, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":404,\"address\":\"warehouse1\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":406,\"address\":\"warehouse4\",\"marketStatus\":\"off\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":407,\"address\":\"warehouse5\",\"marketStatus\":\"on\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Казань\"," +
                                "\"name\":\"fbs\",\"campaignId\":216,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"failed\",\"subStatuses\":[{\"code\":\"work_mode\"}," +
                                "{\"code\":\"feature_cutoff_types\"}]}}" +
                                "]}"
                ),
                Arguments.of("Склады DBS бизнеса с фильтром по статусу",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, DeliveryServiceMarketStatus.OFF),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":406,\"address\":\"warehouse4\",\"marketStatus\":\"off\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}" +
                                "]}"
                ),
                Arguments.of("Склады FBS бизнеса с фильтром адресу частичное совпадение",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                        getPagingParams(10, null),
                        getSearchStringParams("wareh", null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":404,\"address\":\"warehouse1\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":405,\"address\":\"warehouse3\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":406,\"address\":\"warehouse4\",\"marketStatus\":\"off\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":407,\"address\":\"warehouse5\",\"marketStatus\":\"on\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Казань\"," +
                                "\"name\":\"fbs\",\"campaignId\":216,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"failed\",\"subStatuses\":[{\"code\":\"work_mode\"}," +
                                "{\"code\":\"feature_cutoff_types\"}]}}" +
                                "]}"
                ),
                Arguments.of("Склады FBS бизнеса с фильтром адресу полное совпадение совпадение",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                        getPagingParams(10, null),
                        getSearchStringParams("warehouse3", null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":405,\"address\":\"warehouse3\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}" +
                                "]}"
                ),
                Arguments.of("Проверяем регистрозависимость поиска",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                        getPagingParams(10, null),
                        getSearchStringParams("UsE3", null, null, null),
                        "{\"paging\":{},\"warehouses\":[]}"
                ),
                Arguments.of("Один склад для агентства",
                        getAuthorizationParams(AGENCY_UID, 211),
                        getPagingParams(10, null),
                        getSearchStringParams(null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":401,\"status\":\"incorrect\",\"address\":\"warehouse1\"," +
                                "\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"warehouseType" +
                                "\":\"dropship_by_seller\"," +
                                "\"name\":\"dbs\",\"campaignId\":211," +
                                "\"programStatus\":{\"status\":\"failed\"," +
                                "\"subStatuses\":[{\"code\":\"work_mode\"}]}}" +
                                "]}"
                ),
                Arguments.of("Проверяем поиск по названию склада",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, 212),
                        getPagingParams(10, null),
                        getSearchStringParams("bs", null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"id\":404,\"address\":\"warehouse1\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":405,\"address\":\"warehouse3\",\"marketStatus\":\"on\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":406,\"address\":\"warehouse4\",\"marketStatus\":\"off\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\"," +
                                "\"name\":\"fbs\",\"campaignId\":212,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"full\",\"subStatuses\":[]}}," +
                                "{\"id\":407,\"address\":\"warehouse5\",\"marketStatus\":\"on\"" +
                                ",\"shipmentType\":\"EXPRESS\",\"settlement\":\"Казань\"," +
                                "\"name\":\"fbs\",\"campaignId\":216,\"warehouseType\":\"dropship\"," +
                                "\"programStatus\":{\"status\":\"failed\",\"subStatuses\":[{\"code\":\"work_mode\"}," +
                                "{\"code\":\"feature_cutoff_types\"}]}}" +
                                "]}"
                )
        );
    }

    private static Stream<Arguments> getArgsSearchByBusiness() {
        return Stream.of(
                Arguments.of("Все типы складов на бизнесе.",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, null, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Воронеж\",\"campaignId\":219," +
                                "\"marketStatus\":\"on\",\"id\":511,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"crossdock\",\"address\":\"warehouse71\",\"campaignId\":222," +
                                "\"marketStatus\":\"off\",\"id\":514,\"status\":\"disabled\",\"name\":\"fby\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse13\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"campaignId\":220," +
                                "\"marketStatus\":\"on\",\"id\":512,\"status\":\"disabled\",\"name\":\"dbs\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse14\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"campaignId\":221," +
                                "\"marketStatus\":\"on\",\"id\":513,\"status\":\"disabled\",\"name\":\"dbs\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"campaignId\":225," +
                                "\"marketStatus\":\"on\",\"id\":517,\"status\":\"disabled\",\"name\":\"fbs\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}]}"),
                Arguments.of("FBS склады бизнеса",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, null, null,
                                Set.of(DeliveryServiceType.DROPSHIP), null,
                                null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Воронеж\",\"campaignId\":219," +
                                "\"marketStatus\":\"on\",\"id\":511,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"campaignId\":225," +
                                "\"marketStatus\":\"on\",\"id\":517,\"status\":\"disabled\",\"name\":\"fbs\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}]}"),
                Arguments.of("FBS и DBS склады бизнеса",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, null, null,
                                Set.of(DeliveryServiceType.DROPSHIP, DeliveryServiceType.DROPSHIP_BY_SELLER), null,
                                null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Воронеж\",\"campaignId\":219," +
                                "\"marketStatus\":\"on\",\"id\":511,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse13\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"campaignId\":220," +
                                "\"marketStatus\":\"on\",\"id\":512,\"status\":\"disabled\",\"name\":\"dbs\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship_by_seller\",\"address\":\"warehouse14\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Санкт-Петербург\",\"campaignId\":221," +
                                "\"marketStatus\":\"on\",\"id\":513,\"status\":\"disabled\",\"name\":\"dbs\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"campaignId\":225," +
                                "\"marketStatus\":\"on\",\"id\":517,\"status\":\"disabled\",\"name\":\"fbs\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}]}"),
                Arguments.of("Все склады с фильтром по адресу.",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler("warehouse11", null, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}]}"),
                Arguments.of("Все склады с фильтром по городу.",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, 213L, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}]}"),
                Arguments.of("Все склады с фильтром по ЛМС статусу.",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, null, DeliveryServiceMarketStatus.OFF, null,
                                null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"crossdock\",\"address\":\"warehouse71\"," +
                                "\"campaignId\":222,\"marketStatus\":\"off\",\"id\":514,\"name\":\"fby\"}]}"),
                Arguments.of("Все склады с фильтром по ид.",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler("510", null, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}]}"),
                Arguments.of("склады с warehouseType = EXPRESS",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, null, null, null, WarehouseType.EXPRESS, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}]}"),
                Arguments.of("Фильтр по partnerId",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler("500", null, null, null, null, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\",\"partnerId\":500," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}]}"),
                Arguments.of("склады с warehouseType = FBS",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, null, null, null, WarehouseType.FBS, null),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"settlement\":\"Воронеж\",\"campaignId\":219," +
                                "\"marketStatus\":\"on\",\"id\":511,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}," +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse12\"," +
                                "\"shipmentType\":\"IMPORT\",\"campaignId\":225," +
                                "\"marketStatus\":\"on\",\"id\":517,\"status\":\"disabled\",\"name\":\"fbs\"," +
                                "\"programStatus\":{\"status\":\"empty\",\"subStatuses\":[]}}]}"),
                Arguments.of("Все склады с фильтром по юр.инфо",
                        getAuthorizationParams(UID_BUSINESS_ADMIN, null),
                        getPagingParams(10, null),
                        getSearchStringParamsForBusinessHandler(null, null, null, null, null, 134824L),
                        "{\"paging\":{},\"warehouses\":[" +
                                "{\"warehouseType\":\"dropship\",\"address\":\"warehouse11\"," +
                                "\"shipmentType\":\"EXPRESS\",\"settlement\":\"Москва\",\"campaignId\":218," +
                                "\"marketStatus\":\"on\",\"id\":510,\"status\":\"disabled\",\"name\":\"fbs1\"," +
                                "\"programStatus\":{\"status\":\"enabling\",\"subStatuses\":[]}}]}")
        );
    }

    private static String getSearchStringParams(@Nullable String addressOrWarehouseName,
                                                @Nullable DeliveryServiceShipmentType shipmentType,
                                                @Nullable String settlement,
                                                @Nullable DeliveryServiceMarketStatus warehouseStatus) {
        return getSearchStringParams(null, addressOrWarehouseName, shipmentType, settlement, warehouseStatus);
    }

    private static String getSearchStringParams(@Nullable Long businessId,
                                                @Nullable String addressOrWarehouseName,
                                                @Nullable DeliveryServiceShipmentType shipmentType,
                                                @Nullable String settlement,
                                                @Nullable DeliveryServiceMarketStatus warehouseStatus) {
        var params = new StringBuilder();
        if (businessId != null) {
            params.append("&businessId=").append(businessId);
        }
        if (addressOrWarehouseName != null) {
            params.append("&address_or_warehouse_name=").append(addressOrWarehouseName);
        }
        if (shipmentType != null) {
            params.append("&shipment_type=").append(shipmentType.name());
        }
        if (settlement != null) {
            params.append("&settlement=").append(settlement);
        }
        if (warehouseStatus != null) {
            params.append("&warehouse_status=").append(warehouseStatus.name().toLowerCase());
        }
        return params.toString();
    }

    private static String getSearchStringParamsForBusinessHandler(
            @Nullable String searchString,
            @Nullable Long regionId,
            @Nullable DeliveryServiceMarketStatus warehouseStatus,
            @Nullable Set<DeliveryServiceType> deliveryServiceTypes,
            @Nullable WarehouseType warehouseType,
            @Nullable Long requestId) {

        var params = new StringBuilder();
        if (searchString != null) {
            params.append("&search_string=").append(searchString);
        }
        if (regionId != null) {
            params.append("&region_id=").append(regionId);
        }
        if (warehouseStatus != null) {
            params.append("&warehouse_status=").append(warehouseStatus.name().toLowerCase());
        }
        emptyIfNull(deliveryServiceTypes).forEach(deliveryServiceType -> {
            params.append("&warehouse_type=").append(deliveryServiceType.name().toLowerCase());
        });
        if (warehouseType != null) {
            params.append("&type=").append(warehouseType.name());
        }
        if (requestId != null) {
            params.append("&application_request_id=").append(requestId);
        }
        return params.toString();
    }

    private static String getPagingParams(int limit, String pageToken) {
        return "&limit=" + limit + (pageToken == null ? "" : "&page_token=" + pageToken);
    }

    private static String getAuthorizationParams(int uid, Integer id) {
        return "?_user_id=" + uid + (id == null ? "" : "&id=" + id);
    }
}
