package ru.yandex.market.logistic.gateway.common.model.fulfillment;

import java.time.LocalTime;
import java.time.OffsetTime;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class TimeIntervalTest {

    private static final String LOCAL_TIME_FROM = "07:11:22";
    private static final String LOCAL_TIME_TO = "21:33:44";
    private static final String OFFSET_TIME_FROM = "07:11:22+03:00";
    private static final String OFFSET_TIME_TO = "21:33:44+03:00";
    private static final String EXPECTED = "07:11:22+03:00/21:33:44+03:00";

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testOffsetFactory() {
        TimeInterval actual = TimeInterval.of(OffsetTime.parse(OFFSET_TIME_FROM), OffsetTime.parse(OFFSET_TIME_TO));
        softly.assertThat(actual.getFormattedTimeInterval()).isEqualTo(EXPECTED);
    }

    @Test
    public void testLocalFactory() {
        TimeInterval actual = TimeInterval.of(LocalTime.parse(LOCAL_TIME_FROM), LocalTime.parse(LOCAL_TIME_TO));
        softly.assertThat(actual.getFormattedTimeInterval()).isEqualTo(EXPECTED);
    }

    @Test
    public void testLocalConstructor() {
        TimeInterval actual = new TimeInterval(String.format("%s/%s", LOCAL_TIME_FROM, LOCAL_TIME_TO));
        softly.assertThat(actual.getFormattedTimeInterval()).isEqualTo(EXPECTED);
    }

    @Test
    public void testOffsetConstructor() {
        TimeInterval actual = new TimeInterval(String.format("%s/%s", OFFSET_TIME_FROM, OFFSET_TIME_TO));
        softly.assertThat(actual.getFormattedTimeInterval()).isEqualTo(EXPECTED);
    }

    @Test
    public void testOffsetEqualsLocalFactories() {
        TimeInterval offset = TimeInterval.of(OffsetTime.parse(OFFSET_TIME_FROM), OffsetTime.parse(OFFSET_TIME_TO));
        TimeInterval local = TimeInterval.of(LocalTime.parse(LOCAL_TIME_FROM), LocalTime.parse(LOCAL_TIME_TO));
        softly.assertThat(offset).isEqualTo(local);
    }

    @Test
    public void testOffsetEqualsLocalConstructors() {
        TimeInterval local = new TimeInterval(String.format("%s/%s", LOCAL_TIME_FROM, LOCAL_TIME_TO));
        TimeInterval offset = new TimeInterval(String.format("%s/%s", OFFSET_TIME_FROM, OFFSET_TIME_TO));
        softly.assertThat(offset).isEqualTo(local);
    }

}
