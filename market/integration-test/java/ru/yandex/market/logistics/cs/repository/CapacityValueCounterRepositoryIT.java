package ru.yandex.market.logistics.cs.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
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
import ru.yandex.market.logistics.cs.domain.domain.CapacityCounter;
import ru.yandex.market.logistics.cs.domain.entity.CapacityValueCounter;
import ru.yandex.market.logistics.cs.domain.enumeration.DayOffType;
import ru.yandex.market.logistics.cs.domain.enumeration.UnitType;
import ru.yandex.market.logistics.cs.domain.exception.DayOffChangeException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Репозиторий счетчиков капасити")
@DatabaseSetup("/repository/value_counter/custom/before/base_capacity_tree.xml")
class CapacityValueCounterRepositoryIT extends AbstractIntegrationTest {
    private static final LocalDate DAY_1 = LocalDate.of(2021, 5, 19);
    private static final LocalDate DAY_2 = LocalDate.of(2021, 5, 20);
    private static final LocalDate DAY_3 = LocalDate.of(2021, 5, 21);

    @Autowired
    private CapacityValueCounterRepository repository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Создаем и получаем все счетчики для дерева за раз")
    void getAndCreateCountersForSubtree() {
        softly.assertThat(repository.getCountersSubtreeForUpdate(
                "6",
                List.of(LocalDate.of(2021, 5, 19))
            ))
            .hasSize(4);
    }

    @Test
    @DisplayName("При создании поддерева не модифицируются существующие счётчики")
    @ExpectedDatabase(
        value = "/repository/value_counter/custom/after/create_7_day1_subtree.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successfullyReturnExistingCounter() {
        CapacityCounter expected = initializeInTransaction(() -> CapacityCounter.from(repository.getOne(70L)));
        CapacityCounter existing = createCounter(7L, "6.7", DAY_1);
        assertEquals(expected, existing);
    }

    @Test
    @DisplayName("При создании счётчика учитывается значение лимита капасити на конкретный день")
    @ExpectedDatabase(
        value = "/repository/value_counter/custom/after/create_1_day2_subtree.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createCounterFromDayLimit() {
        CapacityCounter result = createCounter(1L, "1", DAY_2);

        CapacityCounter expected = counter(1L, 1L, DAY_2, UnitType.ORDER, 3L, 11L);

        assertEntitiesEqual(expected, result);
    }

    @Test
    @DisplayName("При создании счётчика учитывается значение лимита капасити на каждый день")
    @ExpectedDatabase(
        value = "/repository/value_counter/custom/after/create_2_day3_subtree.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createCounterFromAnyDayLimit() {
        CapacityCounter result = createCounter(2L, "1.2", DAY_3);

        CapacityCounter expected = counter(1L, 2L, DAY_3, UnitType.ORDER, 5L, 20L);

        assertEntitiesEqual(expected, result);
    }

    @Test
    @DisplayName("При отсутствии лимитов капасити должен быть создан безлимитный счетчик")
    void createNoLimitCounterIfValueNotExists() {
        CapacityCounter result = createCounter(3L, "1.3", DAY_1);

        CapacityCounter expected = counter(1L, 3L, DAY_1, UnitType.ORDER);

        assertEntitiesEqual(expected, result);
    }

    @Test
    @DisplayName("Находятся нужные счетчики через велью")
    @DatabaseSetup("/repository/value_counter/custom/before/with_counters.xml")
    void findCapacitiesByValue() {
        CapacityCounter base = counter(70L, 7L, DAY_1, UnitType.ORDER, 1L, 70L);

        softly.assertThat(repository.findAllByCapacityValue(70L, DAY_1, UnitType.ORDER))
            .containsOnly(base);

        softly.assertThat(repository.findAllByCapacityValue(70L, null, UnitType.ORDER))
            .containsExactlyInAnyOrder(
                base,
                counter(71L, 7L, DAY_2, UnitType.ORDER, 1L, 70L)
            );
    }

    @Test
    @DisplayName("Находятся нужные безлимитные счетчики через капасити")
    @DatabaseSetup("/repository/value_counter/custom/before/with_unlimited_counters.xml")
    void findUnlimitedCounters() {
        CapacityCounter base = counter(70L, 7L, DAY_1, UnitType.ORDER);

        softly.assertThat(repository.findAllUnlimitedByCapacity(7L, DAY_1, UnitType.ORDER))
            .containsOnly(base);

        softly.assertThat(repository.findAllUnlimitedByCapacity(7L, null, UnitType.ORDER))
            .containsExactlyInAnyOrder(
                base,
                counter(71L, 7L, DAY_2, UnitType.ORDER)
            );
    }

    @Test
    @DisplayName("Находятся нужные счетчики по капасити")
    @DatabaseSetup("/repository/value_counter/custom/before/with_counters.xml")
    void findByCapacity() {
        softly.assertThat(repository.findByCapacity(7L, DAY_1, UnitType.ORDER))
            .isEqualTo(counter(70L, 7L, DAY_1, UnitType.ORDER, 1L, 70L));
    }

    @Test
    @DisplayName("Находятся все родительские счетчики с дейоффами")
    @DatabaseSetup("/repository/value_counter/custom/before/with_day_offed_counters.xml")
    void findAllParentDayOffedCounters() {
        softly.assertThat(repository.findAllParentCountersWithDayOffs(5L))
            .containsExactlyInAnyOrderElementsOf(List.of(
                CapacityValueCounter.builder()
                    .id(100L)
                    .capacityId(2L)
                    .capacityValueId(2L)
                    .unitType(UnitType.ORDER)
                    .day(LocalDate.now())
                    .count(0L)
                    .threshold(0L)
                    .overflow(0L)
                    .dayOff(true)
                    .dayOffType(DayOffType.TECHNICAL)
                    .propagatedFrom(null)
                    .build(),
                CapacityValueCounter.builder()
                    .id(101L)
                    .capacityId(2L)
                    .capacityValueId(3L)
                    .unitType(UnitType.ORDER)
                    .day(LocalDate.now().plusDays(1))
                    .count(0L)
                    .threshold(0L)
                    .overflow(0L)
                    .dayOff(true)
                    .dayOffType(DayOffType.TECHNICAL)
                    .propagatedFrom(null)
                    .build()
            ));
    }

    @Test
    @DisplayName("Дейоффы пропагируются с родительского")
    @DatabaseSetup("/repository/value_counter/custom/before/with_day_offed_counters.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/custom/after/propagated_day_offs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void propagateDayOffs() {
        assertDoesNotThrow(() -> repository.propagateDayOff(100L));
        assertDoesNotThrow(() -> repository.propagateDayOff(101L));
    }

    @Test
    @DisplayName("Пропагация не работает, если на счетчике нет дейоффа")
    @DatabaseSetup("/repository/value_counter/custom/before/with_day_offed_counters.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/custom/before/with_day_offed_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doesntPropagateFromNonDayOffedCounter() {
        assertThrows(
            DayOffChangeException.class,
            () -> repository.propagateDayOff(102L),
            "Unable to propagate a day off. There's no day off in counter 102"
        );
    }

    @Test
    @DisplayName("Пропагация не работает, если счетчике нет")
    @DatabaseSetup("/repository/value_counter/custom/before/with_day_offed_counters.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/custom/before/with_day_offed_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dontPropagateFromNonExistentCounter() {
        assertThrows(
            DayOffChangeException.class,
            () -> repository.propagateDayOff(404L),
            "Unable to propagate a day off. Cannot find counter 404"
        );
    }

    private CapacityCounter createCounter(long capacityId, String path, LocalDate day) {
        return initializeInTransaction(() -> {
            repository.createCounterSubtreeIfNotExists(path, day, UnitType.ORDER);
            return repository.findByCapacity(capacityId, day, UnitType.ORDER);
        });
    }

    private <T> T initializeInTransaction(Supplier<T> supplier) {
        return transactionTemplate.execute(s -> {
            T value = supplier.get();
            Hibernate.initialize(value);
            return value;
        });
    }

    @Nonnull
    private CapacityCounter counter(Long counterId, Long capacityId, LocalDate day, UnitType unitType) {
        return counter(counterId, capacityId, day, unitType, -1L, null);
    }

    @Nonnull
    private CapacityCounter counter(
        Long counterId,
        Long capacityId,
        LocalDate day,
        UnitType unitType,
        @Nullable Long threshold,
        @Nullable Long capacityValueId
    ) {
        return new CapacityCounter(
            counterId,
            capacityId,
            day,
            unitType,
            threshold,
            capacityValueId,
            0L,
            DayOffType.UNSET
        );
    }
}
