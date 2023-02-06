package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory.createClientTask;

public class CreateSelfExportSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long SELF_EXPORT_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean(name = "internalObjectMapper")
    private ObjectMapper mapper;

    @SpyBean
    private MdbDeliveryConsumerClient consumerClient;

    @Autowired
    private CreateSelfExportSuccessExecutor createSelfExportSuccessExecutor;

    @Test
    public void successExecutorBaseWorkflow() throws Exception {
        ClientTask task = createClientTask(
            SELF_EXPORT_ID,
            RequestFlow.DS_CREATE_SELFEXPORT_SUCCESS,
            "fixtures/response/delivery/create_selfexport/delivery_create_selfexport_response.json"
        );
        task.setConsumer(TaskResultConsumer.MDB);
        when(repository.findTask(eq(SELF_EXPORT_ID))).thenReturn(task);

        createSelfExportSuccessExecutor.execute(new ExecutorTaskWrapper(SELF_EXPORT_ID, 0));
        verify(consumerClient).setCreateSelfExportSuccess("100", "ext101", 123L, null);
    }
}
