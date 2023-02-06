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
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowConsumerClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateRegisterSuccessIntegrationTest extends AbstractIntegrationTest {
    private static final long REGISTER_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateRegisterSuccessExecutor createRegisterSuccessExecutor;

    @SpyBean
    protected FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Before
    public void setUp() {
        doNothing().when(fulfillmentWorkflowConsumerClient)
            .setCreateRegisterSuccess(anyString(), anyString(), anyString());
    }

    @Test
    public void successExecutorBaseWorkflow() {
        ClientTask task = getCreateRegisterSuccessTask(
            "fixtures/response/fulfillment/create_register/fulfillment_create_register_response.json"
        );

        when(repository.findTask(eq(REGISTER_ID))).thenReturn(task);

        TaskMessage message = createRegisterSuccessExecutor.execute(new ExecutorTaskWrapper(REGISTER_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowConsumerClient).setCreateRegisterSuccess("1", "ext1", null);
    }

    private ClientTask getCreateRegisterSuccessTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(REGISTER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_REGISTER_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
