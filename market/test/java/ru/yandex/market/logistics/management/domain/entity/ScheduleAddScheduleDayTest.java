package ru.yandex.market.logistics.management.domain.entity;

import java.time.LocalTime;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ScheduleAddScheduleDayTest {

    @Test
    public void SetScheduleDaysIsMainTest() {
        var schedule = new Schedule();

        schedule.setScheduleDays(Set.of(
            new ScheduleDay().setId(1L).setDay(1).setFrom(LocalTime.of(1, 0)).setTo(LocalTime.of(2, 0)).setIsMain(true)
        ));

        schedule.addScheduledDay(
            new ScheduleDay().setId(1L).setDay(1).setFrom(LocalTime.of(1, 0)).setTo(LocalTime.of(2, 0))
        );

        Assertions.assertThat(schedule.getScheduleDays()).isEqualTo(Set.of(
            new ScheduleDay().setId(1L).setDay(1).setFrom(LocalTime.of(1, 0)).setTo(LocalTime.of(2, 0)).setIsMain(true)
        ));
    }
}
