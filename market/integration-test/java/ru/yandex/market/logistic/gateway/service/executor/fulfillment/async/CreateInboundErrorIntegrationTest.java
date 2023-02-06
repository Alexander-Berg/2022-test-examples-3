package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.util.probe.FulfillmentWorkflowMethodCallLoggingAspect;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static ru.yandex.market.logistic.gateway.utils.MockServerUtils.createMockRestServiceServer;

/**
 * Интеграционный тест для {@link CreateInboundErrorExecutor}.
 */
public class CreateInboundErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long INBOUND_ERROR_TASK_ID = 726L;

    private final static long INBOUND_TASK_ID = 543L;

    private final static long YANDEX_ID = 23456L;

    @MockBean
    private ClientTaskRepository repository;

    @Value("${fulfillment.workflow.api.url}")
    protected String ffWorkHost;

    @Autowired
    private CreateInboundErrorExecutor createInboundErrorExecutor;

    private MockRestServiceServer mockServer;

    @Autowired
    private HttpTemplateImpl fulfillmentHttpTemplate;

    @SpyBean
    private FulfillmentWorkflowMethodCallLoggingAspect aspect;

    @Before
    public void setup() {
        mockServer = createMockRestServiceServer(fulfillmentHttpTemplate.getRestTemplate());
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeError() throws Throwable {
        ClientTask task =
            getCreateInboundErrorTask("fixtures/executors/error_task_message.json");

        ClientTask parentTask =
            getCreateInboundTask("fixtures/executors/create_inbound/create_inbound_task_message.json");

        when(repository.findTask(eq(INBOUND_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(parentTask);

        prepareMockServerJsonScenario(mockServer, once(),
            ffWorkHost + "/requests/" + YANDEX_ID + "/reject-by-service", null);

        createInboundErrorExecutor.execute(new ExecutorTaskWrapper(INBOUND_ERROR_TASK_ID, 0));

        verify(aspect).executeWithLogging(any(ProceedingJoinPoint.class));
    }

    private ClientTask getCreateInboundErrorTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(INBOUND_ERROR_TASK_ID);
        task.setRootId(INBOUND_TASK_ID);
        task.setParentId(INBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_INBOUND_ERROR);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }

    private ClientTask getCreateInboundTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(INBOUND_TASK_ID);
        task.setRootId(INBOUND_TASK_ID);
        task.setStatus(TaskStatus.READY);
        task.setFlow(RequestFlow.FF_CREATE_INBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
