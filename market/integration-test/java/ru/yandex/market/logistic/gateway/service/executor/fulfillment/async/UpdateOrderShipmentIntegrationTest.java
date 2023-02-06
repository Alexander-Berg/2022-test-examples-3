package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import javax.annotation.Nonnull;

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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.UPDATE_ORDER_SHIPMENT_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class UpdateOrderShipmentIntegrationTest extends AbstractIntegrationTest {
    private final static long TEST_ORDER_ID = 100L;

    private final static String UNIQ = "6ea161f870ba6574d3bd9bdd19e1e9d8";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private UpdateOrderShipmentExecutor updateOrderShipmentExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(UPDATE_ORDER_SHIPMENT_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void execute() {
        when(repository.findTask(TEST_ORDER_ID)).thenReturn(task());

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/update_order_shipment/fulfillment_update_order_shipment.xml",
            "fixtures/response/fulfillment/update_order_shipment/fulfillment_update_order_shipment.xml"
        );

        TaskMessage message = updateOrderShipmentExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        assertJsonBodyMatches(
            "fixtures/executors/fulfillment_update_order_shipment/task_response.json",
            message.getMessageBody()
        );

        verify(repository).findTask(TEST_ORDER_ID);
    }

    @Nonnull
    private ClientTask task() {
        return new ClientTask()
            .setId(TEST_ORDER_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_UPDATE_ORDER_SHIPMENT)
            .setMessage(getFileContent("fixtures/executors/fulfillment_update_order_shipment/task_message.json"));
    }
}
