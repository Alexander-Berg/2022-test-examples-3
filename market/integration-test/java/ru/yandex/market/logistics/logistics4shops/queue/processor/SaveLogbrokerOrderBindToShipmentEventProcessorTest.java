package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderBindToShipmentPayload;
import ru.yandex.market.logistics.logistics4shops.queue.payload.OrderBindToShipmentEventPayload;
import ru.yandex.market.logistics.logistics4shops.utils.LogisticEventUtil;
import ru.yandex.market.logistics.logistics4shops.utils.ProtobufAssertionsUtils;

@DisplayName("Задача сохранения события о назначении заказу отгрузки")
class SaveLogbrokerOrderBindToShipmentEventProcessorTest extends AbstractIntegrationTest {
    private static final Instant INTERVAL_END_TIME = Instant.parse("2022-07-01T11:29:00.00Z");
    private static final OrderBindToShipmentEventPayload PAYLOAD = defaultPayloadBuilder().build();
    private static final OrderBindToShipmentEventPayload PAYLOAD_WITH_DATE = defaultPayloadBuilder()
        .intervalEndTime(INTERVAL_END_TIME)
        .build();

    @Autowired
    private SaveLogbrokerOrderBindToShipmentEventProcessor processor;

    @Autowired
    private LogisticEventUtil logisticEventUtil;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-02-21T11:30:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() {
        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Order 100100 not found in L4S");
    }

    @Test
    @DisplayName("Успешное обновление отгрузки")
    @DatabaseSetup("/queue/processor/savelogbrokerorderbindtoshipment/before/order.xml")
    @ExpectedDatabase(
        value = "/queue/processor/savelogbrokerorderbindtoshipment/after/event_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shipmentIsUpdated() {
        processor.execute(PAYLOAD);
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(
            softly.assertThat(eventPayload.getOrderBindToShipmentPayload()))
            .isEqualTo(
                OrderBindToShipmentPayload.newBuilder()
                    .setOrderId(100100)
                    .setShopId(200100)
                    .setShipmentId(2)
                    .build()
            );
    }

    @Test
    @DisplayName("Успешное обновление отгрузки и её даты")
    @DatabaseSetup("/queue/processor/savelogbrokerorderbindtoshipment/before/order.xml")
    @ExpectedDatabase(
        value = "/queue/processor/savelogbrokerorderbindtoshipment/after/event_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shipmentAndShipmentDateAreUpdated() {
        processor.execute(PAYLOAD_WITH_DATE);
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(
            softly.assertThat(eventPayload.getOrderBindToShipmentPayload()))
            .isEqualTo(
                OrderBindToShipmentPayload.newBuilder()
                    .setOrderId(100100)
                    .setShopId(200100)
                    .setShipmentId(2)
                    .setShipmentDateTime(
                        Timestamp.newBuilder()
                            .setSeconds(INTERVAL_END_TIME.getEpochSecond())
                            .build()
                    )
                    .build()
            );
    }

    @Nonnull
    private static OrderBindToShipmentEventPayload.OrderBindToShipmentEventPayloadBuilder defaultPayloadBuilder() {
        return OrderBindToShipmentEventPayload.builder()
            .orderBarcode("100100")
            .transportationId(2L)
            .outboundId(3L);
    }
}
