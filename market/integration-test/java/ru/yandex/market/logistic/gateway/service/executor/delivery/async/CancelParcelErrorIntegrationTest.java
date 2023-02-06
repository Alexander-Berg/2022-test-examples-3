package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.mdbclient.model.request.CancelStatus;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.ExecutorException;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест для {@link CancelParcelErrorExecutor}.
 */
public class CancelParcelErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long CANCEL_PARCEL_ERROR_TASK_ID = 70L;

    private final static long CANCEL_PARCEL_ERROR_PARENT_TASK_ID = 60L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @Autowired
    private CancelParcelErrorExecutor cancelParcelErrorExecutor;

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getCancelParcelErrorTask();
        ClientTask parentTask = getCancelParcelErrorParentTask("fixtures/executors/cancel_parcel/cancel_parcel_task_message.json");

        when(repository.findTask(eq(CANCEL_PARCEL_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(CANCEL_PARCEL_ERROR_PARENT_TASK_ID))).thenReturn(parentTask);

        cancelParcelErrorExecutor.execute(new ExecutorTaskWrapper(CANCEL_PARCEL_ERROR_TASK_ID, 0));

        verify(mdbClient).setParcelCancelResult(anyLong(), anyLong(), any(CancelStatus.class));
    }

    @Test(expected = ExecutorException.class)
    public void executeIOException() throws Exception {
        ClientTask task = getCancelParcelErrorTask();
        ClientTask parentTask = getCancelParcelErrorParentTask("fixtures/executors/unreadable.json");
        when(repository.findTask(eq(CANCEL_PARCEL_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(CANCEL_PARCEL_ERROR_PARENT_TASK_ID))).thenReturn(parentTask);
        cancelParcelErrorExecutor.execute(new ExecutorTaskWrapper(CANCEL_PARCEL_ERROR_TASK_ID, 0));
    }

    private ClientTask getCancelParcelErrorTask() {
        ClientTask task = new ClientTask();
        task.setId(CANCEL_PARCEL_ERROR_TASK_ID);
        task.setRootId(CANCEL_PARCEL_ERROR_PARENT_TASK_ID);
        task.setParentId(CANCEL_PARCEL_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CANCEL_PARCEL_ERROR);
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }

    private ClientTask getCancelParcelErrorParentTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CANCEL_PARCEL_ERROR_PARENT_TASK_ID);
        task.setRootId(CANCEL_PARCEL_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.DS_CANCEL_PARCEL);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }
}
