package ru.yandex.market.mbi.bpmn.process.lock;

import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.bpmn.BpmnLockRequest;
import ru.yandex.market.mbi.api.client.entity.bpmn.BpmnLockType;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тесты для {@code lock_with_retry_process}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class LockWithRetryTest extends FunctionalTest {

    @Autowired
    private MbiApiClient mbiApiClient;

    @AfterEach
    void checkMocks() {
        Mockito.verifyNoMoreInteractions(mbiApiClient);
    }

    @Test
    @DisplayName("Не указали тип лока. Ошибка. Не должно быть запросов в апи")
    void lock_withoutType_error() throws InterruptedException {
        Set<Long> entityIds = Set.of(10L, 20L);
        Map<String, Object> params = Map.of(
                "direction", "LOCK",
                "entityIds", entityIds
        );

        Mockito.when(mbiApiClient.lockBpmn(any())).thenReturn(GenericCallResponse.ok());

        ProcessInstance instance = invoke(params);

        // Нет инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance, "call_lock_process");
    }

    @Test
    @DisplayName("Пытаемся захватить лок. Срабатывает ошибка. Делаем ретрай. Ретрай помогает")
    void lock_lessThanRetryLimit_success() throws InterruptedException {
        Set<Long> entityIds = Set.of(10L, 20L);
        Map<String, Object> params = Map.of(
                "direction", "LOCK",
                "entityIds", entityIds,
                "lockType", BpmnLockType.PARTNER_FEED_OFFER_MIGRATION.getId(),
                "retries", 2
        );

        Mockito.when(mbiApiClient.lockBpmn(any()))
                .thenReturn(GenericCallResponse.exception(new RuntimeException()))
                .thenReturn(GenericCallResponse.exception(new RuntimeException()))
                .thenReturn(GenericCallResponse.ok());

        ProcessInstance instance = invoke(params);

        // Был вызов АПИ лока с нужным процессом
        BpmnLockRequest request = getRequest(instance, entityIds);
        Mockito.verify(mbiApiClient, Mockito.times(3)).lockBpmn(request);

        // Нет инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("Пытаемся захватить лок. Срабатывает ошибка. Делаем ретрай. Не помогает. Ошибка")
    void lock_moreThanRetryLimit_error() throws InterruptedException {
        Set<Long> entityIds = Set.of(10L, 20L);
        Map<String, Object> params = Map.of(
                "direction", "LOCK",
                "entityIds", entityIds,
                "lockType", BpmnLockType.PARTNER_FEED_OFFER_MIGRATION.getId(),
                "retries", 2
        );

        Mockito.when(mbiApiClient.lockBpmn(any()))
                .thenReturn(GenericCallResponse.exception(new RuntimeException()));

        ProcessInstance instance = invoke(params);

        // Был вызов АПИ лока с нужным процессом
        BpmnLockRequest request = getRequest(instance, entityIds);
        Mockito.verify(mbiApiClient, Mockito.times(3)).lockBpmn(request);

        // Нет инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance, "call_lock_process");
    }

    @Test
    @DisplayName("Пытаемся захватить лок. Захватываем")
    void lock_okRequest_okResponse() throws InterruptedException {
        Set<Long> entityIds = Set.of(10L, 20L);
        Map<String, Object> params = Map.of(
                "direction", "LOCK",
                "entityIds", entityIds,
                "lockType", BpmnLockType.PARTNER_FEED_OFFER_MIGRATION.getId()
        );

        Mockito.when(mbiApiClient.lockBpmn(any())).thenReturn(GenericCallResponse.ok());

        ProcessInstance instance = invoke(params);

        // Был вызов АПИ лока с нужным процессом
        BpmnLockRequest request = getRequest(instance, entityIds);
        Mockito.verify(mbiApiClient, Mockito.times(1)).lockBpmn(request);

        // Нет инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("Пытаемся освободить лок. Освобождаем")
    void unlock_okRequest_okResponse() throws InterruptedException {
        Set<Long> entityIds = Set.of(10L, 20L);
        Map<String, Object> params = Map.of(
                "direction", "UNLOCK",
                "entityIds", entityIds,
                "lockType", BpmnLockType.PARTNER_FEED_OFFER_MIGRATION.getId()
        );

        Mockito.when(mbiApiClient.unlockBpmn(any())).thenReturn(GenericCallResponse.ok());

        ProcessInstance instance = invoke(params);

        // Был вызов АПИ лока с нужным процессом
        BpmnLockRequest request = getRequest(instance, entityIds);
        Mockito.verify(mbiApiClient, Mockito.times(1)).unlockBpmn(request);

        // Нет инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    private ProcessInstance invoke(Map<String, Object> variables) throws InterruptedException {
        return CamundaTestUtil.invoke(processEngine, "lock_with_retry_process", variables);
    }

    private BpmnLockRequest getRequest(ProcessInstance instance, Set<Long> entityIds) {
        BpmnLockRequest request = new BpmnLockRequest();
        request.setLockType(BpmnLockType.PARTNER_FEED_OFFER_MIGRATION);
        request.setEntityIds(entityIds);
        request.setProcessId(instance.getProcessInstanceId());
        return request;
    }
}
