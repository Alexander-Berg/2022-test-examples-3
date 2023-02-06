package ru.yandex.market.mbi.bpmn.datacamp;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.operation.ExternalOperationResult;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatus;
import ru.yandex.market.mbi.bpmn.function.StatisticsCollector;
import ru.yandex.market.mbi.bpmn.task.mbi.OperationNotifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тесты для {@link OperationNotifier}.
 */
class DataCampMigrationNotifierTest {

    private MbiApiClient mbiApiClient;
    private StatisticsCollector statisticsCollector;
    private OperationNotifier dataCampMigrationNotifier;
    private DelegateExecution delegateExecution;
    private ArgumentCaptor<ExternalOperationResult> argumentCaptor;

    @BeforeEach
    void init() {
        mbiApiClient = Mockito.mock(MbiApiClient.class);
        statisticsCollector = Mockito.mock(StatisticsCollector.class);
        dataCampMigrationNotifier = new OperationNotifier(mbiApiClient, statisticsCollector);
        delegateExecution = Mockito.mock(DelegateExecution.class);
        argumentCaptor = ArgumentCaptor.forClass(ExternalOperationResult.class);

        doReturn(true)
                .when(delegateExecution).hasVariable("operationId");
    }


    @Test
    @DisplayName("migration not finished successfully - update succeed")
    void testFailed() throws Exception {
        doReturn(true)
                .when(delegateExecution).getVariable("failed");
        doReturn(GenericCallResponse.ok())
                .when(mbiApiClient).updateOperationStatus(any());
        dataCampMigrationNotifier.execute(delegateExecution);
        verify(mbiApiClient).updateOperationStatus(argumentCaptor.capture());
        assertEquals(OperationStatus.ERROR, argumentCaptor.getValue().getStatus());
        verify(delegateExecution, times(0)).createIncident(
                eq("Mbi operation status update failed: "),
                eq(""),
                anyString());
    }

    @Test
    @DisplayName("migration succeed - update succeed")
    void testSuccessWithApiFail() throws Exception {
        doReturn(false)
                .when(delegateExecution).getVariable("failed");
        doReturn(GenericCallResponse.ok())
                .when(mbiApiClient).updateOperationStatus(any());
        dataCampMigrationNotifier.execute(delegateExecution);
        verify(mbiApiClient).updateOperationStatus(argumentCaptor.capture());
        assertEquals(OperationStatus.OK, argumentCaptor.getValue().getStatus());
        verify(delegateExecution, times(0)).createIncident(
                eq("Mbi operation status update failed: "),
                eq(""),
                anyString());
    }

    @Test
    @DisplayName("migration succeed - update failed")
    public void notifyExecutionTest() throws Exception {
        doReturn(false)
                .when(delegateExecution).getVariable("failed");
        doReturn(GenericCallResponse.exception(new IllegalStateException()))
                .when(mbiApiClient).updateOperationStatus(any());
        dataCampMigrationNotifier.execute(delegateExecution);
        verify(mbiApiClient).updateOperationStatus(argumentCaptor.capture());
        assertEquals(OperationStatus.OK, argumentCaptor.getValue().getStatus());
        verify(delegateExecution).createIncident(
                eq("Mbi operation status update failed: "),
                eq(""),
                anyString());
    }
}
