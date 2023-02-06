package ru.yandex.market.logistics.logistics4shops.logbroker.processor;

import java.util.List;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.factory.OrderServiceEventFactory;
import ru.yandex.market.logistics.logistics4shops.logbroker.OrderEventMessageHandler;

@DisplayName("Обработка события отмены заказа")
class OrderCancelledEventProcessorTest extends AbstractMbiOrderEventProcessorTest {

    @Autowired
    private OrderEventMessageHandler orderEventProtoMessageHandler;

    @Test
    @DisplayName("Парсинг события об отмене заказа")
    void successOrderCancelledPayloadParsing() {
        assertEvent(
            OrderServiceEventFactory.newOrderCancelledEvent(),
            orderEventProtoMessageHandler.parse(OrderServiceEventFactory.newOrderCancelledEvent().toByteArray())
        );
    }

    @Test
    @DisplayName("Обработка события об отмене заказа")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/new_order_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNewOrderCreatedPayloadProcessing() {
        orderEventProtoMessageHandler.handle(List.of(OrderServiceEventFactory.newOrderCancelledEvent()));
    }
}
