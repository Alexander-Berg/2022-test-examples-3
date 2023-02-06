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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetOrdersDeliveryDateErrorIntegrationTest extends AbstractIntegrationTest {

    private static final Long TASK_ID = 2L;
    private static final Long PARENT_TASK_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @Autowired
    private GetOrdersDeliveryDateErrorExecutor getOrdersDeliveryDateErrorExecutor;

    @Test
    public void executeWhenBusinessErrorIsThrown() throws Exception {
        ClientTask task = getTask("fixtures/executors/get_order/get_business_error_task_message.json");
        ClientTask parentTask = getParentTask();

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(PARENT_TASK_ID))).thenReturn(parentTask);

        getOrdersDeliveryDateErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(mdbClient).setGetOrdersDeliveryDateError(any());
    }

    @Test
    public void executeWhenTechnicalErrorIsThrown() throws Exception {
        ClientTask task = getTask("fixtures/executors/get_order/get_technical_error_task_message.json");
        ClientTask parentTask = getParentTask();

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(PARENT_TASK_ID))).thenReturn(parentTask);

        getOrdersDeliveryDateErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(mdbClient).setGetOrdersDeliveryDateError(any());
    }

    private ClientTask getTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setParentId(PARENT_TASK_ID);
        task.setRootId(PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_GET_ORDERS_DELIVERY_DATE_ERROR);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }

    private ClientTask getParentTask() {
        ClientTask task = new ClientTask();
        task.setId(PARENT_TASK_ID);
        task.setRootId(PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.DS_GET_ORDERS_DELIVERY_DATE);
        task.setMessage(getFileContent("fixtures/executors/get_order/get_orders_delivery_date_task_message.json"));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }
}
