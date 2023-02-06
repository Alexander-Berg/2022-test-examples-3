package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.util.List;

import org.hamcrest.Matchers;
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
import ru.yandex.market.logistics.management.client.async.LmsLgwCallbackClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class PutReferenceWarehousesErrorIntegrationTest extends AbstractIntegrationTest {
    private final static long ERROR_TASK_ID = 10L;
    private final static long ORIGINAL_TASK_ID = 101L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private LmsLgwCallbackClient lmsLgwCallbackClient;

    @Autowired
    private PutReferenceWarehousesErrorExecutor putReferenceWarehousesErrorExecutor;

    @Test
    public void testErrorClientCalling() {
        ClientTask originalTask = getOriginalTask("fixtures/executors/put_reference_warehouses/correct_message.json");
        when(repository.findTask(eq(ORIGINAL_TASK_ID))).thenReturn(originalTask);

        ClientTask errorTask = getErrorTask("fixtures/executors/put_reference_warehouses/error_task_message.json");
        when(repository.findTask(eq(ERROR_TASK_ID))).thenReturn(errorTask);

        putReferenceWarehousesErrorExecutor.execute(new ExecutorTaskWrapper(ERROR_TASK_ID, 0));

        verify(lmsLgwCallbackClient).putReferenceWarehouseError(
            eq(145L),
            (List<Long>) argThat(Matchers.containsInAnyOrder(9955215L, 9955214L)),
            eq("Не найдено событие"),
            isNull()
        );
    }

    private ClientTask getOriginalTask(String filename) {
        ClientTask task = new ClientTask();

        task.setId(ORIGINAL_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_PUT_REFERENCE_WAREHOUSES);
        task.setMessage(getFileContent(filename));

        return task;
    }

    private ClientTask getErrorTask(String filename) {
        ClientTask task = new ClientTask();

        task.setId(ERROR_TASK_ID);
        task.setRootId(ORIGINAL_TASK_ID);
        task.setParentId(ORIGINAL_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_PUT_REFERENCE_WAREHOUSES_ERROR);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);

        return task;
    }
}
