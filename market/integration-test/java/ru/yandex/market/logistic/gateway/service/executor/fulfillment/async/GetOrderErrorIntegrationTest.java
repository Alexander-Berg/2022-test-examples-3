package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrderError;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetOrderErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    private final static long TASK_ID = 100L;

    private final static long PARENT_TASK_ID = 50L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @Autowired
    private GetOrderErrorExecutor executor;

    @Test
    public void testExecuteGetOrderSuccessSuccessfully() {
        ClientTask task = getClientTask("fixtures/executors/error_task_message.json");
        ClientTask parentTask = getParentClientTask(
            "fixtures/executors/get_order/fulfillment_get_order_task_message.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(PARENT_TASK_ID))).thenReturn(parentTask);

        TaskMessage message = executor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(mdbClient).setGetOrderError(eq(new GetOrderError(
            String.valueOf(TEST_ORDER_ID),
            "could not execute statement; SQL [n/a]; constraint [message]; nested exception is " +
                "org.hibernate.exception.ConstraintViolationException: could not execute statement")));

        softAssert.assertThat(message.getMessageBody())
            .as("Asserting that the response message body is null")
            .isNull();
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setParentId(PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_GET_ORDER_ERROR)
            .setMessage(getFileContent(filename))
            .setConsumer(TaskResultConsumer.MDB);
    }

    private ClientTask getParentClientTask(String filename) {
        return new ClientTask()
            .setId(PARENT_TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_GET_ORDER)
            .setMessage(getFileContent(filename))
            .setConsumer(TaskResultConsumer.MDB);
    }
}
