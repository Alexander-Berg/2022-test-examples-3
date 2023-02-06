package ru.yandex.market.logistics.management.service.calendar;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.CalendarDayDto;
import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.domain.entity.CalendarDay;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class CalendarServiceTest extends AbstractContextualTest {

    private static final Long ROOT_CALENDAR_ID = 1L;
    private static final Long CALENDAR_ID_WITH_OVERRIDING = 2L;
    private static final Long CALENDAR_ID_WITHOUT_OVERRIDING = 3L;
    private static final Long CALENDAR_ID_INHERITED_ON_EMPTY_CALENDAR = 4L;

    /**
     * В прошлой версии теста создавались Set, в которых неявно использовался equals() в {@link CalendarDay},
     * сейчас уже нельзя просто использовать equals() в {@link CalendarDayDto}, так как он другой.
     * Этот компаратор собственно и нужен для того, чтобы equals() выглядел, как раньше в expected Setах.
     */
    private static final Comparator<CalendarDayDto> CALENDAR_DAY_DTO_COMPARATOR_FOR_EXPECTED_SETS_CREATION =
        Comparator.comparing(CalendarDayDto::getDay);

    @Autowired
    private CalendarService calendarService;

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findAllDaysByCalendarIdWithoutInheritance() {
        Set<CalendarDayDto> actual = calendarService.getAllDaysByCalendarId(ROOT_CALENDAR_ID);
        Set<CalendarDayDto> expected = getRootDays();
        softly.assertThat(actual).usingFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findAllDaysByCalendarIdWithInheritanceAndOverridingParentDays() {
        Set<CalendarDayDto> actual = calendarService.getAllDaysByCalendarId(CALENDAR_ID_WITH_OVERRIDING);
        Set<CalendarDayDto> expected = getDaysWithOverriding();
        softly.assertThat(actual).usingFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findAllDaysByCalendarIdWithInheritanceAndWithoutOverriding() {
        Set<CalendarDayDto> actual = calendarService.getAllDaysByCalendarId(CALENDAR_ID_WITHOUT_OVERRIDING);
        Set<CalendarDayDto> expected = getDaysWithOverriding();
        softly.assertThat(actual).usingFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findAllDaysByCalendarIdWithCalendarInheritedOnEmptyCalendar() {
        Set<CalendarDayDto> actual = calendarService.getAllDaysByCalendarId(CALENDAR_ID_INHERITED_ON_EMPTY_CALENDAR);
        Set<CalendarDayDto> expected = getDaysWithInheritanceOnEmptyCalendar();
        softly.assertThat(actual).usingFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findNotExistsCalendar() {
        Set<CalendarDayDto> actual = calendarService.getAllDaysByCalendarId(123L);
        softly.assertThat(actual).isEmpty();
    }

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findAllHolidaysByCalendarId() {
        Set<CalendarDayDto> actual = calendarService.getAllHolidaysByCalendarId(CALENDAR_ID_WITH_OVERRIDING);
        Set<CalendarDayDto> expected = Collections.singleton(getHoliday());

        softly.assertThat(actual).usingFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findAllWeekDaysByCalendarId() {
        Set<CalendarDayDto> actual = calendarService.getAllWeekDaysByCalendarId(CALENDAR_ID_WITH_OVERRIDING);
        Set<CalendarDayDto> expected = getDaysWithOverriding();
        expected.removeIf(CalendarDayDto::getIsHoliday);

        softly.assertThat(actual).usingFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expected);
    }


    /**
     * Для каленадаря с id=5 единственным выходным после 2018-10-02 будет 2018-10-07,
     * который добавляется в его календаре.
     */

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findAllHolidaysAfterDateByCalendarId() {
        Set<CalendarDayDto> actual =
            calendarService.getAllHolidaysAfterDate(CALENDAR_ID_INHERITED_ON_EMPTY_CALENDAR,
                LocalDate.of(2018, 10, 2));
        Set<CalendarDayDto> expected = getDayDtoWithInheritanceOnEmptyCalendar();

        softly.assertThat(actual).usingFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * Для каленадаря с id=5 выходными между 2018-10-01 и 2018-10-07 будут 2018-10-01 и 2018-10-07
     */

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void findAllHolidaysBetweenDatesByCalendarId() {
        Set<CalendarDayDto> actual =
            calendarService
                .getAllHolidaysBetweenDates(CALENDAR_ID_INHERITED_ON_EMPTY_CALENDAR,
                    LocalDate.of(2018, 10, 1),
                    LocalDate.of(2018, 10, 7)
                );
        Set<CalendarDayDto> expected = new HashSet<>(2);
        expected.add(getHoliday());
        expected.addAll(getDayDtoWithInheritanceOnEmptyCalendar());

        softly.assertThat(actual).usingFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expected);
    }


    @Test
    @Sql("/data/service/calendar/calendars.sql")
    public void findAllDaysForBatchOfCalendarIds() {
        Map<Long, Set<CalendarDayDto>> daysByCalendarId =
            calendarService.getAllDaysByCalendarId(Arrays.asList(ROOT_CALENDAR_ID, CALENDAR_ID_WITH_OVERRIDING,
                CALENDAR_ID_WITHOUT_OVERRIDING, CALENDAR_ID_INHERITED_ON_EMPTY_CALENDAR));
        softly.assertThat(daysByCalendarId).hasSize(4);

        Set<CalendarDayDto> rootActual = daysByCalendarId.get(ROOT_CALENDAR_ID);
        softly.assertThat(rootActual).isNotNull();
        Set<CalendarDayDto> withOverridingActual = daysByCalendarId.get(CALENDAR_ID_WITH_OVERRIDING);
        softly.assertThat(withOverridingActual).isNotNull();
        Set<CalendarDayDto> withoutOverridingActual = daysByCalendarId.get(CALENDAR_ID_WITHOUT_OVERRIDING);
        softly.assertThat(withoutOverridingActual).isNotNull();
        Set<CalendarDayDto> inheritedOnEmptyCalendarActual =
            daysByCalendarId.get(CALENDAR_ID_INHERITED_ON_EMPTY_CALENDAR);
        softly.assertThat(inheritedOnEmptyCalendarActual).isNotNull();

        Set<CalendarDayDto> rootExpected = getRootDays();
        Set<CalendarDayDto> withOverridingExpected = getDaysWithOverriding();
        Set<CalendarDayDto> withoutOverridingExpected = getDaysWithOverriding();
        Set<CalendarDayDto> inheritedOnEmptyCalendarExpected = getDaysWithInheritanceOnEmptyCalendar();

        softly.assertThat(rootActual).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(rootExpected);
        softly.assertThat(withOverridingActual).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(withOverridingExpected);
        softly.assertThat(withoutOverridingActual).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(withoutOverridingExpected);
        softly.assertThat(inheritedOnEmptyCalendarActual).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(inheritedOnEmptyCalendarExpected);
    }

    /**
     * На данный момент Calendar возвращает только свои дни, без родительских
     */
    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void calendarGetDaysReturnNotInheritedValues() {
        Calendar calendar = calendarService.getCalendarById(CALENDAR_ID_INHERITED_ON_EMPTY_CALENDAR);
        Set<CalendarDay> actual = calendar.getDays();
        Set<CalendarDay> expected = getDayWithInheritanceOnEmptyCalendar();
        softly.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @Sql("/data/service/calendar/calendars.sql")
    void testDeleteParentCalendar() {
        calendarService.deleteCalendarById(3L);
        Calendar child = calendarService.getCalendarById(4L);
        softly.assertThat(child.getParent().getId()).isEqualTo(2L);

        calendarService.deleteCalendarsByIds(Set.of(2L, 1L));
        child = calendarService.getCalendarById(4L);
        softly.assertThat(child.getParent()).isNull();
    }

    @Test
    @DatabaseSetup("/data/service/calendar/non_intersecting_calendars.xml")
    void mergeCalendarsTest_nonIntersecting() {
        Calendar calendar = calendarService.mergeCalendars(50L, 60L);

        softly.assertThat(calendar.getDays()).containsAll(calendarService.getCalendarDaysByCalendarId(50L));
        softly.assertThat(calendar.getDays()).containsAll(calendarService.getCalendarDaysByCalendarId(60L));
    }

    @Test
    @DatabaseSetup("/data/service/calendar/intersecting_calendars.xml")
    void mergeCalendarsTest_intersecting() {
        Calendar calendar = calendarService.mergeCalendars(50L, 60L);
        CalendarDay day1 = new CalendarDay().setDay(LocalDate.of(2020, 8, 3)).setIsHoliday(true);
        CalendarDay day2 = new CalendarDay().setDay(LocalDate.of(2020, 8, 21)).setIsHoliday(true);
        CalendarDay day3 = new CalendarDay().setDay(LocalDate.of(2020, 8, 23)).setIsHoliday(true);
        CalendarDay day4 = new CalendarDay().setDay(LocalDate.of(2020, 8, 29)).setIsHoliday(false);
        CalendarDay day5 = new CalendarDay().setDay(LocalDate.of(2020, 8, 30)).setIsHoliday(false);
        CalendarDay day6 = new CalendarDay().setDay(LocalDate.of(2020, 8, 31)).setIsHoliday(true);

        softly.assertThat(calendar.getDays())
            .containsExactlyInAnyOrderElementsOf(Set.of(day1, day2, day3, day4, day5, day6));
    }

    /**
     * Инициализируем коллекцию в обратном порядке, чтобы сначала добавились дочерние дни,
     * а потом уже родительские.
     */
    private Set<CalendarDayDto> getDaysWithInheritanceOnEmptyCalendar() {
        Set<CalendarDayDto> days = new TreeSet<>(CALENDAR_DAY_DTO_COMPARATOR_FOR_EXPECTED_SETS_CREATION);

        days.addAll(getDayDtoWithInheritanceOnEmptyCalendar());
        days.addAll(getOverrideDays());
        days.addAll(getRootDays());

        return days;
    }

    private Set<CalendarDayDto> getDaysWithOverriding() {
        Set<CalendarDayDto> days = getOverrideDays();
        days.addAll(getRootDays());
        return days;
    }

    private Set<CalendarDayDto> getRootDays() {
        Set<CalendarDayDto> days = new TreeSet<>(CALENDAR_DAY_DTO_COMPARATOR_FOR_EXPECTED_SETS_CREATION);
        days.add(new CalendarDayDto(1L, LocalDate.of(2018, 10, 1), false));
        days.add(new CalendarDayDto(2L, LocalDate.of(2018, 10, 2), false));

        return days;
    }

    private Set<CalendarDayDto> getOverrideDays() {
        Set<CalendarDayDto> days = new TreeSet<>(CALENDAR_DAY_DTO_COMPARATOR_FOR_EXPECTED_SETS_CREATION);
        days.add(getHoliday());
        days.add(new CalendarDayDto(4L, LocalDate.of(2018, 11, 3), false));

        return days;
    }

    private Set<CalendarDay> getDayWithInheritanceOnEmptyCalendar() {
        return Collections.singleton(
            new CalendarDay()
                .setId(5L)
                .setDay(LocalDate.of(2018, 10, 7))
                .setCalendar(new Calendar()
                    .setId(CALENDAR_ID_INHERITED_ON_EMPTY_CALENDAR)
                    .setParent(new Calendar()
                        .setId(CALENDAR_ID_WITHOUT_OVERRIDING)
                        .setParent(getCalendarWithOverriding())
                    )
                )
                .setIsHoliday(true)
        );
    }

    private Set<CalendarDayDto> getDayDtoWithInheritanceOnEmptyCalendar() {
        return Collections.singleton(
            new CalendarDayDto(5L, LocalDate.of(2018, 10, 7), true)
        );
    }

    private CalendarDayDto getHoliday() {
        return new CalendarDayDto(3L, LocalDate.of(2018, 10, 1), true);
    }

    private Calendar getRootCalendar() {
        return new Calendar().setId(ROOT_CALENDAR_ID);
    }

    private Calendar getCalendarWithOverriding() {
        return new Calendar()
            .setId(CALENDAR_ID_WITH_OVERRIDING)
            .setParent(getRootCalendar());
    }
}
