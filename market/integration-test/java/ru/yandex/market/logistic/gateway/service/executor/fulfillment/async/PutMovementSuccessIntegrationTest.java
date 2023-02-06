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

public class PutMovementSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_MOVEMENT_TASK_ID = 100L;

    private static final String PROCESS_ID = "123456";

    private static final long LONG_PROCESS_ID = 123456L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private TransportManagerClient transportManagerClient;

    @SpyBean
    private TMFulfillmentConsumerClient tmFulfillmentConsumerClient;

    @Autowired
    private PutMovementSuccessExecutor putMovementSuccessExecutor;

    @Test
    public void executeSuccess() {
        ClientTask task = getClientTask();
        when(repository.findTask(eq(TEST_MOVEMENT_TASK_ID))).thenReturn(task);

        TaskMessage message = putMovementSuccessExecutor.execute(new ExecutorTaskWrapper(TEST_MOVEMENT_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(tmFulfillmentConsumerClient).setPutMovementSuccess(
            eq("5927638"),
            eq("39292337"),
            eq(145L),
            eq(PROCESS_ID)
        );
        verify(transportManagerClient).setPutMovementSuccess(
            eq(LONG_PROCESS_ID),
            eq("5927638"),
            eq("39292337"),
            eq(145L)
        );
    }

    private ClientTask getClientTask() {
        return getClientTask("fixtures/executors/put_movement/put_movement_success_task_message.json");
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_MOVEMENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_MOVEMENT_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(PROCESS_ID);
        task.setConsumer(TaskResultConsumer.TM);
        return task;
    }

}
