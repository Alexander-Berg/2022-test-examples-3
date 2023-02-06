package ru.yandex.market.mboc.tms.executors.statistic;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.mdm.common.masterdata.model.SolomonMetric;
import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.services.statistics.PgTableStatisticService;
import ru.yandex.market.mboc.common.services.statistics.TableStatistic;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author apluhin
 * @created 10/26/20
 */
public class UpdatePgStatsForSolomonExecutorTest {
    private UpdatePgStatsForSolomonExecutor executor;
    private SolomonPushService solomonPushService;
    private PgTableStatisticService pgTableStatisticService;

    @Before
    public void setUp() throws Exception {
        solomonPushService = Mockito.mock(SolomonPushService.class);
        pgTableStatisticService = Mockito.mock(PgTableStatisticService.class);
        executor = new UpdatePgStatsForSolomonExecutor(
            pgTableStatisticService,
            solomonPushService
        );
    }

    @Test
    public void testPushPgMetrics() {
        when(pgTableStatisticService.getTablesInformation()).thenReturn(
            List.of(
                new TableStatistic("mbo_category", "test_1", Map.of("param1", "value"))
            )
        );
        when(pgTableStatisticService.convertMetric(any())).thenCallRealMethod();
        executor.execute();
        var metricsCaptor = ArgumentCaptor.forClass(List.class);
        verify(solomonPushService, times(1)).push(metricsCaptor.capture());

        var solomonMetric = (List<SolomonMetric>) metricsCaptor.getValue();
        assertEquals(1, solomonMetric.size());
    }
}
