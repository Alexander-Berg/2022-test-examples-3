package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.mdbclient.model.request.CancelStatus;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CancelOrderSuccessIntegrationTest extends AbstractIntegrationTest {

    private final static long CANCEL_ORDER_SUCCESS_TASK_ID = 840L;

    private final static long CANCEL_ORDER_TASK_ID = 400L;

    private final static long YANDEX_ID = 100L;

    @Autowired
    private MdbClient mdbClient;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CancelOrderSuccessExecutor cancelOrderSuccessExecutor;

    @Test
    public void executeSuccess() throws Throwable {
        ClientTask task = getCancelOrderSuccessTask("fixtures/executors/cancel_order/cancel_order_task_response.json");

        when(repository.findTask(eq(CANCEL_ORDER_SUCCESS_TASK_ID))).thenReturn(task);

        cancelOrderSuccessExecutor.execute(new ExecutorTaskWrapper(CANCEL_ORDER_SUCCESS_TASK_ID, 0));

        verify(mdbClient).setFulfillmentOrderCancelResult(YANDEX_ID, CancelStatus.SUCCESS);
    }

    private ClientTask getCancelOrderSuccessTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CANCEL_ORDER_SUCCESS_TASK_ID);
        task.setParentId(CANCEL_ORDER_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CANCEL_ORDER_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }
}
