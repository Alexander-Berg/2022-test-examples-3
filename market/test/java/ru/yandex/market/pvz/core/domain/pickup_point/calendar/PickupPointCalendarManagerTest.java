package ru.yandex.market.pvz.core.domain.pickup_point.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.calendar.OfficialHoliday;
import ru.yandex.market.pvz.core.domain.calendar.OfficialHolidaysManager;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarManager;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarOverride;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarOverrideParams;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointOverridesDiff;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.model.PickupPointSchedule;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.model.PickupPointScheduleDay;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointCalendarManagerTest {

    // В июле 1 день месяца выпадает на понедельник, поэтому от него считать удобно
    private static final LocalDate FROM = LocalDate.of(2020, 6, 1);
    private static final LocalDate TO = LocalDate.of(2020, 6, 7);
    private static final String REGION = "Москва и Московская область";

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;

    @MockBean
    private OfficialHolidaysManager officialHolidaysManager;

    private final PickupPointCalendarManager calendarManager;

    @BeforeEach
    void setup() {
        when(officialHolidaysManager.getMap(any())).thenCallRealMethod();
    }

    @Test
    void testBuildCalendarWithOfficialHolidays() {

        when(officialHolidaysManager.get(eq(FROM), eq(TO))).thenReturn(List.of(
                new OfficialHoliday(day(DayOfWeek.WEDNESDAY), null, true),
                new OfficialHoliday(day(DayOfWeek.THURSDAY), null, true),
                new OfficialHoliday(day(DayOfWeek.SATURDAY), null, false)
        ));

        PickupPointSchedule schedule = PickupPointSchedule.builder()
                .worksOnHoliday(false)
                .scheduleDays(getDefaultWeek())
                .calendarOverrides(List.of(
                        new PickupPointCalendarOverride(null, day(DayOfWeek.TUESDAY), true),
                        new PickupPointCalendarOverride(null, day(DayOfWeek.THURSDAY), false)
                ))
                .build();


        Map<LocalDate, Boolean> calendar = calendarManager.buildHolidayCalendar(REGION, schedule, FROM, TO);

        assertThat(calendar).containsExactlyInAnyOrderEntriesOf(Map.of(
                day(DayOfWeek.MONDAY), false, // обычный рабочий день
                day(DayOfWeek.TUESDAY), true, // оверрайд (выходной)
                day(DayOfWeek.WEDNESDAY), true, // госпраздник
                day(DayOfWeek.THURSDAY), false, // госпраздник, но оверрайдом рабочий
                day(DayOfWeek.FRIDAY), false, // обычный рабочий день
                day(DayOfWeek.SATURDAY), false, // выходной, но оверрайдом рабочий
                day(DayOfWeek.SUNDAY), true // обычный выходной день
        ));

    }

    @Test
    void testBuildCalendarWithoutOfficialHolidays() {

        when(officialHolidaysManager.get(eq(FROM), eq(TO))).thenReturn(List.of(
                new OfficialHoliday(day(DayOfWeek.WEDNESDAY), null, true),
                new OfficialHoliday(day(DayOfWeek.THURSDAY), null, true),
                new OfficialHoliday(day(DayOfWeek.SATURDAY), null, false)
        ));

        PickupPointSchedule schedule = PickupPointSchedule.builder()
                .worksOnHoliday(true)
                .scheduleDays(getDefaultWeek())
                .calendarOverrides(Collections.emptyList())
                .build();

        Map<LocalDate, Boolean> calendar = calendarManager.buildHolidayCalendar(REGION, schedule, FROM, TO);

        assertThat(calendar).containsExactlyInAnyOrderEntriesOf(Map.of(
                day(DayOfWeek.MONDAY), false,
                day(DayOfWeek.TUESDAY), false,
                day(DayOfWeek.WEDNESDAY), false,
                day(DayOfWeek.THURSDAY), false,
                day(DayOfWeek.FRIDAY), false,
                day(DayOfWeek.SATURDAY), true,
                day(DayOfWeek.SUNDAY), true
        ));

    }

    @Test
    void testComputeOverrideDiffWithOfficialHolidays() {

        when(officialHolidaysManager.get(any())).thenReturn(List.of(
                new OfficialHoliday(day(DayOfWeek.WEDNESDAY), null, true),
                new OfficialHoliday(day(DayOfWeek.THURSDAY), null, true),
                new OfficialHoliday(day(DayOfWeek.SATURDAY), null, false)
        ));

        PickupPointSchedule schedule = PickupPointSchedule.builder()
                .worksOnHoliday(false)
                .scheduleDays(getDefaultWeek())
                .calendarOverrides(List.of(
                        new PickupPointCalendarOverride(null, day(DayOfWeek.TUESDAY), true),
                        new PickupPointCalendarOverride(null, day(DayOfWeek.THURSDAY), false),
                        new PickupPointCalendarOverride(null, day(DayOfWeek.FRIDAY), false)
                ))
                .build();


        PickupPointOverridesDiff diff = calendarManager.computeOverrideDiff(
                schedule,
                List.of(
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.MONDAY), true), // create
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.TUESDAY), false), // delete
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.WEDNESDAY), false), // create
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.THURSDAY), true), // delete
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.FRIDAY), true), // update
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.SATURDAY), true), // create
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.SUNDAY), true)), // do nothing
                FROM
        );


        assertThat(diff.getToCreate()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(List.of(
                        new PickupPointCalendarOverride(null, day(DayOfWeek.MONDAY), true),
                        new PickupPointCalendarOverride(null, day(DayOfWeek.WEDNESDAY), false),
                        new PickupPointCalendarOverride(null, day(DayOfWeek.SATURDAY), true)
                ));

        assertThat(diff.getToUpdate()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(List.of(
                        new PickupPointCalendarOverride(null, day(DayOfWeek.FRIDAY), true)
                ));

        assertThat(diff.getToDelete().stream().map(PickupPointCalendarOverride::getDate).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(List.of(
                        day(DayOfWeek.TUESDAY),
                        day(DayOfWeek.THURSDAY)
                ));
    }

    @Test
    void testComputeOverrideDiffWithoutOfficialHolidays() {

        when(officialHolidaysManager.get(any())).thenReturn(List.of(
                new OfficialHoliday(day(DayOfWeek.WEDNESDAY), null, true),
                new OfficialHoliday(day(DayOfWeek.THURSDAY), null, true),
                new OfficialHoliday(day(DayOfWeek.SATURDAY), null, false)
        ));

        PickupPointSchedule schedule = PickupPointSchedule.builder()
                .worksOnHoliday(true)
                .scheduleDays(getDefaultWeek())
                .calendarOverrides(List.of(
                        new PickupPointCalendarOverride(null, day(DayOfWeek.TUESDAY), true)
                ))
                .build();


        PickupPointOverridesDiff diff = calendarManager.computeOverrideDiff(
                schedule,
                List.of(
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.TUESDAY), false), // delete
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.FRIDAY), true), // create
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.SATURDAY), false)), // create
                FROM
        );


        assertThat(diff.getToCreate()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(List.of(
                        new PickupPointCalendarOverride(null, day(DayOfWeek.FRIDAY), true),
                        new PickupPointCalendarOverride(null, day(DayOfWeek.SATURDAY), false)
                ));

        assertThat(diff.getToUpdate()).usingRecursiveFieldByFieldElementComparator().isEmpty();

        assertThat(diff.getToDelete().stream().map(PickupPointCalendarOverride::getDate).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(List.of(
                        day(DayOfWeek.TUESDAY)
                ));
    }

    @Test
    void testThrowsOnDaysDuplication() {
        PickupPointSchedule schedule = PickupPointSchedule.builder()
                .worksOnHoliday(true)
                .scheduleDays(getDefaultWeek())
                .calendarOverrides(List.of())
                .build();


        assertThatThrownBy(() -> calendarManager.computeOverrideDiff(
                schedule,
                List.of(
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.MONDAY), false),
                        new PickupPointCalendarOverrideParams(day(DayOfWeek.MONDAY), true)),
                FROM
        ));
    }

    @Test
    void workingTime() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime now = OffsetDateTime.of(FROM, LocalTime.of(DEFAULT_TIME_FROM.plusHours(1).getHour(), 0), zone);
        clock.setFixed(now.toInstant(), zone);

        assertThat(calendarManager.isWorkingTime(pickupPoint, now)).isTrue();
    }

    @Test
    void notWorkingTime() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime now = OffsetDateTime.of(FROM, LocalTime.of(DEFAULT_TIME_FROM.minusHours(1).getHour(), 0), zone);
        clock.setFixed(now.toInstant(), zone);

        assertThat(calendarManager.isWorkingTime(pickupPoint, now)).isFalse();
    }

    @Test
    void notWorkingTimeBecauseHoliday() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .isWorkingDay(false)
                                                .build()
                                ))
                                .build())
                        .build())
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime now = OffsetDateTime.of(
                day(DayOfWeek.SATURDAY), LocalTime.of(DEFAULT_TIME_FROM.plusHours(1).getHour(), 0), zone);
        clock.setFixed(now.toInstant(), zone);

        assertThat(calendarManager.isWorkingTime(pickupPoint, now)).isFalse();
    }

    private List<PickupPointScheduleDay> getDefaultWeek() {
        return Arrays.stream(DayOfWeek.values())
                .map(dow -> PickupPointScheduleDay.builder()
                        .dayOfWeek(dow)
                        .isWorkingDay(dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY)
                        .build())
                .collect(Collectors.toList());
    }

    private LocalDate day(DayOfWeek dayOfWeek) {
        return FROM.plusDays(dayOfWeek.ordinal());
    }

}
