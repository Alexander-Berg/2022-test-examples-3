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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class PutReferenceWarehousesSuccessIntegrationTest extends AbstractIntegrationTest {
    private final static long TASK_ID = 101L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private LmsLgwCallbackClient lmsLgwCallbackClient;

    @Autowired
    private PutReferenceWarehousesSuccessExecutor putReferenceWarehousesSuccessExecutor;

    @Test
    public void testSuccessClientCalling() {
        ClientTask task = getClientTask("fixtures/executors/put_reference_warehouses/correct_message_response.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        putReferenceWarehousesSuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(lmsLgwCallbackClient).putReferenceWarehouseSuccess(
            eq(145L),
            (List<Long>) argThat(Matchers.containsInAnyOrder(9955215L, 9955214L))
        );
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();

        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_PUT_REFERENCE_WAREHOUSES_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);

        return task;
    }
}
