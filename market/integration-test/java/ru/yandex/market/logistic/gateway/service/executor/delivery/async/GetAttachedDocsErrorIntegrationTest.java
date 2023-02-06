package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.MdbDeliveryConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory.createClientTask;

public class GetAttachedDocsErrorIntegrationTest extends AbstractIntegrationTest {
    private final static long PARENT_TASK_ID = 99L;
    private final static long TASK_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private MdbDeliveryConsumerClient consumerClient;

    @Autowired
    private GetAttachedDocsErrorExecutor getAttachedDocsErrorExecutor;

    @Test
    public void executeSuccessWRegister() {
        ClientTask parentTask = createClientTask(
            PARENT_TASK_ID,
            RequestFlow.DS_GET_ATTACHED_DOCS,
            "fixtures/executors/get_attached_docs/get_attached_docs_message_w_register.json"
        );
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        ClientTask task = createClientTask(
            TASK_ID,
            PARENT_TASK_ID,
            RequestFlow.DS_GET_ATTACHED_DOCS_ERROR,
            "fixtures/executors/get_attached_docs/get_attached_docs_error.json"
        );
        task.setConsumer(TaskResultConsumer.MDB);
        when(repository.findTask(TASK_ID)).thenReturn(task);

        getAttachedDocsErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
        verify(consumerClient, times(1)).setGetAttachedDocsError(
            eq(ResourceId.builder().setYandexId("101").build()),
            eq(145L),
            eq("Error 100"),
            isNull()
        );
    }

    @Test
    public void executeSuccessWoRegister() {
        ClientTask parentTask = createClientTask(
            PARENT_TASK_ID,
            RequestFlow.DS_GET_ATTACHED_DOCS,
            "fixtures/executors/get_attached_docs/get_attached_docs_message.json"
        );
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        ClientTask task = createClientTask(
            TASK_ID,
            PARENT_TASK_ID,
            RequestFlow.DS_GET_ATTACHED_DOCS_ERROR,
            "fixtures/executors/get_attached_docs/get_attached_docs_error.json"
        );
        task.setConsumer(TaskResultConsumer.MDB);
        when(repository.findTask(TASK_ID)).thenReturn(task);

        getAttachedDocsErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
        verify(consumerClient, times(1)).setGetAttachedDocsError(
            isNull(),
            eq(145L),
            eq("Error 100"),
            isNull()
        );
    }
}
