package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

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
import ru.yandex.market.logistic.gateway.service.consumer.TMDeliveryConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CancelMovementSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_MOVEMENT_TASK_ID = 100L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private TMDeliveryConsumerClient tmDeliveryConsumerClient;

    @Autowired
    private CancelMovementSuccessExecutor cancelMovementSuccessExecutor;

    @Test
    public void executeSuccessTm() {
        ClientTask task = getClientTask();
        when(repository.findTask(eq(TEST_MOVEMENT_TASK_ID))).thenReturn(task);

        TaskMessage message = cancelMovementSuccessExecutor.execute(
            new ExecutorTaskWrapper(TEST_MOVEMENT_TASK_ID, 0)
        );

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(tmDeliveryConsumerClient).setCancelMovementSuccess(
            eq("5927638"),
            eq("39292337"),
            eq(145L),
            eq(PROCESS_ID)
        );
    }

    private ClientTask getClientTask() {
        return getClientTask("fixtures/executors/cancel_movement/task_message.json");
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_MOVEMENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CANCEL_MOVEMENT_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(PROCESS_ID);
        task.setConsumer(TaskResultConsumer.TM);
        return task;
    }

}
