package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistics.lom.client.async.LomDeliveryServiceConsumerClient;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateRecipientErrorIntegrationTest extends AbstractIntegrationTest {

    private static final Long TASK_ID = 2L;
    private static final Long PARENT_TASK_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private LomDeliveryServiceConsumerClient lomClient;

    @Autowired
    private UpdateRecipientErrorExecutor updateRecipientErrorExecutor;

    @Test
    public void executeLomWhenBusinessErrorIsThrown() throws Exception {
        performExecuteWithFilenameAndConsumer(
            "fixtures/executors/update_order/update_business_error_task_message_4000.json",
            TaskResultConsumer.LOM
        );

        verify(lomClient).setUpdateOrderRecipientError(
            null,
            "LO-12345",
            125L,
            false,
            4000,
            "Не найдено событие"
        );}

    @Test
    public void executeLomWhenBusinessErrorNotThrownIfCodeNot4000() throws Exception {
       performExecuteWithFilenameAndConsumer(
           "fixtures/executors/update_order/update_business_error_task_message.json",
           TaskResultConsumer.LOM
       );

        verify(lomClient).setUpdateOrderRecipientError(
            null,
            "LO-12345",
            125L,
            false,
            9404,
            "Не найдено событие"
        );
    }

    @Test
    public void executeLomWhenTechnicalErrorIsThrown() throws Exception {
        performExecuteWithFilenameAndConsumer(
            "fixtures/executors/update_order/update_technical_error_task_message.json",
            TaskResultConsumer.LOM
        );
        verify(lomClient).setUpdateOrderRecipientError(
            null,
            "LO-12345",
            125L,
            false,
            null,
            "Cannot use the ROLLBACK statement within an INSERT-EXEC statement."
        );}

    private void performExecuteWithFilenameAndConsumer(String filename, TaskResultConsumer taskResultConsumer) {
        ClientTask task = getTask(
            filename,
            taskResultConsumer
        );
        ClientTask parentTask = getParentTask(taskResultConsumer);

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(PARENT_TASK_ID))).thenReturn(parentTask);

        updateRecipientErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    private ClientTask getTask(String filename, TaskResultConsumer taskResultConsumer) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setParentId(PARENT_TASK_ID);
        task.setRootId(PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_UPDATE_RECIPIENT_ERROR);
        task.setMessage(getFileContent(filename));
        task.setConsumer(taskResultConsumer);
        return task;
    }

    private ClientTask getParentTask(TaskResultConsumer taskResultConsumer){
        ClientTask task = new ClientTask();
        task.setId(PARENT_TASK_ID);
        task.setRootId(PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.DS_UPDATE_RECIPIENT);
        task.setMessage(getFileContent(
            String.format(
                "fixtures/executors/update_order/update_recipient_task_message_%s.json",
                taskResultConsumer.getName().toLowerCase()
            )
        ));
        task.setConsumer(taskResultConsumer);
        return task;
    }
}
