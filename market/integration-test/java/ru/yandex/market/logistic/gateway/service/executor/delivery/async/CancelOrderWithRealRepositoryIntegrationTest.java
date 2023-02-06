package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CANCEL_ORDER_DS;


@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CancelOrderWithRealRepositoryIntegrationTest extends AbstractIntegrationTest {

    private final static long ORDER_TASK_ID = 50L;

    @Autowired
    private CancelOrderExecutor cancelOrderExecutor;

    @Test
    @DatabaseSetup("classpath:repository/state/execution-after-retry-for-cancel-order.xml")
    public void retryOfCancelOrderFlow() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(CANCEL_ORDER_DS);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/delivery/cancel_order/delivery_cancel_order.xml",
            "fixtures/response/delivery/cancel_order/delivery_cancel_order.xml");

        TaskMessage response = cancelOrderExecutor.execute(new ExecutorTaskWrapper(ORDER_TASK_ID, 0));
        softAssert.assertThat(new JsonMatcher(getFileContent("fixtures/executors/cancel_order/cancel_order_task_response.json"))
            .matches(response.getMessageBody()))
            .as("Asserting that JSON response is correct")
            .isTrue();
        mockServer.verify();
    }
}
