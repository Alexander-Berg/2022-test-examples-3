package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.ExecutorException;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.PUT_INBOUND_FF;

/**
 * Интеграционный тест для {@link PutInboundExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PutInboundIntegrationTest extends AbstractIntegrationTest {

    private final static long INBOUND_TASK_ID = 50L;

    private final static String UNIQ = "056O5sTu33EdNWdJL83lSAHpl8ptKVqc";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private PutInboundExecutor putInboundExecutor;

    @Before
    public void setup() {
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(PUT_INBOUND_FF);

        ClientTask task =
            getCreateInboundTask("fixtures/executors/put_inbound/put_inbound_task_message_success.json");

        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/common/put_inbound_request.xml",
            "fixtures/response/common/put_inbound_response.xml");

        TaskMessage message = putInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));

        assertJsonBodyMatches(
            "fixtures/executors/put_inbound/put_inbound_task_response.json",
            message.getMessageBody()
        );

        mockServer.verify();
    }

    @Test
    public void executeSuccessWithRestrictedData() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(PUT_INBOUND_FF);

        ClientTask task =
            getCreateInboundTask("fixtures/executors/put_inbound/put_inbound_restricted_task_message_success.json");

        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/common/put_inbound_restricted_request.xml",
            "fixtures/response/common/put_inbound_response.xml");

        TaskMessage message = putInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));

        assertJsonBodyMatches(
            "fixtures/executors/put_inbound/put_inbound_task_response.json",
            message.getMessageBody()
        );

        mockServer.verify();
    }

    @Test(expected = RequestValidationException.class)
    public void executeRequestFormatException() {
        ClientTask task =
            getCreateInboundTask("fixtures/executors/broken_message_with_only_partner.json");
        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);
        putInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));
    }

    @Test(expected = ExecutorException.class)
    public void executeRequestNullYandexId() {
        ClientTask task = getCreateInboundTask(
            "fixtures/executors/put_inbound/put_inbound_task_message_null_yandex_id.json"
        );
        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);
        putInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));
    }

    private ClientTask getCreateInboundTask(final String filename) {
        ClientTask task = new ClientTask();
        task.setId(INBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_INBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
