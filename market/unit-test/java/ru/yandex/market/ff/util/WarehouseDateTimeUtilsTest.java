package ru.yandex.market.ff.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;

public class WarehouseDateTimeUtilsTest extends SoftAssertionSupport {

    @Test
    public void toInstant() {
        assertions.assertThat(WarehouseDateTimeUtils.toInstant(LocalDateTime.parse("2020-02-25T01:01:01"), null))
                .isEqualTo(Instant.parse("2020-02-24T22:01:01Z"));
        assertions.assertThat(WarehouseDateTimeUtils.toInstant(LocalDateTime.parse("2020-02-25T01:01:01"), 172L))
                .isEqualTo(Instant.parse("2020-02-24T22:01:01Z"));
        assertions.assertThat(WarehouseDateTimeUtils.toInstant(LocalDateTime.parse("2020-02-25T01:01:01"), 17217217L))
                .isEqualTo(Instant.parse("2020-02-24T22:01:01Z"));
        assertions.assertThat(WarehouseDateTimeUtils.toInstant(LocalDateTime.parse("2020-02-25T01:01:01"), 300L))
                .isEqualTo(Instant.parse("2020-02-24T20:01:01Z"));
    }

    @Test
    public void toOffsetDateTime() {
        assertions.assertThat(WarehouseDateTimeUtils.toOffsetDateTime(
                LocalDateTime.parse("2020-02-25T01:01:01"), null))
                .isEqualTo(OffsetDateTime.parse("2020-02-25T01:01:01+03:00"));
        assertions.assertThat(WarehouseDateTimeUtils.toOffsetDateTime(
                LocalDateTime.parse("2020-02-25T01:01:01"), 172L))
                .isEqualTo(OffsetDateTime.parse("2020-02-25T01:01:01+03:00"));
        assertions.assertThat(WarehouseDateTimeUtils.toOffsetDateTime(
                LocalDateTime.parse("2020-02-25T01:01:01"), 17217217L))
                .isEqualTo(OffsetDateTime.parse("2020-02-25T01:01:01+03:00"));
        assertions.assertThat(WarehouseDateTimeUtils.toOffsetDateTime(
                LocalDateTime.parse("2020-02-25T01:01:01"), 300L))
                .isEqualTo(OffsetDateTime.parse("2020-02-25T01:01:01+05:00"));
    }

    @Test
    public void toLocalDateTime() {
        assertions.assertThat(WarehouseDateTimeUtils.toLocalDateTime(Instant.parse("2020-02-24T22:01:01Z"), null))
                .isEqualTo(LocalDateTime.parse("2020-02-25T01:01:01"));
        assertions.assertThat(WarehouseDateTimeUtils.toLocalDateTime(Instant.parse("2020-02-24T22:01:01Z"), 172L))
                .isEqualTo(LocalDateTime.parse("2020-02-25T01:01:01"));
        assertions.assertThat(WarehouseDateTimeUtils.toLocalDateTime(Instant.parse("2020-02-24T22:01:01Z"), 17217217L))
                .isEqualTo(LocalDateTime.parse("2020-02-25T01:01:01"));
        assertions.assertThat(WarehouseDateTimeUtils.toLocalDateTime(Instant.parse("2020-02-24T22:01:01Z"), 300L))
                .isEqualTo(LocalDateTime.parse("2020-02-25T03:01:01"));
    }

    @Test
    public void fromMoscowToWarehouseLocalDate() {
        assertions.assertThat(WarehouseDateTimeUtils.fromMoscowToWarehouseLocalDate(
                LocalDateTime.parse("2020-02-25T01:01:01"), null))
                .isEqualTo(LocalDateTime.parse("2020-02-25T01:01:01"));
        assertions.assertThat(WarehouseDateTimeUtils.fromMoscowToWarehouseLocalDate(
                LocalDateTime.parse("2020-02-25T01:01:01"), 172L))
                .isEqualTo(LocalDateTime.parse("2020-02-25T01:01:01"));
        assertions.assertThat(WarehouseDateTimeUtils.fromMoscowToWarehouseLocalDate(
                LocalDateTime.parse("2020-02-25T01:01:01"), 17217217L))
                .isEqualTo(LocalDateTime.parse("2020-02-25T01:01:01"));
        assertions.assertThat(WarehouseDateTimeUtils.fromMoscowToWarehouseLocalDate(
                LocalDateTime.parse("2020-02-25T01:01:01"), 300L))
                .isEqualTo(LocalDateTime.parse("2020-02-25T03:01:01"));
    }
}
