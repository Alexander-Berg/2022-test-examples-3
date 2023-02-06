package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateRegisterSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long REGISTER_ID = 1L;
    private static final String PROCESS_ID = "123";

    private static final String REGISTER_ID_STRING = String.valueOf(REGISTER_ID);

    private static final String EXTERNAL_REGISTER_ID_STRING = "ext1";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private TransportManagerClient transportManagerClient;

    @Autowired
    private CreateRegisterSuccessExecutor createRegisterSuccessExecutor;

    @Test
    public void successExecutorBaseWorkflow() throws Exception {
        ClientTask task = getCreateIntakeTask("fixtures/response/delivery/create_register" +
            "/delivery_create_register_response.json");

        when(repository.findTask(eq(REGISTER_ID))).thenReturn(task);

        TaskMessage message = createRegisterSuccessExecutor.execute(new ExecutorTaskWrapper(REGISTER_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(transportManagerClient).setCreateRegisterSuccess(
            PROCESS_ID,
            REGISTER_ID_STRING,
            EXTERNAL_REGISTER_ID_STRING
        );
    }

    private ClientTask getCreateIntakeTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(REGISTER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CREATE_REGISTER);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.TM);
        task.setProcessId(PROCESS_ID);
        return task;
    }
}
