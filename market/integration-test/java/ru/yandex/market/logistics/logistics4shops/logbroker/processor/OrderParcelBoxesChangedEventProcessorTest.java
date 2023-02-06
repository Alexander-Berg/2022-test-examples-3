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

@DisplayName("Обработка события изменения коробок заказа")
class OrderParcelBoxesChangedEventProcessorTest extends AbstractMbiOrderEventProcessorTest {

    @Test
    @DisplayName("Парсинг события об изменении коробок")
    void successOrderParcelBoxesChangedPayload() {
        assertEvent(
            orderParcelBoxesChangedEvent(),
            orderEventProtoMessageHandler.parse(orderParcelBoxesChangedEvent().toByteArray())
        );
    }

    @Test
    @DisplayName("Изменение коробок в заказе: заказ не существует")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/create_cp_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/lost_order_not_exist_places_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void placesChangedOrderNotExist() {
        softly.assertThatCode(() -> orderEventProtoMessageHandler.handle(List.of(orderParcelBoxesChangedEvent())))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Изменение коробок в заказе: событие уже было обработано")
    @DatabaseSetup(
        value = {
            "/logbroker/orderEvent/places_changed_cp.xml",
            "/logbroker/orderEvent/ready_to_ship.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/places_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void placesChangedAlreadyProcessed() {
        orderEventProtoMessageHandler.handle(List.of(orderParcelBoxesChangedEvent(100101L)));
    }

    @Test
    @DisplayName("Изменение коробок в заказе: не было ACCEPTED")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/create_cp_task_no_accepted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/lost_order_without_accepted_places_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void placesChangedOrderWithoutAccepted() {
        orderEventProtoMessageHandler.handle(List.of(orderParcelBoxesChangedEvent(100100L)));
    }

    @Test
    @DisplayName("Изменение коробок в заказе: не было READY_TO_SHIP")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/no_lost_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void placesChangedOrderWithoutReadyToShip() {
        orderEventProtoMessageHandler.handle(List.of(orderParcelBoxesChangedEvent(100101L)));
    }

    @Test
    @DisplayName("Изменение коробок в заказе: есть потерянный READY_TO_SHIP")
    @DatabaseSetup(value = "/logbroker/orderEvent/ready_to_ship_lost_cp_after_1.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/120_118_lost_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/create_cp_100101_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void placesChangedOrderWithLostReadyToShip() {
        orderEventProtoMessageHandler.handle(List.of(orderParcelBoxesChangedEvent(100101L)));
    }

    @Test
    @DisplayName("Изменение коробок в заказе: успех")
    @DatabaseSetup(value = "/logbroker/orderEvent/ready_to_ship.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/places_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void placesChanged() {
        orderEventProtoMessageHandler.handle(List.of(orderParcelBoxesChangedEvent(100101L)));
    }

    @Test
    @DisplayName("Изменение коробок в заказе: успех, для заказа уже был 118")
    @DatabaseSetup(
        value = {
            "/logbroker/orderEvent/ready_to_ship.xml",
            "/logbroker/orderEvent/places_changed_cp_older.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/places_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/deleted_order_checkpoint.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void placesChangedOrderHadCp() {
        orderEventProtoMessageHandler.handle(List.of(orderParcelBoxesChangedEvent(100101L)));
    }

    @Test
    @DisplayName("Изменение коробок в заказе: успех, для заказа есть потерянный 118")
    @DatabaseSetup(
        value = {
            "/logbroker/orderEvent/ready_to_ship.xml",
            "/logbroker/orderEvent/places_changed_lost_cp_older.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/places_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/deleted_lost_checkpoint.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void placesChangedOrderHadLostCp() {
        orderEventProtoMessageHandler.handle(List.of(orderParcelBoxesChangedEvent(100101L)));
    }

    @Nonnull
    private OrderEvent orderParcelBoxesChangedEvent() {
        return orderParcelBoxesChangedEvent(1000L);
    }

    @Nonnull
    private OrderEvent orderParcelBoxesChangedEvent(long orderId) {
        return OrderServiceEventFactory.orderParcelBoxesChangedEvent(orderId);
    }

}
