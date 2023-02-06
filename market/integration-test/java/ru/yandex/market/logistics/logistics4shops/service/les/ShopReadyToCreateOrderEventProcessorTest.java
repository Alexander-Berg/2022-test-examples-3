package ru.yandex.market.logistics.logistics4shops.service.les;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.lom.ShopReadyToCreateOrderEvent;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.logging.code.OrderEventCode;
import ru.yandex.market.logistics.logistics4shops.queue.payload.OrderPayload;
import ru.yandex.market.logistics.logistics4shops.queue.processor.PushReadyToShipOnDeadlineProcessor;
import ru.yandex.market.logistics.logistics4shops.service.les.processor.ShopReadyToCreateOrderEventProcessor;
import ru.yandex.market.logistics.logistics4shops.utils.QueueTaskChecker;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.logistics.logistics4shops.config.IntegrationTestConfiguration.TEST_REQUEST_ID;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logHasCode;

@DisplayName("Обработчик событий LES: создание заказа в магазине")
@DatabaseSetup("/service/les/shopreadytocreateorder/before/shop_ready_to_create_order_event_before.xml")
@ParametersAreNonnullByDefault
class ShopReadyToCreateOrderEventProcessorTest extends AbstractIntegrationTest {
    private static final Instant NOW = Instant.parse("2022-02-21T11:30:00.00Z");
    private static final LocalDate SHIPMENT_DATE = LocalDate.of(2022, 2, 20);
    private static final Instant SHIPMENT_DATE_TIME = Instant.parse("2022-02-21T17:30:00.00Z");
    private static final Instant SHIPMENT_DATE_TIME_IN_PAST = Instant.parse("2022-02-21T10:30:00.00Z");

    @Autowired
    private ShopReadyToCreateOrderEventProcessor processor;

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    private static final List<String> EXPRESS_TAGS = List.of("EXPRESS");

    @BeforeEach
    void setUp() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успешное создание дропшип заказа")
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/shop_ready_to_create_order_event_after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/create_dropship_order_event.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateOrder() {
        processor.process(event("100100", 200100L, 300100L, List.of(), null), "1");

        assertLogs().noneMatch(logHasCode(OrderEventCode.ORDER_CREATION_WITHOUT_PARTNER_MAPPING));
    }

    @Test
    @DisplayName("Успешное создание дропшип заказа: в базе нет маппинга")
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/shop_ready_to_create_order_event_no_mapping_after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/create_dropship_order_event.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateOrderNoMapping() {
        processor.process(event("100100", 200101L, 300100L, List.of(), null), "1");
        assertMismatchErrorLogged();
    }

    @Test
    @DisplayName("Успешное создание экспресс заказа")
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/shop_ready_to_create_order_event_after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/create_express_order_event.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateExpressOrder() {
        processor.process(event("100100", 200100L, 300100L, EXPRESS_TAGS, null), "1");

        assertLogs().noneMatch(logHasCode(OrderEventCode.ORDER_CREATION_WITHOUT_PARTNER_MAPPING));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешное создание экспресс заказа: нужно проставление 120 по дедлайну сборки")
    @DatabaseSetup(
        value = "/service/les/shopreadytocreateorder/before/partner_ready_to_ship_on_deadline.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/shop_ready_to_create_order_event_after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/"
            + "create_express_order_event_with_ready_to_ship_on_deadline.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateExpressOrderReadyToShipOnDeadline(String name, Instant shipmentDateTime, Duration delay) {
        processor.process(event("100100", 200100L, 300100L, SHIPMENT_DATE, shipmentDateTime, EXPRESS_TAGS, null), "1");

        OrderPayload expectedPayload = OrderPayload.builder()
            .externalId("100100")
            .requestId(TEST_REQUEST_ID + "/3")
            .build();
        assertLogs().noneMatch(logHasCode(OrderEventCode.ORDER_CREATION_WITHOUT_PARTNER_MAPPING));
        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            PushReadyToShipOnDeadlineProcessor.class,
            expectedPayload,
            delay
        );
    }

    @Nonnull
    private static Stream<Arguments> testCreateExpressOrderReadyToShipOnDeadline() {
        return Stream.of(
            Arguments.of("Дедлайн сборки в будущем", SHIPMENT_DATE_TIME, Duration.ofHours(6)),
            Arguments.of("Дедлайн сборки в прошлом", SHIPMENT_DATE_TIME_IN_PAST, Duration.ofHours(0))
        );
    }

    @Test
    @DisplayName("Успешное создание экспресс заказа: в базе нет маппинга")
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/shop_ready_to_create_order_event_no_mapping_after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/create_express_order_event.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateExpressOrderNoMapping() {
        processor.process(event("100100", 200101L, 300100L, EXPRESS_TAGS, null), "1");
        assertMismatchErrorLogged();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка создания заказа: невалидное событие")
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/before/shop_ready_to_create_order_event_before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/empty_events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateOrderInvalidEvent(@SuppressWarnings("unused") String name, ShopReadyToCreateOrderEvent event) {
        processor.process(event, "1");
    }

    @Nonnull
    private static Stream<Arguments> testCreateOrderInvalidEvent() {
        return Stream.of(
            Arguments.of("Нет идентификатора заказа", event(null, 200100L, 300100L, List.of(), null)),
            Arguments.of("Нет идентификатора магазина", event("100100", null, 300100L, List.of(), null)),
            Arguments.of("Нет идентификатора партнера", event("100100", 200100L, null, List.of(), null))
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Создание заказа: не создаются ивенты")
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/shop_ready_to_create_order_event_after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/empty_events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateOrderNoEvent(@SuppressWarnings("unused") String name, ShopReadyToCreateOrderEvent event) {
        processor.process(event, "1");
    }

    @Nonnull
    private static Stream<Arguments> testCreateOrderNoEvent() {
        return Stream.of(
            Arguments.of(
                "Нет даты отгрузки для дропшип заказа",
                event("100100", 200100L, 300100L, null, Instant.ofEpochSecond(1645358400L), List.of(), null)
            ),
            Arguments.of(
                "Нет дедлайна сборки для экспресс заказа",
                event("100100", 200100L, 300100L, LocalDate.of(2022, 2, 20), null, EXPRESS_TAGS, null)
            )
        );
    }

    @Test
    @DisplayName("Успешное создание DSB-dropoff заказа")
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/shop_ready_to_create_order_event_dbs_do_after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/create_dropship_order_event.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateDbsDropoffOrder() {
        processor.process(event("100100", 200100L, 300100L, List.of(), 12345654321L), "1");

        assertLogs().noneMatch(logHasCode(OrderEventCode.ORDER_CREATION_WITHOUT_PARTNER_MAPPING));
    }

    @Test
    @DisplayName("Пропуск события - заказ с таким externalId уже есть в базе")
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/before/shop_ready_to_create_order_event_before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/shopreadytocreateorder/after/empty_events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testSkipExistingOrder() {
        processor.process(event("1001001", 200100L, 300100L, List.of(), 12345654321L), "1");

        assertLogs().noneMatch(logHasCode(OrderEventCode.ORDER_CREATION_WITHOUT_PARTNER_MAPPING));
    }

    private void assertMismatchErrorLogged() {
        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.error("Requested creation of order with shop and partner that don't have mapping")
                .setLoggingCode(OrderEventCode.ORDER_CREATION_WITHOUT_PARTNER_MAPPING)
                .setEntities(Map.of(
                    "order", List.of("100100"),
                    "event", List.of("1"),
                    "mbiPartner", List.of("200101"),
                    "lmsPartner", List.of("300100")
                ))
        ));
    }

    @Nonnull
    private static ShopReadyToCreateOrderEvent event(
        String orderId,
        Long shopId,
        Long partnerId,
        List<String> tags,
        Long shipmentLogisticsPointId
    ) {
        return event(
            orderId,
            shopId,
            partnerId,
            SHIPMENT_DATE,
            SHIPMENT_DATE_TIME,
            tags,
            shipmentLogisticsPointId
        );
    }

    @Nonnull
    private static ShopReadyToCreateOrderEvent event(
        String orderId,
        Long shopId,
        Long partnerId,
        LocalDate shipmentDate,
        Instant shipmentDateTime,
        List<String> tags,
        Long shipmentLogisticsPointId
    ) {
        return new ShopReadyToCreateOrderEvent(
            orderId,
            shopId,
            partnerId,
            shipmentDate,
            shipmentDateTime,
            tags,
            shipmentLogisticsPointId
        );
    }
}
