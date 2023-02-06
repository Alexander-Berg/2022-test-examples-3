package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateIntakeIntegrationTest extends AbstractIntegrationTest {
    private final static long TEST_INTAKE_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateIntakeExecutor createIntakeExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(PartnerMethod.CREATE_INTAKE_FF);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessWithAllParameters() throws Exception {
        when(repository.findTask(eq(TEST_INTAKE_ID))).thenReturn(createClientTaskWithAllParameters());

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/create_intake/with_all_parameters.xml",
            "fixtures/response/fulfillment/create_intake/success.xml");

        TaskMessage message = createIntakeExecutor.execute(new ExecutorTaskWrapper(TEST_INTAKE_ID, 0));

        softAssert.assertThat(
            new JsonMatcher(getFileContent("fixtures/executors/fulfillment/create_intake/task_response.json"))
                .matches(message.getMessageBody())
        )
            .as("Asserting that JSON response is correct")
            .isTrue();
    }

    @Test
    public void executeSuccessWithOnlyRequiredParameters() throws Exception {
        when(repository.findTask(eq(TEST_INTAKE_ID))).thenReturn(createClientTaskWithOnlyRequiredParameters());

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/create_intake/with_only_required_parameters.xml",
            "fixtures/response/fulfillment/create_intake/success.xml");

        TaskMessage message = createIntakeExecutor.execute(new ExecutorTaskWrapper(TEST_INTAKE_ID, 0));
        assertJsonBodyMatches("fixtures/executors/fulfillment/create_intake/task_response.json", message.getMessageBody());
    }

    private ClientTask createClientTaskWithAllParameters() {
        return createClientTask("fixtures/executors/fulfillment/create_intake/with_all_parameters.json");
    }

    private ClientTask createClientTaskWithOnlyRequiredParameters() {
        return createClientTask("fixtures/executors/fulfillment/create_intake/with_only_required_parameters.json");
    }

    private ClientTask createClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_INTAKE_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_INTAKE);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
