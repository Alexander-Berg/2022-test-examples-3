package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Before;
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
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowConsumerClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateSelfExportSuccessIntegrationTest extends AbstractIntegrationTest {
    private static final long INTAKE_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateSelfExportSuccessExecutor createSelfExportSuccessExecutor;

    @SpyBean
    protected FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Before
    public void setUp() {
        doNothing().when(fulfillmentWorkflowConsumerClient)
            .setCreateSelfExportSuccess(anyString(), anyString(), anyLong(), any());
    }

    @Test
    public void successExecutorBaseWorkflow() {
        ClientTask task = getCreateSelfExportTask("fixtures/executors/fulfillment/create_self_export/task_response.json");

        when(repository.findTask(eq(INTAKE_ID))).thenReturn(task);

        TaskMessage message = createSelfExportSuccessExecutor.execute(new ExecutorTaskWrapper(INTAKE_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowConsumerClient).setCreateSelfExportSuccess(
            "self-export-yandex-id-1",
            "self-export-partner-id-1",
            145L,
            null
        );
    }

    private ClientTask getCreateSelfExportTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(INTAKE_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_SELF_EXPORT_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
