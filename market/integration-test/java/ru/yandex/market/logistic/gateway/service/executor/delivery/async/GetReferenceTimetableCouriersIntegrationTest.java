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
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_REFERENCE_TIMETABLE_COURIERS_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetReferenceTimetableCouriersIntegrationTest extends AbstractIntegrationTest {
    private final static long TASK_ID = 50L;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private GetReferenceTimetableCouriersExecutor getReferenceTimetableCouriersExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws IOException {
        mockServer = createMockServerByRequest(GET_REFERENCE_TIMETABLE_COURIERS_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getClientTask("fixtures/executors/get_reference_timetable_couriers/correct_message.json");

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            GATEWAY_URL,
            "fixtures/request/delivery/get_reference_timetable_couriers/get_reference_timetable_couriers.xml",
            "fixtures/response/delivery/get_reference_timetable_couriers/get_reference_timetable_couriers.xml"
        );

        TaskMessage response = getReferenceTimetableCouriersExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
        softAssert.assertThat(
            new JsonMatcher(
                getFileContent("fixtures/executors/get_reference_timetable_couriers/correct_message_response.json")
            )
                .matches(response.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();
        mockServer.verify();
    }

    @Test(expected = RequestStateErrorException.class)
    public void executeFailed() throws IOException {
        ClientTask task = getClientTask("fixtures/executors/get_reference_timetable_couriers/correct_message.json");

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            GATEWAY_URL,
            "fixtures/request/delivery/get_reference_timetable_couriers/get_reference_timetable_couriers.xml",
            "fixtures/response/delivery/get_reference_timetable_couriers/get_reference_timetable_couriers_error.xml"
        );

        getReferenceTimetableCouriersExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_GET_REFERENCE_TIMETABLE_COURIERS);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
