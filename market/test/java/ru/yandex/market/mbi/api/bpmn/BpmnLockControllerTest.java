package ru.yandex.market.mbi.api.bpmn;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponseStatus;
import ru.yandex.market.mbi.api.client.entity.bpmn.BpmnLockRequest;
import ru.yandex.market.mbi.api.client.entity.bpmn.BpmnLockType;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.api.controller.operation.BpmnLockController;

/**
 * Тесты для {@link BpmnLockController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class BpmnLockControllerTest extends FunctionalTest {

    private static final String PROCESS_ID = "process_1";

    @Test
    @DisplayName("У сущности нет лока. Передали 1 entity_id. Успешно захватили лок")
    @DbUnitDataSet(
            before = "BpmnLockControllerTest/csv/base.before.csv",
            after = "BpmnLockControllerTest/csv/successfulLock.after.csv"
    )
    void withoutLock_sendLockForOneEntity_successfulLock() {
        BpmnLockRequest request = getFeedRequest(1001L);
        GenericCallResponse response = mbiApiClient.lockBpmn(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @Test
    @DisplayName("У сущности нет лока. Передали несколько entity_id. Успешно захватили лок")
    @DbUnitDataSet(
            before = "BpmnLockControllerTest/csv/base.before.csv",
            after = "BpmnLockControllerTest/csv/successfulTwoLocks.after.csv"
    )
    void withoutLock_sendLockForTwoEntities_successfulLock() {
        BpmnLockRequest request = getFeedRequest(1001L, 1002L);
        GenericCallResponse response = mbiApiClient.lockBpmn(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @Test
    @DisplayName("У сущности есть лок. Лок от того же процесса. Успешно повторно захватили лок")
    @DbUnitDataSet(
            before = {
                    "BpmnLockControllerTest/csv/base.before.csv",
                    "BpmnLockControllerTest/csv/withLock.before.csv"
            },
            after = "BpmnLockControllerTest/csv/withLock.before.csv"
    )
    void withLock_sendSameLock_successfulLock() {
        BpmnLockRequest request = getFeedRequest(1001L);
        GenericCallResponse response = mbiApiClient.lockBpmn(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("controllerMethods")
    @DisplayName("Не передаем тип лока. Ошибка")
    @DbUnitDataSet(
            before = "BpmnLockControllerTest/csv/base.before.csv"
    )
    void invalidRequest_withoutLockType_error(String name,
                                              BiFunction<MbiApiClient, BpmnLockRequest, GenericCallResponse> operation) {
        BpmnLockRequest request = getFeedRequest(1001L);
        request.setLockType(null);
        GenericCallResponse response = operation.apply(mbiApiClient, request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("lockType is required", response.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("controllerMethods")
    @DisplayName("Не передаем id процесса. Ошибка")
    @DbUnitDataSet(
            before = "BpmnLockControllerTest/csv/base.before.csv"
    )
    void invalidRequest_withoutProcessId_error(String name,
                                               BiFunction<MbiApiClient, BpmnLockRequest, GenericCallResponse> operation) {
        BpmnLockRequest request = getFeedRequest(1001L);
        request.setProcessId(null);
        GenericCallResponse response = operation.apply(mbiApiClient, request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("processId is required", response.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("controllerMethods")
    @DisplayName("Не передаем id сущностей. Ошибка")
    @DbUnitDataSet(
            before = "BpmnLockControllerTest/csv/base.before.csv"
    )
    void invalidRequest_withoutEntityIds_error(String name,
                                               BiFunction<MbiApiClient, BpmnLockRequest, GenericCallResponse> operation) {
        BpmnLockRequest request = getFeedRequest(1001L);
        request.setEntityIds(null);
        GenericCallResponse response = operation.apply(mbiApiClient, request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("entityIds is required", response.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("controllerMethods")
    @DisplayName("Передаем пустой список id сущностей. Ошибка")
    @DbUnitDataSet(
            before = "BpmnLockControllerTest/csv/base.before.csv"
    )
    void invalidRequest_withEmptyEntityIds_error(String name,
                                                 BiFunction<MbiApiClient, BpmnLockRequest, GenericCallResponse> operation) {
        BpmnLockRequest request = getFeedRequest(1001L);
        request.setEntityIds(Set.of());
        GenericCallResponse response = operation.apply(mbiApiClient, request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("entityIds is required", response.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("controllerMethods")
    @DisplayName("У одного из локов процесс отличается от того, что пришел в запросе. Ошибка")
    @DbUnitDataSet(
            before = {
                    "BpmnLockControllerTest/csv/base.before.csv",
                    "BpmnLockControllerTest/csv/differentProcesses.before.csv"
            }
    )
    void withLock_differentProcesses_error(String name,
                                           BiFunction<MbiApiClient, BpmnLockRequest, GenericCallResponse> operation) {
        BpmnLockRequest request = getFeedRequest(1001L, 1002L);
        GenericCallResponse response = operation.apply(mbiApiClient, request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("Lock was acquired by another process_id", response.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("controllerMethods")
    @DisplayName("В запросе 2 сущности. Лок только на одной из них. Ошибка")
    @DbUnitDataSet(
            before = {
                    "BpmnLockControllerTest/csv/base.before.csv",
                    "BpmnLockControllerTest/csv/withLock.before.csv"
            }
    )
    void lockForTwoEntities_onlyOneWithLock_error(String name,
                                                  BiFunction<MbiApiClient, BpmnLockRequest, GenericCallResponse> operation) {
        BpmnLockRequest request = getFeedRequest(1001L, 1002L);
        GenericCallResponse response = operation.apply(mbiApiClient, request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("Lock was acquired by another process_id", response.getMessage());
    }

    @Test
    @DisplayName("У сущности нет лока. Пытаемся разлочить. Успешно")
    @DbUnitDataSet(
            before = "BpmnLockControllerTest/csv/base.before.csv",
            after = "BpmnLockControllerTest/csv/emptyLocks.after.csv"
    )
    void withoutLock_sendUnlock_successful() {
        BpmnLockRequest request = getFeedRequest(1001L);
        GenericCallResponse response = mbiApiClient.unlockBpmn(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @Test
    @DisplayName("У сущности есть лок. Пытаемся разлочить. Успешно")
    @DbUnitDataSet(
            before = {
                    "BpmnLockControllerTest/csv/base.before.csv",
                    "BpmnLockControllerTest/csv/withTwoLock.before.csv"
            },
            after = "BpmnLockControllerTest/csv/successfulUnlock.after.csv"
    )
    void withLock_sendUnlock_successful() {
        BpmnLockRequest request = getFeedRequest(1001L);
        GenericCallResponse response = mbiApiClient.unlockBpmn(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @Test
    @DisplayName("2 сущности с одним локом. Пытаемся разлочить. Успешно")
    @DbUnitDataSet(
            before = {
                    "BpmnLockControllerTest/csv/base.before.csv",
                    "BpmnLockControllerTest/csv/withTwoLock.before.csv"
            },
            after = "BpmnLockControllerTest/csv/emptyLocks.after.csv"
    )
    void withTwoLock_sendUnlock_successful() {
        BpmnLockRequest request = getFeedRequest(1001L, 1002L);
        GenericCallResponse response = mbiApiClient.unlockBpmn(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    private static Stream<Arguments> controllerMethods() {
        return Stream.of(
                Arguments.of(
                        "lockBpmn",
                        (BiFunction<MbiApiClient, BpmnLockRequest, GenericCallResponse>) MbiApiClient::lockBpmn
                ),
                Arguments.of(
                        "unlockBpmn",
                        (BiFunction<MbiApiClient, BpmnLockRequest, GenericCallResponse>) MbiApiClient::unlockBpmn
                )
        );
    }

    private BpmnLockRequest getFeedRequest(Long... entityIds) {
        return getRequest(BpmnLockType.PARTNER_FEED_OFFER_MIGRATION, entityIds);
    }

    private BpmnLockRequest getRequest(BpmnLockType type, Long... entityIds) {
        BpmnLockRequest request = new BpmnLockRequest();
        request.setLockType(type);
        request.setProcessId(PROCESS_ID);
        request.setEntityIds(Set.of(entityIds));
        return request;
    }
}
