package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.TMFulfillmentConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutMovementErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long PUT_MOVEMENT_TASK_ID = 100L;

    private static final long PUT_MOVEMENT_PARENT_TASK_ID = 90L;

    private static final String PROCESS_ID = "123456";

    private static final long LONG_PROCESS_ID = 123456L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private TransportManagerClient transportManagerClient;

    @SpyBean
    private TMFulfillmentConsumerClient tmFulfillmentConsumerClient;

    @Autowired
    private PutMovementErrorExecutor putMovementErrorExecutor;

    @Test
    public void executeError() {
        ClientTask task = getClientTask("fixtures/executors/put_movement/put_movement_error_task_message.json");
        ClientTask parentTask = getParentClientTask("fixtures/executors/put_movement/put_movement_task_message.json");

        when(repository.findTask(PUT_MOVEMENT_TASK_ID)).thenReturn(task);
        when(repository.findTask(PUT_MOVEMENT_PARENT_TASK_ID)).thenReturn(parentTask);

        TaskMessage message = putMovementErrorExecutor.execute(new ExecutorTaskWrapper(PUT_MOVEMENT_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(tmFulfillmentConsumerClient).setPutMovementError(
            eq("5927638"),
            eq("39292337"),
            eq(145L),
            eq(PROCESS_ID),
            eq("Some terrible error")
        );
        verify(transportManagerClient).setPutMovementError(
            eq(LONG_PROCESS_ID),
            eq("5927638"),
            eq("39292337"),
            eq(145L),
            eq("Some terrible error")
        );
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(PUT_MOVEMENT_TASK_ID)
            .setRootId(PUT_MOVEMENT_PARENT_TASK_ID)
            .setParentId(PUT_MOVEMENT_PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_PUT_MOVEMENT_ERROR)
            .setProcessId(PROCESS_ID)
            .setConsumer(TaskResultConsumer.TM)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String filename) {
        return new ClientTask()
            .setId(PUT_MOVEMENT_PARENT_TASK_ID)
            .setRootId(PUT_MOVEMENT_PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_PUT_MOVEMENT)
            .setMessage(getFileContent(filename));
    }

}
