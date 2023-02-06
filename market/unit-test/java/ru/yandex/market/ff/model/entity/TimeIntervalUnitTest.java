package ru.yandex.market.ff.model.entity;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;

public class TimeIntervalUnitTest extends SoftAssertionSupport {

    @Test
    public void isIntervalFitsWhenIntervalIsFullDay() {
        TimeInterval interval = createTimeInterval(11, 11);
        assertions.assertThat(interval.isIntervalFits(ofHour(12), ofHour(14))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(11), ofHour(11))).isTrue();
    }

    @Test
    public void isIntervalFitsInSameDay() {
        TimeInterval interval = createTimeInterval(12, 22);
        assertions.assertThat(interval.isIntervalFits(ofHour(12), ofHour(14))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(15), ofHour(21))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(18), ofHour(22))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(12), ofHour(22))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(8), ofHour(10))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(10), ofHour(14))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(18), ofHour(23))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(18), ofTime(22, 30))).isFalse();
    }

    @Test
    public void isIntervalFitsWhenIntervalFromMidnight() {
        TimeInterval interval = createTimeInterval(0, 11);
        assertions.assertThat(interval.isIntervalFits(ofHour(0), ofHour(2))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(5), ofHour(10))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(9), ofHour(11))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(0), ofHour(11))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(12), ofHour(15))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(23), ofHour(1))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(10), ofHour(12))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(10), ofTime(11, 10))).isFalse();
    }

    @Test
    public void isIntervalFitsWhenIntervalTillMidnight() {
        TimeInterval interval = createTimeInterval(19, 0);
        assertions.assertThat(interval.isIntervalFits(ofHour(19), ofHour(22))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(20), ofHour(21))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(22), ofHour(0))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(19), ofHour(0))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(15), ofHour(17))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(18), ofHour(20))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(23), ofHour(2))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(20), ofTime(0, 1))).isFalse();
    }

    @Test
    public void isIntervalFitsWhenIntervalCrossesMidnight() {
        TimeInterval interval = createTimeInterval(19, 2);
        assertions.assertThat(interval.isIntervalFits(ofHour(19), ofHour(22))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(19), ofHour(1))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(20), ofHour(21))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(0), ofHour(1))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(1), ofHour(2))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(19), ofHour(2))).isTrue();
        assertions.assertThat(interval.isIntervalFits(ofHour(15), ofHour(17))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(18), ofHour(20))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(1), ofHour(3))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(18), ofHour(3))).isFalse();
        assertions.assertThat(interval.isIntervalFits(ofHour(22), ofTime(2, 15))).isFalse();
    }

    private TimeInterval createTimeInterval(int hourFrom, int hourTo) {
        TimeInterval timeInterval = new TimeInterval();
        timeInterval.setFrom(ofHour(hourFrom));
        timeInterval.setTo(ofHour(hourTo));
        return timeInterval;
    }

    private LocalTime ofHour(int hour) {
        return ofTime(hour, 0);
    }

    private LocalTime ofTime(int hour, int minute) {
        return LocalTime.of(hour, minute);
    }
}
