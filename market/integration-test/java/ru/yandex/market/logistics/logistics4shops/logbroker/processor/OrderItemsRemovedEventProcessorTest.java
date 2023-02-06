package ru.yandex.market.logistics.logistics4shops.logbroker.processor;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.factory.OrderServiceEventFactory;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent;

@DisplayName("Обработка события удаления товаров из заказа")
class OrderItemsRemovedEventProcessorTest extends AbstractMbiOrderEventProcessorTest {

    @Test
    @DisplayName("Парсинг события об удалении товаров")
    void successOrderItemsRemovedPayload() {
        assertEvent(
            orderItemsRemovedEvent(),
            orderEventProtoMessageHandler.parse(orderItemsRemovedEvent().toByteArray())
        );
    }

    @Test
    @DisplayName("Удаление товаров из заказа: заказ не существует")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/lost_order_not_exist_items_removed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/create_cp_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void itemsRemovedOrderNotExist() {
        softly.assertThatCode(() -> orderEventProtoMessageHandler.handle(List.of(orderItemsRemovedEvent())))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Удаление товаров из заказа: событие уже было обработано")
    @DatabaseSetup(value = "/logbroker/orderEvent/items_removed_cp.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/status_changed_items_removed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void itemsRemovedAlreadyProcessed() {
        orderEventProtoMessageHandler.handle(List.of(orderItemsRemovedEvent(100100L)));
    }

    @Test
    @DisplayName("Удаление товаров из заказа: не было ACCEPTED")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/create_cp_task_no_accepted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/lost_order_without_accepted_items_removed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void itemsRemovedOrderWithoutAccepted() {
        orderEventProtoMessageHandler.handle(List.of(orderItemsRemovedEvent(100100L)));
    }

    @Test
    @DisplayName("Удаление товаров из заказа: был READY_TO_SHIP")
    @DatabaseSetup(value = "/logbroker/orderEvent/ready_to_ship.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/with_ready_to_ship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void itemsRemovedOrderWithReadyToShip() {
        orderEventProtoMessageHandler.handle(List.of(orderItemsRemovedEvent(100101L)));
    }

    @Test
    @DisplayName("Удаление товаров из заказа: был потерянный READY_TO_SHIP")
    @DatabaseSetup(value = "/logbroker/orderEvent/ready_to_ship_lost_cp.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void itemsRemovedOrderWithLostReadyToShip() {
        orderEventProtoMessageHandler.handle(List.of(orderItemsRemovedEvent(100101L)));
    }

    @Test
    @DisplayName("Удаление товаров из заказа: успех")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/status_changed_items_removed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void itemsRemovedOrder() {
        orderEventProtoMessageHandler.handle(List.of(orderItemsRemovedEvent(100101L)));
    }

    @Nonnull
    private OrderEvent orderItemsRemovedEvent() {
        return orderItemsRemovedEvent(1000L);
    }

    @Nonnull
    private OrderEvent orderItemsRemovedEvent(long orderId) {
        return OrderServiceEventFactory.orderItemsRemovedEvent(orderId);
    }
}
