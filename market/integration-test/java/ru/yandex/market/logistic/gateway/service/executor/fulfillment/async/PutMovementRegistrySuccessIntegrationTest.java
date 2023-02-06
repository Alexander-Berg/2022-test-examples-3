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
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutMovementRegistrySuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Autowired
    private PutMovementRegistrySuccessExecutor putMovementRegistrySuccessExecutor;

    @Test
    public void executeSuccess() {
        ClientTask task = getClientTask(
            "fixtures/executors/put_movement_registry/task_response_success.json"
        );
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        TaskMessage message = putMovementRegistrySuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowConsumerClient).setPutMovementRegistrySuccess(
            ResourceId.builder().setYandexId("5927638").setPartnerId("39292337").build(),
            145L,
            PROCESS_ID
        );
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_MOVEMENT_REGISTRY_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(PROCESS_ID);
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
