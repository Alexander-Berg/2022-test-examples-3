package ru.yandex.market.checkout.helpers;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.stock.ItemsRefreezeQCProcessor;
import ru.yandex.market.checkout.checkouter.tasks.refreeze.ItemsRefreezeQCMessage;
import ru.yandex.market.checkout.common.TestHelper;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

@TestHelper
public class QueuedCallsHelper {
    @Autowired
    private ItemsRefreezeQCProcessor itemsRefreezeQCProcessor;

    public void runItemsRefreezeQCProcessor(long orderId, Collection<OrderItem> orderItems) {
        try {
            var orderItemIds = orderItems.stream().map(OrderItem::getId).collect(Collectors.toList());
            var message = new ItemsRefreezeQCMessage(orderId, orderItemIds, System.currentTimeMillis());
            QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                    orderId,
                    new ObjectMapper().writeValueAsString(message),
                    0,
                    Instant.now(),
                    orderId
            );

            itemsRefreezeQCProcessor.process(execution);
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }
}
