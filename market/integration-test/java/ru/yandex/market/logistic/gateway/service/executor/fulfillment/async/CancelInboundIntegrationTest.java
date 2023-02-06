package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionRequestFormatException;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CANCEL_INBOUND_FF;

/**
 * Интеграционный тест для {@link CancelInboundExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CancelInboundIntegrationTest extends AbstractIntegrationTest {

    private final static long INBOUND_TASK_ID = 50L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CancelInboundExecutor cancelInboundExecutor;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CANCEL_INBOUND_FF);

        ClientTask task =
            getCancelInboundTask("fixtures/executors/cancel_inbound/cancel_inbound_task_message.json");

        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/fulfillment/cancel_inbound/fulfillment_cancel_inbound.xml",
            "fixtures/response/fulfillment/cancel_inbound/fulfillment_cancel_inbound.xml");

        TaskMessage response = cancelInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));
        assertJsonBodyMatches("fixtures/executors/cancel_inbound/cancel_inbound_task_response.json", response.getMessageBody());
        mockServer.verify();
    }

    @Test(expected = ServiceInteractionRequestFormatException.class)
    public void executeRequestFormatException() throws Exception {
        ClientTask task =
            getCancelInboundTask("fixtures/executors/broken_message_with_only_partner.json");
        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);
        cancelInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));
    }

    private ClientTask getCancelInboundTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(INBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CANCEL_INBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
