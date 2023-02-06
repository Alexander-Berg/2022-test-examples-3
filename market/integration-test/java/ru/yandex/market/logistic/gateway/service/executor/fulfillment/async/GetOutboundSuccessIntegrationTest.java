package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Outbound;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetOutboundSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_OUTBOUND_TASK_ID = 100L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private FulfillmentWorkflowAsyncClientApi fulfillmentWorkflowAsyncClientApi;

    @Autowired
    private GetOutboundSuccessExecutor getOutboundSuccessExecutor;

    @Test
    public void executeSuccess() {
        ClientTask task = getClientTask("fixtures/executors/get_outbound/task_response.json");
        when(repository.findTask(eq(TEST_OUTBOUND_TASK_ID))).thenReturn(task);

        TaskMessage message = getOutboundSuccessExecutor.execute(new ExecutorTaskWrapper(TEST_OUTBOUND_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowAsyncClientApi).setFfGetOutboundSuccess(
            any(Outbound.class),
            anyList(),
            eq(145L),
            eq(PROCESS_ID)
        );
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_OUTBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_GET_OUTBOUND_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(PROCESS_ID);
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
