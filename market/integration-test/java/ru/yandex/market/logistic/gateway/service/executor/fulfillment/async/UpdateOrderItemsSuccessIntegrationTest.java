package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import ru.yandex.market.logistic.gateway.service.consumer.LomFulfillmentConsumerClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateOrderItemsSuccessIntegrationTest extends AbstractIntegrationTest {
    private final static long UPDATE_ORDER_ITEMS_SUCCESS_TASK_ID = 564L;

    private final static long YANDEX_ID = 12345;
    @Value("${fulfillment.workflow.api.url}")
    protected String ffWorkHost;
    @MockBean
    private ClientTaskRepository repository;
    @Autowired
    private UpdateOrderItemsSuccessExecutor updateOrderItemsSuccessExecutor;

    @SpyBean
    private LomFulfillmentConsumerClient lomFulfillmentConsumerClient;

    @Before
    public void setUp() {
        doNothing().when(lomFulfillmentConsumerClient)
            .setUpdateOrderItemsSuccess(anyString(), anyLong(), any());
    }

    @Test
    public void successExecutorBaseWorkflow() {
        ClientTask task = getUpdateOrderItemsTask(
            "fixtures/executors/fulfillment_update_order_items/update_order_items_response.json"
        );

        when(repository.findTask(eq(UPDATE_ORDER_ITEMS_SUCCESS_TASK_ID))).thenReturn(task);

        TaskMessage message = updateOrderItemsSuccessExecutor.execute(
            new ExecutorTaskWrapper(UPDATE_ORDER_ITEMS_SUCCESS_TASK_ID, 0)
        );
        softAssert.assertThat(message.getMessageBody()).isNull();
        verify(lomFulfillmentConsumerClient).setUpdateOrderItemsSuccess(
            String.valueOf(YANDEX_ID),
            145L,
            null
        );
    }

    private ClientTask getUpdateOrderItemsTask(final String filename) {
        ClientTask task = new ClientTask();
        task.setId(UPDATE_ORDER_ITEMS_SUCCESS_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_UPDATE_ORDER_ITEMS_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.LOM);
        return task;
    }
}
