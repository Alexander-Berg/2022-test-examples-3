package ru.yandex.calendar.test.auto.dtf;


import java.util.stream.Stream;

import lombok.Value;
import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.calendar.util.dates.AuxDateTime;
import ru.yandex.calendar.util.dates.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

public class DtfConsumedCellsTest extends AbstractDtfTestCase {
    @Value
    private static class ConsumedCell {
        int expected;
        Chronology dtf;
        long ms1;
        long ms2;
    }

    public static Stream<Arguments> parameters() {
        val now = AuxDateTime.NOW();
        return StreamEx.of(tzId2ChronoDtfMap.values())
                .flatMap(p -> getConsumedCells(now, p))
                .map(Arguments::of);
    }

    private static Stream<ConsumedCell> getConsumedCells(long now, Chronology p) {
        val ms = new DateTime(now, p).withTimeAtStartOfDay().getMillis();
        return StreamEx.of(
                cell(1, p, ms, ms),
                cell(1, p, ms, ms + 1),
                cell(1, p, ms - 1, ms)
        );
    }

    private static ConsumedCell cell(int expected, Chronology dtf, long ms1, long ms2) {
        return new ConsumedCell(expected, dtf, ms1, ms2);
    }

    @ParameterizedTest(name = "{index}")
    @DisplayName("Check consumed cells.")
    @MethodSource("parameters")
    public void consumedCells(ConsumedCell cell) {
        val ms1 = cell.getMs1();
        val ms2 = cell.getMs2();
        val dtf = cell.getDtf();
        val expected = cell.getExpected();

        assertThat(DateTimeFormatter.getConsumedGridCells(ms1, ms2, dtf.getZone())).isEqualTo(expected);
    }
}
