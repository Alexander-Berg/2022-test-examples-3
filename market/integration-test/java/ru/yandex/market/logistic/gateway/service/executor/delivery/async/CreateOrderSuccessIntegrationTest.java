package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.MdbDeliveryConsumerClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CreateOrderSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_ORDER_ID = 100L;

    private static final String PROCESS_ID = "processIdABC123";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @SpyBean
    private MdbDeliveryConsumerClient mdbDeliveryConsumerClient;

    @Autowired
    private CreateOrderSuccessExecutor createOrderSuccessExecutor;

    @After
    public void after() {
        verifyNoMoreInteractions(mdbClient);
    }

    @Test
    public void executeSuccessWithParcelId() throws Exception {
        ClientTask task = getClientTaskWithParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        TaskMessage message = createOrderSuccessExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        verify(mdbClient).setDsOrderSuccess(eq(TEST_ORDER_ID), eq("200"), eq("YNM8790030"));
        verify(mdbDeliveryConsumerClient).setCreateOrderSuccess(
            eq(String.valueOf(TEST_ORDER_ID)),
            eq("200"),
            eq("YNM8790030"),
            any(),
            eq(PROCESS_ID)
        );

        softAssert.assertThat(message.getMessageBody()).isNull();
    }

    @Test
    public void executeSuccessWithoutParcelId() throws Exception {
        ClientTask task = getClientTaskWithoutParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        TaskMessage message = createOrderSuccessExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        verify(mdbClient).setDsOrderSuccess(eq(TEST_ORDER_ID), isNull(), eq("YNM8790030"));
        verify(mdbDeliveryConsumerClient).setCreateOrderSuccess(
            eq(String.valueOf(TEST_ORDER_ID)),
            isNull(),
            eq("YNM8790030"),
            any(),
            eq(PROCESS_ID)
        );

        softAssert.assertThat(message.getMessageBody()).isNull();
    }

    private ClientTask getClientTaskWithParcelId() {
        return getClientTask(
            "fixtures/executors/create_order/create_order_success_task_message_with_parcel_id.json"
        );
    }

    private ClientTask getClientTaskWithoutParcelId() {
        return getClientTask(
            "fixtures/executors/create_order/create_order_success_task_message_without_parcel_id.json"
        );
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CREATE_ORDER_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(PROCESS_ID);
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }
}
