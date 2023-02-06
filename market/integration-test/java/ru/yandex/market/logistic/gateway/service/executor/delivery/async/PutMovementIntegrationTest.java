package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.PUT_MOVEMENT_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PutMovementIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_MOVEMENT_TASK_ID = 100L;

    private static final String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private PutMovementExecutor putMovementExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(PUT_MOVEMENT_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getClientTask();
        when(repository.findTask(eq(TEST_MOVEMENT_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/common/put_movement/request.xml",
            "fixtures/response/common/put_movement/response.xml"
        );

        TaskMessage message = putMovementExecutor.execute(new ExecutorTaskWrapper(TEST_MOVEMENT_TASK_ID, 0));
        checkTaskMessage(message);
    }

    @Test
    public void executeWithExtraFields() throws Exception {
        ClientTask task = getClientTask(
            "fixtures/executors/put_movement/put_movement_with_extra_fields_task_message.json"
        );
        when(repository.findTask(eq(TEST_MOVEMENT_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/common/put_movement/request_with_extra_fields.xml",
            "fixtures/response/common/put_movement/response.xml"
        );

        TaskMessage message = putMovementExecutor.execute(new ExecutorTaskWrapper(TEST_MOVEMENT_TASK_ID, 0));
        checkTaskMessage(message);
    }

    private ClientTask getClientTask() {
        return getClientTask("fixtures/executors/put_movement/put_movement_task_message.json");
    }

    private void checkTaskMessage(TaskMessage message) {
        softAssert.assertThat(
                new JsonMatcher(getFileContent("fixtures/executors/put_movement/put_movement_task_response.json"))
                    .matches(message.getMessageBody())
            )
            .as("Method response does not matches with expected JSON")
            .isTrue();
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TEST_MOVEMENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.DS_PUT_MOVEMENT)
            .setMessage(getFileContent(filename));
    }

}
