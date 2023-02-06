package ru.yandex.market.logistics.logistics4shops.logbroker.processor;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.factory.OrderServiceEventFactory;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent;
import ru.yandex.market.mbi.orderservice.proto.event.model.Substatus;

@DisplayName("Обработка события изменения подстатуса заказа")
@ParametersAreNonnullByDefault
class MerchantOrderSubstatusEventChangedProcessorTest extends AbstractMbiOrderEventProcessorTest {

    @Test
    @DisplayName("Парсинг события об изменении сабстатуса")
    void successMerchantOrderSubstatusChangedPayload() {
        assertEvent(
            merchantOrderSubstatusEvent(),
            orderEventProtoMessageHandler.parse(merchantOrderSubstatusEvent().toByteArray())
        );
    }

    @Test
    @DisplayName("Изменение статуса: заказ не существует")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/order_not_exist.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/lost_order_not_exist_packaging.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/create_cp_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusChangedOrderNotExist() {
        softly.assertThatCode(() -> orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent())))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Изменение статуса: заказ не существует, статус UNKNOWN")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/lost_order_not_exist_unknown.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/create_cp_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusChangedOrderNotExistUnknownStatus() {
        softly.assertThatCode(() -> orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(
                1000L,
                Substatus.UNKNOWN_SUBSTATUS
            ))))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Изменение статуса: SHIPPED")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusShipped() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(100101L, Substatus.SHIPPED)));
    }

    @Test
    @DisplayName("Изменение статуса: UNKNOWN_SUBSTATUS")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusUnknown() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(
            100101L,
            Substatus.UNKNOWN_SUBSTATUS
        )));
    }

    @Test
    @DisplayName("Изменение статуса: STARTED, событие уже было обработано")
    @DatabaseSetup(value = "/logbroker/orderEvent/started_cp.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/logbroker/orderEvent/has_shop_order_id.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/status_changed_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusStartedAlreadyProcessed() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(100100L, Substatus.STARTED)));
    }

    @Test
    @DisplayName("Изменение статуса: STARTED")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/status_changed_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusStarted() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(100100L, Substatus.STARTED)));
    }

    @Test
    @DisplayName("Изменение статуса: READY_TO_SHIP")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/status_changed_ready_to_ship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusReadyToShip() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(100101L, Substatus.READY_TO_SHIP)));
    }

    @Test
    @DisplayName("Изменение статуса: READY_TO_SHIP, было автопроставление")
    @DatabaseSetup(value = "/logbroker/orderEvent/ready_to_ship_automatic.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/status_changed_automatic_ready_to_ship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusReadyToShipAutomatic() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(100101L, Substatus.READY_TO_SHIP)));
    }

    @Test
    @DisplayName("Изменение статуса: READY_TO_SHIP, до этого уже был")
    @DatabaseSetup(value = "/logbroker/orderEvent/ready_to_ship.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/status_changed_second_ready_to_ship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusReadyToShipSecond() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(100101L, Substatus.READY_TO_SHIP)));
    }

    @Test
    @DisplayName("Изменение статуса: PACKAGING")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/status_changed_packaging.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusPackaging() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(100101L, Substatus.PACKAGING)));
    }

    @Test
    @DisplayName("Изменение статуса: у заказа не было STARTED")
    @ExpectedDatabase(
        value = "/logbroker/orderEvent/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void substatusChangedOrderWithoutStarted() {
        orderEventProtoMessageHandler.handle(List.of(merchantOrderSubstatusEvent(100100L, Substatus.READY_TO_SHIP)));
    }

    @Nonnull
    private OrderEvent merchantOrderSubstatusEvent() {
        return merchantOrderSubstatusEvent(1000L, Substatus.PACKAGING);
    }

    @Nonnull
    private OrderEvent merchantOrderSubstatusEvent(Long orderId, Substatus substatus) {
        return OrderServiceEventFactory.merchantOrderSubstatusEvent(orderId, substatus);
    }
}
