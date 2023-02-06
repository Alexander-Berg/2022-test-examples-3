package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.mdbclient.model.response.CreateOrderResponse;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.MdbFulfillmentConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateOrderSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_ORDER_ID = 100L;

    private static final String TRACK_ID = "YNM8790030";

    private static final String PROCESS_ID = "processIdABC123";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @SpyBean
    private MdbFulfillmentConsumerClient mdbFulfillmentConsumerClient;

    @Autowired
    private CreateOrderSuccessExecutor createOrderSuccessExecutor;

    @Before
    public void setup() {
        when(mdbClient.setFfOrderSuccess(eq(TEST_ORDER_ID), eq(TRACK_ID)))
            .thenReturn(new CreateOrderResponse(CreateOrderResponse.Status.ORDER_DELIVERY_WAS_SET, null));
    }

    @Test
    public void executeCreateOrderSuccessExecutorSuccessfully() {
        ClientTask task = getClientTask(
            "fixtures/executors/fulfillment_create_order/create_fulfillment_order_success.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        TaskMessage message = createOrderSuccessExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        verify(mdbClient).setFfOrderSuccess(eq(TEST_ORDER_ID), eq(TRACK_ID));
        verify(mdbFulfillmentConsumerClient).setCreateOrderSuccess(
            eq(String.valueOf(TEST_ORDER_ID)),
            eq(TRACK_ID),
            isNull(),
            eq(PROCESS_ID)
        );

        softAssert.assertThat(message.getMessageBody()).isNull();
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_ORDER_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        task.setProcessId(PROCESS_ID);
        return task;
    }
}
