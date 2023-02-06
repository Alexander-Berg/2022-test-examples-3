package ru.yandex.market.logistics.management.domain.entity;

import java.time.LocalTime;
import java.util.Set;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.logistics.management.AbstractTest;

/**
 * Schedule.setScheduleDays() сохраняет основной интервал.
 */
public class ScheduleSetScheduleDaysIsMainTest  extends AbstractTest {

    @Test
    public void SetScheduleDaysIsMainTest() {
        var schedule = new Schedule();

        schedule.setScheduleDays(Set.of(
            new ScheduleDay().setId(1L).setDay(1).setFrom(LocalTime.of(1, 0)).setTo(LocalTime.of(2, 0)),
            new ScheduleDay().setId(2L).setDay(1).setFrom(LocalTime.of(3, 0)).setTo(LocalTime.of(4, 0)).setIsMain(true)
        ));

        Assertions.assertThat(getScheduleDayWithId(schedule, 1).isMain()).isFalse();
        Assertions.assertThat(getScheduleDayWithId(schedule, 2).isMain()).isTrue();

        schedule.setScheduleDays(Set.of(
            new ScheduleDay().setDay(1).setFrom(LocalTime.of(1, 0)).setTo(LocalTime.of(2, 0)),
            new ScheduleDay().setDay(1).setFrom(LocalTime.of(4, 0)).setTo(LocalTime.of(5, 0)).setIsMain(true)
        ));

        Assertions.assertThat(getScheduleDayWithId(schedule, 1).isMain()).isFalse();
        Assertions.assertThat(getScheduleDayWithId(schedule, 2).isMain()).isTrue();
    }

    @Nonnull
    private ScheduleDay getScheduleDayWithId(Schedule schedule, long id) {
        return schedule.getScheduleDays().stream()
            .filter(scheduleDay -> scheduleDay.getId() == id)
            .findFirst()
            .orElseThrow();
    }
}
