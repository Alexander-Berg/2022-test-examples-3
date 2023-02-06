package ru.yandex.market.delivery.deliveryintegrationtests.yard.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolkit.Delayer;

import ru.yandex.market.delivery.deliveryintegrationtests.yard.client.GraphTemplate;
import ru.yandex.market.delivery.deliveryintegrationtests.yard.client.YardApi;
import ru.yandex.market.delivery.deliveryintegrationtests.yard.step.YardSteps;

@DisplayName("Yard Test Курьерское приложение")
public class YardCourierTest {

    public static final List<Long> GRAPH_IDS =
            Arrays.asList(83732000L, 83732001L, 83732002L, 83732003L, 83732004L, 83732005L);
    private final static Logger log = LoggerFactory.getLogger(YardCourierTest.class);

    private final YardSteps yardSteps = new YardSteps();
    private static final YardApi yardApi = YardApi.getInstance();

    @BeforeAll
    public static void beforeAll() {
        for (Long graphId : GRAPH_IDS) {
            yardApi.createGraph(GraphTemplate.COURIER_GRAPH, graphId);
        }
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
    @DisplayName("Проверка прямого флоу на одном клиенте")
    public void oneClientFlowTest() {
        long serviceId = 83732000;
        String clientId = generateClientId();
        log.info("Register client {}", clientId);
        yardSteps.registerInTimeClients(List.of(clientId), serviceId);
        yardSteps.waitClientInStatus(clientId, serviceId, "WAITING");
        yardSteps.pushEvent(clientId, "ENTERED", serviceId);
        yardSteps.waitClientInStatus(clientId, serviceId, "PROCESSING");
        yardSteps.pushEvent(clientId, "FINISHED", serviceId);
        yardSteps.waitClientInStatus(clientId, serviceId, "PROCESSED");
        yardSteps.pushEvent(clientId, "LEFT", serviceId);
        yardSteps.waitClientInStatus(clientId, serviceId, "LEAVE_THE_SERVICE");
    }


    @Test
    @DisplayName("Тест сильно рано приехавших")
    public void tooEarlyClientWaitTest() {
        long serviceId = 83732001;
        String clientId = generateClientId();
        //Создаем сильно рано приехавшего
        yardSteps.registerAheadOfTimeClients(List.of(clientId), 35, serviceId);
        //Не переходит в статус
        yardSteps.waitClientInStatus(clientId, serviceId, "REGISTERED");
        Delayer.delay(1, TimeUnit.MINUTES);
        yardSteps.validateNotInStatus(clientId, serviceId, "ALLOCATED");
        yardSteps.validateNotInStatus(clientId, serviceId, "WAITING");
    }

    @Test
    @DisplayName("Тест просто рано приехавших если очередь свободна")
    public void earlyClientDoNotWaitWhenQueueAlmostEmptyTest() {
        long serviceId = 83732002;
        String clientId = generateClientId();
        //Создаем не сильно рано приехавшего
        yardSteps.registerAheadOfTimeClients(List.of(clientId), 25, serviceId);
        //Переходит в статус
        yardSteps.waitClientInStatus(clientId, serviceId, "WAITING");
    }

    @Test
    @DisplayName("Тест просто рано приехавших если очередь не достаточно свободна")
    public void earlyClientWhenQueueNotAlmostEmptyTest() {
        long serviceId = 83732003;
        //Заполняем очередь вовремя прибывшими
        List<String> clientIds = generateClientIds(3);
        yardSteps.registerInTimeClients(clientIds, serviceId);
        //Создаем не сильно рано приехавшего
        String clientId = generateClientId();
        yardSteps.registerAheadOfTimeClients(List.of(clientId), 25, serviceId);
        //Проверяем что клиент не переходит в статус
        Delayer.delay(1, TimeUnit.MINUTES);
        yardSteps.validateStatus(clientId, serviceId, "REGISTERED");
        yardSteps.validateNotInStatus(clientId, serviceId, "ALLOCATED");
    }

    @Test
    @DisplayName("Опоздавших пускаем каждым 15ым")
    public void lateClientEvery15thTest() {
        long serviceId = 83732004;
        //Забиваем очередь 5ю клиентами вовремя пришедшими
        List<String> clientIds = generateClientIds(5);
        yardSteps.registerInTimeClients(clientIds, serviceId);
        yardSteps.waitClientsInStatus(clientIds, serviceId, "WAITING");
        //Создаем опоздавшего
        String lateClientId = generateClientId();
        yardSteps.registerLateClients(List.of(lateClientId), 60, serviceId);
        //Проверяем у опоздавшего статус
        yardSteps.waitClientInStatus(lateClientId, serviceId, "ALLOCATED");
        //Забиваем очередь еще вовремя пришедшими
        List<String> clientIdsSecondBatch = generateClientIds(15);
        yardSteps.registerInTimeClients(clientIdsSecondBatch, serviceId);
        //Процессим первых 5
        yardSteps.processClients(clientIds, serviceId);
        yardSteps.waitClientsInStatus(clientIds, serviceId, "LEAVE_THE_SERVICE");
        yardSteps.validateStatus(lateClientId, serviceId, "ALLOCATED");
        //Процессим 10
        yardSteps.processClients(clientIdsSecondBatch.subList(0, 9), serviceId);
        yardSteps.waitClientsInStatus(clientIdsSecondBatch.subList(0, 9), serviceId, "LEAVE_THE_SERVICE");
        yardSteps.validateStatus(lateClientId, serviceId, "ALLOCATED");
        yardSteps.processClients(clientIdsSecondBatch.subList(9, 11), serviceId);
        //Проверяем что у опоздавшего статус поменялся
        yardSteps.waitClientInStatus(lateClientId, serviceId, "WAITING");
    }

    @Test
    @DisplayName("Тест капастити")
    public void capacityWorksTest() {
        long serviceId = 83732005;
        //Забиваем капасити полностью
        List<String> clientIds = generateClientIds(5);
        yardSteps.registerInTimeClients(clientIds, serviceId);
        yardSteps.waitClientsInStatus(clientIds, serviceId, "WAITING");
        String clientId = generateClientId();
        yardSteps.registerInTimeClients(List.of(clientId), serviceId);
        yardSteps.waitClientInStatus(clientId, serviceId, "ALLOCATED");
        yardSteps.processClients(clientIds, serviceId);
        yardSteps.waitClientInStatus(clientId, serviceId, "WAITING");
    }

    private String generateClientId() {
        return UUID.randomUUID().toString();
    }

    private List<String> generateClientIds(int n) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(generateClientId());
        }
        return result;
    }

}
