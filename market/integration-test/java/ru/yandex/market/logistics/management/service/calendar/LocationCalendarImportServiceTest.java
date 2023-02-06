package ru.yandex.market.logistics.management.service.calendar;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.CalendarDayDto;
import ru.yandex.market.logistics.management.domain.dto.CalendarHolidayDto;
import ru.yandex.market.logistics.management.domain.dto.CalendarHolidayType;
import ru.yandex.market.logistics.management.domain.dto.Locations;
import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.domain.entity.CalendarDay;
import ru.yandex.market.logistics.management.domain.entity.LocationCalendar;
import ru.yandex.market.logistics.management.repository.LocationCalendarRepository;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.TransactionalUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.domain.dto.CalendarHolidayType.HOLIDAY;
import static ru.yandex.market.logistics.management.domain.dto.CalendarHolidayType.WEEKEND;

@CleanDatabase
@Transactional
@SuppressWarnings("checkstyle:MagicNumber")
class LocationCalendarImportServiceTest extends AbstractContextualTest {

    @Autowired
    private LocationCalendarsSyncService locationCalendarImportService;

    @Autowired
    private YaCalendarService yaCalendarService;

    @Autowired
    private YaCalendarFetcher yaCalendarFetcher;

    @Autowired
    private LocationCalendarRepository locationCalendarRepository;

    @Autowired
    private TestableClock clock;

    @Autowired
    private CalendarService calendarService;

    private static final LocalDate NOW = LocalDate.of(2018, 12, 15);
    //to date = 2019-01-29
    private static final Period DAYS_45 = Period.ofDays(45);
    private static final List<Integer> RUSSIA_LIST = Collections.singletonList(Locations.RUSSIA);
    private static final List<Integer> TURKEY_LIST = Collections.singletonList(Locations.TURKEY);

    private static final Map<Integer, List<CalendarHolidayDto>> YA_CALENDAR_HOLIDAYS = ImmutableMap.of(
        Locations.RUSSIA, ImmutableList.of(
            calendarHolidayDto(2018, 12, 15, WEEKEND),
            calendarHolidayDto(2018, 12, 16, WEEKEND),
            calendarHolidayDto(2018, 12, 22, WEEKEND),
            calendarHolidayDto(2018, 12, 23, WEEKEND),
            calendarHolidayDto(2018, 12, 30, WEEKEND),
            calendarHolidayDto(2018, 12, 31, "Новый год", WEEKEND),
            calendarHolidayDto(2019, 1, 1, HOLIDAY),
            calendarHolidayDto(2019, 1, 2, HOLIDAY),
            calendarHolidayDto(2019, 1, 3, HOLIDAY),
            calendarHolidayDto(2019, 1, 4, HOLIDAY),
            calendarHolidayDto(2019, 1, 5, HOLIDAY),
            calendarHolidayDto(2019, 1, 6, HOLIDAY),
            calendarHolidayDto(2019, 1, 7, "Рождество", HOLIDAY),
            calendarHolidayDto(2019, 1, 8, HOLIDAY),
            calendarHolidayDto(2019, 1, 12, WEEKEND),
            calendarHolidayDto(2019, 1, 13, WEEKEND),
            calendarHolidayDto(2019, 1, 19, WEEKEND),
            calendarHolidayDto(2019, 1, 20, WEEKEND),
            calendarHolidayDto(2019, 1, 26, WEEKEND),
            calendarHolidayDto(2019, 1, 27, WEEKEND)
        ),
        Locations.TURKEY, ImmutableList.of(
            calendarHolidayDto(2018, 12, 15, WEEKEND),
            calendarHolidayDto(2018, 12, 16, WEEKEND),
            calendarHolidayDto(2018, 12, 22, WEEKEND),
            calendarHolidayDto(2018, 12, 23, WEEKEND),
            calendarHolidayDto(2018, 12, 29, WEEKEND),
            calendarHolidayDto(2018, 12, 30, WEEKEND),
            calendarHolidayDto(2019, 1, 1, "New Year's Day", HOLIDAY),
            calendarHolidayDto(2019, 1, 5, WEEKEND),
            calendarHolidayDto(2019, 1, 6, WEEKEND),
            calendarHolidayDto(2019, 1, 12, WEEKEND),
            calendarHolidayDto(2019, 1, 13, WEEKEND),
            calendarHolidayDto(2019, 1, 19, WEEKEND),
            calendarHolidayDto(2019, 1, 20, WEEKEND),
            calendarHolidayDto(2019, 1, 26, WEEKEND),
            calendarHolidayDto(2019, 1, 27, WEEKEND)
        )
    );

    @SuppressWarnings("ParameterNumber")
    @ParameterizedTest(name = "{index}: {8}")
    @ArgumentsSource(ArgumentsProviderImpl.class)
    @Commit
    void testCalendarImport(
        List<Integer> locationsToImport,
        Period periodToImport,
        List<LocationCalendar> existingLocationCalendars,
        List<Calendar> existingCalendars,
        Map<Integer, List<CalendarHolidayDto>> yaCalendarHolidays,
        LocalDate now,
        List<LocationCalendar> expectedLocationCalendars,
        Map<Long, Set<CalendarDayDto>> expectedCalendars,
        String description
    ) {

        arrange(locationsToImport,
            periodToImport,
            existingLocationCalendars,
            existingCalendars,
            yaCalendarHolidays,
            now
        );

        locationCalendarImportService.syncLocationCalendars();

        TransactionalUtils.doWithinRollbackTransaction(() -> asserts(expectedLocationCalendars, expectedCalendars));
    }

    @Test
    void testOneFetcherFailed() {

        clock.setFixed(NOW.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        locationCalendarImportService.setLocationCalendarFetcherMap(
            toYaCalendarFetcherMap(Arrays.asList(Locations.RUSSIA, Locations.TURKEY))
        );

        when(yaCalendarService.fetchCalendarHolidays(eq(Locations.RUSSIA), any(), any()))
            .thenThrow(new RuntimeException("network exception"));
        when(yaCalendarService.fetchCalendarHolidays(eq(Locations.TURKEY), any(), any()))
            .thenReturn(YA_CALENDAR_HOLIDAYS.get(Locations.TURKEY));

        locationCalendarImportService.syncLocationCalendars();

        Map<Long, Set<CalendarDayDto>> expectedCalendars =
            Collections.singletonMap(1L, transform(YA_CALENDAR_HOLIDAYS.get(Locations.TURKEY)));
        asserts(mapYaCalendarHolidaysToLocationCalendars(Locations.TURKEY), expectedCalendars);
    }

    @Test
    void testAllFetchersFailed() {

        clock.setFixed(NOW.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        locationCalendarImportService.setLocationCalendarFetcherMap(
            toYaCalendarFetcherMap(Arrays.asList(Locations.RUSSIA, Locations.TURKEY))
        );

        when(yaCalendarService.fetchCalendarHolidays(eq(Locations.RUSSIA), any(), any()))
            .thenThrow(new RuntimeException("network exception RUSSIA"));
        when(yaCalendarService.fetchCalendarHolidays(eq(Locations.TURKEY), any(), any()))
            .thenThrow(new RuntimeException("network exception TURKEY"));

        assertThatThrownBy(locationCalendarImportService::syncLocationCalendars)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to import all");
    }


    private void asserts(List<LocationCalendar> expectedLocationCalendars,
                         Map<Long, Set<CalendarDayDto>> expectedCalendars) {
        List<LocationCalendar> newStateLocationCalendars = locationCalendarRepository.findAll();

        softly.assertThat(newStateLocationCalendars).containsExactlyInAnyOrderElementsOf(expectedLocationCalendars);

        Map<LocationCalendar, LocationCalendar> expectedLocationCalendarsMap =
            expectedLocationCalendars.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));

        Set<Long> calendarIds = newStateLocationCalendars.stream()
            .map(LocationCalendar::getCalendarId)
            .collect(Collectors.toSet());
        Map<Long, Set<CalendarDayDto>> daysByCalendarId = calendarService.getAllDaysByCalendarId(calendarIds);
        for (LocationCalendar newStateLocationCalendar : newStateLocationCalendars) {
            LocationCalendar expectedLocationCalendar = expectedLocationCalendarsMap.remove(newStateLocationCalendar);

            softly.assertThat(expectedLocationCalendar)
                .as("location calendar is not expected!")
                .isNotNull();

            softly.assertThat(newStateLocationCalendar.getCalendarId())
                .as("location calendar has no reference to Calendar")
                .isNotNull();

            assertHasSameDates(expectedCalendars.get(expectedLocationCalendar.getCalendarId()),
                daysByCalendarId.get(newStateLocationCalendar.getCalendarId()));
        }

        softly.assertThat(expectedLocationCalendarsMap)
            .as("expected other Location Calendar, but not found!")
            .isEmpty();

    }

    private void assertHasSameDates(Set<CalendarDayDto> expected, Set<CalendarDayDto> actual) {
        Set<LocalDate> expectedDates = expected.stream().map(CalendarDayDto::getDay).collect(Collectors.toSet());
        Set<LocalDate> actualDays = actual.stream().map(CalendarDayDto::getDay).collect(Collectors.toSet());
        softly.assertThat(actualDays)
            .as("new and expected CalendarDays arent equal")
            .containsExactlyInAnyOrderElementsOf(expectedDates);
    }

    private void arrange(List<Integer> locationsToImport,
                         Period periodToImport,
                         List<LocationCalendar> locationCalendars,
                         List<Calendar> calendars,
                         Map<Integer, List<CalendarHolidayDto>> yaCalendarHolidays,
                         LocalDate now) {


        clock.setFixed(now.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        locationCalendarImportService.setLocationCalendarFetcherMap(toYaCalendarFetcherMap(locationsToImport));
        locationCalendarImportService.setPeriodToImport(periodToImport);

        calendars.forEach(calendarService::saveCalendar);
        locationCalendarRepository.saveAll(locationCalendars);
        TransactionalUtils.commit();

        for (Map.Entry<Integer, List<CalendarHolidayDto>> entry : yaCalendarHolidays.entrySet()) {
            when(yaCalendarService.fetchCalendarHolidays(entry.getKey(), now, now.plus(periodToImport)))
                .thenReturn(entry.getValue());
        }
    }

    private Map<Integer, CalendarDaysFetcher> toYaCalendarFetcherMap(List<Integer> locations) {
        return locations.stream().collect(Collectors.toMap(Function.identity(), location -> yaCalendarFetcher));
    }


    private static LocationCalendar toLocationCalendar(int locationId, long calendarId) {
        return locationCalendar(locationId, calendarId);
    }

    private static List<LocationCalendar> mapYaCalendarHolidaysToLocationCalendars(Integer... locations) {
        List<LocationCalendar> result = new ArrayList<>();

        for (int i = 0; i < locations.length; i++) {
            result.add(toLocationCalendar(locations[i], i + 1));
        }

        return result;
    }

    private static LocationCalendar locationCalendar(int locationId, Long calendarId) {
        LocationCalendar locationCalendar = new LocationCalendar();
        locationCalendar.setLocationId(locationId);
        locationCalendar.setCalendarId(calendarId);

        return locationCalendar;
    }

    private static CalendarDayDto calendarDay(int year, int month, int day) {
        return new CalendarDayDto(1, LocalDate.of(year, month, day), true);
    }

    private static CalendarDayDto calendarDay(LocalDate date) {
        return new CalendarDayDto(1, date, true);
    }

    private static Set<CalendarDayDto> transform(List<CalendarHolidayDto> holidays) {
        return holidays.stream()
            .map(
                dto -> calendarDay(dto.getDate())
            )
            .collect(Collectors.toSet());
    }

    private static CalendarHolidayDto calendarHolidayDto(int year, int month, int day, CalendarHolidayType type) {
        return calendarHolidayDto(year, month, day, null, type);
    }

    private static CalendarHolidayDto calendarHolidayDto(int year,
                                                         int month,
                                                         int day,
                                                         String desc,
                                                         CalendarHolidayType type) {
        return new CalendarHolidayDto(
            LocalDate.of(year, month, day),
            desc,
            type
        );
    }

    private static Calendar createCalendar(Set<CalendarDayDto> days) {
        List<CalendarDay> calendarDays = days.stream()
            .map(day -> new CalendarDay().setDay(day.getDay()).setIsHoliday(day.getIsHoliday()))
            .collect(Collectors.toList());
        return new Calendar().addCalendarDays(calendarDays);
    }

    public static class ArgumentsProviderImpl implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            return Stream.of(arguments1(), arguments2(), arguments3(), arguments4(), arguments5(), arguments6(),
                arguments7());
        }

        /**
         * существующих дней нет, все новые загрузились.
         */
        private static Arguments arguments1() {
            return Arguments.of(
                RUSSIA_LIST,
                DAYS_45,
                Collections.emptyList(),
                Collections.emptyList(),
                YA_CALENDAR_HOLIDAYS,
                NOW,
                mapYaCalendarHolidaysToLocationCalendars(Locations.RUSSIA),
                ImmutableMap.of(1L, transform(YA_CALENDAR_HOLIDAYS.get(Locations.RUSSIA))),
                "no existing, all new added"
            );
        }

        /**
         * один существующий день, он в прошлом, его не трогаем, все новые загрузились.
         */
        private static Arguments arguments2() {
            CalendarDayDto russiaDayInPast = calendarDay(2018, 12, 5);

            List<LocationCalendar> existing = Collections.singletonList(
                locationCalendar(
                    Locations.RUSSIA,
                    1L
                )
            );

            List<Calendar> existingCalendars = ImmutableList.of(
                createCalendar(
                    ImmutableSet.of(russiaDayInPast)
                )
            );

            List<LocationCalendar> expected = mapYaCalendarHolidaysToLocationCalendars(Locations.RUSSIA);

            Set<CalendarDayDto> expectedRussian = transform(YA_CALENDAR_HOLIDAYS.get(Locations.RUSSIA));
            expectedRussian.add(russiaDayInPast);

            return Arguments.of(
                RUSSIA_LIST,
                DAYS_45,
                existing,
                existingCalendars,
                YA_CALENDAR_HOLIDAYS,
                NOW,
                expected,
                ImmutableMap.of(1L, expectedRussian),
                "one day existing, all new added"
            );
        }


        /**
         * один существующий день, обновляем его, все новые загрузились.
         */
        private static Arguments arguments3() {
            List<LocationCalendar> existing = Collections.singletonList(
                locationCalendar(
                    Locations.RUSSIA,
                    1L
                )
            );

            List<Calendar> existingCalendars = ImmutableList.of(
                createCalendar(
                    ImmutableSet.of(calendarDay(2018, 12, 31))
                )
            );

            return Arguments.of(
                RUSSIA_LIST,
                DAYS_45,
                existing,
                existingCalendars,
                YA_CALENDAR_HOLIDAYS,
                NOW,
                mapYaCalendarHolidaysToLocationCalendars(Locations.RUSSIA),
                ImmutableMap.of(1L, transform(YA_CALENDAR_HOLIDAYS.get(Locations.RUSSIA))),
                "one day existing, the one updated, other new added"
            );
        }

        /**
         * один существующий день, который перестал быть праздником, его удаляем, все новые загрузились.
         */
        private static Arguments arguments4() {
            List<LocationCalendar> existing = Collections.singletonList(
                locationCalendar(
                    Locations.RUSSIA,
                    1L
                )
            );

            List<Calendar> existingCalendars = ImmutableList.of(
                createCalendar(
                    ImmutableSet.of(calendarDay(2018, 12, 25))
                )
            );

            return Arguments.of(
                RUSSIA_LIST,
                DAYS_45,
                existing,
                existingCalendars,
                YA_CALENDAR_HOLIDAYS,
                NOW,
                mapYaCalendarHolidaysToLocationCalendars(Locations.RUSSIA),
                ImmutableMap.of(1L, transform(YA_CALENDAR_HOLIDAYS.get(Locations.RUSSIA))),
                "one day existing, the one deleted, other new added"
            );
        }

        /**
         * один существующий день, за пределами загружаемого, его удаляем, все новые загрузились.
         */
        private static Arguments arguments5() {
            List<LocationCalendar> existing = Collections.singletonList(
                locationCalendar(
                    Locations.RUSSIA,
                    1L
                )
            );

            List<Calendar> existingCalendars = ImmutableList.of(
                createCalendar(
                    ImmutableSet.of(
                        calendarDay(2020, 12, 31)
                    )
                )
            );

            return Arguments.of(
                RUSSIA_LIST,
                DAYS_45,
                existing,
                existingCalendars,
                YA_CALENDAR_HOLIDAYS,
                NOW,
                mapYaCalendarHolidaysToLocationCalendars(Locations.RUSSIA),
                ImmutableMap.of(1L, transform(YA_CALENDAR_HOLIDAYS.get(Locations.RUSSIA))),
                "one day existing in the future, the one deleted, other new added"
            );
        }


        /**
         * несколько локаций, существующих дней нет.
         */
        private static Arguments arguments6() {

            Long calendarId = 1L;
            Map<Long, Set<CalendarDayDto>> expectedCalendars = ImmutableMap.of(
                calendarId++, transform(YA_CALENDAR_HOLIDAYS.get(Locations.RUSSIA)),
                calendarId++, transform(YA_CALENDAR_HOLIDAYS.get(Locations.TURKEY))
            );

            return Arguments.of(
                ListUtils.union(TURKEY_LIST, RUSSIA_LIST),
                DAYS_45,
                Collections.emptyList(),
                Collections.emptyList(),
                YA_CALENDAR_HOLIDAYS,
                NOW,
                mapYaCalendarHolidaysToLocationCalendars(Locations.RUSSIA, Locations.TURKEY),
                expectedCalendars,
                "two locations, all new added"
            );
        }

        /**
         * несколько локаций, существующие есть в прошлом, существующие есть в будущем, несколько новых дней.
         */
        private static Arguments arguments7() {

            CalendarDayDto russiaDayInPast = calendarDay(2018, 12, 5);
            CalendarDayDto turkeyDayInPast = calendarDay(2018, 12, 5);

            List<LocationCalendar> existing = ImmutableList.of(
                locationCalendar(
                    Locations.RUSSIA,
                    1L
                ),
                locationCalendar(
                    Locations.TURKEY,
                    2L
                )
            );

            List<Calendar> existingCalendars = ImmutableList.of(
                createCalendar(
                    ImmutableSet.of(
                        russiaDayInPast,
                        calendarDay(2018, 12, 29),
                        calendarDay(2018, 12, 31))
                ),
                createCalendar(
                    ImmutableSet.of(
                        turkeyDayInPast,
                        calendarDay(2018, 12, 24),
                        calendarDay(2019, 1, 1))
                )
            );

            long calendarId = 1L;
            List<LocationCalendar> expected =
                mapYaCalendarHolidaysToLocationCalendars(Locations.RUSSIA, Locations.TURKEY);

            Set<CalendarDayDto> expectedTurkey = transform(YA_CALENDAR_HOLIDAYS.get(Locations.TURKEY));
            expectedTurkey.add(turkeyDayInPast);

            Set<CalendarDayDto> expectedRussian = transform(YA_CALENDAR_HOLIDAYS.get(Locations.RUSSIA));
            expectedRussian.add(russiaDayInPast);

            Map<Long, Set<CalendarDayDto>> expectedCalendars = ImmutableMap.of(
                calendarId++, expectedRussian,
                calendarId++, expectedTurkey
            );

            return Arguments.of(
                ListUtils.union(TURKEY_LIST, RUSSIA_LIST),
                DAYS_45,
                existing,
                existingCalendars,
                YA_CALENDAR_HOLIDAYS,
                NOW,
                expected,
                expectedCalendars,
                "two locations, some in the past, some deleted, some new"
            );
        }
    }
}
