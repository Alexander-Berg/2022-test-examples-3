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

public class CreateRegisterErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long REGISTER_ID = 1L;

    private static final String REGISTER_ID_STRING = String.valueOf(REGISTER_ID);

    private final static long CREATE_REGISTER_ERROR_TASK_ID = 70L;

    private final static long CREATE_REGISTER_ERROR_PARENT_TASK_ID = 60L;
    private static final String PROCESS_ID = "123";
    private static final String ERROR_MSG = "abcd";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private TransportManagerClient transportManagerClient;

    @Autowired
    private CreateRegisterErrorExecutor createRegisterErrorExecutor;

    @Test
    public void errorExecutorBaseWorkflow() {
        ClientTask task = getCreateRegisterErrorTask();
        ClientTask parentTask =
            getCreateRegisterErrorParentTask("fixtures/executors/create_register" +
                "/create_register_with_all_parameters_task.json");

        when(repository.findTask(eq(CREATE_REGISTER_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(CREATE_REGISTER_ERROR_PARENT_TASK_ID))).thenReturn(parentTask);

        TaskMessage message = createRegisterErrorExecutor.execute(
            new ExecutorTaskWrapper(CREATE_REGISTER_ERROR_TASK_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(transportManagerClient).setCreateRegisterError(PROCESS_ID, REGISTER_ID_STRING, ERROR_MSG);
    }

    private ClientTask getCreateRegisterErrorTask() {
        ClientTask task = new ClientTask();
        task.setId(CREATE_REGISTER_ERROR_TASK_ID);
        task.setRootId(CREATE_REGISTER_ERROR_PARENT_TASK_ID);
        task.setParentId(CREATE_REGISTER_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CREATE_REGISTER_ERROR);
        task.setConsumer(TaskResultConsumer.TM);
        task.setProcessId(PROCESS_ID);
        task.setMessage("{\"error\":\"" + ERROR_MSG + "\"}");
        return task;
    }

    private ClientTask getCreateRegisterErrorParentTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CREATE_REGISTER_ERROR_PARENT_TASK_ID);
        task.setRootId(CREATE_REGISTER_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.DS_CREATE_REGISTER);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }
}
