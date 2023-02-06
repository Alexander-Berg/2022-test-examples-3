package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.PUT_OUTBOUND_DOCUMENTS_FF;

/**
 * Интеграционный тест для {@link PutOutboundDocumentsExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PutOutboundDocumentsIntegrationTest extends AbstractIntegrationTest {

    private final static long TASK_ID = 50L;

    private final static String UNIQ = "6ea161f870ba6574d3bd9bdd19e1e9d8";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private PutOutboundDocumentsExecutor putOutboundDocumentsExecutor;

    @Before
    public void setup() {
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(PUT_OUTBOUND_DOCUMENTS_FF);

        ClientTask task =
            getTask("fixtures/executors/put_outbound_documents/put_outbound_documents_task_message_success.json");

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/fulfillment/put_outbound_documents/put_outbound_documents_request.xml",
            "fixtures/request/fulfillment/put_outbound_documents/put_outbound_documents_response.xml");

        TaskMessage message = putOutboundDocumentsExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        assertJsonBodyMatches(
            "fixtures/executors/put_outbound_documents/put_outbound_documents_task_response.json",
            message.getMessageBody()
        );

        mockServer.verify();
    }

    @Test(expected = ServiceInteractionRequestFormatException.class)
    public void executeRequestFormatException() {
        ClientTask task =
            getTask("fixtures/executors/broken_message_with_only_partner.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);
        putOutboundDocumentsExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    private ClientTask getTask(final String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_OUTBOUND_DOCUMENTS);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
