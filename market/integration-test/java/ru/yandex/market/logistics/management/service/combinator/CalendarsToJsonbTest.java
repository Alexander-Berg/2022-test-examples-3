package ru.yandex.market.logistics.management.service.combinator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.CalendarDay;
import ru.yandex.market.logistics.management.service.calendar.CalendarService;

@DatabaseSetup("/data/service/combinator/db/before/calendars_to_jsonb.xml")
class CalendarsToJsonbTest extends AbstractContextualTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CalendarService calendarService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private static final LocalDate HOLIDAY_1 = localDate(1);
    private static final LocalDate HOLIDAY_2 = localDate(2);
    private static final LocalDate WORK_DAY_3 = localDate(3);
    private static final LocalDate HOLIDAY_4 = localDate(4);
    private static final LocalDate WORK_DAY_5 = localDate(5);

    @BeforeEach
    void setup() {
        transactionTemplate.execute(status -> {
            calendarService.getCalendarById(100L).addCalendarDay(calendarDay(HOLIDAY_1, true));
            calendarService.getCalendarById(101L).addCalendarDay(calendarDay(HOLIDAY_2, true));
            calendarService.getCalendarById(102L).addCalendarDay(calendarDay(WORK_DAY_3, false));
            calendarService.getCalendarById(103L).addCalendarDay(calendarDay(HOLIDAY_4, true));
            calendarService.getCalendarById(104L).addCalendarDay(calendarDay(WORK_DAY_5, false));
            return null;
        });
    }

    @DisplayName("Конвертация календарей с наследованием в jsonb для выгрузки в YT")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void calendarsWithInheritanceConvertToJsonbCorrectly(
        String caseName,
        long[] calendarIds,
        boolean isHoliday,
        Set<LocalDate> expectedDates
    ) {
        softly.assertThat(convertToJsonbViaDatabaseFunction(calendarIds, isHoliday)).isEqualTo(expectedDates);
    }

    @Nonnull
    public static Stream<Arguments> calendarsWithInheritanceConvertToJsonbCorrectly() {
        return Stream.of(
            Arguments.of("Без предков", new long[]{100}, true, Set.of(HOLIDAY_1)),
            Arguments.of("Множество предков, выходные", new long[]{103}, true, Set.of(HOLIDAY_1, HOLIDAY_2, HOLIDAY_4)),
            Arguments.of("Множество предков, рабочие", new long[]{104}, false, Set.of(WORK_DAY_3, WORK_DAY_5)),
            Arguments.of("Нет календаря с таким id", new long[]{105}, true, Set.of())
        );
    }

    @Nonnull
    @SneakyThrows
    private Set<LocalDate> convertToJsonbViaDatabaseFunction(long[] calendarIds, boolean isHoliday) {
        String calendarDatesString = jdbcTemplate.queryForObject(
            String.format(
                "select calendars_to_jsonb(array[%s], %s)::text",
                Arrays.stream(calendarIds).mapToObj(String::valueOf).collect(Collectors.joining(", ")),
                isHoliday
            ),
            String.class
        );
        return objectMapper.readValue(calendarDatesString, CalendarDates.class).getDates();

    }

    @Nonnull
    private CalendarDay calendarDay(LocalDate localDate, boolean isHoliday) {
        return new CalendarDay()
            .setDay(localDate)
            .setIsHoliday(isHoliday);
    }

    @Nonnull
    private static LocalDate localDate(int addDaysCount) {
        return LocalDate.now().plusDays(addDaysCount);
    }

    @Data
    @Accessors(chain = true)
    public static class CalendarDates {
        private Set<LocalDate> dates;
    }
}
