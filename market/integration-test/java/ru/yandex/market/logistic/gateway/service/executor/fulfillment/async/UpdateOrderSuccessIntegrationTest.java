package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.TplFulfillmentConsumerClient;
import ru.yandex.market.tpl.client.ff.FulfillmentResponseConsumerClient;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateOrderSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long ORDER_ID = 5927638L;
    private static final long PARTNER_ID = 145L;
    private static final String TRACK_ID = "EXT101811250";
    private static final String PROCESS_ID = "123";

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private FulfillmentResponseConsumerClient fulfillmentResponseConsumerClient;
    @SpyBean
    private TplFulfillmentConsumerClient tplFulfillmentConsumerClient;

    @Autowired
    private UpdateOrderSuccessExecutor updateOrderSuccessExecutor;

    @Test
    public void executeError() throws Exception {
        ClientTask task = getUpdateOrderTask("fixtures/executors/fulfillment_create_order/fulfillment_update_order_task_response.json");

        when(repository.findTask(eq(ORDER_ID))).thenReturn(task);

        TaskMessage message = updateOrderSuccessExecutor.execute(new ExecutorTaskWrapper(ORDER_ID, 0));

        softAssert.assertThat(message.getMessageBody()).isNull();
        verify(tplFulfillmentConsumerClient).setUpdateOrderSuccess(
            String.valueOf(ORDER_ID),
            TRACK_ID,
            PARTNER_ID,
            PROCESS_ID
        );
    }

    private ClientTask getUpdateOrderTask(String filename) {
        return new ClientTask()
            .setId(ORDER_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_UPDATE_ORDER)
            .setMessage(getFileContent(filename))
            .setConsumer(TaskResultConsumer.TPL)
            .setMessage(getFileContent(filename))
            .setProcessId(PROCESS_ID);
    }

}
