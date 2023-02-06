package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.mdbclient.model.request.CancelStatus;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
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
 * Интеграционный тест для {@link CancelParcelSuccessExecutor}.
 */
public class CancelParcelSuccessIntegrationTest extends AbstractIntegrationTest {
    private final static long CANCEL_PARCEL_SUCCESS_TASK_ID = 90L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @Autowired
    private CancelParcelSuccessExecutor cancelParcelSuccessExecutor;

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getCancelParcelSuccessTask("fixtures/executors/cancel_parcel/cancel_parcel_task_response.json");

        when(repository.findTask(eq(CANCEL_PARCEL_SUCCESS_TASK_ID))).thenReturn(task);

        cancelParcelSuccessExecutor.execute(new ExecutorTaskWrapper(CANCEL_PARCEL_SUCCESS_TASK_ID, 0));

        verify(mdbClient).setParcelCancelResult(anyLong(), anyLong(), any(CancelStatus.class));
    }

    private ClientTask getCancelParcelSuccessTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(CANCEL_PARCEL_SUCCESS_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CANCEL_PARCEL_SUCCESS);
        task.setConsumer(TaskResultConsumer.MDB);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
