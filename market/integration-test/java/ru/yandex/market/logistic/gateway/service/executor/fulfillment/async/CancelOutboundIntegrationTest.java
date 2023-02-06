package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

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
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CANCEL_OUTBOUND_FF;

/**
 * Интеграционный тест для {@link CancelOutboundExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CancelOutboundIntegrationTest extends AbstractIntegrationTest {

    private final static long OUTBOUND_TASK_ID = 50L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CancelOutboundExecutor cancelOutboundExecutor;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CANCEL_OUTBOUND_FF);

        ClientTask task = getCancelOutboundTask("fixtures/executors/cancel_outbound/cancel_outbound_task_message.json");

        when(repository.findTask(eq(OUTBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/fulfillment/cancel_outbound/fulfillment_cancel_outbound.xml",
            "fixtures/response/fulfillment/cancel_outbound/fulfillment_cancel_outbound.xml");

        TaskMessage response = cancelOutboundExecutor.execute(new ExecutorTaskWrapper(OUTBOUND_TASK_ID, 0));
        assertJsonBodyMatches("fixtures/executors/cancel_outbound/cancel_outbound_task_response.json", response.getMessageBody());
        mockServer.verify();
    }

    @Test(expected = ServiceInteractionRequestFormatException.class)
    public void executeRequestFormatException() throws Exception {
        ClientTask task = getCancelOutboundTask("fixtures/executors/broken_message_with_only_partner.json");
        when(repository.findTask(eq(OUTBOUND_TASK_ID))).thenReturn(task);
        cancelOutboundExecutor.execute(new ExecutorTaskWrapper(OUTBOUND_TASK_ID, 0));
    }

    private ClientTask getCancelOutboundTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(OUTBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CANCEL_OUTBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
