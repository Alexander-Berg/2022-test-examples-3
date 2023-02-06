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
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CREATE_TRANSFER_FF;

/**
 * Интеграционный тест для {@link CreateTransferExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateTransferIntegrationTest extends AbstractIntegrationTest {

    private final static long TRANSFERS_TASK_ID = 50L;

    private final static String PARTNER_URL = "https://localhost/query-gateway";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateTransferExecutor createTransferExecutor;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CREATE_TRANSFER_FF);

        ClientTask task = getCreateTransferTask("fixtures/executors/create_transfer/create_transfer_task_message.json");

        when(repository.findTask(eq(TRANSFERS_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, PARTNER_URL,
            "fixtures/request/fulfillment/create_transfer/fulfillment_create_transfer.xml",
            "fixtures/response/fulfillment/create_transfer/fulfillment_create_transfer.xml");

        TaskMessage message = createTransferExecutor.execute(new ExecutorTaskWrapper(TRANSFERS_TASK_ID, 0));

        assertJsonBodyMatches("fixtures/executors/create_transfer/create_transfer_task_response.json", message.getMessageBody());

        mockServer.verify();
    }

    private ClientTask getCreateTransferTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(TRANSFERS_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_TRANSFER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
