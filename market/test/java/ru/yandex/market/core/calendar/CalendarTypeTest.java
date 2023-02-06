package ru.yandex.market.core.calendar;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.test.utils.UniquenessTestUtils;

/**
 * Тесты для {@link CalendarType}.
 */
class CalendarTypeTest {

    @Test
    void testUniqueness() {
        UniquenessTestUtils.checkUniqueness(CalendarType.class);
    }

}
