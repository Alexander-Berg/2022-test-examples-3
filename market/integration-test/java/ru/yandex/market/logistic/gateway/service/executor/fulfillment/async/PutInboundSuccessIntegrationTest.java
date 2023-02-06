package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

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
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowConsumerClient;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static ru.yandex.market.logistic.gateway.utils.MockServerUtils.createMockRestServiceServer;

public class PutInboundSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_INBOUND_TASK_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @Autowired
    private PutInboundSuccessExecutor putInboundSuccessExecutor;

    @Value("${fulfillment.workflow.api.url}")
    protected String ffWorkHost;

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
    public void executeSuccess() {
        ClientTask task = getClientTask();
        when(repository.findTask(eq(TEST_INBOUND_TASK_ID))).thenReturn(task);

        int YANDEX_ID = 12345;
        prepareMockServerJsonScenario(mockServer, once(),
            ffWorkHost + "/requests/" + YANDEX_ID + "/accept-by-service",
            "fixtures/request/common/put_inbound_success.json", null);

        TaskMessage message = putInboundSuccessExecutor.execute(new ExecutorTaskWrapper(TEST_INBOUND_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowConsumerClient).setPutInboundSuccess(
            eq("12345"),
            eq("inbound_partner_id"),
            eq(145L),
            eq(TEST_PROCESS_ID_STRING)
        );
    }

    private ClientTask getClientTask() {
        return getClientTask("fixtures/executors/put_inbound/put_inbound_task_response.json");
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_INBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_INBOUND_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(TEST_PROCESS_ID_STRING);
        task.setConsumer(TaskResultConsumer.FF_WF_API);
        return task;
    }
}
