package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutInboundRegistryErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;

    private static final long PARENT_TASK_ID = 90L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private FulfillmentWorkflowAsyncClientApi fulfillmentWorkflowAsyncClientApi;

    @MockBean
    private TransportManagerClient transportManagerClient;

    @Autowired
    private PutInboundRegistryErrorExecutor putInboundRegistryErrorExecutor;

    @Test
    public void executeErrorFfWf() {
        TaskMessage message = execute(TaskResultConsumer.FF_WF_API);

        softAssert.assertThat(message.getMessageBody()).isNull();
        verify(fulfillmentWorkflowAsyncClientApi).acceptErrorPutRegistry(
                5927638L,
                "Some terrible error"
        );
    }

    @Test
    public void executeErrorTm() {
        TaskMessage message = execute(TaskResultConsumer.TM);

        softAssert.assertThat(message.getMessageBody()).isNull();
        verify(transportManagerClient).setPutInboundRegistryError(
            123456L,
            "5927638",
            "39292337",
            145L,
            "Some terrible error"
        );
    }

    private TaskMessage execute(TaskResultConsumer consumer)  {
        String messageContent = getFileContent("fixtures/executors/put_inbound_registry/task_message_ds.json");

        ClientTask task = getClientTask("fixtures/executors/put_inbound_registry/task_response_error.json", consumer);
        ClientTask parentTask = getParentClientTask(messageContent);

        when(repository.findTask(TASK_ID)).thenReturn(task);
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        return putInboundRegistryErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    private ClientTask getClientTask(String filename, TaskResultConsumer consumer) {
        return new ClientTask()
            .setId(TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setParentId(PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_PUT_INBOUND_REGISTRY_ERROR)
            .setProcessId(PROCESS_ID)
            .setConsumer(consumer)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String messageContent) {
        return new ClientTask()
            .setId(PARENT_TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_PUT_INBOUND_REGISTRY)
            .setMessage(messageContent);
    }
}
