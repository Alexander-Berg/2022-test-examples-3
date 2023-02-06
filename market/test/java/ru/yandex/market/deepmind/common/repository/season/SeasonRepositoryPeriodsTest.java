package ru.yandex.market.deepmind.common.repository.season;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.availability.SeasonPeriodUtils.MMDD;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 28.11.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SeasonRepositoryPeriodsTest {
    private static MMDD period(Month month, int day) {
        return new MMDD(month.getValue(), day);
    }

    @Test
    public void testPeriodWithoutYearDifferentStartYear() {
        MMDD start = new MMDD(11, 1);
        MMDD end = new MMDD(2, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 1, 5), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYearDifferentStartYearSameMonth() {
        MMDD start = new MMDD(1, 20);
        MMDD end = new MMDD(1, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 1, 5), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYearSameMonthNoMatch() {
        MMDD start = new MMDD(1, 20);
        MMDD end = new MMDD(1, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 1, 15), start, end)).isFalse();
    }

    @Test
    public void testPeriodWithoutYearDifferentEndYear() {
        MMDD start = new MMDD(11, 1);
        MMDD end = new MMDD(2, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 12, 1), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYearDifferentEndYearNoMatch() {
        MMDD start = new MMDD(11, 1);
        MMDD end = new MMDD(2, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 3, 1), start, end)).isFalse();
    }

    @Test
    public void testPeriodWithoutYearDifferentEndYearNoMatchLeft() {
        MMDD start = new MMDD(11, 1);
        MMDD end = new MMDD(2, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 10, 1), start, end)).isFalse();
    }

    @Test
    public void testPeriodWithoutYearDifferentEndYearSameMonth() {
        MMDD start = new MMDD(1, 20);
        MMDD end = new MMDD(1, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 1, 25), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYearDifferentEndYearSameMonthInclusiveStart() {
        MMDD start = new MMDD(1, 20);
        MMDD end = new MMDD(1, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 1, 20), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYearDifferentEndYearSameMonthInclusiveEnd() {
        MMDD start = new MMDD(1, 20);
        MMDD end = new MMDD(1, 10);
        assertThat(MMDD
            .match(LocalDate.of(2000, 1, 10), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYear() {
        MMDD start = new MMDD(1, 1);
        MMDD end = new MMDD(3, 1);
        assertThat(MMDD
            .match(LocalDate.of(2000, 2, 1), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYearInclusiveStart() {
        MMDD start = new MMDD(1, 1);
        MMDD end = new MMDD(3, 1);
        assertThat(MMDD
            .match(LocalDate.of(2000, 1, 1), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYearInclusiveEnd() {
        MMDD start = new MMDD(1, 1);
        MMDD end = new MMDD(3, 1);
        assertThat(MMDD
            .match(LocalDate.of(2000, 3, 1), start, end)).isTrue();
    }

    @Test
    public void testPeriodWithoutYearNoMatch() {
        MMDD start = new MMDD(1, 1);
        MMDD end = new MMDD(3, 1);
        assertThat(MMDD
            .match(LocalDate.of(2000, 4, 1), start, end)).isFalse();
    }

    @Test
    public void endOfFebruaryShouldBeYearTolerant() {
        assertThat(MMDD
            .match(LocalDate.of(2000, Month.FEBRUARY, 29), period(Month.FEBRUARY, 1), period(Month.FEBRUARY, 28)))
            .isTrue();

        assertThat(MMDD
            .match(LocalDate.of(2001, Month.FEBRUARY, 28), period(Month.FEBRUARY, 29), period(Month.MARCH, 3)))
            .isTrue();
    }

    @Test
    public void mmDDToString() {
        var parse = MMDD.parse("01-11");
        assertThat(parse.toString()).isEqualTo("01-11");

        var parse2 = MMDD.parse("02-29");
        assertThat(parse2.toString()).isEqualTo("02-29");
    }
}
