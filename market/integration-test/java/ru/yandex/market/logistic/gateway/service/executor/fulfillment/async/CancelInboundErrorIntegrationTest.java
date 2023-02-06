package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static ru.yandex.market.logistic.gateway.utils.MockServerUtils.createMockRestServiceServer;

/**
 * Интеграционный тест для {@link CancelInboundErrorExecutor}.
 */
public class CancelInboundErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long INBOUND_ERROR_TASK_ID = 840L;

    private final static long INBOUND_TASK_ID = 400L;

    private final static long YANDEX_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @Value("${fulfillment.workflow.api.url}")
    protected String ffWorkHost;

    @Autowired
    private CancelInboundErrorExecutor cancelInboundErrorExecutor;

    private MockRestServiceServer mockServer;

    @Autowired
    private HttpTemplateImpl fulfillmentHttpTemplate;

    @Before
    public void setup() {
        mockServer = createMockRestServiceServer(fulfillmentHttpTemplate.getRestTemplate());
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeError() throws Exception {
        ClientTask task =
            getCancelInboundErrorTask("fixtures/executors/error_task_message.json");

        ClientTask parentTask =
            getCancelInboundTask("fixtures/executors/cancel_inbound/cancel_inbound_task_message.json");

        when(repository.findTask(eq(INBOUND_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(parentTask);

        prepareMockServerJsonScenario(mockServer, once(),
            ffWorkHost + "/requests/" + YANDEX_ID + "/reject-cancellation", null);

        cancelInboundErrorExecutor.execute(new ExecutorTaskWrapper(INBOUND_ERROR_TASK_ID, 0));
    }

    private ClientTask getCancelInboundErrorTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(INBOUND_ERROR_TASK_ID);
        task.setRootId(INBOUND_TASK_ID);
        task.setParentId(INBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CANCEL_INBOUND_ERROR);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }

    private ClientTask getCancelInboundTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(INBOUND_TASK_ID);
        task.setRootId(INBOUND_TASK_ID);
        task.setStatus(TaskStatus.READY);
        task.setFlow(RequestFlow.FF_CANCEL_INBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
