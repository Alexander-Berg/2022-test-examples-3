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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateReturnRegisterIntegrationTest extends AbstractIntegrationTest {
    private final static long TEST_REGISTER_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateReturnRegisterExecutor createReturnRegisterExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(PartnerMethod.CREATE_RETURN_REGISTER_FF);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessWithAllParameters() throws Exception {
        ClientTask task = getClientTaskWithAllParameters();
        when(repository.findTask(eq(TEST_REGISTER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/create_return_register/create_return_register.xml",
            "fixtures/response/fulfillment/create_return_register/success.xml");

        TaskMessage message = createReturnRegisterExecutor.execute(new ExecutorTaskWrapper(TEST_REGISTER_ID, 0));

        assertJsonBodyMatches("fixtures/executors/fulfillment_create_return_register/task_response.json", message.getMessageBody());
    }

    private ClientTask getClientTaskWithAllParameters() {
        return getClientTask("fixtures/executors/fulfillment_create_return_register/with_all_parameters.json");
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_REGISTER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_RETURN_REGISTER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
