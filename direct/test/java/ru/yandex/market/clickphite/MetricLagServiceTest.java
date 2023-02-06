package ru.yandex.market.clickphite;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.clickphite.metric.MetricQueue;

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;


/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 14.04.17
 */
public class MetricLagServiceTest {
    @Test
    public void processQueue() throws Exception {
        RangeSet<Integer> rangeSet = TreeRangeSet.create();
        int metricPeriodSeconds = 5;
        rangeSet.add(Range.closed(0, 5));
        rangeSet.add(Range.closed(5, 10));
        rangeSet.add(Range.closed(10, 15));
        rangeSet.add(Range.closed(25, 30));
        rangeSet.add(Range.closed(35, 40));
        MetricQueue metricQueue = new MetricQueue(rangeSet, new TreeMap<>(), 10);

        checkMetrics(getQueueMetrics(metricQueue::get, metricPeriodSeconds, 4), 0, 0, 0);
        checkMetrics(getQueueMetrics(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 4), 0, 0, 0);

        checkMetrics(getQueueMetrics(metricQueue::get, metricPeriodSeconds, 27), 22, 3, 15);
        checkMetrics(getQueueMetrics(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 27), 12, 1, 5);

        metricQueue.remove(System.currentTimeMillis(), Range.open(10, 15));
        checkMetrics(getQueueMetrics(metricQueue::get, metricPeriodSeconds, 37), 32, 3, 15);
        checkMetrics(getQueueMetrics(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 37), 7, 1, 5);

        metricQueue.add(System.currentTimeMillis(), TreeRangeSet.create(Collections.singletonList(Range.closed(15, 20))));
        checkMetrics(getQueueMetrics(metricQueue::get, metricPeriodSeconds, 37), 32, 4, 20);
        checkMetrics(getQueueMetrics(metricQueue::getAfterMaxProcessed, metricPeriodSeconds, 37), 17, 2, 10);
    }

    private MetricLagService.QueueMetrics getQueueMetrics(Function<Integer, List<Range<Integer>>> queryExtractor,
                                                          int metricPeriodSeconds, int timestamp) {
        return MetricLagService.getQueueMetrics(queryExtractor.apply(timestamp), metricPeriodSeconds, timestamp);
    }

    private void checkMetrics(MetricLagService.QueueMetrics metrics,
                              int expectedLagSeconds, int expectedQueuePeriods, int expectedQueueSeconds) {

        Assert.assertEquals(expectedLagSeconds, metrics.getLagSeconds());
        Assert.assertEquals(expectedQueuePeriods, metrics.getQueuePeriods());
        Assert.assertEquals(expectedQueueSeconds, metrics.getQueueSeconds());
    }
}