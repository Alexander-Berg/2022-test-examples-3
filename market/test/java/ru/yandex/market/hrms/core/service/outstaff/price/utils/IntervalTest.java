package ru.yandex.market.hrms.core.service.outstaff.price.utils;

import java.time.Duration;
import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.hrms.core.service.util.Interval;

class IntervalTest {

    @Test
    void testInitializationOfEqualTimesIsOk() {
        Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"));
        Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Duration.ofMinutes(1));
    }

    @Test
    void testInitializationOfFinishTimeIsFailedTimesIsOk() {
        Assertions.assertThatCode(() -> {
            Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T09:00:00.00Z"));
        }).hasMessageContaining("is less or equal then");
        Assertions.assertThatCode(() -> {
            Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:00:00.00Z"));
        }).hasMessageContaining("is less or equal then");
        Assertions.assertThatCode(() -> {
            Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Duration.ofMinutes(0));
        }).hasMessageContaining("is less or equal then");

        // won't fail
        var interval1 = Interval.safeOf(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:00:00.00Z"));
        Assertions.assertThat(interval1).isNull();

        var interval2 = Interval.safeOf(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T09:00:00.00Z"));
        Assertions.assertThat(interval2).isNull();
    }

    @Test
    void intersect() {
        var one = Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));

        var intersect1 = one.intersect(one);
        Assertions.assertThat(intersect1).isEqualTo(one);

        var intersect2 = one.intersect(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")));
        Assertions.assertThat(intersect2)
                .isEqualTo(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")));

        var intersect3 = one.intersect(Interval.of(Instant.parse("2007-12-03T10:20:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")));
        Assertions.assertThat(intersect3)
                .isEqualTo(Interval.of(Instant.parse("2007-12-03T10:20:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")));

        var intersect4 = one.intersect(Interval.of(Instant.parse("2007-12-03T09:50:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")));
        Assertions.assertThat(intersect4)
                .isEqualTo(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")));

        var intersect5 = one.intersect(Interval.of(Instant.parse("2007-12-03T09:50:00.00Z"), Instant.parse("2007-12-03T09:51:00.00Z")));
        Assertions.assertThat(intersect5).isNull();

        var intersect6 = one.intersect(Interval.of(Instant.parse("2007-12-03T11:50:00.00Z"), Instant.parse("2007-12-03T11:55:00.00Z")));
        Assertions.assertThat(intersect6).isNull();

        var intersect7 = one.intersect(Interval.of(Instant.parse("2007-12-03T09:20:00.00Z"), Instant.parse("2007-12-03T11:30:00.00Z")));
        Assertions.assertThat(intersect7)
                .isEqualTo(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")));
    }

    @Test
    void truncateDuration() {
        var interval = Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));

        Assertions.assertThat(interval.truncateDuration(Duration.ofMinutes(30)))
                .isEqualTo(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")));
        Assertions.assertThat(interval.truncateDuration(Duration.ofMinutes(60)))
                .isEqualTo(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")));
        Assertions.assertThat(interval.truncateDuration(Duration.ofMinutes(70)))
                .isEqualTo(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")));

        Assertions.assertThatCode(() -> interval.truncateDuration(Duration.ZERO)).hasMessageContaining("Illegal zero duration");
    }
}
