package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.exceptions.ValidationException;
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
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.PUT_REFERENCE_WAREHOUSES_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PutReferenceWarehousesIntegrationTest extends AbstractIntegrationTest {
    private final static long TASK_ID = 101L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private PutReferenceWarehousesExecutor putReferenceWarehousesExecutor;

    private MockRestServiceServer mockServer;

    @Test
    public void testSuccessExecution() throws Exception {
        mockServer = createMockServerByRequest(PUT_REFERENCE_WAREHOUSES_DS);

        ClientTask task =
            getClientTask("fixtures/executors/put_reference_warehouses/correct_message.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            GATEWAY_URL,
            "fixtures/request/delivery/put_reference_warehouses/put_reference_warehouses.xml",
            "fixtures/response/delivery/put_reference_warehouses/put_reference_warehouses.xml"
        );

        TaskMessage response = putReferenceWarehousesExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        softAssert.assertThat(
            new JsonMatcher(getFileContent("fixtures/executors/put_reference_warehouses/correct_message_response.json"))
            .matches(response.getMessageBody())
        )
        .as("Method response does not matches with expected JSON")
        .isTrue();

        mockServer.verify();
    }

    @Test(expected = ValidationException.class)
    public void testExecutionWithoutWarehouses() throws Exception {
        ClientTask task = getClientTask("fixtures/executors/put_reference_warehouses/broken_message.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        putReferenceWarehousesExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();

        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_PUT_REFERENCE_WAREHOUSES);
        task.setMessage(getFileContent(filename));

        return task;
    }
}
