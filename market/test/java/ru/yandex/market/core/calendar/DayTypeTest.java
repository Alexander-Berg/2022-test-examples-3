package ru.yandex.market.core.calendar;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.test.utils.UniquenessTestUtils;

/**
 * Тесты для {@link DayType}.
 */
class DayTypeTest {

    @Test
    void testUniqueness() {
        UniquenessTestUtils.checkUniqueness(DayType.class);
    }

}
