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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.UPDATE_ORDER_ITEMS_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class UpdateOrderItemsIntegrationTest extends AbstractIntegrationTest {

    private final static long ORDER_TASK_ID = 50L;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private UpdateOrderItemsExecutor updateOrderItemsExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws IOException {
        mockServer = createMockServerByRequest(UPDATE_ORDER_ITEMS_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getClientTask("fixtures/executors/update_order/update_order_items_task_message.json");

        when(repository.findTask(eq(ORDER_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            GATEWAY_URL,
            "fixtures/request/delivery/update_order_items/delivery_update_order_items.xml",
            "fixtures/response/delivery/update_order_items/delivery_update_order_items.xml");

        TaskMessage response = updateOrderItemsExecutor.execute(new ExecutorTaskWrapper(ORDER_TASK_ID, 0));
        softAssert.assertThat(
            new JsonMatcher(
                getFileContent("fixtures/executors/update_order/update_order_items_task_response.json")
            )
            .matches(response.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();
        mockServer.verify();
    }

    @Test(expected = RequestStateErrorException.class)
    public void executeFailed() throws IOException {
        MockRestServiceServer mockServer = createMockServerByRequest(UPDATE_ORDER_ITEMS_DS);

        ClientTask task = getClientTask("fixtures/executors/update_order/update_order_items_task_message.json");

        when(repository.findTask(eq(ORDER_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/update_order_items/delivery_update_order_items.xml",
            "fixtures/response/delivery/update_order_items/delivery_update_order_items_error.xml"
        );

        updateOrderItemsExecutor.execute(new ExecutorTaskWrapper(ORDER_TASK_ID, 0));
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(ORDER_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CANCEL_ORDER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
