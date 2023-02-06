package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import javax.annotation.Nonnull;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateOrderShipmentErrorIntegrationTest extends AbstractIntegrationTest {
    private static final long ERROR_TASK_ID = 564L;
    private static final long ERROR_PARENT_TASK_ID = 145L;
    private static final String ORDER_ID = "5927638";
    private static final long PARTNER_ID = 145L;
    protected static final String MESSAGE = "could not execute statement; SQL [n/a]; constraint [message]; " +
        "nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private LomFulfillmentConsumerClient lomFulfillmentConsumerClient;

    @Autowired
    private UpdateOrderShipmentErrorExecutor updateOrderShipmentErrorExecutor;

    @Before
    public void setup() {
        doNothing().when(lomFulfillmentConsumerClient).setUpdateOrderShipmentError(ORDER_ID, PARTNER_ID, null, MESSAGE);
    }

    @Test
    public void executeError() {
        when(repository.findTask(ERROR_TASK_ID)).thenReturn(task());
        when(repository.findTask(eq(ERROR_PARENT_TASK_ID))).thenReturn(parentTask());

        TaskMessage message = updateOrderShipmentErrorExecutor.execute(new ExecutorTaskWrapper(ERROR_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(lomFulfillmentConsumerClient).setUpdateOrderShipmentError(ORDER_ID, 145L, null, MESSAGE);
    }

    @Nonnull
    private ClientTask task() {
        return new ClientTask()
            .setId(ERROR_TASK_ID)
            .setRootId(ERROR_PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_UPDATE_ORDER_SHIPMENT_ERROR)
            .setMessage(getFileContent("fixtures/executors/fulfillment_update_order_shipment/task_error.json"))
            .setParentId(ERROR_PARENT_TASK_ID)
            .setConsumer(TaskResultConsumer.LOM);
    }

    @Nonnull
    private ClientTask parentTask() {
        return new ClientTask()
            .setId(ERROR_PARENT_TASK_ID)
            .setRootId(ERROR_PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_UPDATE_ORDER_SHIPMENT)
            .setMessage(getFileContent("fixtures/executors/fulfillment_update_order_shipment/task_message.json"))
            .setConsumer(TaskResultConsumer.LOM);
    }
}
