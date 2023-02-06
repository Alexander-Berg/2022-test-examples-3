package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.common.PartnerMethod;
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

@DatabaseSetup("/repository/state/partners_properties.xml")
public class PutRegisterIntegrationTest extends AbstractIntegrationTest {
    private static final long PUT_REGISTER_TASK_ID = 1;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private PutRegisterExecutor putRegisterExecutor;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(PartnerMethod.PUT_REGISTER_FF);

        ClientTask task =
            getPutRegisterTask("fixtures/executors/fulfillment_put_register/put_register_request.json");

        when(repository.findTask(eq(PUT_REGISTER_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/put_register/fulfillment_put_register.xml",
            "fixtures/response/fulfillment/put_register/fulfillment_put_register.xml");

        TaskMessage message = putRegisterExecutor.execute(new ExecutorTaskWrapper(PUT_REGISTER_TASK_ID, 0));

        softAssert.assertThat(
            new JsonMatcher(getFileContent("fixtures/executors/fulfillment_put_register/put_register_response.json"))
                .matches(message.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();

        mockServer.verify();
    }

    @Test(expected = RequestStateErrorException.class)
    public void executeFailed() throws IOException {
        MockRestServiceServer mockServer = createMockServerByRequest(PartnerMethod.PUT_REGISTER_FF);

        ClientTask task =
            getPutRegisterTask("fixtures/executors/fulfillment_put_register/put_register_request.json");

        when(repository.findTask(eq(PUT_REGISTER_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/put_register/fulfillment_put_register.xml",
            "fixtures/response/fulfillment/put_register/fulfillment_put_register_error.xml");

        putRegisterExecutor.execute(new ExecutorTaskWrapper(PUT_REGISTER_TASK_ID, 0));
    }

    private ClientTask getPutRegisterTask(final String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(PUT_REGISTER_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_REGISTER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
