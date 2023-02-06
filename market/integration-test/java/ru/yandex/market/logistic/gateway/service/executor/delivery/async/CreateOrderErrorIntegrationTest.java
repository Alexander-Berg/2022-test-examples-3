package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateOrderErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    private final static long TEST_PARCEL_ID = 200L;

    private final static long TASK_ID = 100L;

    private final static long PARENT_TASK_ID = 50L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @Autowired
    private CreateOrderErrorExecutor createOrderErrorExecutor;

    @Test
    public void executeError() {
        ClientTask task = getClientTask("fixtures/executors/error_task_message.json");
        ClientTask parentTask = getParentClientTask("fixtures/executors/create_order/create_order_error_parent_task_message.json");

        when(repository.findTask(TASK_ID)).thenReturn(task);
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        createOrderErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(mdbClient).setCreateOrderError(TEST_ORDER_ID, TEST_PARCEL_ID);
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setParentId(PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.DS_CREATE_ORDER_ERROR)
            .setConsumer(TaskResultConsumer.MDB)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String filename) {
        return new ClientTask()
            .setId(PARENT_TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.DS_CREATE_ORDER)
            .setConsumer(TaskResultConsumer.MDB)
            .setMessage(getFileContent(filename));
    }
}
