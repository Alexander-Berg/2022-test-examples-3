package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CREATE_OUTBOUND_FF;

/**
 * Интеграционный тест для {@link CreateOutboundExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateOutboundIntegrationTest extends AbstractIntegrationTest {

    private static final long OUTBOUND_TASK_ID = 50L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateOutboundExecutor createOutboundExecutor;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CREATE_OUTBOUND_FF);

        ClientTask task = getCreateOutboundTask("fixtures/executors/create_outbound/create_outbound_task_message.json");

        when(repository.findTask(eq(OUTBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/fulfillment/create_outbound/fulfillment_create_outbound.xml",
            "fixtures/response/fulfillment/create_outbound/fulfillment_create_outbound.xml");

        TaskMessage message = createOutboundExecutor.execute(new ExecutorTaskWrapper(OUTBOUND_TASK_ID, 0));

        assertJsonBodyMatches("fixtures/executors/create_outbound/create_outbound_task_response.json", message.getMessageBody());

        mockServer.verify();
    }

    private ClientTask getCreateOutboundTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(OUTBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_OUTBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
