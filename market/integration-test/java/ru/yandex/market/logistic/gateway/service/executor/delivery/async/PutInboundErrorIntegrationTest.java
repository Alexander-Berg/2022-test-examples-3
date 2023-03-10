package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

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
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowDeliveryConsumerClient;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static ru.yandex.market.logistic.gateway.utils.MockServerUtils.createMockRestServiceServer;

public class PutInboundErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long PUT_INBOUND_TASK_ID = 100L;

    private static final long PUT_INBOUND_PARENT_TASK_ID = 90L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private FulfillmentWorkflowDeliveryConsumerClient fulfillmentWorkflowDeliveryConsumerClient;

    @Autowired
    private PutInboundErrorExecutor putInboundErrorExecutor;

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
    public void executeError() {
        ClientTask task = getClientTask("fixtures/executors/put_inbound/put_inbound_task_message_error.json");
        ClientTask parentTask = getParentClientTask("fixtures/executors/put_inbound/put_inbound_task_message_success.json");

        when(repository.findTask(PUT_INBOUND_TASK_ID)).thenReturn(task);
        when(repository.findTask(PUT_INBOUND_PARENT_TASK_ID)).thenReturn(parentTask);

        int YANDEX_ID = 12345;
        prepareMockServerJsonScenario(mockServer, once(),
            ffWorkHost + "/requests/" + YANDEX_ID + "/reject-by-service", null);

        TaskMessage message = putInboundErrorExecutor.execute(new ExecutorTaskWrapper(PUT_INBOUND_TASK_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(fulfillmentWorkflowDeliveryConsumerClient).setPutInboundError(
            eq(String.valueOf(YANDEX_ID)),
            isNull(),
            eq(145L),
            eq(TEST_PROCESS_ID_STRING),
            eq("???????????? ?????? ?????????????????? ??????????????")
        );
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(PUT_INBOUND_TASK_ID)
            .setRootId(PUT_INBOUND_PARENT_TASK_ID)
            .setParentId(PUT_INBOUND_PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.DS_PUT_INBOUND_ERROR)
            .setProcessId(TEST_PROCESS_ID_STRING)
            .setConsumer(TaskResultConsumer.FF_WF_API)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String filename) {
        return new ClientTask()
            .setId(PUT_INBOUND_PARENT_TASK_ID)
            .setRootId(PUT_INBOUND_PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.DS_PUT_MOVEMENT)
            .setMessage(getFileContent(filename));
    }
}
