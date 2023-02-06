package ru.yandex.market.core.schedule;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.schedule.ScheduleLine.DayOfWeek;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author ivmelnik
 * @since 21.08.17
 */
@DbUnitDataSet(before = "CpaScheduleDaoTest.csv")
class CpaScheduleDaoTest extends FunctionalTest {

    private static final int SCHEDULE_1_ID = 774;
    private static final Schedule SCHEDULE_1 = new Schedule(SCHEDULE_1_ID,
            Arrays.asList(
                    new ScheduleLine(DayOfWeek.MONDAY, 0, 540, 480),
                    new ScheduleLine(DayOfWeek.WEDNESDAY, 0, 660, 600),
                    new ScheduleLine(DayOfWeek.SATURDAY, 0, 600, 120),
                    new ScheduleLine(DayOfWeek.SATURDAY, 0, 900, 180)
            )
    );

    private static final int SCHEDULE_2_ID = 800;
    private static final Schedule SCHEDULE_2 = new Schedule(SCHEDULE_2_ID,
            Arrays.asList(
                    new ScheduleLine(DayOfWeek.MONDAY, 0, 660, 660),
                    new ScheduleLine(DayOfWeek.TUESDAY, 0, 600, 600),
                    new ScheduleLine(DayOfWeek.WEDNESDAY, 0, 600, 600),
                    new ScheduleLine(DayOfWeek.THURSDAY, 0, 660, 660),
                    new ScheduleLine(DayOfWeek.FRIDAY, 0, 480, 720)
            ));

    @Autowired
    private ScheduleDao cpaScheduleDao;

    @Test
    void testDefault() {
        final Schedule defaultSchedule = cpaScheduleDao.getScheduleOrDefault(12345);
        Assertions.assertEquals(1, defaultSchedule.getLines().size());
        final ScheduleLine line = defaultSchedule.getLines().get(0);
        Assertions.assertEquals(DayOfWeek.MONDAY, line.getStartDay());
        Assertions.assertEquals(DayOfWeek.SUNDAY, line.getEndDay());
        Assertions.assertEquals(LocalTime.of(9, 0), line.localStartTime());
        Assertions.assertEquals(LocalTime.of(19, 0), line.localEndTime());
    }

    @Test
    void insertAndGet() {
        cpaScheduleDao.insertSchedule(SCHEDULE_1);
        Schedule schedule = cpaScheduleDao.getSchedule(SCHEDULE_1_ID);
        assertThat(schedule, equalTo(SCHEDULE_1));
    }

    @Test
    void insertAndList() {
        cpaScheduleDao.insertSchedule(SCHEDULE_1);
        cpaScheduleDao.insertSchedule(SCHEDULE_2);
        List<Schedule> schedules = cpaScheduleDao.listSchedules();
        checkTwoSchedules(schedules);
    }

    @Test
    void insertAndListByIds() {
        cpaScheduleDao.insertSchedule(SCHEDULE_1);
        cpaScheduleDao.insertSchedule(SCHEDULE_2);
        String idsSubSql = "SELECT ID FROM SHOPS_WEB.DATASOURCE WHERE ID IN (?, ?)";
        List<Schedule> schedules = cpaScheduleDao.listSchedules(idsSubSql, SCHEDULE_1_ID, SCHEDULE_2_ID);
        checkTwoSchedules(schedules);
    }

    @Test
    void insertDeleteAndList() {
        cpaScheduleDao.insertSchedule(SCHEDULE_1);
        cpaScheduleDao.insertSchedule(SCHEDULE_2);
        cpaScheduleDao.deleteSchedules(SCHEDULE_2_ID);
        List<Schedule> schedules = cpaScheduleDao.listSchedules();
        assertThat(schedules.size(), equalTo(1));
        assertThat(schedules.get(0), equalTo(SCHEDULE_1));
    }

    @Test
    void insertDelete() {
        cpaScheduleDao.insertSchedule(SCHEDULE_1);
        cpaScheduleDao.insertSchedule(SCHEDULE_2);
        cpaScheduleDao.deleteSchedules(SCHEDULE_1_ID, SCHEDULE_2_ID);
        List<Schedule> schedules = cpaScheduleDao.listSchedules();
        assertThat(schedules.size(), equalTo(0));
    }

    private void checkTwoSchedules(List<Schedule> schedules) {
        assertThat(schedules.size(), equalTo(2));
        for (Schedule schedule : schedules) {
            if (schedule.getId() == SCHEDULE_1_ID) {
                assertThat(schedule, equalTo(SCHEDULE_1));
            }
            if (schedule.getId() == SCHEDULE_2_ID) {
                assertThat(schedule, equalTo(SCHEDULE_2));
            }
        }
    }

}
