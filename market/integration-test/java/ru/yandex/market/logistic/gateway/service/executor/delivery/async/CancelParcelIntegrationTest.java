package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionRequestFormatException;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CANCEL_ORDER_DS;

/**
 * Интеграционный тест для {@link CancelParcelExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CancelParcelIntegrationTest extends AbstractIntegrationTest {

    private final static long CANCEL_PARCEL_TASK_ID = 60L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CancelParcelExecutor cancelParcelExecutor;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CANCEL_ORDER_DS);

        ClientTask task =
            getCancelParcelTask("fixtures/executors/cancel_parcel/cancel_parcel_task_message.json");

        when(repository.findTask(eq(CANCEL_PARCEL_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/delivery/cancel_parcel/delivery_cancel_parcel.xml",
            "fixtures/response/delivery/cancel_parcel/delivery_cancel_parcel.xml");

        TaskMessage response = cancelParcelExecutor.execute(new ExecutorTaskWrapper(CANCEL_PARCEL_TASK_ID, 0));
        softAssert.assertThat(new JsonMatcher(getFileContent("fixtures/executors/cancel_parcel/cancel_parcel_task_response.json"))
            .matches(response.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();
        mockServer.verify();
    }

    @Test(expected = ServiceInteractionRequestFormatException.class)
    public void executeRequestFormatException() throws Exception {
        ClientTask task =
            getCancelParcelTask("fixtures/executors/broken_message_with_only_partner.json");
        when(repository.findTask(eq(CANCEL_PARCEL_TASK_ID))).thenReturn(task);
        cancelParcelExecutor.execute(new ExecutorTaskWrapper(CANCEL_PARCEL_TASK_ID, 0));
    }

    private ClientTask getCancelParcelTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(CANCEL_PARCEL_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CANCEL_PARCEL);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
