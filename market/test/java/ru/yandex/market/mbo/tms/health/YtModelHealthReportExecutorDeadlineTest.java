package ru.yandex.market.mbo.tms.health;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.KeyValueMapService;
import ru.yandex.market.mbo.tms.health.published.guru.KpiStatsReport;
import ru.yandex.market.mbo.tms.health.sessions.Scale;
import ru.yandex.utils.Pair;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 25.05.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class YtModelHealthReportExecutorDeadlineTest {

    private YtModelHealthReportExecutor executor;

    private KeyValueMapService keyValueMapService;

    private YtHealthMapReduceService ytHealthMapReduceService;

    private KpiStatsReport kpiStatsReport;

    @Before
    public void before() {
        keyValueMapService = mock(KeyValueMapService.class);
        ytHealthMapReduceService = mock(YtHealthMapReduceService.class);
        kpiStatsReport = mock(KpiStatsReport.class);
        doReturn(Pair.makePair(null, null)).when(kpiStatsReport).getContextAndSupply();
        executor = new YtModelHealthReportExecutor();
        executor.setYtHealthMapReduceService(ytHealthMapReduceService);
        executor.setKpiStatsReport(kpiStatsReport);
        executor.setKeyValueMapService(keyValueMapService);
    }

    @Test
    public void regularDaily() throws Exception {
        executor.setScale(Scale.DAY);
        executor.setClock(clock(2000, Month.MAY, 7, 13, 0));
        executor.doRealJob(null);

        verify(keyValueMapService).put("model-health-report-deadline-day", "2000-05-08T23:00:00");
    }

    @Test
    public void dailyLateRun() throws Exception {
        executor.setScale(Scale.DAY);
        executor.setClock(clock(2000, Month.MAY, 7, 23, 59));
        executor.doRealJob(null);

        verify(keyValueMapService).put("model-health-report-deadline-day", "2000-05-08T23:00:00");
    }

    @Test
    public void regularWeekly() throws Exception {
        executor.setScale(Scale.WEEK);
        executor.setClock(clock(2000, Month.MAY, 7, 14, 0)); // it is sunday, trust me
        executor.doRealJob(null);

        verify(keyValueMapService).put("model-health-report-deadline-week", "2000-05-14T23:00:00");
    }

    @Test
    public void weeklyInMiddleOfWeek() throws Exception {
        executor.setScale(Scale.WEEK);
        executor.setClock(clock(2000, Month.MAY, 9, 14, 0));
        executor.doRealJob(null);

        verify(keyValueMapService).put("model-health-report-deadline-week", "2000-05-14T23:00:00");
    }

    @Test
    public void monthlyRegular() throws Exception {
        executor.setScale(Scale.MONTH);
        executor.setClock(clock(2000, Month.MAY, 31, 14, 0));
        executor.doRealJob(null);

        verify(keyValueMapService).put("model-health-report-deadline-month", "2000-06-30T23:00:00");
    }

    @Test
    public void regular() throws Exception {
        executor.setClock(clock(2000, Month.MAY, 6, 13, 0));
        executor.doRealJob(null);
        verify(keyValueMapService).put("model-health-report-deadline-day", "2000-05-07T23:00:00");

        executor.setClock(clock(2000, Month.MAY, 7, 14, 0));
        executor.doRealJob(null);
        verify(keyValueMapService).put("model-health-report-deadline-day", "2000-05-08T23:00:00");
        verify(keyValueMapService).put("model-health-report-deadline-week", "2000-05-14T23:00:00");

        executor.setClock(clock(2000, Month.MAY, 31, 14, 0));
        executor.doRealJob(null);
        verify(keyValueMapService).put("model-health-report-deadline-day", "2000-06-01T23:00:00");
        verify(keyValueMapService).put("model-health-report-deadline-month", "2000-06-30T23:00:00");
    }

    @Test
    public void monthlyInMiddleOfMonth() throws Exception {
        executor.setScale(Scale.MONTH);
        executor.setClock(clock(2000, Month.JUNE, 2, 19, 0));
        executor.doRealJob(null);

        verify(keyValueMapService).put("model-health-report-deadline-month", "2000-06-30T23:00:00");
    }

    @Test
    public void dontUpdateDeadlineIfFailed() throws Exception {
        executor.setScale(Scale.MONTH);
        executor.setClock(clock(2000, Month.JULY, 15, 19, 0));
        Exception exception = new RuntimeException();
        doThrow(exception)
            .when(kpiStatsReport)
            .getContextAndSupply();

        assertThatThrownBy(() -> executor.doRealJob(null)).isSameAs(exception);

        verify(keyValueMapService, never()).put(anyString(), anyString());
    }



    private Clock clock(int year, Month month, int day, int hour, int minutes) {
        ZoneOffset zone = ZoneOffset.UTC;
        return Clock.fixed(LocalDateTime.of(year, month, day, hour, minutes).toInstant(zone), zone);
    }
}
