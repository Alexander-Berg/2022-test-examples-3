package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetOrderIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    private final static String UNIQ = "6ea161f870ba6574d3bd9bdd19e1e9d8";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private GetOrderExecutor executor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(PartnerMethod.GET_ORDER_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void testExecuteGetOrderSuccessfully() {
        executeAndAssertGetOrderSuccessfully(StringUtils.EMPTY);
    }

    @Test
    public void testExecuteGetOrderWithOnlyOrderIdAndPlacesSuccessfully() {
        executeAndAssertGetOrderSuccessfully("_with_only_order_id_and_places");
    }

    @Test
    public void testExecuteGetOrderWithInvalidUndefinedCount() {
        prepareGetOrderExecution("_with_invalid_undefined_count");
        assertThatThrownBy(() -> executor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining(
                "interpolatedMessage='must be greater than or equal to 0', propertyPath=order.items[1].undefinedCount"
            );
    }

    @Test(expected = RequestStateErrorException.class)
    public void testExecuteGetOrderWithError() {
        prepareGetOrderExecution("_error");
        executor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));
    }

    private void prepareGetOrderExecution(String responseFilePostfix) {
        ClientTask task = getClientTask("fixtures/executors/get_order/fulfillment_get_order_task_message.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_order/fulfillment_get_order.xml",
            "fixtures/response/fulfillment/get_order/fulfillment_get_order" + responseFilePostfix + ".xml");
    }

    private void executeAndAssertGetOrderSuccessfully(String responseFilePostfix) {
        prepareGetOrderExecution(responseFilePostfix);
        TaskMessage message = executor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));
        assertJsonBodyMatches("fixtures/executors/get_order/" +
                "fulfillment_get_order_success_task_message" + responseFilePostfix + ".json",
            message.getMessageBody());
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_GET_ORDER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
