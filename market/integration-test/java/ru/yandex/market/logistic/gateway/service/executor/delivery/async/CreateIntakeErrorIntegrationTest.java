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
import ru.yandex.market.logistic.gateway.service.consumer.TMDeliveryConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory.createClientTask;

public class CreateIntakeErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long INTAKE_ID = 101L;
    private final static long INTAKE_PARENT_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateIntakeErrorExecutor createIntakeErrorExecutor;

    @SpyBean
    TMDeliveryConsumerClient consumerClient;

    @Test
    public void errorExecutorBaseWorkflow() throws Exception {
        ClientTask parentTask = createClientTask(
            INTAKE_PARENT_ID,
            RequestFlow.DS_CREATE_INTAKE,
            "fixtures/request/delivery/create_intake/delivery_create_intake.json"
        );
        when(repository.findTask(eq(INTAKE_PARENT_ID))).thenReturn(parentTask);

        ClientTask task = createClientTask(
            INTAKE_ID,
            INTAKE_PARENT_ID,
            RequestFlow.DS_CREATE_INTAKE_ERROR
        );
        task.setConsumer(TaskResultConsumer.TM);
        when(repository.findTask(eq(INTAKE_ID))).thenReturn(task);

        createIntakeErrorExecutor.execute(new ExecutorTaskWrapper(INTAKE_ID, 0));
        verify(consumerClient).setCreateIntakeError(eq("100"), eq(1L), isNull(), isNull());
    }
}
