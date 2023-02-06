package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.ExpectedCount;
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
import static ru.yandex.market.logistic.gateway.utils.MockServerUtils.createMockRestServiceServer;

/**
 * Интеграционный тест для {@link CreateOutboundErrorExecutor}.
 */
public class
CreateOutboundErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long OUTBOUND_TASK_ID = 50L;

    private static final long OUTBOUND_ERROR_TASK_ID = 51L;

    private static final long YANDEX_ID = 10708L;

    @MockBean
    private ClientTaskRepository repository;

    @Value("${fulfillment.workflow.api.url}")
    private String ffWorkHost;

    @Autowired
    private CreateOutboundErrorExecutor createOutboundErrorExecutor;

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
    public void executeSuccess() throws Exception {
        ClientTask task = getCreateOutboundErrorTask("fixtures/executors/error_task_message.json");
        ClientTask parentTask = getCreateOutboundTask("fixtures/executors/create_outbound/create_outbound_task_message.json");

        when(repository.findTask(eq(OUTBOUND_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(OUTBOUND_TASK_ID))).thenReturn(parentTask);

        prepareMockServerJsonScenario(mockServer, ExpectedCount.once(),
            ffWorkHost + "/requests/" + YANDEX_ID + "/reject-by-service", null);

        createOutboundErrorExecutor.execute(new ExecutorTaskWrapper(OUTBOUND_ERROR_TASK_ID, 0));
    }

    private ClientTask getCreateOutboundTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(OUTBOUND_TASK_ID);
        task.setRootId(OUTBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_OUTBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }

    private ClientTask getCreateOutboundErrorTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(OUTBOUND_ERROR_TASK_ID);
        task.setRootId(OUTBOUND_TASK_ID);
        task.setParentId(OUTBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_OUTBOUND_ERROR);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
