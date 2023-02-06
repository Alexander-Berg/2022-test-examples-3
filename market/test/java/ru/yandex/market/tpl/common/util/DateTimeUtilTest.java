package ru.yandex.market.tpl.common.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
class DateTimeUtilTest {

    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault());

    @Test
    void dateString() {
        Instant now = clock.instant();
        Instant tomorrow = clock.instant().plus(1, ChronoUnit.DAYS);
        Instant dayAfterTomorrow = clock.instant().plus(2, ChronoUnit.DAYS);
        Instant twoDaysAfterTomorrow = clock.instant().plus(3, ChronoUnit.DAYS);

        assertThat(DateTimeUtil.dateString(now, clock)).isEqualTo("сегодня");
        assertThat(DateTimeUtil.dateString(tomorrow, clock)).isEqualTo("завтра");
        assertThat(DateTimeUtil.dateString(dayAfterTomorrow, clock)).isEqualTo("послезавтра");
        assertThat(DateTimeUtil.dateString(twoDaysAfterTomorrow, clock)).isEqualTo("вс, 4 января");
    }

    @Test
    void shouldMakeCorrectIntervalBefore15() {
        Interval interval = DateTimeUtil.toExpectedDeliveryInterval(time(12, 13));

        assertThat(interval.getStart()).isEqualTo(time(11, 40));
        assertThat(interval.getEnd()).isEqualTo(time(12, 40));
    }

    @Test
    void shouldMakeCorrectIntervalAfter15() {
        Interval interval = DateTimeUtil.toExpectedDeliveryInterval(time(12, 18));

        assertThat(interval.getStart()).isEqualTo(time(11, 50));
        assertThat(interval.getEnd()).isEqualTo(time(12, 50));
    }

    @Test
    void shouldMakeCorrectIntervalAfter30() {
        Interval interval = DateTimeUtil.toExpectedDeliveryInterval(time(12, 32));

        assertThat(interval.getStart()).isEqualTo(time(12, 0));
        assertThat(interval.getEnd()).isEqualTo(time(13, 0));
    }

    private Instant time(int hour, int min) {
        Instant time = DateTimeUtil.todayAtHour(hour, clock);
        return time.plus(min, ChronoUnit.MINUTES);
    }

    @Test
    void durationOfIntersection() {
        assertThat(DateTimeUtil.durationOfIntersection(
                LocalTimeInterval.valueOf("10:00-14:00"),
                LocalTimeInterval.valueOf("14:00-18:00"))
        ).isEqualTo(Duration.ZERO);

        assertThat(DateTimeUtil.durationOfIntersection(
                LocalTimeInterval.valueOf("10:00-14:00"),
                LocalTimeInterval.valueOf("10:00-18:00"))
        ).isEqualTo(Duration.ofHours(4));

        assertThat(DateTimeUtil.durationOfIntersection(
                LocalTimeInterval.valueOf("10:00-18:00"),
                LocalTimeInterval.valueOf("10:00-18:00"))
        ).isEqualTo(Duration.ofHours(8));

        assertThat(DateTimeUtil.durationOfIntersection(
                LocalTimeInterval.valueOf("10:00-18:00"),
                LocalTimeInterval.valueOf("11:00-18:00"))
        ).isEqualTo(Duration.ofHours(7));

        assertThat(DateTimeUtil.durationOfIntersection(
                LocalTimeInterval.valueOf("10:00-14:00"),
                LocalTimeInterval.valueOf("11:00-18:00"))
        ).isEqualTo(Duration.ofHours(3));

        assertThat(DateTimeUtil.durationOfIntersection(
                LocalTimeInterval.valueOf("12:00-18:00"),
                LocalTimeInterval.valueOf("10:00-14:00"))
        ).isEqualTo(Duration.ofHours(2));

        assertThat(DateTimeUtil.durationOfIntersection(
                LocalTimeInterval.valueOf("10:00-14:00"),
                LocalTimeInterval.valueOf("18:00-22:00")
        )).isEqualTo(Duration.ZERO);
    }

    @Test
    void intersect() {
        Optional<LocalTimeInterval> intersectionCase1 = DateTimeUtil.intersection(
                List.of(
                        LocalTimeInterval.valueOf("10:00-14:00"),
                        LocalTimeInterval.valueOf("10:00-18:00"),
                        LocalTimeInterval.valueOf("12:00-16:00")
                ));

        assertThat(intersectionCase1).isPresent();
        assertThat(intersectionCase1.get()).isEqualTo(LocalTimeInterval.valueOf("12:00-14:00"));


        Optional<LocalTimeInterval> intersectionCase2 = DateTimeUtil.intersection(
                List.of(LocalTimeInterval.valueOf("10:00-14:00")));

        assertThat(intersectionCase2).isPresent();
        assertThat(intersectionCase2.get()).isEqualTo(LocalTimeInterval.valueOf("10:00-14:00"));


        Optional<LocalTimeInterval> intersectionCase3 = DateTimeUtil.intersection(
                List.of(
                        LocalTimeInterval.valueOf("10:00-14:00"),
                        LocalTimeInterval.valueOf("18:00-22:00")
                ));
        assertThat(intersectionCase3).isEmpty();

        Optional<LocalTimeInterval> intersectionCase4 = DateTimeUtil.intersection(
                List.of(
                        LocalTimeInterval.valueOf("10:00-14:00"),
                        LocalTimeInterval.valueOf("14:00-18:00")
                ));
        assertThat(intersectionCase4).isEmpty();
    }

}
