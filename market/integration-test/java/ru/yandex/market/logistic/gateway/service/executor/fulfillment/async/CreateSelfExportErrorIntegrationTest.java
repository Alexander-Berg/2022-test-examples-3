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
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowConsumerClient;
import ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateSelfExportErrorIntegrationTest extends AbstractIntegrationTest {
    private static final long CREATE_SELF_EXPORT_ERROR_TASK_ID = 726L;
    private static final long CREATE_SELF_EXPORT_ERROR_PARENT_TASK_ID = 543L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateSelfExportErrorExecutor createSelfExportErrorExecutor;

    @SpyBean
    protected FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Before
    public void setUp() {
        doNothing().when(fulfillmentWorkflowConsumerClient)
            .setCreateSelfExportError(anyString(), anyLong(), any(), anyString());
    }

    @Test
    public void successExecutorBaseWorkflow() {
        ClientTask task = ClientTaskFactory.createClientTask(
                CREATE_SELF_EXPORT_ERROR_TASK_ID,
                CREATE_SELF_EXPORT_ERROR_PARENT_TASK_ID,
                RequestFlow.FF_CREATE_SELF_EXPORT_ERROR,
                "fixtures/executors/error_task_message.json"
            );
        task.setConsumer(TaskResultConsumer.FF_WF_API);

        ClientTask parentTask = ClientTaskFactory.createClientTask(
            CREATE_SELF_EXPORT_ERROR_PARENT_TASK_ID,
            RequestFlow.FF_CREATE_SELF_EXPORT,
            "fixtures/executors/fulfillment/create_self_export/with_all_parameters.json"
        );

        when(repository.findTask(eq(CREATE_SELF_EXPORT_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(CREATE_SELF_EXPORT_ERROR_PARENT_TASK_ID))).thenReturn(parentTask);

        TaskMessage message = createSelfExportErrorExecutor.execute(new ExecutorTaskWrapper(CREATE_SELF_EXPORT_ERROR_TASK_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowConsumerClient).setCreateSelfExportError(
            eq("self-export-yandex-id-1"),
            eq(145L),
            isNull(),
            eq("could not execute statement; SQL [n/a]; constraint [message]; nested exception is " +
                    "org.hibernate.exception.ConstraintViolationException: could not execute statement"));
    }
}
