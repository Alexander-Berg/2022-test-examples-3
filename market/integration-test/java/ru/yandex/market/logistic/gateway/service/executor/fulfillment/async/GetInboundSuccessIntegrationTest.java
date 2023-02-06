package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Inbound;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.restricted.GetInboundRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetInboundSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_INBOUND_TASK_ID = 100L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private FulfillmentWorkflowAsyncClientApi asyncClientApi;

    @Autowired
    private GetInboundSuccessExecutor getInboundSuccessExecutor;

    @Test
    public void executeSuccess() {
        ClientTask task = getClientTask("fixtures/executors/get_inbound/task_response.json");
        when(repository.findTask(eq(TEST_INBOUND_TASK_ID))).thenReturn(task);

        TaskMessage message = getInboundSuccessExecutor.execute(new ExecutorTaskWrapper(TEST_INBOUND_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(asyncClientApi).setFfGetInboundSuccess(
            any(Inbound.class),
            anyList(),
            isNull(),
            eq(145L),
            eq(PROCESS_ID)
        );
    }

    @Test
    public void executeSuccessWithRestrictedData() {
        ClientTask task = getClientTask("fixtures/executors/get_inbound/task_response_with_restricted_data.json");
        when(repository.findTask(eq(TEST_INBOUND_TASK_ID))).thenReturn(task);

        TaskMessage message = getInboundSuccessExecutor.execute(new ExecutorTaskWrapper(TEST_INBOUND_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(asyncClientApi).setFfGetInboundSuccess(
            any(Inbound.class),
            anyList(),
            any(GetInboundRestrictedData.class),
            eq(145L),
            eq(PROCESS_ID)
        );
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_INBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_GET_INBOUND_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(PROCESS_ID);
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
