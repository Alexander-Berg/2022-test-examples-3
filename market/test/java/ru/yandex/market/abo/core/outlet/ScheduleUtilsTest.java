package ru.yandex.market.abo.core.outlet;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.abo.core.outlet.maps.model.OneDaySchedule;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author komarovns
 * @date 12.02.2020
 */
class ScheduleUtilsTest {
    @ParameterizedTest(name = "testHumanReadableSchedule_{index}")
    @MethodSource("testHumanReadableScheduleSource")
    void testHumanReadableSchedule(String expected, List<OneDaySchedule> scheduleLines) {
        assertEquals(expected, ScheduleUtils.humanReadable(scheduleLines));
    }

    public static Stream<Arguments> testHumanReadableScheduleSource() {
        return Stream.of(
                Arguments.of("пн-вс 09:00-20:00", List.of(
                        line(MONDAY, 540, 1200),
                        line(TUESDAY, 540, 1200),
                        line(THURSDAY, 540, 1200),
                        line(WEDNESDAY, 540, 1200),
                        line(FRIDAY, 540, 1200),
                        line(SATURDAY, 540, 1200),
                        line(SUNDAY, 540, 1200))
                ),
                Arguments.of("пн-вт 09:00-20:00, чт-вс 09:00-20:00", List.of(
                        line(MONDAY, 540, 1200),
                        line(TUESDAY, 540, 1200),
                        line(THURSDAY, 540, 1200),
                        line(FRIDAY, 540, 1200),
                        line(SATURDAY, 540, 1200),
                        line(SUNDAY, 540, 1200))
                ),
                Arguments.of("пн 09:00-20:00, вт 09:00-20:01, ср-чт 09:00-20:00, пт 09:01-20:00, сб 09:00-20:00, вс 00:00-24:00", List.of(
                        line(SUNDAY, 0, 1440),
                        line(MONDAY, 540, 1200),
                        line(TUESDAY, 540, 1201),
                        line(WEDNESDAY, 540, 1200),
                        line(THURSDAY, 540, 1200),
                        line(FRIDAY, 541, 1200),
                        line(SATURDAY, 540, 1200))
                ),
                Arguments.of("вт 00:00-24:00", List.of(line(TUESDAY, 0, 1440)))
        );
    }

    public static OneDaySchedule line(DayOfWeek day, int from, int to) {
        return new OneDaySchedule(day, from, to);
    }
}