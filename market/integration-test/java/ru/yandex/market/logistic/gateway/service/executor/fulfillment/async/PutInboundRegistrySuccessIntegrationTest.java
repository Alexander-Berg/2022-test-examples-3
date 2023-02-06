package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutInboundRegistrySuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private FulfillmentWorkflowAsyncClientApi fulfillmentWorkflowAsyncClientApi;

    @MockBean
    private TransportManagerClient transportManagerClient;

    @Autowired
    private PutInboundRegistrySuccessExecutor putInboundRegistrySuccessExecutor;

    @Test
    public void executeSuccessFfWf() {
        TaskMessage message = execute(TaskResultConsumer.FF_WF_API);

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowAsyncClientApi).acceptSuccessfulPutRegistry(
            ResourceId.builder().setYandexId("5927638").setPartnerId("39292337").build()
        );
    }

    @Test
    public void executeSuccessTm() {
        TaskMessage message = execute(TaskResultConsumer.TM);

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(transportManagerClient).setPutInboundRegistrySuccess(
            123456L,
            "5927638",
            "39292337",
            145L
        );
    }

    private TaskMessage execute(TaskResultConsumer consumer) {
        ClientTask task = getClientTask(
            "fixtures/executors/put_inbound_registry/task_response_success.json", consumer
        );
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        return putInboundRegistrySuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    private ClientTask getClientTask(String filename, TaskResultConsumer consumer) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_INBOUND_REGISTRY_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(PROCESS_ID);
        task.setConsumer(consumer);
        return task;
    }
}
