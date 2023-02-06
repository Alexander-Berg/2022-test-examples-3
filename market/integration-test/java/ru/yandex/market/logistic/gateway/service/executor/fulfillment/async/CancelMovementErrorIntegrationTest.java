package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

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

public class CancelMovementErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;

    private static final long PARENT_TASK_ID = 90L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private TMFulfillmentConsumerClient tmFulfillmentConsumerClient;

    @Autowired
    private CancelMovementErrorExecutor cancelMovementErrorExecutor;

    @Test
    public void executeError() {
        ClientTask task = getClientTask("fixtures/executors/cancel_movement/error_task_message.json");
        ClientTask parentTask =
            getParentClientTask("fixtures/executors/cancel_movement/task_message.json");

        when(repository.findTask(TASK_ID)).thenReturn(task);
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        TaskMessage message = cancelMovementErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(tmFulfillmentConsumerClient).setCancelMovementError(
            eq("5927638"),
            eq("39292337"),
            eq(145L),
            eq(PROCESS_ID),
            eq("Some terrible error")
        );
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setParentId(PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_CANCEL_MOVEMENT_ERROR)
            .setProcessId(PROCESS_ID)
            .setConsumer(TaskResultConsumer.TM)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String filename) {
        return new ClientTask()
            .setId(PARENT_TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_CANCEL_MOVEMENT)
            .setMessage(getFileContent(filename));
    }

}
