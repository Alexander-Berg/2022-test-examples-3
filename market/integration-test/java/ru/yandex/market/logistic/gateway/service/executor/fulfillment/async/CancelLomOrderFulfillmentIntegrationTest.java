package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistics.lom.client.async.LomFulfillmentConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ответ на отмену заказа в DS в LOM.
 */
public class CancelLomOrderFulfillmentIntegrationTest extends AbstractIntegrationTest {
    private static final long CANCEL_ORDER_TASK_ID = 100L;
    private static final String ORDER_ID = "100";
    private static final long PARTNER_ID = 145L;

    @MockBean
    private ClientTaskRepository repository;
    @MockBean
    private LomFulfillmentConsumerClient lomClient;

    @Autowired
    private CancelOrderSuccessExecutor cancelOrderSuccessExecutor;
    @Autowired
    private CancelOrderErrorExecutor cancelOrderErrorExecutor;

    @Test
    public void executeSuccess() throws Exception {
        mockTaskMessage("fixtures/executors/cancel_order/cancel_order_task_response.json");

        cancelOrderSuccessExecutor.execute(new ExecutorTaskWrapper(CANCEL_ORDER_TASK_ID, 0));

        verify(lomClient).setCancelOrderSuccess(eq(TEST_PROCESS_ID), eq(ORDER_ID), eq(PARTNER_ID));
    }

    @Test
    public void executeError() {
        mockTaskMessage("fixtures/executors/cancel_order/cancel_order_task_message.json");

        cancelOrderErrorExecutor.execute(new ExecutorTaskWrapper(CANCEL_ORDER_TASK_ID, 0));

        verify(lomClient).setCancelOrderError(
            eq(TEST_PROCESS_ID),
            eq(ORDER_ID),
            eq(PARTNER_ID),
            eq(false),
            eq("Error 100"),
            eq(9404)
        );
    }

    private void mockTaskMessage(String messageUrl) {
        ClientTask task = getCancelOrderTask(messageUrl);
        when(repository.findTask(eq(CANCEL_ORDER_TASK_ID))).thenReturn(task);
    }

    @Nonnull
    private ClientTask getCancelOrderTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CANCEL_ORDER_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CANCEL_ORDER_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.LOM);
        task.setParentId(CANCEL_ORDER_TASK_ID);
        task.setRootId(CANCEL_ORDER_TASK_ID);
        task.setProcessId(TEST_PROCESS_ID_STRING);
        return task;
    }
}
