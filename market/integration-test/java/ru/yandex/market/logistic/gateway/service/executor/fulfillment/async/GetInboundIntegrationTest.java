package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_INBOUND_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetInboundIntegrationTest extends AbstractIntegrationTest {
    private static final long GET_INBOUND_TASK_ID = 100L;
    private static final String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private GetInboundExecutor getInboundExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        mockServer = createMockServerByRequest(GET_INBOUND_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() {
        ClientTask task = getClientTask("fixtures/executors/get_inbound/task_message.json");
        when(repository.findTask(eq(GET_INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/common/get_inbound/request.xml",
            "fixtures/response/fulfillment/get_inbound/response.xml"
        );

        TaskMessage message = getInboundExecutor.execute(new ExecutorTaskWrapper(GET_INBOUND_TASK_ID, 0));
        String expectedContent = getFileContent("fixtures/executors/get_inbound/task_response_FF.json");

        softAssert.assertThat(new JsonMatcher(expectedContent).matches(message.getMessageBody()))
            .as("Method response does not match expected JSON")
            .isTrue();
    }

    @Test(expected = RequestStateErrorException.class)
    public void executeFailure() {
        ClientTask task = getClientTask("fixtures/executors/get_inbound/task_message.json");
        when(repository.findTask(eq(GET_INBOUND_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/common/get_inbound/request.xml",
            "fixtures/response/fulfillment/get_inbound/response_error.xml"
        );

        getInboundExecutor.execute(new ExecutorTaskWrapper(GET_INBOUND_TASK_ID, 0));
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(GET_INBOUND_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_GET_INBOUND)
            .setMessage(getFileContent(filename));
    }
}
