package ru.yandex.market.mbi.bpmn.util;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.mockito.Mockito;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.operation.ExternalOperationResult;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatistics;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatus;

import static org.mockito.ArgumentMatchers.any;

/**
 * Утилитарный класс для тестов с mbi-api.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
public class MbiApiTestUtil {

    private MbiApiTestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Замокать успешное сохранение статуса через mbi-api.
     */
    public static void mockMbiNotifierResponse(MbiApiClient mbiApiClient) {
        Mockito.when(mbiApiClient.updateOperationStatus(any()))
                .thenReturn(GenericCallResponse.ok());
    }

    /**
     * Проверить сохранение статуса операции.
     */
    public static void checkOperationStatus(MbiApiClient mbiApiClient,
                                            ProcessInstance instance,
                                            OperationStatus expectedStatus,
                                            Map<String, Object> stats) {
        checkOperationStatus(mbiApiClient, instance.getProcessInstanceId(), expectedStatus, stats);
    }

    /**
     * Проверить сохранение статуса операции.
     */
    public static void checkOperationStatus(MbiApiClient mbiApiClient,
                                            String processInstanceId,
                                            OperationStatus expectedStatus,
                                            Map<String, Object> stats) {
        ExternalOperationResult requestResult = new ExternalOperationResult();
        requestResult.setExternalId(processInstanceId);
        requestResult.setStatus(expectedStatus);
        requestResult.setStatistics(new OperationStatistics(stats));

        Mockito.verify(mbiApiClient).updateOperationStatus(requestResult);
    }
}
