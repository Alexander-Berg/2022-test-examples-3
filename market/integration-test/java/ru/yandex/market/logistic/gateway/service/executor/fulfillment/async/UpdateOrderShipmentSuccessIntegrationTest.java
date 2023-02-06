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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateOrderShipmentSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long ORDER_ID = 5927638L;
    private static final long PARTNER_ID = 145L;
    private static final String PROCESS_ID = "123";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private LomFulfillmentConsumerClient lomFulfillmentConsumerClient;

    @Autowired
    private UpdateOrderShipmentSuccessExecutor updateOrderShipmentSuccessExecutor;

    @Before
    public void setUp() {
        doNothing().when(lomFulfillmentConsumerClient).setUpdateOrderShipmentSuccess(
            String.valueOf(ORDER_ID),
            PARTNER_ID,
            PROCESS_ID
        );
    }

    @Test
    public void execute() {
        when(repository.findTask(ORDER_ID)).thenReturn(task());

        TaskMessage message = updateOrderShipmentSuccessExecutor.execute(new ExecutorTaskWrapper(ORDER_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();
        verify(lomFulfillmentConsumerClient).setUpdateOrderShipmentSuccess(
            String.valueOf(ORDER_ID),
            PARTNER_ID,
            PROCESS_ID
        );
    }

    @Nonnull
    private ClientTask task() {
        return new ClientTask()
            .setId(ORDER_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_UPDATE_ORDER_SHIPMENT_SUCCESS)
            .setMessage(getFileContent("fixtures/executors/fulfillment_update_order_shipment/task_response.json"))
            .setConsumer(TaskResultConsumer.LOM)
            .setProcessId(PROCESS_ID);
    }
}
