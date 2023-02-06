package ru.yandex.calendar.test.auto.dtf;

import java.util.stream.Stream;

import lombok.Value;
import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.calendar.util.dates.AuxDateTime;
import ru.yandex.calendar.util.dates.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

public class DtfDiffInDaysTest extends AbstractDtfTestCase {
    @Value
    private static class DiffInDays {
        int expected;
        Chronology dtf;
        long ms1;
        long ms2;
    }

    public static Stream<Arguments> parameters() {
        return StreamEx.of(tzId2ChronoDtfMap.values())
                .flatMap(DtfDiffInDaysTest::getDiffInDays)
                .map(Arguments::of);
    }

    private static Stream<DiffInDays> getDiffInDays(Chronology p) {
        val ms1 = new DateTime(2005, 3, 26, 12, 0, 0, 0, p).getMillis();
        val ms2 = new DateTime(2005, 3, 26, 23, 59, 59, 999, p).getMillis();
        val ms3 = new LocalDate(2005, 3, 26, p).toDateTimeAtStartOfDay(p.getZone()).getMillis();
        return StreamEx.of(
                diff(0, p, ms1, AuxDateTime.MS_PER_HOUR),
                diff(0, p, ms1, AuxDateTime.MS_PER_HOUR * 12 - 1),
                diff(1, p, ms1, AuxDateTime.MS_PER_HOUR * 12),
                diff(2, p, ms1, AuxDateTime.MS_PER_HOUR * 37),
                diff(-1, p, ms1, AuxDateTime.MS_PER_HOUR * -12 - 1),
                diff(0, p, ms1, AuxDateTime.MS_PER_HOUR * -2),
                diff(1, p, ms2, 1L),
                diff(0, p, ms3, 0L),
                diff(-1, p, ms3, -1L),
                diff(1, p, ms3, AuxDateTime.MS_PER_HOUR * 24)
        );
    }

    private static DiffInDays diff(int expected, Chronology dtf, long ms1, long delta) {
        return new DiffInDays(expected, dtf, ms1, ms1 + delta);
    }

    @ParameterizedTest(name = "{index}")
    @DisplayName("Check days between midnights.")
    @MethodSource("parameters")
    public void daysBtwMidnights(DiffInDays diff) {
        val ms1 = diff.getMs1();
        val ms2 = diff.getMs2();
        val dtf = diff.getDtf();
        val expected = diff.getExpected();
        assertThat(DateTimeFormatter.getDaysBtwMidnights(ms1, ms2, dtf.getZone())).isEqualTo(expected);
    }
}
