package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static ru.yandex.market.logistic.gateway.utils.MockServerUtils.createMockRestServiceServer;

/**
 * Интеграционный тест для {@link CreateReturnInboundSuccessExecutor}
 */
public class CreateReturnInboundSuccessIntegrationTest extends AbstractIntegrationTest {

    private final static long RETURN_INBOUND_SUCCESS_TASK_ID = 564L;

    private final static long YANDEX_ID = 101L;

    @MockBean
    private ClientTaskRepository repository;

    @Value("${fulfillment.workflow.api.url}")
    protected String ffWorkHost;

    @Autowired
    private CreateReturnInboundSuccessExecutor createReturnInboundSuccessExecutor;

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
    public void executeSuccess() throws Throwable {
        ClientTask task = getCreateInboundSuccessTask("fixtures/executors/create_return_inbound/create_return_inbound_executor_response.json");

        when(repository.findTask(eq(RETURN_INBOUND_SUCCESS_TASK_ID))).thenReturn(task);

        prepareMockServerJsonScenario(mockServer, once(),
            ffWorkHost + "/requests/" + YANDEX_ID + "/accept-by-service",
            "fixtures/request/fulfillment/create_return_inbound/fulfillment_create_return_inbound_success.json", null);

        createReturnInboundSuccessExecutor.execute(new ExecutorTaskWrapper(RETURN_INBOUND_SUCCESS_TASK_ID, 0));

        verify(aspect).executeWithLogging(any(ProceedingJoinPoint.class));
    }

    private ClientTask getCreateInboundSuccessTask(final String filename) {
        ClientTask task = new ClientTask();
        task.setId(RETURN_INBOUND_SUCCESS_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_RETURN_INBOUND_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
