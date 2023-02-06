package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.MdbDeliveryConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory.createClientTask;

public class CreateSelfExportErrorIntegrationTest extends AbstractIntegrationTest {
    private static final long SELF_EXPORT_ID = 101L;
    private static final long SELF_EXPORT_PARENT_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private MdbDeliveryConsumerClient consumerClient;

    @Autowired
    private CreateSelfExportErrorExecutor createSelfExportErrorExecutor;

    @Test
    public void errorExecutorBaseWorkflow() throws Exception {
        ClientTask parentTask = createClientTask(
                SELF_EXPORT_PARENT_ID,
                RequestFlow.DS_CREATE_SELFEXPORT,
            "fixtures/request/delivery/create_selfexport/delivery_create_selfexport.json"

        );
        when(repository.findTask(eq(SELF_EXPORT_PARENT_ID))).thenReturn(parentTask);

        ClientTask task = createClientTask(
                SELF_EXPORT_ID,
                SELF_EXPORT_PARENT_ID,
                RequestFlow.DS_CREATE_SELFEXPORT_ERROR
        );
        task.setConsumer(TaskResultConsumer.MDB);
        when(repository.findTask(eq(SELF_EXPORT_ID))).thenReturn(task);

        createSelfExportErrorExecutor.execute(new ExecutorTaskWrapper(SELF_EXPORT_ID, 0));
        verify(consumerClient).setCreateSelfExportError(eq("100"), eq(125L), isNull(), isNull());
    }
}
