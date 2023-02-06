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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateIntakeErrorIntegrationTest extends AbstractIntegrationTest {
    private static final long CREATE_INTAKE_ERROR_TASK_ID = 726L;

    private static final long CREATE_INTAKE_ERROR_PARENT_TASK_ID = 543L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateIntakeErrorExecutor createIntakeErrorExecutor;

    @SpyBean
    protected FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Before
    public void setUp() {
        doNothing().when(fulfillmentWorkflowConsumerClient)
            .setCreateIntakeError(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    public void successExecutorBaseWorkflow() {
        ClientTask task = getCreateIntakeErrorTask("fixtures/executors/error_task_message.json");

        ClientTask parentTask =
            getCreateIntakeParentTask("fixtures/executors/fulfillment/create_intake/with_all_parameters.json");

        when(repository.findTask(eq(CREATE_INTAKE_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(CREATE_INTAKE_ERROR_PARENT_TASK_ID))).thenReturn(parentTask);

        TaskMessage message = createIntakeErrorExecutor.execute(new ExecutorTaskWrapper(CREATE_INTAKE_ERROR_TASK_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowConsumerClient).setCreateIntakeError(
            eq("intake-yandex-id-1"),
            eq(145L),
            isNull(),
            eq("could not execute statement; SQL [n/a]; constraint [message]; nested exception is " +
                    "org.hibernate.exception.ConstraintViolationException: could not execute statement"));
    }

    private ClientTask getCreateIntakeErrorTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CREATE_INTAKE_ERROR_TASK_ID);
        task.setRootId(CREATE_INTAKE_ERROR_PARENT_TASK_ID);
        task.setParentId(CREATE_INTAKE_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_INTAKE_ERROR);
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        task.setMessage(getFileContent(filename));
        return task;
    }

    private ClientTask getCreateIntakeParentTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CREATE_INTAKE_ERROR_PARENT_TASK_ID);
        task.setRootId(CREATE_INTAKE_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.FF_CREATE_INTAKE);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
