package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.MoveOrderParams;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.checkout.checkouter.order.MoveOrderQueueCall.QUEUED_CALL_PAYLOAD_OBJECT_MAPPER;

public class MoveOrderToUidProcessorTest extends AbstractServicesTestBase {

    @Autowired
    OrderServiceHelper orderServiceHelper;
    @Autowired
    private MoveOrderToUidProcessor processor;

    @Test
    @DisplayName("Попытка обработать заказ у которого UserId равен toUid")
    void moveOrderWithUserIdEqualToUid() throws Exception {
        Order order = OrderProvider.getBlueOrder();
        orderServiceHelper.saveOrder(order);

        LocalDate callCreationDate = LocalDate.of(2020, 1, 17);

        String payload = QUEUED_CALL_PAYLOAD_OBJECT_MAPPER.
                writeValueAsString(new MoveOrderParams(111, order.getUid(), ClientInfo.SYSTEM));

        QueuedCallProcessor.QueuedCallExecution queuedCallExecution = new QueuedCallProcessor.QueuedCallExecution(
                order.getId(),
                payload,
                1,
                callCreationDate.atTime(12, 0).toInstant(ZoneOffset.UTC),
                order.getId()
        );

        ExecutionResult result = processor.process(queuedCallExecution);

        assertThat(result, equalTo(ExecutionResult.SUCCESS));
    }

}
