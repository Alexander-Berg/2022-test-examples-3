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
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.PUT_TRIP_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PutTripIntegrationTest  extends AbstractIntegrationTest {

    private static final long TEST_TRIP_TASK_ID = 100L;

    private static final String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private PutTripExecutor putTripExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(PUT_TRIP_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getClientTask();
        when(repository.findTask(eq(TEST_TRIP_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/put_trip/request.xml",
            "fixtures/response/delivery/put_trip/response.xml"
        );

        TaskMessage message = putTripExecutor.execute(new ExecutorTaskWrapper(TEST_TRIP_TASK_ID, 0));
        checkTaskMessage(message);
    }

    private ClientTask getClientTask() {
        return getClientTask("fixtures/executors/put_trip/put_trip_task_message.json");
    }

    private void checkTaskMessage(TaskMessage message) {
        softAssert.assertThat(
                new JsonMatcher(getFileContent("fixtures/executors/put_trip/put_trip_task_response.json"))
                    .matches(message.getMessageBody())
            )
            .as("Method response does not matches with expected JSON")
            .isTrue();
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TEST_TRIP_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.DS_PUT_TRIP)
            .setMessage(getFileContent(filename));
    }

}
