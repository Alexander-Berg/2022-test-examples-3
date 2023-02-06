package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.yandex.market.logistic.gateway.model.fulfillment.GetMovementExecutorResponse;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.TMFulfillmentConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetMovementSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_MOVEMENT_TASK_ID = 100L;

    private static final String PROCESS_ID = "123456";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private TMFulfillmentConsumerClient tmFulfillmentConsumerClient;

    @Autowired
    private GetMovementSuccessExecutor getMovementSuccessExecutor;

    @Test
    public void executeSuccess() throws IOException {
        String content = getFileContent("fixtures/executors/get_movement/get_movement_task_response_ff.json");
        GetMovementExecutorResponse response = new ObjectMapper().readValue(content, GetMovementExecutorResponse.class);
        ClientTask task = getClientTask(content);
        when(repository.findTask(eq(TEST_MOVEMENT_TASK_ID))).thenReturn(task);

        TaskMessage message = getMovementSuccessExecutor.execute(new ExecutorTaskWrapper(TEST_MOVEMENT_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(tmFulfillmentConsumerClient).setGetMovementSuccess(
            response.getMovement(),
            response.getCourier(),
            response.getRegistries(),
            145L,
            PROCESS_ID
        );
    }

    private ClientTask getClientTask(String message) {
        ClientTask task = new ClientTask();
        task.setId(TEST_MOVEMENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_GET_MOVEMENT_SUCCESS);
        task.setMessage(message);
        task.setProcessId(PROCESS_ID);
        task.setConsumer(TaskResultConsumer.TM);
        return task;
    }

}
