package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;


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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CREATE_RETURN_INBOUND_FF;

/**
 * Интеграционный тест для {@link CreateReturnInboundExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateReturnInboundIntegrationTest extends AbstractIntegrationTest {

    private final static long RETURN_INBOUND_TASK_ID = 50L;

    private final static String UNIQ = "056O5sTu33EdNWdJL83lSAHpl8ptKVqc";

    private final static String PARTNER_URL =
        "http://partner-api-mock.tst.vs.market.yandex.net:80/1P-createReturnInbound/mock/check";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateReturnInboundExecutor createReturnInboundExecutor;

    @Before
    public void setup() {
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CREATE_RETURN_INBOUND_FF);

        ClientTask task =
            getCreateInboundTask("fixtures/executors/create_return_inbound/create_return_inbound_task_message.json");

        when(repository.findTask(eq(RETURN_INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, PARTNER_URL,
            "fixtures/request/fulfillment/create_return_inbound/fulfillment_create_return_inbound.xml",
            "fixtures/response/fulfillment/create_return_inbound/fulfillment_create_return_inbound.xml");

        TaskMessage message = createReturnInboundExecutor.execute(new ExecutorTaskWrapper(RETURN_INBOUND_TASK_ID, 0));

        softAssert.assertThat(new JsonMatcher(getFileContent("fixtures/executors/create_return_inbound/create_return_inbound_task_response.json"))
            .matches(message.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();

        mockServer.verify();
    }

    @Test(expected = ServiceInteractionRequestFormatException.class)
    public void executeRequestFormatException() {
        ClientTask task =
            getCreateInboundTask("fixtures/executors/broken_message_with_only_partner.json");
        when(repository.findTask(eq(RETURN_INBOUND_TASK_ID))).thenReturn(task);
        createReturnInboundExecutor.execute(new ExecutorTaskWrapper(RETURN_INBOUND_TASK_ID, 0));
    }

    @Test(expected = ExecutorException.class)
    public void executeRequestUnknownInboundType() {
        ClientTask task =
            getCreateInboundTask("fixtures/executors/create_return_inbound/create_return_inbound_task_message_with_unknown_inbound_type.json");
        when(repository.findTask(eq(RETURN_INBOUND_TASK_ID))).thenReturn(task);
        createReturnInboundExecutor.execute(new ExecutorTaskWrapper(RETURN_INBOUND_TASK_ID, 0));
    }

    private ClientTask getCreateInboundTask(final String filename) {
        ClientTask task = new ClientTask();
        task.setId(RETURN_INBOUND_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_INBOUND);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
