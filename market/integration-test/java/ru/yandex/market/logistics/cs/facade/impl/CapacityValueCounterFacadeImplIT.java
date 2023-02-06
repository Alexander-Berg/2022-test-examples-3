package ru.yandex.market.logistics.cs.facade.impl;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.config.FeaturePropertiesConfiguration;
import ru.yandex.market.logistics.cs.dbqueue.dayoff.bycapacity.ServiceToDayByCapacityProducer;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceCounterBatchPayload;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptor;
import ru.yandex.market.logistics.cs.domain.entity.CapacityValue;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.domain.enumeration.UnitType;
import ru.yandex.market.logistics.cs.domain.jdbc.ServiceVersionMapping;
import ru.yandex.market.logistics.cs.facade.CapacityValueCounterFacade;
import ru.yandex.market.logistics.cs.repository.CapacityValueRepository;
import ru.yandex.market.logistics.cs.service.CapacityValueCounterService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Фасад для работы со счетчиками капасити")
class CapacityValueCounterFacadeImplIT extends AbstractIntegrationTest {
    private static final LocalDate DAY_1 = LocalDate.of(2077, 5, 19);
    private static final LocalDate DAY_2 = LocalDate.of(2077, 5, 20);

    @Autowired
    private ServiceToDayByCapacityProducer producer;

    @Autowired
    private CapacityValueCounterFacade facade;

    @Autowired
    private CapacityValueRepository capacityValueRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    CapacityValueCounterService capacityValueCounterService;

    @Autowired
    private FeaturePropertiesConfiguration featureProperties;

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_40_route_increment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_payload_processing.xml",
    })
    @DisplayName("Корркетный инкремент при непересекающихся путях в дереве")
    void nonOverlappingRouteIncrement() {
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(new ServiceDeliveryDescriptor(40L, DAY_1, 2)),
            1,
            false
        )));
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_40_50_route_increment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_payload_processing.xml",
    })
    @DisplayName("Корркетный инкремент при пересекающихся путях в дереве")
    void singleIncrementForOverlappingRouteIncrement() {
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(
                new ServiceDeliveryDescriptor(40L, DAY_1, 2),
                new ServiceDeliveryDescriptor(50L, DAY_1, 2)
            ),
            1,
            false
        )));
        verify(producer).enqueue(5L, DAY_1);
        verifyNoMoreInteractions(producer);
    }

    @Test
    @DatabaseSetup("/repository/value_counter/service_counter_batch/before/huge_service_count.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/huge_service_count.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Большое количество сервисов для счетчиков евента")
    public void hugeAmountOfServicesTest() {
        assertDoesNotThrow(
            () -> processPayload(
                new ServiceCounterBatchPayload(
                    0L,
                    EventType.NEW,
                    List.of(
                        new ServiceDeliveryDescriptor(1992094L, DAY_1, 2),
                        new ServiceDeliveryDescriptor(1998412L, DAY_1, 2),
                        new ServiceDeliveryDescriptor(2679226L, DAY_1, 2),
                        new ServiceDeliveryDescriptor(2869209L, DAY_1, 2),
                        new ServiceDeliveryDescriptor(2869341L, DAY_1, 2),
                        new ServiceDeliveryDescriptor(2869362L, DAY_1, 2),
                        new ServiceDeliveryDescriptor(2869371L, DAY_1, 2),
                        new ServiceDeliveryDescriptor(2869380L, DAY_1, 2),
                        new ServiceDeliveryDescriptor(1992093L, DAY_1, 2)
                    ),
                    1,
                    false
                ),
                new ServiceVersionMapping(Map.of(
                    1992094L, 1L,
                    1998412L, 1L,
                    2679226L, 1L,
                    2869209L, 1L,
                    2869341L, 1L,
                    2869362L, 1L,
                    2869371L, 1L,
                    2869380L, 1L,
                    1992093L, 5L
                ))
            )
        );

        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_40_route_increment_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_payload_processing.xml",
    })
    @DisplayName("Корректное проставление выходного дня при переполнении в непересекающихся путях дерева")
    void nonOverlappingRouteIncrementDayOff() {
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(new ServiceDeliveryDescriptor(40L, DAY_2, 2)),
            2,
            false
        )));
        verify(producer).enqueue(2L, DAY_2);
        verify(producer).enqueue(4L, DAY_2);
        verify(producer).enqueue(5L, DAY_2);
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_50_route_with_dayoff_increment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_payload_processing_manual_dayoff.xml",
    })
    @DisplayName("Попытка проставления дейоффа в счётчик, на котором уже стоит MANUAL dayoff")
    void testManualDayOffIsAlreadySet() {
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(new ServiceDeliveryDescriptor(50L, DAY_1, 3)),
            1,
            false
        )));
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_40_50_route_increment_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_payload_processing.xml",
    })
    @DisplayName("Корректное проставление выходного дня при переполнении в пересекающихся путях дерева")
    void singleIncrementForOverlappingRouteIncrementDayOff() {
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(
                new ServiceDeliveryDescriptor(40L, DAY_1, 8),
                new ServiceDeliveryDescriptor(50L, DAY_1, 8)
            ),
            4,
            false
        )));

        verify(producer).enqueue(1L, DAY_1);
        verify(producer).enqueue(2L, DAY_1);
        verify(producer).enqueue(3L, DAY_1);
        verify(producer).enqueue(4L, DAY_1);
        verify(producer).enqueue(5L, DAY_1);
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_40_50_route_increment_unset_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_payload_processing.xml",
    })
    @DisplayName("Корректное снятие выходного дня при переполнении в пересекающихся путях дерева")
    void singleIncrementForOverlappingRouteIncrementUnsetDayOff() {
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(
                new ServiceDeliveryDescriptor(40L, DAY_1, 8),
                new ServiceDeliveryDescriptor(50L, DAY_1, 8)
            ),
            4,
            false
        )));

        verify(producer).enqueue(1L, DAY_1);
        verify(producer).enqueue(2L, DAY_1);
        verify(producer).enqueue(3L, DAY_1);
        verify(producer).enqueue(4L, DAY_1);
        verify(producer).enqueue(5L, DAY_1);
        verifyNoMoreInteractions(producer);

        reset(producer);
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(
                new ServiceDeliveryDescriptor(20L, DAY_1, -2)
            ),
            -1,
            false
        )));
        verifyRecalculationFired(DAY_1, 1L, 2L, 3L, 4L);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/capacity_value_change/after/after_unlimited_counter_10_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/capacity_value_change/before/before_capacity_value_adding.xml",
    })
    @DisplayName("Обновление существующего безлимитного счётчика при добавлении лимита на каждый день")
    void allDayLimitAddingIfUnlimitedCounterExists() {
        inTransaction(() -> capacityValueRepository.save(CapacityValue.builder()
            .id(100L)
            .capacityId(1L)
            .value(5L)
            .unitType(UnitType.ORDER)
            .build()));

        facade.reflectCapacityValueCreation(List.of(100L));
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/capacity_value_change/after/after_unlimited_counter_10_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/capacity_value_change/before/before_capacity_value_adding.xml",
    })
    @DisplayName("Обновление существующего безлимитного счётчика при добавлении лимита на конкретный день")
    void dayLimitAddingIfUnlimitedCounterExists() {
        inTransaction(() -> capacityValueRepository.save(CapacityValue.builder()
            .id(100L)
            .capacityId(1L)
            .value(5L)
            .unitType(UnitType.ORDER)
            .day(DAY_2)
            .build()));

        facade.reflectCapacityValueCreation(List.of(100L));
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/capacity_value_change/after/after_unlimited_counter_10_update_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/capacity_value_change/before/before_capacity_value_adding.xml",
    })
    @DisplayName("Обновление существующего безлимитного счётчика при добавлении лимита на каждый день приводит к " +
        "проставлению выходного дня")
    void allDayLimitAddingCausesDayOffSetting() {
        inTransaction(() -> capacityValueRepository.save(CapacityValue.builder()
            .id(100L)
            .capacityId(1L)
            .value(3L)
            .unitType(UnitType.ORDER)
            .build()));

        facade.reflectCapacityValueCreation(List.of(100L));
        verifyRecalculationFired(DAY_2, 1L, 2L, 3L, 4L, 5L);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/capacity_value_change/after/after_limited_counter_20_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/capacity_value_change/before/before_capacity_value_adding.xml",
    })
    @DisplayName("Обновление существующего ограниченного счётчика при добавлении лимита на конкретный день")
    void dayLimitAddingIfAlreadyPointingToAllDayLimit() {
        inTransaction(() -> capacityValueRepository.save(CapacityValue.builder()
            .id(100L)
            .capacityId(2L)
            .value(7L)
            .unitType(UnitType.ORDER)
            .day(DAY_1)
            .build()));

        facade.reflectCapacityValueCreation(List.of(100L));
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/capacity_value_change/after/after_limited_capacity_value_added.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/capacity_value_change/before/before_specific_capacity_value_adding.xml",
    })
    @DisplayName("Создаём ограничение на конкретный день при отсутствии общего и проверяем, что оно не затронет " +
        "счётчики на другие дни")
    void testCountersNotAffectedBySpecificLimit() {
        inTransaction(() -> capacityValueRepository.save(CapacityValue.builder()
            .id(100L)
            .capacityId(2L)
            .value(15L)
            .unitType(UnitType.ORDER)
            .day(DAY_1)
            .build()));

        facade.reflectCapacityValueCreation(List.of(100L));
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/capacity_value_change/after/after_limited_counter_20_update_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/capacity_value_change/before/before_capacity_value_adding.xml",
    })
    @DisplayName("Обновление существующего ограниченного счётчика при добавлении лимита на конкретный день приводит " +
        "к проставлению выходного дня")
    void dayLimitAddingCausesDayOffSetting() {
        inTransaction(() -> capacityValueRepository.save(CapacityValue.builder()
            .id(100L)
            .capacityId(2L)
            .value(1L)
            .unitType(UnitType.ORDER)
            .day(DAY_1)
            .build()));

        facade.reflectCapacityValueCreation(List.of(100L));

        verifyRecalculationFired(DAY_1, 2L, 4L, 5L);
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/capacity_value_change/after/after_limited_counter_21_remove_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/capacity_value_change/before/before_capacity_value_removing.xml",
    })
    @DisplayName("Обновление существующего ограниченного счётчика при удалении лимита на конкретный день приводит " +
        "к проставлению выходного дня")
    void dayLimitRemovingCausesDayOffSetting() {
        CapacityValue removedValue = initializeInTransaction(() -> capacityValueRepository.getOne(21L))
            .toBuilder()
            .build();
        inTransaction(() -> capacityValueRepository.deleteById(21L));

        inTransaction(() -> facade.reflectCapacityValueRemoval(List.of(removedValue)));

        verifyRecalculationFired(DAY_2, 2L, 4L, 5L);
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/capacity_value_change/after/after_limited_counter_20_remove_dayoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/capacity_value_change/before/before_capacity_value_removing.xml",
    })
    @DisplayName("Обновление существующего ограниченного счётчика при удалении лимита на конкретный день приводит " +
        "к снятию выходного дня")
    void dayLimitRemovingCausesDayOffUnsetting() {
        CapacityValue removedValue = initializeInTransaction(() -> capacityValueRepository.getOne(20L))
            .toBuilder()
            .build();
        inTransaction(() -> capacityValueRepository.deleteById(20L));

        inTransaction(() -> facade.reflectCapacityValueRemoval(List.of(removedValue)));

        verifyRecalculationFired(DAY_1, 2L, 4L, 5L);
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_50_route_dummy_batch_increment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_payload_processing.xml",
    })
    @DisplayName("Dummy-батч не нкрементит счетчики капасити при выключенном процессинге")
    void dummyBatchProcessing() {
        featureProperties.setIncrementDummyEnabled(false);
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(
                new ServiceDeliveryDescriptor(40L, DAY_1, 5),
                new ServiceDeliveryDescriptor(50L, DAY_1, 5)
            ),
            1,
            true
        )));
        verify(capacityValueCounterService, never()).increment2(any(), anyLong(), anyBoolean());
        verifyNoMoreInteractions(producer);
        featureProperties.setIncrementDummyEnabled(true);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_50_route_dummy_batch_increment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_payload_processing.xml",
    })
    @DisplayName("Dummy-батч не влияет на значения счётчиков при включенном процессинге")
    void dummyBatchProcessingEnabled() {
        featureProperties.setIncrementDummyEnabled(true);
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(
                new ServiceDeliveryDescriptor(40L, DAY_1, 5),
                new ServiceDeliveryDescriptor(50L, DAY_1, 5)
            ),
            1,
            true
        )));
        featureProperties.setIncrementDummyEnabled(false);
        verify(capacityValueCounterService, times(8)).increment2(any(), eq(0L), anyBoolean());
        verifyNoMoreInteractions(producer);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/value_counter/service_counter_batch/after/after_past_payload_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup({
        "/repository/value_counter/service_counter_batch/before/base_past_capacity_tree.xml",
        "/repository/value_counter/service_counter_batch/before/before_past_payload_processing.xml",
    })
    @DisplayName("Проверяем, что счётчики не изменяются для заказов из прошлого")
    void testDoNotIncrementInPast() {
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            0L,
            EventType.NEW,
            List.of(
                new ServiceDeliveryDescriptor(50L, getUtcDateMinusDays(0), 33),
                new ServiceDeliveryDescriptor(50L, getUtcDateMinusDays(1), 33),
                new ServiceDeliveryDescriptor(50L, getUtcDateMinusDays(2), 33)
            ),
            33,
            false
        )));
        assertDoesNotThrow(() -> processPayload(new ServiceCounterBatchPayload(
            1L,
            EventType.NEW,
            List.of(
                new ServiceDeliveryDescriptor(50L, getUtcDateMinusDays(0), -10),
                new ServiceDeliveryDescriptor(50L, getUtcDateMinusDays(1), -10),
                new ServiceDeliveryDescriptor(50L, getUtcDateMinusDays(2), -10)
            ),
            -10,
            false
        )));
        verifyNoMoreInteractions(producer);
    }

    private LocalDate getUtcDateMinusDays(int days) {
        return LocalDate.now(ZoneOffset.UTC).minusDays(days);
    }

    private void verifyRecalculationFired(LocalDate day, long... capacityIds) {
        for (long capacityId : capacityIds) {
            verify(producer).enqueue(capacityId, day);
        }
        verifyNoMoreInteractions(producer);
    }

    private void processPayload(ServiceCounterBatchPayload payload, @Nullable ServiceVersionMapping expected) {
        inTransaction(() -> {
            ServiceVersionMapping mappings = facade.processServiceCounter(payload);
            if (expected != null) {
                softly.assertThat(mappings).isEqualTo(expected);
            }
        });
    }

    private void processPayload(ServiceCounterBatchPayload payload) {
        processPayload(payload, null);
    }

    private <T> T inTransaction(Supplier<T> supplier) {
        return transactionTemplate.execute(status -> supplier.get());
    }

    private <T> T initializeInTransaction(Supplier<T> supplier) {
        return inTransaction(() -> {
            T value = supplier.get();
            Hibernate.initialize(value);
            return value;
        });
    }

    private void inTransaction(Runnable runnable) {
        inTransaction(() -> {
            runnable.run();
            //noinspection ReturnOfNull
            return null;
        });
    }
}
