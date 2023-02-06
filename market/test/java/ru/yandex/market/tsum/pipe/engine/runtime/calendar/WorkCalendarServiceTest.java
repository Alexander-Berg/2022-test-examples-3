package ru.yandex.market.tsum.pipe.engine.runtime.calendar;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.clients.utils.ZoneUtils;
import ru.yandex.market.tsum.pipe.engine.definition.common.JobSchedulerConstraintEntity;
import ru.yandex.market.tsum.pipe.engine.definition.common.SchedulerIntervalEntity;
import ru.yandex.market.tsum.pipe.engine.definition.common.TypeOfSchedulerConstraint;
import ru.yandex.market.tsum.pipe.engine.definition.common.WeekSchedulerConstraintEntity;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestBeansConfig;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 22.03.2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestBeansConfig.class,
    PipeServicesConfig.class,
    WorkCalendarTestConfig.class,
    MockCuratorConfig.class
})
public class WorkCalendarServiceTest {
    private static final ZoneId MOSCOW_ZONE_ID = ZoneId.of(ZoneUtils.MOSCOW_ZONE);

    @Autowired
    private WorkCalendarProvider workCalendarProvider;
    @Autowired
    private WorkCalendarService workCalendarService;

    @Test
    public void allowStartNowTest() {
        LocalDateTime nowDateTime = LocalDateTime.now(MOSCOW_ZONE_ID);

        DayOfWeek nowDayOfWeek = nowDateTime.getDayOfWeek();
        TypeOfSchedulerConstraint nowConstraint = workCalendarProvider.getTypeOfDay(nowDateTime.toLocalDate());

        JobSchedulerConstraintEntity constraints = new JobSchedulerConstraintEntity();
        WeekSchedulerConstraintEntity weekSchedulerConstraint = new WeekSchedulerConstraintEntity();
        constraints.getWeekConstraints().put(nowConstraint, weekSchedulerConstraint);

        weekSchedulerConstraint.addAllowedDayOfWeek(nowDayOfWeek, new SchedulerIntervalEntity());
        Instant scheduleTime = workCalendarService.getNextAllowedDate(constraints);
        Assert.assertTrue(!Instant.now().isBefore(scheduleTime));
    }

    @Test
    public void allowStartInNextWeek() {
        LocalDateTime nowDateTime = LocalDateTime.now(MOSCOW_ZONE_ID);

        DayOfWeek nowDayOfWeek = nowDateTime.getDayOfWeek();
        TypeOfSchedulerConstraint nowConstraint = workCalendarProvider.getTypeOfDay(nowDateTime.toLocalDate());

        JobSchedulerConstraintEntity constraints = new JobSchedulerConstraintEntity();
        WeekSchedulerConstraintEntity weekSchedulerConstraint = new WeekSchedulerConstraintEntity();
        constraints.getWeekConstraints().put(nowConstraint, weekSchedulerConstraint);

        weekSchedulerConstraint.addAllowedDayOfWeek(nowDayOfWeek,
            new SchedulerIntervalEntity(0, dateTimeToMinutesOfDay(nowDateTime) - 1));

        Instant scheduleTime = workCalendarService.getNextAllowedDate(constraints);

        LocalDateTime nextWeekTime = nowDateTime.plusWeeks(1).truncatedTo(ChronoUnit.DAYS);
        ZoneOffset moscowOffset = MOSCOW_ZONE_ID.getRules().getOffset(nextWeekTime);

        Assert.assertEquals(nextWeekTime.toInstant(moscowOffset), scheduleTime);
    }

    @Test
    public void allowStartLaterToday() {
        LocalDateTime nowDateTime = LocalDateTime.now(MOSCOW_ZONE_ID);

        DayOfWeek nowDayOfWeek = nowDateTime.getDayOfWeek();
        TypeOfSchedulerConstraint nowConstraint = workCalendarProvider.getTypeOfDay(nowDateTime.toLocalDate());

        JobSchedulerConstraintEntity constraints = new JobSchedulerConstraintEntity();
        WeekSchedulerConstraintEntity weekSchedulerConstraint = new WeekSchedulerConstraintEntity();
        constraints.getWeekConstraints().put(nowConstraint, weekSchedulerConstraint);

        weekSchedulerConstraint.addAllowedDayOfWeek(nowDayOfWeek,
            new SchedulerIntervalEntity(
                dateTimeToMinutesOfDay(nowDateTime) + 2,
                (int) TimeUnit.DAYS.toMinutes(1) - 1
            ));
        Instant scheduleTime = workCalendarService.getNextAllowedDate(constraints);
        LocalDateTime nextDayTime = nowDateTime.plusDays(1).truncatedTo(ChronoUnit.DAYS);
        ZoneOffset moscowOffset = MOSCOW_ZONE_ID.getRules().getOffset(nextDayTime);
        Assert.assertTrue(Instant.now().isBefore(scheduleTime)
            && nextDayTime.toInstant(moscowOffset).isAfter(scheduleTime));
    }

    private int dateTimeToMinutesOfDay(LocalDateTime dateTime) {
        return (int) TimeUnit.HOURS.toMinutes(dateTime.getHour()) + dateTime.getMinute();
    }

    @Test
    public void nextDayAllowedDateTest() {
        LocalDate nowNextDate = LocalDate.now(MOSCOW_ZONE_ID).plusDays(1);
        DayOfWeek nextDayOfWeek = nowNextDate.getDayOfWeek();
        TypeOfSchedulerConstraint nextConstraint = workCalendarProvider.getTypeOfDay(nowNextDate);
        JobSchedulerConstraintEntity nextDayConstraints = new JobSchedulerConstraintEntity();
        WeekSchedulerConstraintEntity nextWeekSchedulerConstraint = new WeekSchedulerConstraintEntity()
            .addAllowedDayOfWeek(nextDayOfWeek, new SchedulerIntervalEntity());
        nextDayConstraints.getWeekConstraints().put(nextConstraint, nextWeekSchedulerConstraint);

        Assert.assertTrue(Instant.now().isBefore(workCalendarService.getNextAllowedDate(nextDayConstraints)));
    }

    @Test
    public void canRunNowTest() {
        Instant instant = Instant.now();
        LocalDate nowDate = LocalDate.now(MOSCOW_ZONE_ID);
        DayOfWeek nowDayOfWeek = nowDate.getDayOfWeek();

        JobSchedulerConstraintEntity constraints = new JobSchedulerConstraintEntity();
        WeekSchedulerConstraintEntity weekSchedulerConstraint = new WeekSchedulerConstraintEntity();
        constraints.getWeekConstraints().put(TypeOfSchedulerConstraint.WORK, weekSchedulerConstraint);

        Assert.assertNull(workCalendarService.tryFindInstantForCurrentDay(
            constraints,
            TypeOfSchedulerConstraint.WORK,
            instant));

        weekSchedulerConstraint.addAllowedDayOfWeek(nowDayOfWeek, new SchedulerIntervalEntity());
        Assert.assertNotNull(workCalendarService.tryFindInstantForCurrentDay(
            constraints,
            TypeOfSchedulerConstraint.WORK,
            instant));
    }

    @Test
    public void canRunAtWorkDayTest() {
        LocalDate nowDate = LocalDate.now(MOSCOW_ZONE_ID);
        JobSchedulerConstraintEntity workConstraint = createOnlyForType(TypeOfSchedulerConstraint.WORK);

        Assert.assertNotNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.WORK,
            nowDate));
        Assert.assertNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.PRE_HOLIDAY,
            nowDate));
        Assert.assertNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.HOLIDAY,
            nowDate));
    }

    @Test
    public void canRunAtPreHolidayDayTest() {
        LocalDate nowDate = LocalDate.now(MOSCOW_ZONE_ID);
        JobSchedulerConstraintEntity workConstraint = createOnlyForType(TypeOfSchedulerConstraint.PRE_HOLIDAY);

        Assert.assertNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.WORK,
            nowDate));
        Assert.assertNotNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.PRE_HOLIDAY,
            nowDate));
        Assert.assertNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.HOLIDAY,
            nowDate));
    }

    @Test
    public void canRunAtHolidayDayTest() {
        LocalDate nowDate = LocalDate.now(MOSCOW_ZONE_ID);
        JobSchedulerConstraintEntity workConstraint = createOnlyForType(TypeOfSchedulerConstraint.HOLIDAY);

        Assert.assertNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.WORK,
            nowDate));
        Assert.assertNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.PRE_HOLIDAY,
            nowDate));
        Assert.assertNotNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.HOLIDAY,
            nowDate));
    }

    @Test
    public void canRunAtPreHolidayWithWorkConstraintTest() {
        LocalDate nowDate = LocalDate.now(MOSCOW_ZONE_ID);

        Map<TypeOfSchedulerConstraint, WeekSchedulerConstraintEntity> constraintEntity = new HashMap<>();
        constraintEntity.put(TypeOfSchedulerConstraint.WORK, new WeekSchedulerConstraintEntity(new SchedulerIntervalEntity()));
        JobSchedulerConstraintEntity workConstraint = new JobSchedulerConstraintEntity(constraintEntity);

        Assert.assertNotNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.WORK,
            nowDate));
        Assert.assertNotNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.PRE_HOLIDAY,
            nowDate));
        Assert.assertNull(workCalendarService.tryFindStartInstantForDay(
            workConstraint,
            TypeOfSchedulerConstraint.HOLIDAY,
            nowDate));
    }

    private JobSchedulerConstraintEntity createOnlyForType(TypeOfSchedulerConstraint typeOfSchedulerConstraint) {
        Map<TypeOfSchedulerConstraint, WeekSchedulerConstraintEntity> constraintEntity =
            Arrays.stream(TypeOfSchedulerConstraint.values())
                .collect(Collectors.toMap(Function.identity(),
                    t -> t.equals(typeOfSchedulerConstraint)
                        ? new WeekSchedulerConstraintEntity(new SchedulerIntervalEntity())
                        : new WeekSchedulerConstraintEntity()));

        return new JobSchedulerConstraintEntity(constraintEntity);
    }
}
