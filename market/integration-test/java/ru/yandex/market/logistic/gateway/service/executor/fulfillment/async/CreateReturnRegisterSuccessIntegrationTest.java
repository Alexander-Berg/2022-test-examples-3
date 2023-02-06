package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.util.Arrays;

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
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowConsumerClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateReturnRegisterSuccessIntegrationTest extends AbstractIntegrationTest {
    private static final long REGISTER_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateReturnRegisterSuccessExecutor createReturnRegisterSuccessExecutor;

    @SpyBean
    private FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Before
    public void setUp() {
        doNothing().when(fulfillmentWorkflowConsumerClient)
            .setCreateReturnRegisterSuccess(anyString(), anyListOf(String.class), any());
    }
    @Test
    public void successExecutorBaseWorkflow() {
        ClientTask task = getCreateRegisterSuccessTask(
            "fixtures/response/fulfillment/create_return_register/response.json"
        );

        when(repository.findTask(eq(REGISTER_ID))).thenReturn(task);

        TaskMessage message = createReturnRegisterSuccessExecutor.execute(new ExecutorTaskWrapper(REGISTER_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();
        verify(fulfillmentWorkflowConsumerClient).setCreateReturnRegisterSuccess(
            "3",
            Arrays.asList("1", "2"),
            null
        );
    }

    private ClientTask getCreateRegisterSuccessTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(REGISTER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_RETURN_REGISTER_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
