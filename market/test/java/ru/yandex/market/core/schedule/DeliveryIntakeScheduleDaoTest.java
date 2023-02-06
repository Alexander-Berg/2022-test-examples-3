package ru.yandex.market.core.schedule;

import java.util.Arrays;
import java.util.List;

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
@DbUnitDataSet(before = "DeliveryIntakeScheduleDaoTest.csv")
class DeliveryIntakeScheduleDaoTest extends FunctionalTest {

    private static final int BOXBERRY_ID = 106;
    private static final Schedule BOXBERRY_SCHEDULE = new Schedule(BOXBERRY_ID,
            Arrays.asList(
                    new ScheduleLineWithDefault(DayOfWeek.MONDAY, 0, 540, 480, true),
                    new ScheduleLineWithDefault(DayOfWeek.WEDNESDAY, 0, 660, 600, false),
                    new ScheduleLineWithDefault(DayOfWeek.SATURDAY, 0, 600, 120, false),
                    new ScheduleLineWithDefault(DayOfWeek.SATURDAY, 0, 900, 180, false)
            )
    );

    private static final int POST_ID = 123;
    private static final Schedule POST_SCHEDULE = new Schedule(POST_ID,
            Arrays.asList(
                    new ScheduleLineWithDefault(DayOfWeek.MONDAY, 0, 660, 660, false),
                    new ScheduleLineWithDefault(DayOfWeek.TUESDAY, 0, 600, 600, false),
                    new ScheduleLineWithDefault(DayOfWeek.WEDNESDAY, 0, 600, 600, false),
                    new ScheduleLineWithDefault(DayOfWeek.THURSDAY, 0, 660, 660, true),
                    new ScheduleLineWithDefault(DayOfWeek.FRIDAY, 0, 480, 720, false)
            ));

    @Autowired
    private ScheduleDao deliveryIntakeScheduleDao;

    @Test
    @DbUnitDataSet
    void insertAndGet() {
        deliveryIntakeScheduleDao.insertSchedule(POST_SCHEDULE);
        Schedule intakeSchedule = deliveryIntakeScheduleDao.getSchedule(POST_ID);
        assertThat(intakeSchedule, equalTo(POST_SCHEDULE));
    }

    @Test
    @DbUnitDataSet
    void insertAndList() {
        deliveryIntakeScheduleDao.insertSchedule(POST_SCHEDULE);
        deliveryIntakeScheduleDao.insertSchedule(BOXBERRY_SCHEDULE);
        List<Schedule> schedules = deliveryIntakeScheduleDao.listSchedules();
        assertThat(schedules.size(), equalTo(2));
        for (Schedule schedule : schedules) {
            if (schedule.getId() == POST_ID) {
                assertThat(schedule, equalTo(POST_SCHEDULE));
            }
            if (schedule.getId() == BOXBERRY_ID) {
                assertThat(schedule, equalTo(BOXBERRY_SCHEDULE));
            }
        }
    }

    @Test
    @DbUnitDataSet
    void insertDeleteAndList() {
        deliveryIntakeScheduleDao.insertSchedule(POST_SCHEDULE);
        deliveryIntakeScheduleDao.insertSchedule(BOXBERRY_SCHEDULE);
        deliveryIntakeScheduleDao.deleteSchedules(POST_ID);
        List<Schedule> schedules = deliveryIntakeScheduleDao.listSchedules();
        assertThat(schedules.size(), equalTo(1));
        assertThat(schedules.get(0), equalTo(BOXBERRY_SCHEDULE));
    }

}
