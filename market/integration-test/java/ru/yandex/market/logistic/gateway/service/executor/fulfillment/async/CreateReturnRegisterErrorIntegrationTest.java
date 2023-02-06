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

public class CreateReturnRegisterErrorIntegrationTest extends AbstractIntegrationTest {
    private static final long CREATE_RETURN_REGISTER_ERROR_TASK_ID = 726L;

    private static final long CREATE_RETURN_REGISTER_ERROR_PARENT_TASK_ID = 543L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateReturnRegisterErrorExecutor createReturnRegisterErrorExecutor;

    @SpyBean
    private FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Before
    public void setUp() {
        doNothing().when(fulfillmentWorkflowConsumerClient)
            .setCreateReturnRegisterError(anyString(), anyListOf(String.class), any(), anyString());
    }

    @Test
    public void successExecutorBaseWorkflow() {
        ClientTask task =
            getCreateRegisterErrorTask("fixtures/executors/error_task_message.json");

        ClientTask parentTask =
            getCreateRegisterParentTask(
                "fixtures/executors/fulfillment_create_return_register/with_all_parameters.json"
            );

        when(repository.findTask(eq(CREATE_RETURN_REGISTER_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(CREATE_RETURN_REGISTER_ERROR_PARENT_TASK_ID))).thenReturn(parentTask);

        TaskMessage message = createReturnRegisterErrorExecutor.execute(
            new ExecutorTaskWrapper(CREATE_RETURN_REGISTER_ERROR_TASK_ID, 0)
        );
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowConsumerClient).setCreateReturnRegisterError(
            "3",
            Arrays.asList("1", "2"),
            null,
            "could not execute statement; SQL [n/a]; constraint [message]; nested exception is" +
                " org.hibernate.exception.ConstraintViolationException: could not execute statement"
        );
    }

    private ClientTask getCreateRegisterErrorTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CREATE_RETURN_REGISTER_ERROR_TASK_ID);
        task.setRootId(CREATE_RETURN_REGISTER_ERROR_PARENT_TASK_ID);
        task.setParentId(CREATE_RETURN_REGISTER_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_RETURN_REGISTER_ERROR);
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        task.setMessage(getFileContent(filename));
        return task;
    }

    private ClientTask getCreateRegisterParentTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CREATE_RETURN_REGISTER_ERROR_PARENT_TASK_ID);
        task.setRootId(CREATE_RETURN_REGISTER_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.FF_CREATE_RETURN_REGISTER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
