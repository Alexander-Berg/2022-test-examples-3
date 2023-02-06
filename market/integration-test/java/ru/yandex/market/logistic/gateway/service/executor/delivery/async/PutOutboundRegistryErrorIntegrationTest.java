package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.PutOutboundRegistryRequest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutOutboundRegistryErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;

    private static final long PARENT_TASK_ID = 90L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private FulfillmentWorkflowAsyncClientApi fulfillmentWorkflowAsyncClientApi;

    @Autowired
    private PutOutboundRegistryErrorExecutor putOutboundRegistryErrorExecutor;

    @Test
    public void executeError() throws IOException {
        String messageContent = getFileContent("fixtures/executors/put_outbound_registry/task_message_ds.json");
        PutOutboundRegistryRequest expectedRequest =
            new ObjectMapper().readValue(messageContent, PutOutboundRegistryRequest.class);

        ClientTask task = getClientTask("fixtures/executors/put_outbound_registry/task_response_error.json");
        ClientTask parentTask = getParentClientTask(messageContent);

        when(repository.findTask(TASK_ID)).thenReturn(task);
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        TaskMessage message = putOutboundRegistryErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowAsyncClientApi).acceptErrorPutRegistry(
                Long.valueOf(expectedRequest.getRegistry().getRegistryId().getYandexId()),
                "Some terrible error"
        );
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setParentId(PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.DS_PUT_OUTBOUND_REGISTRY_ERROR)
            .setProcessId(PROCESS_ID)
            .setConsumer(TaskResultConsumer.FF_WF_API)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String messageContent) {
        return new ClientTask()
            .setId(PARENT_TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.DS_PUT_OUTBOUND_REGISTRY)
            .setMessage(messageContent);
    }
}
