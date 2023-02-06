package ru.yandex.market.logistics.management.domain.entity;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.logistics.management.AbstractTest;

/**
 * Schedule.setScheduleDays() обновляет текущие интервалы расписания, не создавая новые без необходимости.
 */
public class ScheduleSetScheduleDaysReuseEntitiesTest extends AbstractTest {

    @Test
    public void SetScheduleDaysReuseEntitiesSameCountTest() {
        var schedule = new Schedule();

        schedule.setScheduleDays(generateScheduleDays(Set.of(1, 2, 3, 4, 5), LocalTime.of(10, 0), LocalTime.of(11, 0)));
        var scheduleDaysIdsBeforeUpdate = assignIdsToScheduleDays(schedule.getScheduleDays());

        schedule.setScheduleDays(generateScheduleDays(Set.of(1, 2, 3, 6, 7), LocalTime.of(12, 0), LocalTime.of(13, 0)));
        var scheduleDaysIdsAfterUpdate = getScheduleDaysIds(schedule.getScheduleDays());

        Assertions.assertThat(scheduleDaysIdsBeforeUpdate.size()).isEqualTo(5);
        Assertions.assertThat(scheduleDaysIdsAfterUpdate.size()).isEqualTo(5);

        Assertions.assertThat(scheduleDaysIdsBeforeUpdate)
            .containsExactlyInAnyOrderElementsOf(scheduleDaysIdsAfterUpdate);
    }

    @Test
    public void SetScheduleDaysReuseEntitiesBeforeMoreThanAfterTest() {
        var schedule = new Schedule();

        schedule.setScheduleDays(generateScheduleDays(Set.of(1, 2, 3), LocalTime.of(10, 0), LocalTime.of(11, 0)));
        var scheduleDaysIdsBeforeUpdate = assignIdsToScheduleDays(schedule.getScheduleDays());

        schedule.setScheduleDays(generateScheduleDays(Set.of(4, 5), LocalTime.of(12, 0), LocalTime.of(13, 0)));
        var scheduleDaysIdsAfterUpdate = getScheduleDaysIds(schedule.getScheduleDays());

        Assertions.assertThat(scheduleDaysIdsBeforeUpdate.size()).isEqualTo(3);
        Assertions.assertThat(scheduleDaysIdsAfterUpdate.size()).isEqualTo(2);

        Assertions.assertThat(scheduleDaysIdsBeforeUpdate).containsAll(scheduleDaysIdsAfterUpdate);
    }

    @Test
    public void SetScheduleDaysReuseEntitiesAfterMoreThanBeforeTest() {
        var schedule = new Schedule();

        schedule.setScheduleDays(generateScheduleDays(Set.of(1, 2), LocalTime.of(10, 0), LocalTime.of(11, 0)));
        var scheduleDaysIdsBeforeUpdate = assignIdsToScheduleDays(schedule.getScheduleDays());

        schedule.setScheduleDays(generateScheduleDays(Set.of(3, 4, 5), LocalTime.of(12, 0), LocalTime.of(13, 0)));
        var scheduleDaysIdsAfterUpdate = getScheduleDaysIds(schedule.getScheduleDays());

        Assertions.assertThat(scheduleDaysIdsBeforeUpdate.size()).isEqualTo(2);
        Assertions.assertThat(scheduleDaysIdsAfterUpdate.size()).isEqualTo(3);

        Assertions.assertThat(scheduleDaysIdsAfterUpdate).containsAll(scheduleDaysIdsBeforeUpdate);
    }

    private Set<ScheduleDay> generateScheduleDays(Set<Integer> days, LocalTime from, LocalTime to) {
        var scheduleDays = new HashSet<ScheduleDay>();
        days.forEach(day -> scheduleDays.add(new ScheduleDay().setDay(day).setFrom(from).setTo(to)));
        return scheduleDays;
    }

    private List<Long> assignIdsToScheduleDays(Set<ScheduleDay> scheduleDays) {
        Random random = new Random();
        scheduleDays.forEach(scheduleDay -> scheduleDay.setId(random.nextLong()));
        return getScheduleDaysIds(scheduleDays);
    }

    private List<Long> getScheduleDaysIds(Set<ScheduleDay> scheduleDays) {
        return scheduleDays.stream()
            .map(ScheduleDay::getId)
            .collect(Collectors.toList());
    }
}
