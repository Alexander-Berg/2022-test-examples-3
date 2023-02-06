package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

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
import ru.yandex.market.logistic.gateway.service.consumer.TplFulfillmentConsumerClient;
import ru.yandex.market.tpl.client.ff.FulfillmentResponseConsumerClient;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateOrderErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long ORDER_ID = 5927638L;
    private static final long PARTNER_ID = 145L;
    private static final long UPDATE_ORDER_ERROR_TASK_ID = 70L;
    private static final String PROCESS_ID = "123";

    private final static long UPDATE_ORDER_ERROR_PARENT_TASK_ID = 60L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private FulfillmentResponseConsumerClient fulfillmentResponseConsumerClient;
    @SpyBean
    private TplFulfillmentConsumerClient tplFulfillmentConsumerClient;

    @Autowired
    private UpdateOrderErrorExecutor updateOrderErrorExecutor;

    @Test
    public void executeUsualWorkWithError() throws Exception {
        ClientTask task = getUpdateOrderErrorTask();
        ClientTask parentTask =
            getUpdateOrderErrorParentTask("fixtures/executors/fulfillment_create_order/fulfillment_update_order_task_message.json");

        when(repository.findTask(eq(UPDATE_ORDER_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(UPDATE_ORDER_ERROR_PARENT_TASK_ID))).thenReturn(parentTask);

        TaskMessage message = updateOrderErrorExecutor.execute(new ExecutorTaskWrapper(UPDATE_ORDER_ERROR_TASK_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();
        verify(tplFulfillmentConsumerClient).setUpdateOrderError(String.valueOf(ORDER_ID), PARTNER_ID, PROCESS_ID, null);
    }

    private ClientTask getUpdateOrderErrorTask() {
        return new ClientTask()
            .setId(UPDATE_ORDER_ERROR_TASK_ID)
            .setRootId(UPDATE_ORDER_ERROR_PARENT_TASK_ID)
            .setParentId(UPDATE_ORDER_ERROR_PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_UPDATE_ORDER_ERROR)
            .setConsumer(TaskResultConsumer.TPL)
            .setProcessId(PROCESS_ID);
    }

    private ClientTask getUpdateOrderErrorParentTask(String filename) {
        return new ClientTask()
            .setId(UPDATE_ORDER_ERROR_PARENT_TASK_ID)
            .setRootId(UPDATE_ORDER_ERROR_PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_UPDATE_ORDER)
            .setMessage(getFileContent(filename))
            .setConsumer(TaskResultConsumer.TPL);
    }

}
