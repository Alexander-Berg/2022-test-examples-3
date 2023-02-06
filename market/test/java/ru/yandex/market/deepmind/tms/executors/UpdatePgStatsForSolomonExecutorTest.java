package ru.yandex.market.deepmind.tms.executors;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.deepmind.tms.services.PgTableStatisticService;
import ru.yandex.market.deepmind.tms.services.TableStatistic;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdatePgStatsForSolomonExecutorTest {
    private UpdatePgStatsForSolomonExecutor executor;
    private DeepmindSolomonPushService solomonPushService;
    private PgTableStatisticService pgTableStatisticService;

    @Before
    public void setUp() throws Exception {
        solomonPushService = Mockito.mock(DeepmindSolomonPushService.class);
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

        var solomonMetric = (List<Sensor>) metricsCaptor.getValue();
        assertEquals(1, solomonMetric.size());
    }
}
