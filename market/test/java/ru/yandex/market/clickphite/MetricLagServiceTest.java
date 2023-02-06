package ru.yandex.market.clickphite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.junit.Test;

import ru.yandex.market.health.KeyValueMetricSupplier;
import ru.yandex.market.health.SimpleMetricSupplier;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.metric.MetricQueue;
import ru.yandex.market.health.configs.clickphite.metric.MetricQueueReceiveTimeUpdater;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.monitoring.MonitoringUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 14.04.17
 */
public class MetricLagServiceTest {
    @Test
    public void lagMonitoring() {
        testLagMonitoring(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 25, 25, 25), MonitoringStatus.CRITICAL, true);
        testLagMonitoring(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 15, 15, 15), MonitoringStatus.WARNING, true);
        testLagMonitoring(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), MonitoringStatus.OK, false);
    }

    private static void testLagMonitoring(
        List<Integer> realTimeLagsSeconds,
        MonitoringStatus expectedMonitoringStatus,
        boolean expectedLagMode
    ) {
        ComplicatedMonitoring complicatedMonitoring = new ComplicatedMonitoring();
        ClickphiteService clickphiteService = mock(ClickphiteService.class);

        MetricLagService metricLagService = getMetricLagService(clickphiteService, complicatedMonitoring);

        MonitoringUnit monitoringUnit = complicatedMonitoring.getOrAddUnit("lag");
        monitoringUnit.disableMonitoringDelay();

        metricLagService.updateLagModeAndMonitoring(realTimeLagsSeconds, ArrayListMultimap.create());
        assertEquals(expectedMonitoringStatus, monitoringUnit.getStatus());
        verify(clickphiteService).setLagMode(expectedLagMode);
        verifyNoMoreInteractions(clickphiteService);
    }

    @Test
    public void lagMonitoringByPeriod() {
        ComplicatedMonitoring complicatedMonitoring = new ComplicatedMonitoring();
        ClickphiteService clickphiteService = mock(ClickphiteService.class);
        MetricLagService metricLagService = getMetricLagService(clickphiteService, complicatedMonitoring);
        Map<String, Integer> workerPoolLagThresholdSeconds = new HashMap<>();
        workerPoolLagThresholdSeconds.put("CRIT", 10);
        workerPoolLagThresholdSeconds.put("WARN", 10);
        workerPoolLagThresholdSeconds.put("OK", 10);
        metricLagService.setWorkerPoolLagThresholdSeconds(
            workerPoolLagThresholdSeconds
        );

        List<Integer> critLag = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 25, 25, 25);
        List<Integer> warnLag = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 15, 15, 15);
        List<Integer> okLag = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        Multimap<String, Integer> lagByPeriods = ArrayListMultimap.create();
        String critPool = "CRIT";
        String warnPool = "WARN";
        String okPool = "OK";
        critLag.forEach(l -> lagByPeriods.put(critPool, l));
        warnLag.forEach(l -> lagByPeriods.put(warnPool, l));
        okLag.forEach(l -> lagByPeriods.put(okPool, l));

        List<Integer> totalLag = new ArrayList<>();
        totalLag.addAll(critLag);
        totalLag.addAll(warnLag);
        totalLag.addAll(okLag);

        MonitoringUnit monitoringUnit = complicatedMonitoring.getOrAddUnit("lag");
        monitoringUnit.disableMonitoringDelay();

        metricLagService.updateLagModeAndMonitoring(totalLag, lagByPeriods);
        verify(clickphiteService).setLagMode(critPool, true);
        verify(clickphiteService).setLagMode(warnPool, true);
        verify(clickphiteService).setLagMode(okPool, false);
        assertEquals(MonitoringStatus.OK, monitoringUnit.getStatus());
        verify(clickphiteService).setLagMode(false);
        verifyNoMoreInteractions(clickphiteService);
    }

    private static MetricLagService getMetricLagService(
        ClickphiteService clickphiteService,
        ComplicatedMonitoring complicatedMonitoring
    ) {
        MetricLagService metricLagService = new MetricLagService();
        metricLagService.setClickphiteService(clickphiteService);
        metricLagService.setMonitoring(complicatedMonitoring);
        metricLagService.setLagMonitoringUnitDelayMinutes(1);
        metricLagService.setLagMonitoringQuantile(80);
        metricLagService.setLagMonitoringWarnThresholdSeconds(10);
        metricLagService.setLagMonitoringCritThresholdSeconds(20);
        metricLagService.setMetricDelaySeconds(30);
        metricLagService.afterPropertiesSet();
        metricLagService.setKeyValueMetricSupplier(new KeyValueMetricSupplier());
        metricLagService.setLagMetricSupplier(new SimpleMetricSupplier());
        return metricLagService;
    }

    @Test
    public void processQueue() throws Exception {
        RangeSet<Integer> rangeSet = TreeRangeSet.create();
        int metricPeriodSeconds = 5;
        rangeSet.add(Range.closed(0, 5));
        rangeSet.add(Range.closed(5, 10));
        rangeSet.add(Range.closed(10, 15));
        rangeSet.add(Range.closed(25, 30));
        rangeSet.add(Range.closed(35, 40));
        MetricQueue metricQueue = new MetricQueue(rangeSet, new TreeMap<>(), 10, new TreeMap<>());

        checkMetrics(getQueueMetrics(metricQueue::get, metricPeriodSeconds, 4), 0, 0, 0);
        checkMetrics(getQueueMetrics(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 4), 0, 0, 0);

        checkMetrics(getQueueMetrics(metricQueue::get, metricPeriodSeconds, 27), 22, 3, 15);
        checkMetrics(getQueueMetrics(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 27), 12, 1, 5);

        metricQueue.remove(System.currentTimeMillis(), Range.open(10, 15));
        checkMetrics(getQueueMetrics(metricQueue::get, metricPeriodSeconds, 37), 32, 3, 15);
        checkMetrics(getQueueMetrics(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 37), 7, 1, 5);

        metricQueue.add(System.currentTimeMillis(), TreeRangeSet.create(Collections.singletonList(Range.closed(15,
            20))), MetricPeriod.ONE_MIN, false, false);
        checkMetrics(getQueueMetrics(metricQueue::get, metricPeriodSeconds, 37), 32, 4, 20);
        checkMetrics(getQueueMetrics(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 37), 17, 2, 10);
    }

    @Test
    public void processQueueForReceiveLag() throws Exception {
        RangeSet<Integer> rangeSet = TreeRangeSet.create();
        int metricPeriodSeconds = 5;
        rangeSet.add(Range.closed(0, 5));
        rangeSet.add(Range.closed(5, 10));
        rangeSet.add(Range.closed(10, 15));
        rangeSet.add(Range.closed(25, 30));
        rangeSet.add(Range.closed(35, 40));
        MetricQueue metricQueue = new MetricQueue(rangeSet, new TreeMap<>(), 10, new TreeMap<>());

        checkMetrics(
            getQueueMetricsForReceiveLag(metricQueue::get, metricPeriodSeconds, 27, 0),
            0, 3, 15
        );
        checkMetrics(
            getQueueMetricsForReceiveLag(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 27, 5),
            5, 1, 5
        );
    }

    @Test
    public void simpleRangeReceiveLagDataTimeTest() {
        int currentTime = 3600;
        SortedMap<Integer, Integer> startRangeByReceiveTimes = new TreeMap<>();
        int realtimeLagInterval = MetricQueueReceiveTimeUpdater.getRealtimeLagSecondsInterval(MetricPeriod.ONE_MIN);
        int nonRealtimeStartTime = currentTime - realtimeLagInterval - 30;
        startRangeByReceiveTimes.put(nonRealtimeStartTime, nonRealtimeStartTime + 1);
        int realtimeStartTime = currentTime - 30;
        startRangeByReceiveTimes.put(realtimeStartTime, realtimeStartTime + 1);

        MetricQueue metricQueue = new MetricQueue(TreeRangeSet.create(), new TreeMap<>(), 0, startRangeByReceiveTimes);
        assertEquals(
            currentTime,
            metricQueue.getRealtimeReceiveLagDataTime(MetricPeriod.ONE_MIN, currentTime)
        );
        assertEquals(
            currentTime - realtimeLagInterval,
            metricQueue.getFullReceiveLagDataTime(MetricPeriod.ONE_MIN));
    }

    @Test
    public void boundaryConditionReceiveLagDataTimeTest() {
        SortedMap<Integer, Integer> startRangeByReceiveTimes = new TreeMap<>();
        startRangeByReceiveTimes.put(60, 61);

        MetricQueue metricQueue = new MetricQueue(TreeRangeSet.create(), new TreeMap<>(), 0, startRangeByReceiveTimes);
        assertEquals(120, metricQueue.getFullReceiveLagDataTime(MetricPeriod.ONE_MIN));
    }

    @Test
    public void oldRangeReceiveLagDataTimeTest() {
        SortedMap<Integer, Integer> startRangeByReceiveTimes = new TreeMap<>();
        startRangeByReceiveTimes.put(70, 130);

        MetricQueue metricQueue = new MetricQueue(TreeRangeSet.create(), new TreeMap<>(), 0, startRangeByReceiveTimes);
        assertEquals(130, metricQueue.getFullReceiveLagDataTime(MetricPeriod.ONE_MIN));
    }

    private MetricLagService.QueueMetrics getQueueMetrics(
        Function<Integer, List<Range<Integer>>> queryExtractor,
        int metricPeriodSeconds,
        int timestamp
    ) {
        return MetricLagService.getQueueMetricsByBuildTimes(
            queryExtractor.apply(timestamp),
            metricPeriodSeconds,
            timestamp,
            false,
            0
        );
    }

    private MetricLagService.QueueMetrics getQueueMetricsForReceiveLag(
        Function<Integer, List<Range<Integer>>> queryExtractor,
        int metricPeriodSeconds,
        int timestamp,
        int receiveLag
    ) {
        return MetricLagService.getQueueMetricsByBuildTimes(
            queryExtractor.apply(timestamp),
            metricPeriodSeconds,
            timestamp,
            true,
            receiveLag
        );
    }

    private void checkMetrics(MetricLagService.QueueMetrics metrics,
                              int expectedLagSeconds, int expectedQueuePeriods, int expectedQueueSeconds) {

        assertEquals(expectedLagSeconds, metrics.getLagSeconds());
        assertEquals(expectedQueuePeriods, metrics.getQueuePeriods());
        assertEquals(expectedQueueSeconds, metrics.getQueueSeconds());
    }
}
