package ru.yandex.market.core.delivery.calendar.impl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Вспомогательные методы для тестирование календарей.
 *
 * @author ivmelnik
 * @since 26.12.17
 */
public final class CalendarTestHelper {

    private CalendarTestHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Получить список дат по начальной дате {@code beginDate} и номерам, относительно нее {@code daysNumbers}.
     */
    @Nonnull
    public static List<LocalDate> getLocalDates(@Nonnull LocalDate beginDate, int... daysNumbers) {
        return Arrays.stream(daysNumbers)
                .boxed()
                .map(beginDate::plusDays)
                .collect(Collectors.toList());
    }

}
