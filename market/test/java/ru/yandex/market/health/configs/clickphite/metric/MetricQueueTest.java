package ru.yandex.market.health.configs.clickphite.metric;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.clickphite.MetricPeriod;

public class MetricQueueTest {

    @Test
    public void collapseMainRangeSetTest() {
        int currentTime = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        MetricQueue metricQueue = new MetricQueue();
        metricQueue.add(0L, TreeRangeSet.create(Arrays.asList(
            //old time to collapse
            Range.closed(1, 2),
            Range.closed(3, 10),
            //old time to ignore
            Range.closed(1810, 3600),
            //realtime to ignore
            Range.closed(currentTime - 10, currentTime),
            Range.closed(currentTime + 10, currentTime + 20),
            //future times to collapse
            Range.closed(currentTime + 3600, currentTime + 3610),
            Range.closed(currentTime + 3620, currentTime + 3630),
            //future time to ignore
            Range.closed(currentTime + 5440, currentTime + 5450)
        )), MetricPeriod.ONE_MIN, false, false);

        metricQueue.fullCompact(MetricPeriod.ONE_MIN, 0, 1800, false);

        List<Range<Integer>> compactedRanges = metricQueue.get(0);
        Collections.reverse(compactedRanges);
        Assertions.assertEquals(
            Arrays.asList(
                Range.closed(1, 10),
                Range.closed(1810, 3600),
                Range.closed(currentTime - 10, currentTime),
                Range.closed(currentTime + 10, currentTime + 20),
                Range.closed(currentTime + 3600, currentTime + 3630),
                Range.closed(currentTime + 5440, currentTime + 5450)
            ),
            compactedRanges
        );
    }

    @Test
    public void collapseDiffRangeSetTest() {
        MetricQueue metricQueue = new MetricQueue();
        metricQueue.add(0L, TreeRangeSet.create(List.of(
            Range.closed(1, 100)
        )), MetricPeriod.ONE_MIN, false, true);

        metricQueue.add(1L, TreeRangeSet.create(List.of(
            Range.closed(70, 90)
        )), MetricPeriod.ONE_MIN, false, true);

        metricQueue.add(2L, TreeRangeSet.create(List.of(
            Range.closed(50, 80)
        )), MetricPeriod.ONE_MIN, false, true);

        Assertions.assertEquals(
            TreeRangeSet.create(List.of(
                Range.closedOpen(1, 50),
                Range.openClosed(90, 100)
            )),
            metricQueue.diff.get(0L)
        );
        Assertions.assertEquals(
            TreeRangeSet.create(List.of(Range.openClosed(80, 90))),
            metricQueue.diff.get(1L)
        );
        Assertions.assertEquals(
            TreeRangeSet.create(List.of(Range.closed(50, 80))),
            metricQueue.diff.get(2L)
        );
    }
}
