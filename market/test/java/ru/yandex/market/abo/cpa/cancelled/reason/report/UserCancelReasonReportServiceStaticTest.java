package ru.yandex.market.abo.cpa.cancelled.reason.report;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.cpa.cancelled.reason.report.UserCancelReasonReportService.DateStat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.abo.cpa.cancelled.reason.report.UserCancelReasonReportService.DateReasonStat;
import static ru.yandex.market.abo.cpa.cancelled.reason.report.UserCancelReasonReportService.groupStatByDate;

/**
 * @author antipov93.
 * @date 09.10.18.
 */
public class UserCancelReasonReportServiceStaticTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Test
    public void testTransform() {
        List<DateReasonStat> dateReasonStats = Arrays.asList(
                new DateReasonStat(TODAY, 1, 2),
                new DateReasonStat(TODAY, 2, 5),
                new DateReasonStat(TODAY, 6, 2),
                new DateReasonStat(YESTERDAY, 3, 3),
                new DateReasonStat(YESTERDAY, 5, 4),
                new DateReasonStat(TOMORROW, 1, 10)
        );
        List<DateStat> dateStats = groupStatByDate(dateReasonStats);

        DateStat todayStat = find(TODAY, dateStats);
        DateStat yesterdayStat = find(YESTERDAY, dateStats);
        DateStat tomorrowStat = find(TOMORROW, dateStats);

        assertEquals(todayStat.getTotalCount(), 2 + 5 + 2);
        assertEquals(todayStat.getCount(1), 2);
        assertEquals(todayStat.getCount(2), 5);
        assertEquals(todayStat.getCount(6), 2);
        assertEquals(todayStat.getCount(3), 0);

        assertEquals(yesterdayStat.getTotalCount(), 3 + 4);
        assertEquals(yesterdayStat.getCount(3), 3);
        assertEquals(yesterdayStat.getCount(5), 4);

        assertEquals(tomorrowStat.getTotalCount(), 10);
        assertEquals(tomorrowStat.getCount(1), 10);
    }

    private static DateStat find(
            LocalDate date,
            List<DateStat> statsByDate) {
        return statsByDate.stream().filter(stat -> stat.getDate().equals(date)).findFirst().orElse(null);
    }

}
