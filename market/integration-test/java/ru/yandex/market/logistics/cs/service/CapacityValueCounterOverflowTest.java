package ru.yandex.market.logistics.cs.service;

import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceCounterBatchPayload;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptor;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.facade.CapacityValueCounterFacade;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.market.logistics.cs.util.DateTimeUtils.nowDayUtc;

@DisplayName("Тестирование инкрементации счётчика overflow")
public class CapacityValueCounterOverflowTest extends AbstractIntegrationTest {

    private static final long SERVICE_1 = 1L;
    private static final long SERVICE_2 = 2L;
    private static final long SERVICE_3 = 3L;

    @Autowired
    private CapacityValueCounterFacade facade;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @DisplayName("Проверяем, что ни один overflow не был инкрементирован")
    @DatabaseSetup("/repository/value_counter/overflow/before/base_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/overflow/after/overflow_fields_have_not_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testOverflowHaveNotChanged() {
        // Setup
        var payload = ServiceCounterBatchPayload.builder()
            .eventId(0L)
            .eventType(EventType.NEW)
            .counters(List.of(
                descriptor(SERVICE_1, nowDayUtc(), 5),
                descriptor(SERVICE_2, nowDayUtc(), 11),
                descriptor(SERVICE_3, nowDayUtc(), 1)
            ))
            .orderCount(2)
            .dummy(false)
            .build();

        // Action
        assertDoesNotThrow(() -> processPayload(payload));
    }

    @DisplayName("Проверяем инкрементацию overflow в кейсе (не было дейоффа -> проставили дейофф)")
    @DatabaseSetup("/repository/value_counter/overflow/before/base_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/overflow/after/simple_overflow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSimpleOverflow() {
        // Setup
        var payload = ServiceCounterBatchPayload.builder()
            .eventId(0L)
            .eventType(EventType.NEW)
            .counters(List.of(
                descriptor(SERVICE_3, nowDayUtc(), 14)
            ))
            .orderCount(5)
            .dummy(false)
            .build();

        // Action
        assertDoesNotThrow(() -> processPayload(payload));
    }

    @DisplayName("Проверяем инкрементацию overflow в кейсе (был THRESHOLD дейофф -> прилетели ещё заказы)")
    @DatabaseSetup("/repository/value_counter/overflow/before/threshold_dayoff.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/overflow/after/threshold_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testThresholdDayOffOverflow() {
        // Setup
        var payload = ServiceCounterBatchPayload.builder()
            .eventId(0L)
            .eventType(EventType.NEW)
            .counters(List.of(
                descriptor(SERVICE_3, nowDayUtc(), 5)
            ))
            .orderCount(1)
            .dummy(false)
            .build();

        // Action
        assertDoesNotThrow(() -> processPayload(payload));
    }

    @DisplayName("Проверяем инкрементацию overflow в кейсе (был TECHNICAL дейофф -> прилетели ещё заказы)")
    @DatabaseSetup("/repository/value_counter/overflow/before/technical_dayoff.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/overflow/after/technical_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testThresholdTechnicalDayOffOverflow() {
        // Setup
        var payload = ServiceCounterBatchPayload.builder()
            .eventId(0L)
            .eventType(EventType.NEW)
            .counters(List.of(
                descriptor(SERVICE_3, nowDayUtc(), 5)
            ))
            .orderCount(1)
            .dummy(false)
            .build();

        // Action
        assertDoesNotThrow(() -> processPayload(payload));
    }

    @DisplayName("Проверяем инкрементацию overflow в кейсе (был PROPAGATED дейофф -> прилетели ещё заказы)")
    @DatabaseSetup("/repository/value_counter/overflow/before/propagated_dayoff.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/overflow/after/propagated_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testPropagatedOverflow() {
        // Setup
        var payload = ServiceCounterBatchPayload.builder()
            .eventId(0L)
            .eventType(EventType.NEW)
            .counters(List.of(
                descriptor(SERVICE_3, nowDayUtc(), 17)
            ))
            .orderCount(2)
            .dummy(false)
            .build();

        // Action
        assertDoesNotThrow(() -> processPayload(payload));
    }

    @DisplayName("Проверяем инкрементацию overflow в кейсе (был MANUAL дейофф -> прилетели ещё заказы)")
    @DatabaseSetup("/repository/value_counter/overflow/before/manual_dayoff.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/overflow/after/manual_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testManualOverflow() {
        // Setup
        var payload = ServiceCounterBatchPayload.builder()
            .eventId(0L)
            .eventType(EventType.NEW)
            .counters(List.of(
                descriptor(SERVICE_3, nowDayUtc(), 11)
            ))
            .orderCount(2)
            .dummy(false)
            .build();

        // Action
        assertDoesNotThrow(() -> processPayload(payload));
    }

    @DisplayName("Проверяем инкрементацию overflow в кейсе (был THRESHOLD дейофф -> заказ отменили)")
    @DatabaseSetup("/repository/value_counter/overflow/before/decrease_counter_without_overflow_decrease.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/overflow/after/decrease_counter_without_overflow_decrease.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testDecreaseCounterWithoutOverflowDecrease() {
        // Setup
        var payload = ServiceCounterBatchPayload.builder()
            .eventId(0L)
            .eventType(EventType.CANCELLED)
            .counters(List.of(
                descriptor(SERVICE_3, nowDayUtc(), -12)
            ))
            .orderCount(-1)
            .dummy(false)
            .build();

        // Action
        assertDoesNotThrow(() -> processPayload(payload));
    }

    @DisplayName("Проверяем инкрементацию overflow в кейсе (Не инкрементируем overflow на фиктивном счётчике)")
    @DatabaseSetup("/repository/value_counter/overflow/before/fictitious_counter_overflow.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/overflow/after/fictitious_counter_overflow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testFictitiousCounterOverflow() {
        // Setup
        var payload = ServiceCounterBatchPayload.builder()
            .eventId(0L)
            .eventType(EventType.NEW)
            .counters(List.of(
                descriptor(SERVICE_3, nowDayUtc(), 14)
            ))
            .orderCount(2)
            .dummy(false)
            .build();

        // Action
        assertDoesNotThrow(() -> processPayload(payload));
    }

    private ServiceDeliveryDescriptor descriptor(Long serviceId, LocalDate day, int itemCount) {
        return new ServiceDeliveryDescriptor(serviceId, day, itemCount);
    }

    private void processPayload(ServiceCounterBatchPayload payload) {
        transactionTemplate.execute(status -> facade.processServiceCounter(payload));
    }

}
