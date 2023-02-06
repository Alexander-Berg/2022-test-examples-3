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
 * Интеграционный тест для {@link CancelOutboundSuccessExecutor}.
 */
public class CancelOutboundSuccessIntegrationTest extends AbstractIntegrationTest {

    private final static long OUTBOUND_SUCCESS_TASK_ID = 510L;

    private final static long YANDEX_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @Value("${fulfillment.workflow.api.url}")
    protected String ffWorkHost;

    @Autowired
    private CancelOutboundSuccessExecutor cancelOutboundSuccessExecutor;

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
        ClientTask task = getCancelOutboundSuccessTask("fixtures/executors/cancel_outbound/cancel_outbound_task_response.json");

        when(repository.findTask(eq(OUTBOUND_SUCCESS_TASK_ID))).thenReturn(task);

        prepareMockServerJsonScenario(mockServer, once(),
            ffWorkHost + "/requests/" + YANDEX_ID + "/accept-cancellation", null);

        cancelOutboundSuccessExecutor.execute(new ExecutorTaskWrapper(OUTBOUND_SUCCESS_TASK_ID, 0));
    }

    private ClientTask getCancelOutboundSuccessTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(OUTBOUND_SUCCESS_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CANCEL_OUTBOUND_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
