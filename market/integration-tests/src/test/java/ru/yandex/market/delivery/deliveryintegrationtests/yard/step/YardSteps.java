package ru.yandex.market.delivery.deliveryintegrationtests.yard.step;


import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Iterables;
import io.qameta.allure.Step;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;
import toolkit.Delayer;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.yard.client.GraphTemplate;
import ru.yandex.market.delivery.deliveryintegrationtests.yard.client.YardApi;
import ru.yandex.market.delivery.deliveryintegrationtests.yard.dto.YardClientEventDto;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Resource.Classpath({"delivery/report.properties"})
public class YardSteps {

    private final Logger log = LoggerFactory.getLogger(YardSteps.class);
    private final YardApi yardApi = YardApi.getInstance();


    @Step("Регистрация клиента {clientId} с датой прибытия {arrivalPlannedDate}")
    private Long registerClient(
            String clientId,
            Long testServiceId,
            ZonedDateTime arrivalPlannedDate,
            ZonedDateTime arrivalPlannedDateTo
    ) {
        return registerClient(
                clientId,
                testServiceId,
                "Test Testov",
                "+700010020099",
                arrivalPlannedDate,
                arrivalPlannedDateTo
        );
    }

    private Long registerClient(
            String clientId,
            Long serviceId,
            String name,
            String phone,
            ZonedDateTime arrivalPlannedDate,
            ZonedDateTime arrivalPlannedDateTo
    ) {
        return yardApi.registerClient(
                clientId,
                serviceId,
                name,
                phone,
                arrivalPlannedDate,
                arrivalPlannedDateTo
        )
                .extract()
                .jsonPath()
                .getLong("id");
    }

    @Step("Регистрация клиентов прибывших вовремя")
    public void registerInTimeClients(List<String> clientIds, long serviceId) {
        for (String clientId : clientIds) {

            ZonedDateTime from = ZonedDateTime.now();
            ZonedDateTime to = from.plus(60, ChronoUnit.MINUTES);

            registerClient(
                    clientId,
                    serviceId,
                    from,
                    to
            );
            Delayer.delay(5, TimeUnit.SECONDS);
        }
    }

    @Step("Регистрация клиентов опоздавших на {minutes} минут")
    public void registerLateClients(List<String> clientIds, int minutes, long serviceId) {
        for (String clientId : clientIds) {
            ZonedDateTime from = ZonedDateTime.now().minus(minutes, ChronoUnit.MINUTES);
            ZonedDateTime to = from.plus(60, ChronoUnit.MINUTES);
            registerClient(
                    clientId,
                    serviceId,
                    from,
                    to
            );
            Delayer.delay(1, TimeUnit.SECONDS);
        }
    }

    @Step("Регистрация клиентов прибывших раньше на {minutes} минут")
    public void registerAheadOfTimeClients(List<String> clientIds, int minutes, long serviceId) {
        for (String clientId : clientIds) {
            ZonedDateTime from = ZonedDateTime.now().plus(minutes, ChronoUnit.MINUTES);
            ZonedDateTime to = from.plus(60, ChronoUnit.MINUTES);
            registerClient(
                    clientId,
                    serviceId,
                    from,
                    to
            );
            Delayer.delay(1, TimeUnit.SECONDS);
        }
    }

    public void processClients(List<String> clientIds, long serviceId) {
        processClients(clientIds, serviceId, clientIds.size());
    }

    @Step("Обрабатываем клиентов")
    private void processClients(List<String> clientIds, long serviceId, int size) {
        Iterables.partition(clientIds, 5).forEach(batch -> {
                    for (String clientId : batch) {
                        pushEvent(clientId, "ENTERED", serviceId);
                    }
                    Delayer.delay(5, TimeUnit.SECONDS);
                    for (String clientId : batch) {
                        pushEvent(clientId, "FINISHED", serviceId);
                    }
                    Delayer.delay(5, TimeUnit.SECONDS);
                    for (String clientId : batch) {
                        pushEvent(clientId, "LEFT", serviceId);
                    }
                }
        );
    }

    @Step("Отправка события {state} для {clientId}")
    public void pushEvent(String clientId, String state, long serviceId) {

        YardClientEventDto yardClientEventDto = new YardClientEventDto();
        yardClientEventDto.setClientId(clientId);
        yardClientEventDto.setEventDate(LocalDateTime.now());
        yardClientEventDto.setEventType(state);

        yardApi.pushEvent(
                serviceId,
                List.of(yardClientEventDto)
        );

    }

    @Step("Клиент {clientId} в статусе {state}")
    public void validateStatus(String clientId, long serviceId, String... state) {
        yardApi.getClientInfo(
                serviceId,
                clientId
        ).body("stateName", in(state));

    }

    @Step("Клиент {clientId} в статусе {state}")
    public void validateNotInStatus(String clientId, long serviceId, String state) {

        yardApi.getClientInfo(
                serviceId,
                clientId
        ).body("stateName", not(state));

    }


    @Step("Проверяем что клиент {clientId} в статусе {state}")
    private void verifyClientInStatus(String clientId, long serviceId, String state) {
        yardApi.getClientInfo(
                serviceId,
                clientId
        ).body("stateName", is(state));
    }

    private void verifyClientsInStatus(Collection<String> clientIds, long serviceId, String state) {
        for (String clientId : clientIds) {
            verifyClientInStatus(clientId, serviceId, state);
        }
    }

    @Step("Ждем пока клиент перейдет в статус {state}")
    public void waitClientInStatus(String clientId, long serviceId, String state) {
        Delayer.delay(5, TimeUnit.SECONDS);
        Retrier.retry(() -> verifyClientInStatus(clientId, serviceId, state),
                Retrier.RETRIES_BIG,
                5,
                TimeUnit.SECONDS
        );
    }

    @Step("Ждем пока клиенты перейдут в статус {state}")
    public void waitClientsInStatus(Collection<String> clientIds, long serviceId, String state) {
        Delayer.delay(5, TimeUnit.SECONDS);
        Retrier.retry(() -> verifyClientsInStatus(clientIds, serviceId, state),
                Retrier.RETRIES_BIG,
                5,
                TimeUnit.SECONDS
        );
    }

    @Step("Создаем граф")
    public void createGraph(GraphTemplate graphTemplate, Long graphId) {
        yardApi.createGraph(graphTemplate, graphId);
    }

    @Step("Удаляем граф")
    public void deleteGraph(Long graphId) {
        yardApi.deleteGraph(graphId);
    }

    @Step("Получаем граф {graphId}")
    public Map<String, UUID> getService(UUID serviceUUID, String capacityName, String capacityUnitName) {
        ValidatableResponse response = yardApi.getService(serviceUUID);
        JsonPath jsonPath = response.extract().jsonPath();
        UUID capacityUUID = jsonPath.getUUID("capacities.find{it.name == '" + capacityName + "'}.uuid");
        UUID capacityUnitUUID = jsonPath.getUUID(
                "capacities.find{it.name == '" + capacityName + "'}" +
                        ".capacityUnits.find{it.readableName == '" + capacityUnitName + "'}.uuid");

        return Map.of("capacityUUID", capacityUUID, "capacityUnitUUID", capacityUnitUUID);
    }

    @Step("Войти в окно {capacityUnitId} оператором {login}")
    public void operatorLoginInWindow(String login, UUID capacityUnitUUID) {
        log.info("operator login");
        ValidatableResponse response = yardApi.operatorCommand(login, capacityUnitUUID, "login");
        statusCheck(response, "PAUSED");
    }

    @Step("Оператором {login} нажимаем \"Старт\"")
    public void operatorStart(String login, UUID capacityUnitUUID) {
        log.info("operator start");
        ValidatableResponse response = yardApi.operatorCommand(login, capacityUnitUUID, "start");
        statusCheck(response, "ON_LINE");
    }

    @Step("Зарегистрировать водитель {licencePlateNumber} на {reqType}")
    public void driverRegisterInReqType(String licencePlateNumber, UUID capacityUnitUUID, String reqType) {
        log.info("driver register");
        ValidatableResponse validatableResponse =
                yardApi.driverRegisterInReqType(capacityUnitUUID, licencePlateNumber, reqType);
        validatableResponse.body("ticketNumber", not(emptyOrNullString()));
    }

    @Step("Проверяем что в окно {capacityUnitUUID} заасайнился клиент")
    public Long verifyClientAssigned(String login, UUID capacityUnitUUID) {
        ValidatableResponse response = yardApi.waitForClient(login, capacityUnitUUID);
        response.body("assignedClientId", not(emptyOrNullString()));
        return response.extract().jsonPath().getLong("assignedClientId");
    }

    @Step("Ждем пока в окно {capacityUnitUUID} заасайнится клиент")
    public Long waitClientAssigned(String login, UUID capacityUnitUUID) {
        log.info("wait client assigned");
        Delayer.delay(1, TimeUnit.SECONDS);
        return Retrier.retry(() -> verifyClientAssigned(login, capacityUnitUUID),
                10,
                1,
                TimeUnit.SECONDS
        );
    }

    @Step("Вызвать клиента {assignedClientId} в окно {capacityUnitUUID}")
    public void callDriver(UUID serviceUUID, UUID capacityUnitUUID, Long assignedClientId, String login) {
        log.info("call driver");
        ValidatableResponse response = yardApi.windowPush(serviceUUID, capacityUnitUUID, assignedClientId, "CALL", login);
        statusCheck(response, "WAITING");
    }

    @Step("\"Начать работу\" с клиентом {assignedClientId}")
    public void startProcessingDriver(UUID serviceUUID, UUID capacityUnitUUID, Long assignedClientId, String login) {
        log.info("processing driver");
        ValidatableResponse response =
                yardApi.windowPush(serviceUUID, capacityUnitUUID, assignedClientId, "DISPATCHER_START_PROCESSING", login);
        statusCheck(response, "PROCESSING");
    }

    @Step("\"Завершить\" работу с клиентом {assignedClientId}")
    public void submitDriver(UUID serviceUUID, UUID capacityUnitUUID, Long assignedClientId, String login) {
        log.info("submit driver");

        ValidatableResponse response =
                yardApi.windowSubmit(serviceUUID, capacityUnitUUID, assignedClientId, "ACCEPT", login);
        statusCheck(response, "ON_LINE");
    }

    @Step("Проверяем, что статус == {status}")
    public void statusCheck(ValidatableResponse response, String status) {
        response.body("status", is(status));
    }

    @Step("\"Неявка\" клиента {assignedClientId}")
    public void cancelDriver(UUID serviceUUID, UUID capacityUnitUUID, Long assignedClientId, String login) {
        ValidatableResponse response = yardApi.windowPush(serviceUUID, capacityUnitUUID, assignedClientId, "CANCEL", login);
        statusCheck(response, "ON_LINE");
    }

    @Step("\"Отказ\" работу с {assignedClientId}")
    public void declineDriver(UUID serviceUUID, UUID capacityUnitUUID, Long assignedClientId, String login) {
        ValidatableResponse response =
                yardApi.windowPush(serviceUUID, capacityUnitUUID, assignedClientId, "DECLINE", login);
        statusCheck(response, "ON_LINE");
    }
}
