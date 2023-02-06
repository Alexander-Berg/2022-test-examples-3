package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
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
public class UpdateCourierIntegrationTest extends AbstractIntegrationTest {
    private final static long TASK_ID = 50L;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private UpdateCourierExecutor updateCourierExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws IOException {
        mockServer = createMockServerByRequest(PartnerMethod.UPDATE_COURIER_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getClientTask(
            "fixtures/executors/update_courier/update_courier_ds.json"
        );

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            GATEWAY_URL,
            "fixtures/request/delivery/update_courier/update_courier.xml",
            "fixtures/response/delivery/update_courier/update_courier.xml"
        );

        TaskMessage response = updateCourierExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
        softAssert.assertThat(
            new JsonMatcher(
                getFileContent("fixtures/executors/update_courier/update_courier_message_response_ds.json")
            )
                .matches(response.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();
        mockServer.verify();
    }

    @Test(expected = RequestStateErrorException.class)
    public void executeFailed() throws IOException {
        ClientTask task = getClientTask(
            "fixtures/executors/update_courier/update_courier_ds.json"
        );

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            GATEWAY_URL,
            "fixtures/request/delivery/update_courier/update_courier.xml",
            "fixtures/response/delivery/update_courier/update_courier_error.xml"
        );

        updateCourierExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    @Test(expected = RequestValidationException.class)
    public void executeValidationFailed() throws IOException {
        ClientTask task = getClientTask(
            "fixtures/executors/update_courier/update_courier_nulls_ds.json"
        );

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        updateCourierExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_UPDATE_COURIER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
