package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.ItemPlace;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.Korobyte;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.Place;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.UnitId;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrderResult;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetOrderSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TEST_ORDER_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private MdbClient mdbClient;

    @Autowired
    private GetOrderSuccessExecutor executor;

    @Test
    public void testExecuteGetOrderSuccessSuccessfully() {
        executeAndAssertGetOrderSuccessSuccessfully("fixtures/executors/get_order/" +
            "fulfillment_get_order_success_task_message.json",
            createMdbGetOrderResult("ZKZ123456"));
    }

    @Test
    public void testExecuteGetOrderSuccessWithOnlyOrderIdAndPlacesSuccessfully() {
        executeAndAssertGetOrderSuccessSuccessfully("fixtures/executors/get_order/" +
            "fulfillment_get_order_success_task_message_with_only_order_id_and_places.json",
            createMdbGetOrderResult(null));
    }

    private void executeAndAssertGetOrderSuccessSuccessfully(String clientTaskFileName,
                                                             GetOrderResult expectedMdbGetOrderResult) {
        ClientTask task = getClientTask(clientTaskFileName);
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        TaskMessage message = executor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        verify(mdbClient).setGetOrderSuccess(eq(expectedMdbGetOrderResult));

        softAssert.assertThat(message.getMessageBody())
            .as("Asserting that the response message body is null")
            .isNull();
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_GET_ORDER_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.MDB);
        return task;
    }

    private GetOrderResult createMdbGetOrderResult(String partnerId) {
        return new GetOrderResult("100", 145L, partnerId, Collections.singletonList(createMdbPlace()));
    }

    private Place createMdbPlace() {
        return new Place(createMdbPlaceId(), createMdbKorobyte(), Collections.singletonList(createMdbItemPlace()));
    }

    private ResourceId createMdbPlaceId() {
        return new ResourceId(null, "2222");
    }

    private Korobyte createMdbKorobyte() {
        return new Korobyte(45, 16, 21, BigDecimal.valueOf(3.2), BigDecimal.valueOf(2), BigDecimal.valueOf(1.2));
    }

    private ItemPlace createMdbItemPlace() {
        return new ItemPlace(createMdbUnitId(), 1);
    }

    private UnitId createMdbUnitId() {
        return new UnitId("123id", 0L, "75690200345480.Checkouter-test-20");
    }
}
