package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.ExecutorException;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionRequestFormatException;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CREATE_INBOUND_FF;

/**
 * Интеграционный тест для {@link CreateInboundExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateInboundIntegrationTest extends AbstractIntegrationTest {

    private final static long INBOUND_TASK_ID = 50L;

    private final static String UNIQ = "056O5sTu33EdNWdJL83lSAHpl8ptKVqc";

    private final static String PARTNER_URL =
        "http://partner-api-mock.tst.vs.market.yandex.net:80/1P-createInbound/mock/check";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateInboundExecutor createInboundExecutor;

    @Before
    public void setup() {
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CREATE_INBOUND_FF);

        ClientTask task =
            getCreateInboundTask("fixtures/executors/create_inbound/create_inbound_task_message.json");

        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, PARTNER_URL,
            "fixtures/request/fulfillment/create_inbound/fulfillment_create_inbound.xml",
            "fixtures/response/fulfillment/create_inbound/fulfillment_create_inbound.xml");

        TaskMessage message = createInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));

        softAssert.assertThat(new JsonMatcher(getFileContent("fixtures/executors/create_inbound/create_inbound_task_response.json"))
            .matches(message.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();

        mockServer.verify();
    }

    @Test
    public void executeSuccessWithoutInboundType() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CREATE_INBOUND_FF);

        ClientTask task =
            getCreateInboundTask("fixtures/executors/create_inbound/create_inbound_task_message_without_inbound_type.json");

        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, PARTNER_URL,
            "fixtures/request/fulfillment/create_inbound/fulfillment_create_inbound_default_inbound_type.xml",
            "fixtures/response/fulfillment/create_inbound/fulfillment_create_inbound.xml");

        TaskMessage message = createInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));
        assertJsonBodyMatches("fixtures/executors/create_inbound/create_inbound_task_response.json", message.getMessageBody());
        mockServer.verify();
    }

    @Test
    public void executeSuccessWithUnknownCargoType() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CREATE_INBOUND_FF);

        ClientTask task = getCreateInboundTask(
                "fixtures/executors/create_inbound/create_inbound_with_unknown_cargo_type_task_message.json");

        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, PARTNER_URL,
                "fixtures/request/fulfillment/create_inbound/fulfillment_create_inbound_with_unknown_cargo_type.xml",
                "fixtures/response/fulfillment/create_inbound/fulfillment_create_inbound.xml");

        TaskMessage message = createInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));

        softAssert.assertThat(new JsonMatcher(getFileContent("fixtures/executors/create_inbound/create_inbound_task_response.json"))
                .matches(message.getMessageBody()))
                .as("Asserting that JSON response is correct")
                .isTrue();

        mockServer.verify();
    }

    @Test(expected = ServiceInteractionRequestFormatException.class)
    public void executeRequestFormatException() throws Exception {
        ClientTask task =
            getCreateInboundTask("fixtures/executors/broken_message_with_only_partner.json");
        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);
        createInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));
    }

    @Test(expected = ExecutorException.class)
    public void executeRequestUnknownInboundType() throws Exception {
        ClientTask task =
            getCreateInboundTask("fixtures/executors/create_inbound/create_inbound_task_message_with_unknown_inbound_type.json");
        when(repository.findTask(eq(INBOUND_TASK_ID))).thenReturn(task);
        createInboundExecutor.execute(new ExecutorTaskWrapper(INBOUND_TASK_ID, 0));
    }

    private ClientTask getCreateInboundTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(INBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_INBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
