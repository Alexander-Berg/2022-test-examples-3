package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

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

public class CancelOrderErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long CANCEL_ORDER_ERROR_TASK_ID = 840L;

    private final static long CANCEL_ORDER_TASK_ID = 400L;

    private final static long YANDEX_ID = 100L;

    @Autowired
    private MdbClient mdbClient;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CancelOrderErrorExecutor cancelOrderErrorExecutor;

    @Test
    public void executeUsualWorkWithError() throws Throwable {
        ClientTask task = getCancelOrderErrorTask("fixtures/executors/error_task_message.json");

        ClientTask parentTask = getCancelOrderTask("fixtures/executors/cancel_order/cancel_order_task_message.json");

        when(repository.findTask(eq(CANCEL_ORDER_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(CANCEL_ORDER_TASK_ID))).thenReturn(parentTask);

       cancelOrderErrorExecutor.execute(new ExecutorTaskWrapper(CANCEL_ORDER_ERROR_TASK_ID, 0));

        verify(mdbClient).setFulfillmentOrderCancelResult(YANDEX_ID, CancelStatus.FAIL);
    }

    private ClientTask getCancelOrderErrorTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(CANCEL_ORDER_ERROR_TASK_ID);
        task.setRootId(CANCEL_ORDER_TASK_ID);
        task.setParentId(CANCEL_ORDER_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CANCEL_ORDER_ERROR);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }

    private ClientTask getCancelOrderTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(CANCEL_ORDER_TASK_ID);
        task.setRootId(CANCEL_ORDER_TASK_ID);
        task.setStatus(TaskStatus.READY);
        task.setFlow(RequestFlow.FF_CANCEL_ORDER);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }
}
