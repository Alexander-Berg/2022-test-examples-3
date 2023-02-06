package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
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
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CREATE_INTAKE_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateIntakeIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateIntakeExecutor createIntakeExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(CREATE_INTAKE_DS);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessWithAllParameters() throws Exception {
        ClientTask task = getClientTaskWithAllParameters();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_intake/delivery_create_intake_with_all_parameters.xml",
            "fixtures/response/delivery/create_intake/delivery_create_intake_success.xml");

        TaskMessage message = createIntakeExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        softAssert.assertThat(new JsonMatcher(getFileContent("fixtures/executors/create_intake/create_intake_task_response.json")).matches(message.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();
    }

    @Test
    public void executeSuccessWithOnlyRequiredParameters() throws Exception {
        ClientTask task = getClientTaskWithOnlyRequiredParameters();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_intake/delivery_create_intake_with_only_required_parameters.xml",
            "fixtures/response/delivery/create_intake/delivery_create_intake_success.xml");

        TaskMessage message = createIntakeExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        softAssert.assertThat(new JsonMatcher(getFileContent("fixtures/executors/create_intake/create_intake_task_response.json")).matches(message.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();
    }

    private ClientTask getClientTaskWithAllParameters() throws IOException {
        return getClientTask("fixtures/executors/create_intake/create_intake_with_all_parameters_task.json");
    }

    private ClientTask getClientTaskWithOnlyRequiredParameters() throws IOException {
        return getClientTask("fixtures/executors/create_intake/create_intake_with_only_required_parameters_task.json");
    }

    private ClientTask getClientTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CREATE_INTAKE);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
