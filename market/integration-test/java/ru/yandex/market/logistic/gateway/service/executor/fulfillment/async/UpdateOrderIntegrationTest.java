package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.UPDATE_ORDER_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class UpdateOrderIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    private final static String UNIQ = "6ea161f870ba6574d3bd9bdd19e1e9d8";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private UpdateOrderExecutor updateOrderExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(UPDATE_ORDER_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessTaskUpdateOrderExecutor() throws Exception {
        ClientTask task = getClientTask("fixtures/executors/fulfillment_create_order/fulfillment_update_order_task_message.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/update_order/fulfillment_update_order.xml",
            "fixtures/response/fulfillment/update_order/fulfillment_update_order.xml");

        TaskMessage message = updateOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        assertJsonBodyMatches("fixtures/executors/fulfillment_create_order/fulfillment_update_order_task_response.json", message.getMessageBody());
    }

    private ClientTask getClientTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_UPDATE_ORDER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
