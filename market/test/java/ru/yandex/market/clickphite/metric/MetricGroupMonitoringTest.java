package ru.yandex.market.clickphite.metric;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.clickphite.metric.mocks.ComplicatedMonitoringMock;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;
import ru.yandex.market.monitoring.MonitoringStatus;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.clickphite.metric.mocks.MetricContextGroupMock.failingMetric;
import static ru.yandex.market.clickphite.metric.mocks.MetricContextGroupMock.metric;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 01.12.16
 */
public class MetricGroupMonitoringTest {
    private static final int MANY = 100500;
    private static final String METRIC = "metric";
    private static final String METRIC_1 = "metric1";
    private static final String METRIC_2 = "metric2";

    private MetricGroupMonitoring sut;
    private ComplicatedMonitoringMock complicatedMonitoringMock;

    @Before
    public void setUp() throws Exception {
        sut = new MetricGroupMonitoring();
        complicatedMonitoringMock = new ComplicatedMonitoringMock();
        sut.setMonitoring(complicatedMonitoringMock);
        sut.setMassiveMetricFailurePercentToCrit(50);
        sut.setMassiveMetricFailureMinutesToCrit(5);
        sut.setFailedMetricsMinutesToWarn(10);
        sut.setFailedMetricsMinutesToCrit(15);
    }

    @Test(expected = IllegalStateException.class)
    public void failIfMetricsCountIsNotSet() {
        sut.reportSuccess(metric(METRIC));
    }

    @Test
    public void shouldBeOkOnSuccess() {
        sut.setTotalMetricCount(1);
        sut.reportSuccess(metric(METRIC));

        assertEquals(MonitoringStatus.OK, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void shouldWarnIfOneOfManyMetricsFailsForAShortTime() {
        sut.setTotalMetricCount(MANY);
        sut.reportFailure(failingMetric(METRIC, 10));

        assertEquals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void shouldWarnIfOneOfManyMetricsFailsForALongTime() {
        sut.setTotalMetricCount(MANY);
        sut.reportFailure(failingMetric(METRIC, 15));

        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void shouldBecomeOkAfterWarningAfterFirstSuccess() {
        sut.setTotalMetricCount(MANY);
        MetricContextGroup metric = failingMetric(METRIC, 10);
        sut.reportFailure(metric);

        assertEquals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());

        sut.reportSuccess(metric);

        assertEquals(MonitoringStatus.OK, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void shouldBecomeOkAfterWarningIfMetricWasRemoved() {
        sut.setTotalMetricCount(MANY);
        MetricContextGroup metric = failingMetric(METRIC, 10);
        sut.reportFailure(metric);

        assertEquals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());

        sut.actualizeMetrics(Collections.emptyList());

        assertEquals(MonitoringStatus.OK, complicatedMonitoringMock.getUnit().getStatus());
    }


    @Test
    public void warningShouldRemainIfMetricWasNotRemoved() {
        sut.setTotalMetricCount(MANY);
        MetricContextGroup metric = failingMetric(METRIC, 10);
        sut.reportFailure(metric);

        assertEquals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());

        sut.actualizeMetrics(Collections.singletonList(metric(METRIC)));

        assertEquals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void shouldBecomeCritIfTooManyMetricsFail() {
        sut.setTotalMetricCount(3);

        sut.reportFailure(failingMetric(METRIC_1, 10));
        sut.reportFailure(failingMetric(METRIC_2, 5));

        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void shouldBecomeWarnAfterSomeMetricsRecovered() {
        sut.setTotalMetricCount(3);

        sut.reportFailure(failingMetric(METRIC_1, 10));
        sut.reportFailure(failingMetric(METRIC_2, 5));
        sut.reportSuccess(metric(METRIC_2));

        assertEquals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void shouldIgnoreMetricsThatBecomeUnsuccessfulRecently() {
        sut.setTotalMetricCount(MANY);

        sut.reportFailure(failingMetric(METRIC_1, 5));

        assertEquals(MonitoringStatus.OK, complicatedMonitoringMock.getUnit().getStatus());
    }
}
