package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.PutMovementRegistryRequest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowConsumerClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutMovementRegistryErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;

    private static final long PARENT_TASK_ID = 90L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Autowired
    private PutMovementRegistryErrorExecutor putMovementRegistryErrorExecutor;

    @Test
    public void executeError() throws IOException {
        String messageContent = getFileContent("fixtures/executors/put_movement_registry/task_message_ff.json");
        PutMovementRegistryRequest expectedRequest =
            new ObjectMapper().readValue(messageContent, PutMovementRegistryRequest.class);

        ClientTask task = getClientTask("fixtures/executors/put_movement_registry/task_response_error.json");
        ClientTask parentTask = getParentClientTask(messageContent);

        when(repository.findTask(TASK_ID)).thenReturn(task);
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        TaskMessage message = putMovementRegistryErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowConsumerClient).setPutMovementRegistryError(
            expectedRequest.getRegistry().getRegistryId(),
            145L,
            PROCESS_ID,
            "Some terrible error"
        );
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setParentId(PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_PUT_MOVEMENT_REGISTRY)
            .setProcessId(PROCESS_ID)
            .setConsumer(TaskResultConsumer.FF_WF_API)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String messageContent) {
        return new ClientTask()
            .setId(PARENT_TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_PUT_MOVEMENT_REGISTRY)
            .setMessage(messageContent);
    }
}
