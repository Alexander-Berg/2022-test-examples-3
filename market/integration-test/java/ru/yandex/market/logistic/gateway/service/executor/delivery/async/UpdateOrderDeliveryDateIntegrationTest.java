package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class UpdateOrderDeliveryDateIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private UpdateOrderDeliveryDateExecutor updateOrderExecutor;

    @Test
    public void executeWithEmptyUpdateRequestIdSuccess() {
        executeSuccess(
            "fixtures/executors/update_order/update_order_delivery_date_with_empty_update_request_id_task_message.json",
            "fixtures/executors/update_order/update_order_delivery_date_with_empty_update_request_id_task_response.json"
        );
    }

    @Test
    public void executeSuccess() throws IOException {
        executeSuccess(
            "fixtures/executors/update_order/update_order_delivery_date_task_message.json",
            "fixtures/executors/update_order/update_order_delivery_date_task_response.json"
        );
    }

    private void executeSuccess(String taskBodyFilePath, String expectedResponseBodyFilePath) {
        prepareTaskMock(taskBodyFilePath);
        MockRestServiceServer mockServer = prepareDsResponseMock("delivery_update_order_delivery_date.xml");

        TaskMessage message = updateOrderExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        assertThatJson(message.getMessageBody())
            .isEqualTo(getFileContent(expectedResponseBodyFilePath));

        mockServer.verify();
    }

    private void prepareTaskMock(String taskBodyFilePath) {
        ClientTask task = getClientTask(taskBodyFilePath);
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);
        when(repository.save(any(ClientTask.class))).thenReturn(task);
    }

    private MockRestServiceServer prepareDsResponseMock(String responseFilePath) {
        MockRestServiceServer mockServer = createMockServerByRequest(PartnerMethod.UPDATE_ORDER_DELIVERY_DATE_DS);
        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/update_order_delivery_date/delivery_update_order_delivery_date.xml",
            "fixtures/response/delivery/update_order_delivery_date/" + responseFilePath);

        return mockServer;
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setRootId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.NEW);
        task.setFlow(RequestFlow.DS_UPDATE_ORDER_DELIVERY_DATE);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
