package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateOrderItemsErrorIntegrationTest extends AbstractIntegrationTest {
    private final static long UPDATE_ORDER_ITEMS_ERROR_TASK_ID = 564L;
    private final static long UPDATE_ORDER_ITEMS_PARENT_TASK_ID = 1L;

    private final static long YANDEX_ID = 12345;

    @MockBean
    private ClientTaskRepository repository;
    @Autowired
    private UpdateOrderItemsErrorExecutor updateOrderItemsErrorExecutor;

    @SpyBean
    private LomFulfillmentConsumerClient lomFulfillmentConsumerClient;

    @Before
    public void setUp() {
        doNothing().when(lomFulfillmentConsumerClient)
            .setUpdateOrderItemsError(anyString(), anyLong(), isNull(), anyString());
    }

    @Test
    public void failedExecutorBaseWorkflow() {
        ClientTask errorTask = getUpdateOrderItemsErrorTask(
            "fixtures/executors/fulfillment_update_order_items/update_order_items_error.json"
        );
        ClientTask parentTask = getUpdateOrderItemsParentTask(
            "fixtures/executors/fulfillment_update_order_items/update_order_items.json"
        );

        when(repository.findTask(eq(UPDATE_ORDER_ITEMS_ERROR_TASK_ID))).thenReturn(errorTask);
        when(repository.findTask(eq(UPDATE_ORDER_ITEMS_PARENT_TASK_ID))).thenReturn(parentTask);

        TaskMessage message = updateOrderItemsErrorExecutor.execute(
            new ExecutorTaskWrapper(UPDATE_ORDER_ITEMS_ERROR_TASK_ID, 0)
        );
        softAssert.assertThat(message.getMessageBody()).isNull();
        verify(lomFulfillmentConsumerClient).setUpdateOrderItemsError(
            String.valueOf(YANDEX_ID),
            145L,
            null,
            "could not execute statement; SQL [n/a]; constraint [message]; " +
                "nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"
        );
    }

    private ClientTask getUpdateOrderItemsErrorTask(final String filename) {
        return new ClientTask()
            .setId(UPDATE_ORDER_ITEMS_ERROR_TASK_ID)
            .setRootId(UPDATE_ORDER_ITEMS_PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_UPDATE_ORDER_ITEMS_SUCCESS)
            .setMessage(getFileContent(filename))
            .setParentId(UPDATE_ORDER_ITEMS_PARENT_TASK_ID)
            .setConsumer(TaskResultConsumer.LOM);
    }

    private ClientTask getUpdateOrderItemsParentTask(String filename) {
        return new ClientTask()
            .setId(UPDATE_ORDER_ITEMS_PARENT_TASK_ID)
            .setRootId(UPDATE_ORDER_ITEMS_PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_UPDATE_ORDER)
            .setMessage(getFileContent(filename))
            .setConsumer(TaskResultConsumer.LOM);
    }
}
