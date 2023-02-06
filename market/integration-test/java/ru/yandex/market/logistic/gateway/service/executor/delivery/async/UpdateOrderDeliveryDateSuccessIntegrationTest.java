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

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateOrderDeliveryDateSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final Long TASK_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @Autowired
    private UpdateOrderDeliveryDateSuccessExecutor updateOrderDeliveryDateSuccessExecutor;

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getTask();

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        updateOrderDeliveryDateSuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(mdbClient).setUpdateDeliveryDateSuccess(anyLong(), anyLong());
    }

    private ClientTask getTask() {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setRootId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_UPDATE_ORDER_DELIVERY_DATE);
        task.setMessage(getFileContent("fixtures/executors/update_order/update_order_delivery_date_task_response.json"));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }
}
