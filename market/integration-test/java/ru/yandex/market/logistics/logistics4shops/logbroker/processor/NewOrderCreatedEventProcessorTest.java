package ru.yandex.market.logistics.logistics4shops.logbroker.processor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.factory.OrderServiceEventFactory;
import ru.yandex.market.logistics.logistics4shops.logbroker.OrderEventMessageHandler;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderTrait;

@DisplayName("Обработка события создания заказа")
@ParametersAreNonnullByDefault
class NewOrderCreatedEventProcessorTest extends AbstractMbiOrderEventProcessorTest {

    @Autowired
    private OrderEventMessageHandler orderEventProtoMessageHandler;

    @Test
    @DisplayName("Парсинг события о создании заказа")
    void successNewOrderCreatedPayloadParsing() {
        assertEvent(
            newOrderCreatedEvent(Set.of(OrderTrait.ORDER_EDIT_ALLOWED)),
            orderEventProtoMessageHandler.parse(
                newOrderCreatedEvent(Set.of(OrderTrait.ORDER_EDIT_ALLOWED)).toByteArray()
            )
        );
    }

    @Test
    @DisplayName("Обработка события о создании заказа")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/new_order_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNewOrderCreatedPayloadProcessing() {
        orderEventProtoMessageHandler.handle(List.of(newOrderCreatedEvent(Set.of())));
    }

    @Test
    @DisplayName("Обработка события о создании заказа из которого возможно удаление товаров")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/new_order_created_with_items_removal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNewOrderCreatedPayloadProcessingWithItemsRemoval() {
        orderEventProtoMessageHandler.handle(List.of(newOrderCreatedEvent(Set.of(OrderTrait.ORDER_EDIT_ALLOWED))));
    }

    @Nonnull
    private static OrderEvent newOrderCreatedEvent(Collection<OrderTrait> traits) {
        return OrderServiceEventFactory.newOrderCreatedEvent(traits);
    }
}
