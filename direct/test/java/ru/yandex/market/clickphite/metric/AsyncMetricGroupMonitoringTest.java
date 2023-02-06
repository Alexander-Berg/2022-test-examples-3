package ru.yandex.market.clickphite.metric;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clickphite.metric.mocks.ComplicatedMonitoringMock;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.clickphite.metric.mocks.MetricContextGroupMock.failingMetric;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 01.12.16
 */
public class AsyncMetricGroupMonitoringTest {
    private ComplicatedMonitoringMock complicatedMonitoringMock;
    private MetricGroupMonitoring metricGroupMonitoring;
    private AsyncMetricGroupMonitoring sut;

    @Before
    public void setUp() throws Exception {
        complicatedMonitoringMock = new ComplicatedMonitoringMock();
        metricGroupMonitoring = new MetricGroupMonitoring();
        metricGroupMonitoring.setMonitoring(complicatedMonitoringMock);
        sut = new AsyncMetricGroupMonitoring();
        sut.setMetricGroupMonitoring(metricGroupMonitoring);
    }

    @Test
    public void smokeTest() throws Exception {
        metricGroupMonitoring.setFailedMetricsMinutesToWarn(10);
        metricGroupMonitoring.setMassiveMetricFailureMinutesToCrit(5);
        metricGroupMonitoring.setMassiveMetricFailurePercentToCrit(50);

        sut.setTotalMetricCount(3);

        List<Future<?>> futures = Arrays.asList(
            sut.reportFailure(failingMetric("metric1", 10)),
            sut.reportFailure(failingMetric("metric2", 5))
        );

        waitFutures(futures);

        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoringMock.getUnit().getStatus());
    }

    private void waitFutures(List<Future<?>> futures) throws InterruptedException, java.util.concurrent.ExecutionException {
        for (Future<?> future : futures) {
            future.get();
        }
    }
}