package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.TMFulfillmentConsumerClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetMovementErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long PUT_MOVEMENT_TASK_ID = 100L;

    private static final long PUT_MOVEMENT_PARENT_TASK_ID = 90L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private TMFulfillmentConsumerClient tmFulfillmentConsumerClient;

    @Autowired
    private GetMovementErrorExecutor getMovementErrorExecutor;

    @Test
    public void executeError() {
        ClientTask task = getClientTask("fixtures/executors/get_movement/get_movement_error_task_message.json");
        ClientTask parentTask = getParentClientTask("fixtures/executors/get_movement/get_movement_task_message.json");

        when(repository.findTask(PUT_MOVEMENT_TASK_ID)).thenReturn(task);
        when(repository.findTask(PUT_MOVEMENT_PARENT_TASK_ID)).thenReturn(parentTask);

        TaskMessage message = getMovementErrorExecutor.execute(new ExecutorTaskWrapper(PUT_MOVEMENT_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(tmFulfillmentConsumerClient).setGetMovementError(
            ResourceId.builder().setYandexId("5927638").setPartnerId("39292337").build(),
            145L,
            PROCESS_ID,
            "Some terrible error"
        );
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(PUT_MOVEMENT_TASK_ID)
            .setRootId(PUT_MOVEMENT_PARENT_TASK_ID)
            .setParentId(PUT_MOVEMENT_PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.DS_PUT_MOVEMENT_ERROR)
            .setProcessId(PROCESS_ID)
            .setConsumer(TaskResultConsumer.TM)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String filename) {
        return new ClientTask()
            .setId(PUT_MOVEMENT_PARENT_TASK_ID)
            .setRootId(PUT_MOVEMENT_PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.DS_PUT_MOVEMENT)
            .setMessage(getFileContent(filename));
    }

}
