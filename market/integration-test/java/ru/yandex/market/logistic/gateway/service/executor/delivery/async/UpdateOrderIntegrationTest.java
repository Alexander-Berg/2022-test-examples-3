package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class UpdateOrderIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    private final static long TEST_PARTNER_ID = 145L;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private UpdateOrderExecutor updateOrderExecutor;

    @Before
    public void setup() throws IOException {
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    public void executeSuccess() throws Exception {

        MockRestServiceServer mockServer = createMockServerByRequest(PartnerMethod.UPDATE_ORDER_DS);

        ClientTask task = getClientTask("fixtures/executors/update_order/update_order_task_message.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/update_order/delivery_update_order.xml",
            "fixtures/response/delivery/update_order/delivery_update_order.xml");

        TaskMessage message = updateOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        JSONAssert.assertEquals(
            getFileContent("fixtures/executors/update_order/update_order_task_response.json"),
            message.getMessageBody(),
            true
        );
        mockServer.verify();
    }

    private ClientTask getClientTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_UPDATE_ORDER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
