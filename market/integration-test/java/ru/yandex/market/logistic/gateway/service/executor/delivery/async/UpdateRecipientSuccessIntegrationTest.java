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

public class UpdateRecipientSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final Long TASK_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private LomDeliveryServiceConsumerClient lomClient;

    @Autowired
    private UpdateRecipientSuccessExecutor updateRecipientSuccessExecutor;

    @Test
    public void executeSuccessLom() {
        performExecuteWithConsumer(TaskResultConsumer.LOM);
        verify(lomClient).setUpdateOrderRecipientSuccess(null, "LO-12345", 125L);
    }

    private void performExecuteWithConsumer(TaskResultConsumer taskResultConsumer) {
        ClientTask task = getTaskWithConsumer(taskResultConsumer);

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        updateRecipientSuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    private ClientTask getTaskWithConsumer(TaskResultConsumer taskResultConsumer) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setRootId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_UPDATE_RECIPIENT_SUCCESS);
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
