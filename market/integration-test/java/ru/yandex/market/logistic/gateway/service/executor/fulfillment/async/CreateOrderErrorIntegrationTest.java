package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

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
import ru.yandex.market.logistics.lom.client.async.LomFulfillmentConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateOrderErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 5927638L;

    private final static long TASK_ID = 100L;

    private final static long PARENT_TASK_ID = 50L;

    private final static long PARTNER_ID = 145L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @MockBean
    private LomFulfillmentConsumerClient lomClient;

    @Autowired
    private CreateOrderErrorExecutor createOrderErrorExecutor;

    @Test
    public void executeCreateOrderErrorExecutorSuccessfully() {
        ClientTask task = getClientTask(
                "fixtures/executors/error_task_message.json",
                TaskResultConsumer.MDB
        );
        ClientTask parentTask = getParentClientTask(
            "fixtures/executors/fulfillment_create_order/fulfillment_create_order_error_parent_task_message.json",
                TaskResultConsumer.MDB
        );

        when(repository.findTask(TASK_ID)).thenReturn(task);
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        createOrderErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(mdbClient).setCreateOrderError(TEST_ORDER_ID);
    }

    @Test
    public void executeCreateOrderErrorExecutorSuccessfullyWithErrorCode() {
        ClientTask task = getClientTask(
                "fixtures/executors/error_task_message_with_error_code.json",
                TaskResultConsumer.LOM
        );

        ClientTask parentTask = getParentClientTask(
                "fixtures/executors/fulfillment_create_order/fulfillment_create_order_error_parent_task_message.json",
                TaskResultConsumer.LOM
        );

        when(repository.findTask(TASK_ID)).thenReturn(task);
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        createOrderErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(lomClient).setCreateOrderError(
                eq(TEST_PROCESS_ID),
                eq(String.valueOf(TEST_ORDER_ID)),
                eq(PARTNER_ID),
                eq(9999),
                eq("Error message")
        );
    }

    private ClientTask getClientTask(String filename, TaskResultConsumer consumer) {
        return new ClientTask()
            .setProcessId(String.valueOf(TEST_PROCESS_ID))
            .setId(TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setParentId(PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_CREATE_ORDER_ERROR)
            .setMessage(getFileContent(filename))
            .setConsumer(consumer);
    }

    private ClientTask getParentClientTask(String filename, TaskResultConsumer consumer) {
        return new ClientTask()
            .setId(PARENT_TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_CREATE_ORDER_ERROR)
            .setMessage(getFileContent(filename))
            .setConsumer(consumer);
    }
}
