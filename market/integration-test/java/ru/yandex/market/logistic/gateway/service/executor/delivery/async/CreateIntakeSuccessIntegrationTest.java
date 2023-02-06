package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.yandex.market.logistic.gateway.service.consumer.TMDeliveryConsumerClient;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory.createClientTask;

public class CreateIntakeSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long INTAKE_ID = 100L;
    private static final String INTAKE_ID_STRING = "100";
    private static final String INTAKE_EXTERNAL_ID = "ext101";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean(name = "internalObjectMapper")
    private ObjectMapper mapper;

    @Autowired
    private CreateIntakeSuccessExecutor createIntakeSuccessExecutor;

    @SpyBean
    private TMDeliveryConsumerClient consumerClient;

    @Test
    public void successExecutorBaseWorkflow() throws Exception {
        ClientTask task = createClientTask(
            INTAKE_ID,
            RequestFlow.DS_CREATE_INTAKE_SUCCESS,
            "fixtures/response/delivery/create_intake/delivery_create_intake_response.json"
        );
        task.setConsumer(TaskResultConsumer.TM);
        when(repository.findTask(eq(INTAKE_ID))).thenReturn(task);

        doNothing().when(consumerClient).setCreateIntakeSuccess(anyString(), anyString(), anyLong(), anyString());
        TaskMessage message = createIntakeSuccessExecutor.execute(new ExecutorTaskWrapper(INTAKE_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(consumerClient).setCreateIntakeSuccess(
            eq(INTAKE_ID_STRING),
            eq(INTAKE_EXTERNAL_ID),
            anyLong(),
            isNull()
        );
    }
}
