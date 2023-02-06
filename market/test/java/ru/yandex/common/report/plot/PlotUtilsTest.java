package ru.yandex.common.report.plot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import junit.framework.TestCase;
import org.junit.Test;

import static ru.yandex.common.report.plot.PlotUtils.MOSCOW_TIME_ZONE_ID;
import static ru.yandex.common.report.plot.PlotUtils.localDateFromSeconds;
import static ru.yandex.common.report.plot.PlotUtils.secondsFromLocalDate;
import static ru.yandex.common.report.plot.PlotUtils.truncWeek;

/**
 * Тест на {@link PlotUtils}.
 */
public class PlotUtilsTest extends TestCase {

    @Test
    public void testGenerateTimeSeriesWithOptionals() {
        Map<Long, BigDecimal> map = new HashMap<>();
        map.put(1L, BigDecimal.valueOf(1100));
        map.put(6L, BigDecimal.valueOf(2332));
        map.put(12L, BigDecimal.valueOf(200));
        map.put(13L, BigDecimal.valueOf(203));
        map.put(19L, BigDecimal.valueOf(2455));
        map.put(21L, BigDecimal.valueOf(20));
        map.put(22L, BigDecimal.valueOf(3000));
        map.put(25L, BigDecimal.valueOf(21000000));
        Plot<Long, BigDecimal> resultPlot = PlotUtils.generateTimeSeriesWithOptionals(
                -2L,
                27L,
                k -> k + 1,
                k -> {
                    BigDecimal v = map.get(k);
                    if (v != null) {
                        return v;
                    } else {
                        return BigDecimal.ZERO;
                    }
                });
        assertEquals("(-2,0)\n" +
                "(-1,0)\n" +
                "(0,0)\n" +
                "(1,1100)\n" +
                "(2,0)\n" +
                "(3,0)\n" +
                "(4,0)\n" +
                "(5,0)\n" +
                "(6,2332)\n" +
                "(7,0)\n" +
                "(8,0)\n" +
                "(9,0)\n" +
                "(10,0)\n" +
                "(11,0)\n" +
                "(12,200)\n" +
                "(13,203)\n" +
                "(14,0)\n" +
                "(15,0)\n" +
                "(16,0)\n" +
                "(17,0)\n" +
                "(18,0)\n" +
                "(19,2455)\n" +
                "(20,0)\n" +
                "(21,20)\n" +
                "(22,3000)\n" +
                "(23,0)\n" +
                "(24,0)\n" +
                "(25,21000000)\n" +
                "(26,0)", resultPlot.toString());
    }

    @Test
    public void testTimeSeriesDaysTest() {
        Map<Long, BigDecimal> map = new HashMap<>();
        map.put(LocalDate.of(2021, 9, 1)
                .atStartOfDay(MOSCOW_TIME_ZONE_ID)
                .toEpochSecond(), BigDecimal.valueOf(1100));
        map.put(LocalDate.of(2021, 9, 3)
                .atStartOfDay(MOSCOW_TIME_ZONE_ID)
                .toEpochSecond(), BigDecimal.valueOf(200));
        map.put(LocalDate.of(2021, 9, 4)
                .atStartOfDay(MOSCOW_TIME_ZONE_ID)
                .toEpochSecond(), BigDecimal.valueOf(203));
        map.put(LocalDate.of(2021, 9, 6)
                .atStartOfDay(MOSCOW_TIME_ZONE_ID)
                .toEpochSecond(), BigDecimal.valueOf(20));
        map.put(LocalDate.of(2021, 9, 8)
                .atStartOfDay(MOSCOW_TIME_ZONE_ID)
                .toEpochSecond(), BigDecimal.valueOf(21000000));
        Plot<Long, BigDecimal> resultPlot = PlotUtils.generateTimeSeriesWithOptionals(
                LocalDate.of(2021, 9, 1)
                        .atStartOfDay(MOSCOW_TIME_ZONE_ID)
                        .toEpochSecond(),
                secondsFromLocalDate(localDateFromSeconds(
                        LocalDate.of(2021, 9, 8)
                                .atStartOfDay(MOSCOW_TIME_ZONE_ID)
                                .toEpochSecond()).plusDays(1L)),
                k -> secondsFromLocalDate(localDateFromSeconds(k).plusDays(1)),
                k -> {
                    BigDecimal v = map.get(k);
                    if (v != null) {
                        return v;
                    } else {
                        return BigDecimal.ZERO;
                    }
                });

        assertEquals("" +
                "2021-09-01 1100\n" +
                "2021-09-02 0\n" +
                "2021-09-03 200\n" +
                "2021-09-04 203\n" +
                "2021-09-05 0\n" +
                "2021-09-06 20\n" +
                "2021-09-07 0\n" +
                "2021-09-08 21000000", resultPlot.getPoints()
                .stream()
                .map(p -> localDateFromSeconds(p.getKey()) + " " + p.getValue())
                .collect(Collectors.joining("\n")));
    }

    public void testTimeSeriesWeeksTest() {
        Map<Long, BigDecimal> map = new HashMap<>();
        map.put(1559509200L, BigDecimal.valueOf(1100));
        map.put(1560718800L, BigDecimal.valueOf(203));
        map.put(1561928400L, BigDecimal.valueOf(2000));
        map.put(1563138000L, BigDecimal.valueOf(210000));
        Plot<Long, BigDecimal> resultPlot = PlotUtils.generateTimeSeriesWithOptionals(
                truncWeek(LocalDate.of(2019, 4, 5))
                        .atStartOfDay(MOSCOW_TIME_ZONE_ID).toEpochSecond(),
                truncWeek(LocalDate.of(2019, 10, 29))
                        .atStartOfDay(MOSCOW_TIME_ZONE_ID).toEpochSecond(),
                k -> secondsFromLocalDate(localDateFromSeconds(k).plusWeeks(1)),
                k -> {
                    BigDecimal v = map.get(k);
                    if (v != null) {
                        return v;
                    } else {
                        return BigDecimal.ZERO;
                    }
                });

        assertEquals("" +
                "2019-04-01 0\n" +
                "2019-04-08 0\n" +
                "2019-04-15 0\n" +
                "2019-04-22 0\n" +
                "2019-04-29 0\n" +
                "2019-05-06 0\n" +
                "2019-05-13 0\n" +
                "2019-05-20 0\n" +
                "2019-05-27 0\n" +
                "2019-06-03 1100\n" +
                "2019-06-10 0\n" +
                "2019-06-17 203\n" +
                "2019-06-24 0\n" +
                "2019-07-01 2000\n" +
                "2019-07-08 0\n" +
                "2019-07-15 210000\n" +
                "2019-07-22 0\n" +
                "2019-07-29 0\n" +
                "2019-08-05 0\n" +
                "2019-08-12 0\n" +
                "2019-08-19 0\n" +
                "2019-08-26 0\n" +
                "2019-09-02 0\n" +
                "2019-09-09 0\n" +
                "2019-09-16 0\n" +
                "2019-09-23 0\n" +
                "2019-09-30 0", resultPlot.getPoints()
                .stream()
                .map(p -> localDateFromSeconds(p.getKey()) + " " + p.getValue())
                .collect(Collectors.joining("\n")));
    }
}
