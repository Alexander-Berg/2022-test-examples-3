package ru.yandex.direct.core.entity.bs.resync.queue.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class BsResyncPriorityTest {
    private static final long MIN_TABLE_VALUE = -128;
    private static final long MAX_TABLE_VALUE = 127;

    private static final Map<Long, BsResyncPriority> priorityMap = new HashMap<>();

    @BeforeAll
    static void fillPriorityMap() {
        for (BsResyncPriority priority : BsResyncPriority.values()) {
            priorityMap.putIfAbsent(priority.value(), priority);
        }
    }

    /**
     * В таблице {@link ru.yandex.direct.dbschema.ppc.tables.BsResyncQueue} для приоритета используется TINYINT
     */
    @ParameterizedTest
    @EnumSource(BsResyncPriority.class)
    void checkValueForDbRestrictions(BsResyncPriority priority) {
        assertThat(priority.value())
                .isGreaterThanOrEqualTo(MIN_TABLE_VALUE)
                .isLessThanOrEqualTo(MAX_TABLE_VALUE);
    }

    @ParameterizedTest
    @EnumSource(BsResyncPriority.class)
    void checkValueUniqueness(BsResyncPriority priority) {
        BsResyncPriority first = priorityMap.get(priority.value());
        assertThat(priorityMap)
                .withFailMessage("Значение приоритета должно быть уникально (совпадает с %s)", first)
                .containsValue(priority);
    }
}
