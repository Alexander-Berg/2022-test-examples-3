package ru.yandex.market.core.schedule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.schedule.ScheduleLine.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ScheduleFactoryTest {
    @Test
    public void addLine1() throws Exception {
        ScheduleFactory scheduleFactory = new ScheduleFactory();

        scheduleFactory.addLine(DayOfWeek.MONDAY, DayOfWeek.FRIDAY,
                9, 0, 9, 0, 1);
    }

    @Test
    public void addLine() {
        ScheduleFactory scheduleFactory = new ScheduleFactory();
        assertThatThrownBy(() -> scheduleFactory.addLine(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, 9, 0, 9, 0, 2))
                .isInstanceOf(ScheduleFactory.SchedulingException.class);
    }

    @Test
    public void addLineVersion1() throws Exception {
        ScheduleFactory scheduleFactory = new ScheduleFactory();

        scheduleFactory.addLine(DayOfWeek.MONDAY, DayOfWeek.FRIDAY,
                0, 0, 24, 0, 1);

        Schedule schedule = scheduleFactory.getSchedule();
        Assertions.assertNotNull(schedule.getLines());
        Assertions.assertEquals(1, schedule.getLines().size());
        ScheduleLine onlyLine = schedule.getLines().get(0);
        Assertions.assertEquals("0:00", onlyLine.getStartTime());
        Assertions.assertEquals("23:59", onlyLine.getEndTime());
    }

    @Test
    public void addLineVersion2() throws Exception {
        ScheduleFactory scheduleFactory = new ScheduleFactory();

        scheduleFactory.addLine(DayOfWeek.MONDAY, DayOfWeek.FRIDAY,
                0, 0, 24, 0, 2);

        Schedule schedule = scheduleFactory.getSchedule();
        Assertions.assertNotNull(schedule.getLines());
        Assertions.assertEquals(1, schedule.getLines().size());
        ScheduleLine onlyLine = schedule.getLines().get(0);
        Assertions.assertEquals("0:00", onlyLine.getStartTime());
        Assertions.assertEquals("23:59", onlyLine.getEndTime());
    }
}
