package ru.yandex.market.delivery.deliveryintegrationtests.yard.tests;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.yard.client.GraphTemplate;
import ru.yandex.market.delivery.deliveryintegrationtests.yard.client.YardApi;
import ru.yandex.market.delivery.deliveryintegrationtests.yard.step.YardSteps;

import toolkit.Delayer;

@DisplayName("Yard Test ЭО для ФФЦ")
public class YardFFTest {
    public static final String TEST_LOGIN = "test_login";
    public static final List<Long> GRAPH_IDS = Arrays.asList(83732007L, 83732008L, 83732009L);
    private final static Logger log = LoggerFactory.getLogger(YardFFTest.class);
    private static final YardApi yardApi = YardApi.getInstance();
    private static Map<Long, UUID> GRAPH_UUIDS = new HashMap<>();
    private final YardSteps yardSteps = new YardSteps();

    @BeforeAll
    public static void beforeAll() {
        for (Long graphId : GRAPH_IDS) {
            yardApi.createGraph(GraphTemplate.FF_GRAPH, graphId);
        }

        ValidatableResponse response = yardApi.getServicesUUIDs();
        JsonPath jsonPath = response.extract().jsonPath();

        List<Integer> ids = jsonPath.get("id");
        List<String> uuids = jsonPath.get("uuid");

        for (int i = 0; i < ids.size(); i++) {
            GRAPH_UUIDS.put(Long.valueOf(ids.get(i)), UUID.fromString(uuids.get(i)));
        }

        System.out.println();
    }

    @AfterAll
    public static void clean() {
        Delayer.delay(5, TimeUnit.SECONDS);

        ArrayList<Exception> exceptions = new ArrayList<>();
        for (Long graphId : GRAPH_IDS) {
            try {
                yardApi.deleteGraph(graphId);
            } catch (Exception e) {
                log.error("Exception on delete graph", e);
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            RuntimeException aggregatedException = new RuntimeException("Errors while delete graphs");
            exceptions.forEach(aggregatedException::addSuppressed);
            throw aggregatedException;
        }
    }

    @Test
    @DisplayName("Регистрация на разгрузку - обработан успешно")
    public void registerForShipmentSuccessfully() {
        long serviceId = 83732007L;
        UUID serviceUUID = GRAPH_UUIDS.get(serviceId);

        Map<String, UUID> result =
                yardSteps.getService(serviceUUID, "Диспетчерская входящего потока", "1");
        UUID capacityUUID = result.get("capacityUUID");
        UUID capacityUnitUUID = result.get("capacityUnitUUID");

        yardSteps.operatorLoginInWindow(TEST_LOGIN, capacityUnitUUID);
        yardSteps.operatorStart(TEST_LOGIN, capacityUnitUUID);
        yardSteps.driverRegisterInReqType("A123AA123", capacityUUID, "SHIPMENT");
        Long assignedClientId = yardSteps.waitClientAssigned(TEST_LOGIN, capacityUnitUUID);
        yardSteps.callDriver(serviceUUID, capacityUnitUUID, assignedClientId, TEST_LOGIN);
        yardSteps.startProcessingDriver(serviceUUID, capacityUnitUUID, assignedClientId, TEST_LOGIN);
        yardSteps.submitDriver(serviceUUID, capacityUnitUUID, assignedClientId, TEST_LOGIN);
    }

    @Test
    @DisplayName("Регистрация на погрузку - обработан в неявку")
    public void registerForLoading() {
        long serviceId = 83732008L;
        UUID serviceUUID = GRAPH_UUIDS.get(serviceId);

        Map<String, UUID> result =
                yardSteps.getService(serviceUUID, "Диспетчерская входящего потока", "1");
        UUID capacityUUID = result.get("capacityUUID");
        UUID capacityUnitUUID = result.get("capacityUnitUUID");

        yardSteps.operatorLoginInWindow(TEST_LOGIN, capacityUnitUUID);
        yardSteps.operatorStart(TEST_LOGIN, capacityUnitUUID);
        yardSteps.driverRegisterInReqType("O123OO123", capacityUUID, "LOADING");
        Long assignedClientId = yardSteps.waitClientAssigned(TEST_LOGIN, capacityUnitUUID);
        yardSteps.callDriver(serviceUUID, capacityUnitUUID, assignedClientId, TEST_LOGIN);
        yardSteps.cancelDriver(serviceUUID, capacityUnitUUID, assignedClientId, TEST_LOGIN);
    }

    @Test
    @DisplayName("Регистрация на подписание документов - отказ в обработке")
    public void registerForSigningDecline() {
        long serviceId = 83732009L;
        UUID serviceUUID = GRAPH_UUIDS.get(serviceId);

        Map<String, UUID> result =
                yardSteps.getService(serviceUUID, "Диспетчерская входящего потока", "1");
        UUID capacityUUID = result.get("capacityUUID");
        UUID capacityUnitUUID = result.get("capacityUnitUUID");

        yardSteps.operatorLoginInWindow(TEST_LOGIN, capacityUnitUUID);
        yardSteps.operatorStart(TEST_LOGIN, capacityUnitUUID);
        yardSteps.driverRegisterInReqType("O123OO123", capacityUUID, "SIGNING_DOCUMENTS");

        Long assignedClientId = yardSteps.waitClientAssigned(TEST_LOGIN, capacityUnitUUID);
        yardSteps.callDriver(serviceUUID, capacityUnitUUID, assignedClientId, TEST_LOGIN);
        yardSteps.startProcessingDriver(serviceUUID, capacityUnitUUID, assignedClientId, TEST_LOGIN);
        yardSteps.declineDriver(serviceUUID, capacityUnitUUID, assignedClientId, TEST_LOGIN);
    }

}
