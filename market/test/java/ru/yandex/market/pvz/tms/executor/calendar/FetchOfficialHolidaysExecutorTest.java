package ru.yandex.market.pvz.tms.executor.calendar;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.calendar.OfficialHoliday;
import ru.yandex.market.pvz.core.domain.calendar.OfficialHolidaysManager;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.tpl.common.calendar.YaCalendarService;
import ru.yandex.market.tpl.common.calendar.request.CalendarOutMode;
import ru.yandex.market.tpl.common.calendar.request.CalendarTarget;
import ru.yandex.market.tpl.common.calendar.response.CalendarHolidayDto;
import ru.yandex.market.tpl.common.calendar.response.CalendarHolidayType;
import ru.yandex.market.tpl.common.util.exception.TplExternalException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@EmbeddedDbTest
@Import({FetchOfficialHolidaysExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FetchOfficialHolidaysExecutorTest {

    private static final LocalDate FROM = LocalDate.of(2020, 1, 1);
    private static final LocalDate TO = LocalDate.of(2020, 12, 31);

    private final FetchOfficialHolidaysExecutor executor;
    private final OfficialHolidaysManager holidaysManager;
    private final CacheManager cacheManager;
    private final TestableClock clock;

    @MockBean
    private YaCalendarService yaCalendarService;

    @BeforeEach
    void setup() {
        clock.setFixed(
                LocalDate.of(2020, 1, 1).atStartOfDay(clock.getZone()).toInstant(),
                clock.getZone()
        );
        cacheManager.getCache("getOfficialHolidaysBetweenDates").clear();
    }

    @Test
    void testSuccessfulFetch() {
        holidaysManager.update(FROM, TO, List.of(OfficialHoliday.builder()
                .name("9 мая")
                .isWeekend(true)
                .date(LocalDate.of(2020, 5, 9))
                .build()));

        when(yaCalendarService.getHolidays(
                eq(CalendarTarget.RUSSIA),
                any(), any(),
                eq(CalendarOutMode.OVERRIDES)
        )).thenReturn(List.of(
                CalendarHolidayDto.builder()
                        .name("Перенос выходного с 9 мая")
                        .date(LocalDate.of(2020, 5, 10))
                        .type(CalendarHolidayType.WEEKEND)
                        .build(),

                CalendarHolidayDto.builder()
                        .name("День рождения Дениса")
                        .date(LocalDate.of(2020, 5, 29))
                        .type(CalendarHolidayType.HOLIDAY)
                        .build(),

                CalendarHolidayDto.builder()
                        .name("Скучный рабочий день")
                        .date(LocalDate.of(2020, 5, 13))
                        .type(CalendarHolidayType.WEEKDAY)
                        .build()
        ));
        executor.doRealJob(null);

        assertThat(holidaysManager.get(FROM, TO)).containsExactly(
                OfficialHoliday.builder()
                        .name("Перенос выходного с 9 мая")
                        .isWeekend(true)
                        .date(LocalDate.of(2020, 5, 10))
                        .build(),

                OfficialHoliday.builder()
                        .name("Скучный рабочий день")
                        .isWeekend(false)
                        .date(LocalDate.of(2020, 5, 13))
                        .build(),

                OfficialHoliday.builder()
                        .name("День рождения Дениса")
                        .isWeekend(true)
                        .date(LocalDate.of(2020, 5, 29))
                        .build()
        );
    }

    @Test
    void testUnsuccessfulFetch() {
        OfficialHoliday oldHoliday = OfficialHoliday.builder()
                .name("9 мая")
                .isWeekend(true)
                .date(LocalDate.of(2020, 5, 9))
                .build();

        holidaysManager.update(FROM, TO, List.of(oldHoliday));

        when(yaCalendarService.getHolidays(
                eq(CalendarTarget.RUSSIA),
                any(), any(),
                eq(CalendarOutMode.OVERRIDES)
        )).thenThrow(new TplExternalException("API IS BROKEN!!!111"));

        assertThatThrownBy(() -> executor.doRealJob(null));

        assertThat(holidaysManager.get(FROM, TO)).containsExactly(oldHoliday);

    }

}
