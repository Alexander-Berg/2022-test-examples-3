package ru.yandex.market.logistic.gateway.common.model.fulfillment;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class DateTimeTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private final String OFFSET_DATE = "2050-10-20T11:00:00+03:00";
    private final String LOCAL_DATE = "2050-10-20T11:00:00";

    @Test
    public void testOffsetConstructor() {
        OffsetDateTime expected = OffsetDateTime.parse(OFFSET_DATE);
        DateTime dateTime = new DateTime(OFFSET_DATE);
        softly.assertThat(dateTime.getOffsetDateTime()).isEqualTo(expected);
    }

    @Test
    public void testLocalConstructor() {
        LocalDateTime local = LocalDateTime.parse(LOCAL_DATE);
        OffsetDateTime expected = OffsetDateTime.of(local, ZoneOffset.ofHours(3));
        DateTime dateTime = new DateTime(LOCAL_DATE);
        softly.assertThat(dateTime.getOffsetDateTime()).isEqualTo(expected);
    }

    @Test
    public void testOffsetFactory() {
        OffsetDateTime expected = OffsetDateTime.parse(OFFSET_DATE);
        DateTime dateTime = DateTime.fromOffsetDateTime(expected);
        softly.assertThat(dateTime.getOffsetDateTime()).isEqualTo(expected);
    }

    @Test
    public void testLocalFactory() {
        LocalDateTime local = LocalDateTime.parse(LOCAL_DATE);
        OffsetDateTime expected = OffsetDateTime.of(local, ZoneOffset.ofHours(3));
        DateTime dateTime = DateTime.fromLocalDateTime(local);
        softly.assertThat(dateTime.getOffsetDateTime()).isEqualTo(expected);
    }

    @Test
    public void testOffsetEqualsLocalFactories() {
        LocalDateTime local = LocalDateTime.parse(LOCAL_DATE);
        OffsetDateTime offset = OffsetDateTime.parse(OFFSET_DATE);
        DateTime localDateTime = DateTime.fromLocalDateTime(local);
        DateTime offsetDateTime = DateTime.fromOffsetDateTime(offset);
        softly.assertThat(localDateTime).isEqualTo(offsetDateTime);
    }

    @Test
    public void testOffsetEqualsLocalConstructors() {
        DateTime localDateTime = new DateTime(LOCAL_DATE);
        DateTime offsetDateTime = new DateTime(OFFSET_DATE);
        softly.assertThat(localDateTime).isEqualTo(offsetDateTime);
    }

}
