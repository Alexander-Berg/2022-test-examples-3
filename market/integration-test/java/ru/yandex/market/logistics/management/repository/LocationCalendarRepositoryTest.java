package ru.yandex.market.logistics.management.repository;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.CalendarDayDto;
import ru.yandex.market.logistics.management.domain.dto.Locations;
import ru.yandex.market.logistics.management.domain.entity.LocationCalendar;
import ru.yandex.market.logistics.management.service.calendar.CalendarService;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@SuppressWarnings("checkstyle:MagicNumber")
class LocationCalendarRepositoryTest extends AbstractContextualTest {

    @Autowired
    private LocationCalendarRepository locationCalendarRepository;

    @Autowired
    private CalendarService calendarService;

    private final LocalDate today = LocalDate.of(2018, 11, 1);

    @Test
    @Sql("/data/service/calendar/location_calendars.sql")
    @Transactional
    void testFilledDays() {
        LocationCalendar filledDays = locationCalendarRepository.findByLocationId(Locations.RUSSIA);

        softly.assertThat(filledDays).isNotNull();

        Set<CalendarDayDto> allDaysAfterDate = calendarService
            .getAllDaysAfterDate(filledDays.getCalendarId(), today);
        softly.assertThat(allDaysAfterDate)
            .as("days should be after the specified date")
            .extracting(CalendarDayDto::getId)
            .containsExactlyInAnyOrder(2L, 3L);

        softly.assertThat(filledDays.getCalendarId())
            .as("days should belong to the specified calendar")
            .isEqualTo(1L);
    }

    @Test
    @Sql("/data/service/calendar/location_calendars.sql")
    @Transactional
    void testEmptyDays() {

        LocationCalendar emptyDays = locationCalendarRepository.findByLocationId(Locations.MOSCOW);

        softly.assertThat(emptyDays).isNotNull();
        softly.assertThat(emptyDays.getCalendarId()).isEqualTo(3L);
        Set<CalendarDayDto> allDaysAfterDate = calendarService
            .getAllDaysAfterDate(emptyDays.getCalendarId(), today);
        softly.assertThat(allDaysAfterDate).isEmpty();
    }

    @Test
    @Sql("/data/service/calendar/location_calendars.sql")
    @Transactional
    void testDaysInPast() {

        LocationCalendar daysInPast = locationCalendarRepository.findByLocationId(200);

        softly.assertThat(daysInPast).isNotNull();

        softly.assertThat(daysInPast.getCalendarId()).isEqualTo(4L);
        Set<CalendarDayDto> allDaysAfterDate = calendarService
            .getAllDaysAfterDate(daysInPast.getCalendarId(), today);
        softly.assertThat(allDaysAfterDate).isEmpty();
    }
}
