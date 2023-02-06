package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CANCEL_ORDER_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CancelOrderIntegrationTest extends AbstractIntegrationTest {

    private final static long ORDER_TASK_ID = 50L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CancelOrderExecutor cancelOrderExecutor;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CANCEL_ORDER_FF);

        ClientTask task = getCancelOrderTask("fixtures/executors/cancel_order/cancel_order_task_message.json");

        when(repository.findTask(eq(ORDER_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/fulfillment/cancel_order/fulfillment_cancel_order.xml",
            "fixtures/response/fulfillment/cancel_order/fulfillment_cancel_order.xml");

        TaskMessage response = cancelOrderExecutor.execute(new ExecutorTaskWrapper(ORDER_TASK_ID, 0));
        assertJsonBodyMatches("fixtures/executors/cancel_order/cancel_order_task_response.json", response.getMessageBody());
        mockServer.verify();
    }

    @Test(expected = ServiceInteractionRequestFormatException.class)
    public void executeRequestFormatException() throws Exception {
        ClientTask task = getCancelOrderTask("fixtures/executors/broken_message_with_only_partner.json");
        when(repository.findTask(eq(ORDER_TASK_ID))).thenReturn(task);
        cancelOrderExecutor.execute(new ExecutorTaskWrapper(ORDER_TASK_ID, 0));
    }

    private ClientTask getCancelOrderTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(ORDER_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CANCEL_ORDER);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
