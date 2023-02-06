package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetLabelsSuccessIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @Autowired
    private GetLabelsSuccessExecutor getLabelsSuccessExecutor;

    @Test
    public void executeSuccessWithParcelId() throws Exception {
        ClientTask task = getClientTaskWithParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        getLabelsSuccessExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        verify(mdbClient).setDsOrderLabel(TEST_ORDER_ID, "200", "http://pdf.url/label/url.pdf");
    }

    @Test
    public void executeSuccessWithoutParcelId() throws Exception {
        ClientTask task = getClientTaskWithoutParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        getLabelsSuccessExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        verify(mdbClient).setDsOrderLabel(TEST_ORDER_ID, null, "http://pdf.url/label/url.pdf");
    }

    private ClientTask getClientTaskWithParcelId() throws IOException {
        return getClientTask("fixtures/executors/get_labels/get_labels_success_task_message_with_parcel_id.json");
    }

    private ClientTask getClientTaskWithoutParcelId() throws IOException {
        return getClientTask("fixtures/executors/get_labels/get_labels_success_task_message_without_parcel_id.json");
    }

    private ClientTask getClientTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_GET_LABELS_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }
}
