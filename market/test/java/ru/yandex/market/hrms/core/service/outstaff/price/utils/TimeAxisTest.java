package ru.yandex.market.hrms.core.service.outstaff.price.utils;

import java.time.Duration;
import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.hrms.core.service.util.Interval;
import ru.yandex.market.hrms.core.service.util.TimeAxis;

import static org.assertj.core.api.Assertions.withinPercentage;

class TimeAxisTest {

    @Test
    void testInitializationIsOk() {
        TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:01:00.00Z"));
        TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:00:00.00Z"));
    }

    @Test
    void testInitializationOfEqualTimeIsFailed() {
        Assertions.assertThatCode(() -> {
            TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T09:00:00.00Z"));
        }).hasMessageContaining("is less or equal then");
    }

    @Test
    void withoutBlocks() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));

        Assertions.assertThat(axis.getBlockedIntervals()).isEmpty();
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isTrue();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();
    }

    @Test
    void simple() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "10:00 - 10:30");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:00 - 10:30")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void onEdge() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T11:00:00.00Z"), Instant.parse("2007-12-03T11:30:00.00Z"), "11:00 - 11:30");

        Assertions.assertThat(axis.getBlockedIntervals()).isEmpty();
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isTrue();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ZERO);
    }

    @Test
    void onEdge2() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T09:00:00.00Z"), Instant.parse("2007-12-03T10:00:00.00Z"), "09:00 - 10:00");

        Assertions.assertThat(axis.getBlockedIntervals()).isEmpty();
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isTrue();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ZERO);
    }

    @Test
    void severalBlocksWhitFreeSpaceAtEnd() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "10:00 - 10:30");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:40:00.00Z"), "10:30 - 10:40");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:00 - 10:30")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:40:00.00Z")), "10:30 - 10:40")
                .hasSize(2);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:40:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(40));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(20));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.6666, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.333, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void severalBlocksWithFreeSpaceBetween() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "10:00 - 10:30");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:40:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"), "10:40 - 11:00");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:00 - 10:30")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:40:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "10:40 - 11:00")
                .hasSize(2);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:40:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(50));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(10));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.83333, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.16666, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    void severalBlocksWithFreeSpaceBefore() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:15:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "10:15 - 10:30");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"), "10:30 - 11:00");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:15:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:15 - 10:30")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "10:30 - 11:00")
                .hasSize(2);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:15:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(45));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.75, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.25, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void totalBlocked() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:15:00.00Z"), "10:00 - 10:15");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:45:00.00Z"), "10:30 - 10:45");
        var d3 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:15:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "10:15 - 10:30");
        var d4 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:45:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"), "10:45 - 11:00");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:15:00.00Z")), "10:00 - 10:15")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:15:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:15 - 10:30")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:45:00.00Z")), "10:30 - 10:45")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:45:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "10:45 - 11:00")
                .hasSize(4);
        Assertions.assertThat(axis.getFreeIntervals()).isEmpty();
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isTrue();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(d3).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(d4).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void blockWithTooSoonInterval() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T09:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "09:00 - 10:30");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "09:00 - 10:30")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void blockWithTooLateInterval() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:30:00.00Z"), "10:30 - 11:30");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "10:30 - 11:30")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void blockWithBigInterval() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T09:30:00.00Z"), Instant.parse("2007-12-03T11:30:00.00Z"), "10:30 - 11:30");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "10:30 - 11:30")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).isEmpty();
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isTrue();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void severalBlockIntervalsWithOverlaping() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:45:00.00Z"), "10:30 - 10:45");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:20:00.00Z"), Instant.parse("2007-12-03T10:40:00.00Z"), "10:20 - 10:40");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:45:00.00Z")), "10:30 - 10:45")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:20:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:20 - 10:40")
                .hasSize(2);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z")),
                Interval.of(Instant.parse("2007-12-03T10:45:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(25));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(35));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.41666, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.58333, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void severalBlockIntervalsWithOverlaping2() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:45:00.00Z"), "10:30 - 10:45");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:20:00.00Z"), Instant.parse("2007-12-03T10:40:00.00Z"), "10:20 - 10:40");
        var d3 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T11:40:00.00Z"), "10:10 - 11:40");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:45:00.00Z")), "10:30 - 10:45")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:20:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:20 - 10:40")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z")), "10:10 - 11:40")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:45:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "10:10 - 11:40")
                .hasSize(4);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:10:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(50));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(10));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.8333, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.1666, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(10));
        Assertions.assertThat(d3).isEqualTo(Duration.ofMinutes(25));
    }

    @Test
    void simpleDuration() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockAnyFree(Duration.ofMinutes(30), "30 min");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "30 min")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void tooLargeDuration() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockAnyFree(Duration.ofMinutes(90), "90 mins");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "90 mins")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).isEmpty();
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isTrue();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void severalDuration() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockAnyFree(Duration.ofMinutes(50), "50 mins");
        var d2 = axis.blockAnyFree(Duration.ofMinutes(5), "5 mins");
        var d3 = axis.blockAnyFree(Duration.ofMinutes(10), "10 mins");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:50:00.00Z")), "50 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:50:00.00Z"), Instant.parse("2007-12-03T10:55:00.00Z")), "5 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:55:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "10 mins")
                .hasSize(3);
        Assertions.assertThat(axis.getFreeIntervals()).isEmpty();
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isTrue();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(50));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(5));
        Assertions.assertThat(d3).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void combinationOfIntervalAndDuration() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "10:00 - 10:30");
        var d2 = axis.blockAnyFree(Duration.ofMinutes(30), "30 mins");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:00 - 10:30")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "30 mins")
                .hasSize(2);
        Assertions.assertThat(axis.getFreeIntervals()).isEmpty();
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isTrue();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void combinationOfIntervalAndLargeDuration() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "10:00 - 10:30");
        var d2 = axis.blockAnyFree(Duration.ofMinutes(50), "50 mins");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "10:00 - 10:30")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z")), "50 mins")
                .hasSize(2);
        Assertions.assertThat(axis.getFreeIntervals()).isEmpty();
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isTrue();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void combinationOfIntervalsAndDurations() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z"), "10:10 - 10:20");
        var d2 = axis.blockAnyFree(Duration.ofMinutes(20), "20 mins");
        var d3 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z"), "10:40 - 10:45");
        var d4 = axis.blockAnyFree(Duration.ofMinutes(5), "5 mins");
        var d5 = axis.blockAnyFree(Duration.ofMinutes(10), "10 mins");
        var d6 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:40:00.00Z"), Instant.parse("2007-12-03T10:56:00.00Z"), "10:40 - 10:56");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:10:00.00Z")), "20 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z")), "10:10 - 10:20")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:20:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "20 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T10:35:00.00Z")), "5 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:35:00.00Z"), Instant.parse("2007-12-03T10:45:00.00Z")), "10 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:45:00.00Z"), Instant.parse("2007-12-03T10:56:00.00Z")), "10:40 - 10:56")
                .hasSize(6);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:56:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(56));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(4));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.9333, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.0666, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(10));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(20));
        Assertions.assertThat(d3).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(d4).isEqualTo(Duration.ofMinutes(5));
        Assertions.assertThat(d5).isEqualTo(Duration.ofMinutes(10));
        Assertions.assertThat(d6).isEqualTo(Duration.ofMinutes(11));
    }

    @Test
    void combinationOfDurationAndInterval() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockAnyFree(Duration.ofMinutes(30), "30 mins");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "10:00 - 10:30");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z")), "30 mins")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:30:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.5, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(0));
    }

    @Test
    void combinationOfDurationsAndIntervals() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));
        var d1 = axis.blockAnyFree(Duration.ofMinutes(5), "5 mins");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z"), "10:10 - 10:20");
        var d3 = axis.blockAnyFree(Duration.ofMinutes(20), "20 mins");
        var d4 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z"), "10:40 - 10:45");
        var d5 = axis.blockAnyFree(Duration.ofMinutes(5), "5 mins");
        var d6 = axis.blockAnyFree(Duration.ofMinutes(10), "10 mins");
        var d7 = axis.blockIfFree(-1, Instant.parse("2007-12-03T10:45:00.00Z"), Instant.parse("2007-12-03T10:56:00.00Z"), "10:45 - 10:56");

        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T10:05:00.00Z")), "5 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:05:00.00Z"), Instant.parse("2007-12-03T10:10:00.00Z")), "20 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z")), "10:10 - 10:20")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:20:00.00Z"), Instant.parse("2007-12-03T10:35:00.00Z")), "20 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:35:00.00Z"), Instant.parse("2007-12-03T10:40:00.00Z")), "5 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:40:00.00Z"), Instant.parse("2007-12-03T10:50:00.00Z")), "10 mins")
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:50:00.00Z"), Instant.parse("2007-12-03T10:56:00.00Z")), "10:45 - 10:56")
                .hasSize(7);
        Assertions.assertThat(axis.getFreeIntervals()).containsExactlyInAnyOrder(
                Interval.of(Instant.parse("2007-12-03T10:56:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"))
        );
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ofMinutes(60));
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ofMinutes(56));
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ofMinutes(4));
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(0.9333, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0.0666, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isFalse();

        Assertions.assertThat(d1).isEqualTo(Duration.ofMinutes(5));
        Assertions.assertThat(d2).isEqualTo(Duration.ofMinutes(10));
        Assertions.assertThat(d3).isEqualTo(Duration.ofMinutes(20));
        Assertions.assertThat(d4).isEqualTo(Duration.ofMinutes(0));
        Assertions.assertThat(d5).isEqualTo(Duration.ofMinutes(5));
        Assertions.assertThat(d6).isEqualTo(Duration.ofMinutes(10));
        Assertions.assertThat(d7).isEqualTo(Duration.ofMinutes(6));
    }

    @Test
    void blockWithZeroIntervalOrDurationWillFail() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Instant.parse("2007-12-03T11:00:00.00Z"));

        Assertions.assertThatCode(() -> {
            axis.blockIfFree(-1, Instant.parse("2007-12-03T10:10:00.00Z"), Instant.parse("2007-12-03T10:10:00.00Z"), "10:10 - 10:10");
        }).hasMessageContaining("Can't block zero duration");

        Assertions.assertThatCode(() -> {
            axis.blockAnyFree(Duration.ofMinutes(0), "0 mins");
        }).hasMessageContaining("Can't block zero duration");
    }

    @Test
    void blockZeroIntervalWithDurationWontFail() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Duration.ZERO);

        var d1 = axis.blockAnyFree(Duration.ofMinutes(5), "5 mins");
        var d2 = axis.blockAnyFree(Duration.ofMinutes(10), "10 mins");
        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Duration.ofSeconds(1)), "5 mins")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).isEmpty();
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ZERO);
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ZERO);
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ZERO);
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isTrue();

        Assertions.assertThat(d1).isEqualTo(Duration.ZERO);
        Assertions.assertThat(d2).isEqualTo(Duration.ZERO);
    }

    @Test
    void blockZeroIntervalWithTimeWontFail() {
        var axis = TimeAxis.of(Instant.parse("2007-12-03T10:00:00.00Z"), Duration.ZERO);

        var d1 = axis.blockIfFree(-1, Instant.parse("2007-12-03T09:20:00.00Z"), Instant.parse("2007-12-03T10:20:00.00Z"), "09:20 - 10:20");
        var d2 = axis.blockIfFree(-1, Instant.parse("2007-12-03T09:10:00.00Z"), Instant.parse("2007-12-03T10:30:00.00Z"), "09:10 - 10:30");
        Assertions.assertThat(axis.getBlockedIntervals())
                .containsEntry(Interval.of(Instant.parse("2007-12-03T10:00:00.00Z"), Duration.ofSeconds(1)), "09:20 - 10:20")
                .hasSize(1);
        Assertions.assertThat(axis.getFreeIntervals()).isEmpty();
        Assertions.assertThat(axis.getTotalDuration()).isEqualTo(Duration.ZERO);
        Assertions.assertThat(axis.getBlockedDuration()).isEqualTo(Duration.ZERO);
        Assertions.assertThat(axis.getFreeDuration()).isEqualTo(Duration.ZERO);
        Assertions.assertThat(axis.getBlockedPercent()).isCloseTo(1, withinPercentage(0.11));
        Assertions.assertThat(axis.getFreePercent()).isCloseTo(0, withinPercentage(0.11));
        Assertions.assertThat(axis.isAllFree()).isFalse();
        Assertions.assertThat(axis.isAllBlocked()).isTrue();

        Assertions.assertThat(d1).isEqualTo(Duration.ZERO);
        Assertions.assertThat(d2).isEqualTo(Duration.ZERO);
    }
}
