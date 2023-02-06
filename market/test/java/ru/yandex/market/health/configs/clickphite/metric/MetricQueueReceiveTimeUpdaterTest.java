package ru.yandex.market.health.configs.clickphite.metric;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.clickphite.MetricPeriod;

public class MetricQueueReceiveTimeUpdaterTest {
    private long currentTimestamp;
    private int receiveTimeSec;
    private SortedMap<Integer, Integer> startRangeByReceiveTimes;

    @BeforeEach
    public void setUp() {
        currentTimestamp = TimeUnit.DAYS.toMillis(1);
        receiveTimeSec = (int) TimeUnit.MILLISECONDS.toSeconds(currentTimestamp);
        startRangeByReceiveTimes = new TreeMap<>();
    }

    @Test
    public void addNewRangeTest() {
        MetricQueueReceiveTimeUpdater receiveTimeUpdater = new MetricQueueReceiveTimeUpdater(
            startRangeByReceiveTimes,
            MetricPeriod.ONE_MIN,
            currentTimestamp
        );
        Range<Integer> range = Range.closedOpen(receiveTimeSec - 60, receiveTimeSec - 1);

        receiveTimeUpdater.updateStartRanges(range);

        Assertions.assertEquals(1, startRangeByReceiveTimes.size());
        Map.Entry<Integer, Integer> startRangeByReceiveTime = startRangeByReceiveTimes.entrySet().iterator().next();
        Assertions.assertEquals(range.lowerEndpoint(), startRangeByReceiveTime.getKey());
        Assertions.assertEquals(receiveTimeSec, startRangeByReceiveTime.getValue());
    }

    @Test
    public void addRealtimeRangesTest() {
        MetricQueueReceiveTimeUpdater receiveTimeUpdater = new MetricQueueReceiveTimeUpdater(
            startRangeByReceiveTimes,
            MetricPeriod.ONE_MIN,
            currentTimestamp
        );
        Range<Integer> range = Range.closedOpen(receiveTimeSec - 120, receiveTimeSec - 1);

        receiveTimeUpdater.updateStartRanges(range);

        Assertions.assertEquals(2, startRangeByReceiveTimes.size());
        int step = 0;
        for (Map.Entry<Integer, Integer> startRangeByReceiveTime : startRangeByReceiveTimes.entrySet()) {
            Assertions.assertEquals(range.lowerEndpoint() + step, startRangeByReceiveTime.getKey());
            Assertions.assertEquals(receiveTimeSec, startRangeByReceiveTime.getValue());
            step += MetricPeriod.ONE_MIN.getDurationSeconds();
        }
    }

    @Test
    public void addRealtimeFiveMinRangesTest() {
        MetricQueueReceiveTimeUpdater receiveTimeUpdater = new MetricQueueReceiveTimeUpdater(
            startRangeByReceiveTimes,
            MetricPeriod.FIVE_MIN,
            currentTimestamp
        );
        Range<Integer> range = Range.closedOpen(
            receiveTimeSec - (int) TimeUnit.MINUTES.toSeconds(15),
            receiveTimeSec - 1
        );

        receiveTimeUpdater.updateStartRanges(range);

        Assertions.assertEquals(3, startRangeByReceiveTimes.size());
        int step = 0;
        for (Map.Entry<Integer, Integer> startRangeByReceiveTime : startRangeByReceiveTimes.entrySet()) {
            Assertions.assertEquals(range.lowerEndpoint() + step, startRangeByReceiveTime.getKey());
            Assertions.assertEquals(receiveTimeSec, startRangeByReceiveTime.getValue());
            step += MetricPeriod.FIVE_MIN.getDurationSeconds();
        }
    }

    @Test
    public void addOldRangesTest() {
        MetricQueueReceiveTimeUpdater receiveTimeUpdater = new MetricQueueReceiveTimeUpdater(
            startRangeByReceiveTimes,
            MetricPeriod.ONE_MIN,
            currentTimestamp
        );
        Range<Integer> range = Range.closedOpen(
            receiveTimeSec - (int) TimeUnit.HOURS.toSeconds(2),
            receiveTimeSec - (int) TimeUnit.HOURS.toSeconds(1)
        );

        receiveTimeUpdater.updateStartRanges(range);

        Assertions.assertEquals(1, startRangeByReceiveTimes.size());
        Map.Entry<Integer, Integer> startRangeByReceiveTime = startRangeByReceiveTimes.entrySet().iterator().next();
        Assertions.assertEquals(range.lowerEndpoint(), startRangeByReceiveTime.getKey());
        Assertions.assertEquals(receiveTimeSec, startRangeByReceiveTime.getValue());
    }


    @Test
    public void overrideRealtimeRangeTest() {
        int notOverrideTime = receiveTimeSec + 5;
        startRangeByReceiveTimes.put(receiveTimeSec - 60, notOverrideTime);
        startRangeByReceiveTimes.put(receiveTimeSec - 110, receiveTimeSec - 5);
        MetricQueueReceiveTimeUpdater receiveTimeUpdater = new MetricQueueReceiveTimeUpdater(
            startRangeByReceiveTimes,
            MetricPeriod.ONE_MIN,
            currentTimestamp
        );
        Range<Integer> range = Range.closedOpen(receiveTimeSec - 120, receiveTimeSec - 1);

        receiveTimeUpdater.updateStartRanges(range);

        Assertions.assertEquals(2, startRangeByReceiveTimes.size());
        Iterator<Map.Entry<Integer, Integer>> startRangeByReceiveIterator =
            startRangeByReceiveTimes.entrySet().iterator();
        Integer overridedTime = startRangeByReceiveIterator.next().getValue();
        Assertions.assertEquals(receiveTimeSec, overridedTime);
        Integer notOverridedTime = startRangeByReceiveIterator.next().getValue();
        Assertions.assertEquals(notOverrideTime, notOverridedTime);
    }

    @Test
    public void collapseOldRangeTest() {
        int realtimeLag = MetricQueueReceiveTimeUpdater.getRealtimeLagSecondsInterval(MetricPeriod.ONE_MIN);
        startRangeByReceiveTimes.put(receiveTimeSec - realtimeLag - 60, receiveTimeSec - 5);
        startRangeByReceiveTimes.put(receiveTimeSec - realtimeLag - 120, receiveTimeSec - 5);
        startRangeByReceiveTimes.put(receiveTimeSec - realtimeLag - 600, receiveTimeSec - 5);
        MetricQueueReceiveTimeUpdater receiveTimeUpdater = new MetricQueueReceiveTimeUpdater(
            startRangeByReceiveTimes,
            MetricPeriod.ONE_MIN,
            currentTimestamp
        );
        Range<Integer> range = Range.closedOpen(receiveTimeSec - realtimeLag - 600, receiveTimeSec - realtimeLag - 60);

        receiveTimeUpdater.updateStartRanges(range);

        Assertions.assertEquals(1, startRangeByReceiveTimes.size());
        Map.Entry<Integer, Integer> startRangeByReceiveTime = startRangeByReceiveTimes.entrySet().iterator().next();
        Assertions.assertEquals(range.lowerEndpoint(), startRangeByReceiveTime.getKey());
        Assertions.assertEquals(receiveTimeSec, startRangeByReceiveTime.getValue());
    }

    @Test
    public void compactTest() {
        int realtimeLag = MetricQueueReceiveTimeUpdater.getRealtimeLagSecondsInterval(MetricPeriod.ONE_MIN);

        Range<Integer> range = Range.closedOpen(receiveTimeSec - realtimeLag - 1800, receiveTimeSec);
        RangeSet<Integer> mainRangeSet = TreeRangeSet.create();
        mainRangeSet.add(range);

        TreeMap<Integer, Integer> expectedTimes = new TreeMap<>();
        // Значения вне mainRangeSet
        startRangeByReceiveTimes.put(receiveTimeSec - realtimeLag - 3600, receiveTimeSec - 10);
        // Одиночное значение
        startRangeByReceiveTimes.put(receiveTimeSec - realtimeLag - 1800, receiveTimeSec - 10);
        expectedTimes.put(receiveTimeSec - realtimeLag - 1800, receiveTimeSec - 10);
        // Схлопываемые значения
        startRangeByReceiveTimes.put(receiveTimeSec - realtimeLag - 180, receiveTimeSec - 10);
        startRangeByReceiveTimes.put(receiveTimeSec - realtimeLag - 120, receiveTimeSec - 15);
        startRangeByReceiveTimes.put(receiveTimeSec - realtimeLag - 60, receiveTimeSec - 5);
        expectedTimes.put(receiveTimeSec - realtimeLag - 180, receiveTimeSec - 15);
        // Значения вне операции compact (realtime данные)
        startRangeByReceiveTimes.put(receiveTimeSec - 120, receiveTimeSec - 25);
        expectedTimes.put(receiveTimeSec - 120, receiveTimeSec - 25);
        startRangeByReceiveTimes.put(receiveTimeSec - 60, receiveTimeSec - 1);
        expectedTimes.put(receiveTimeSec - 60, receiveTimeSec - 1);

        MetricQueueReceiveTimeUpdater receiveTimeUpdater = new MetricQueueReceiveTimeUpdater(
            startRangeByReceiveTimes,
            MetricPeriod.ONE_MIN,
            currentTimestamp - 30
        );
        receiveTimeUpdater.compact(mainRangeSet);

        Assertions.assertEquals(expectedTimes.size(), startRangeByReceiveTimes.size());
        expectedTimes.forEach((key, value) -> Assertions.assertEquals(value, startRangeByReceiveTimes.get(key)));
    }

    @Test
    public void compactFutureTest() {
        int currentTime = (int) TimeUnit.MILLISECONDS.toSeconds(currentTimestamp);
        startRangeByReceiveTimes.put(0, 3000);
        startRangeByReceiveTimes.put(currentTime + 10, 3600);
        startRangeByReceiveTimes.put(currentTime + 3600, 3900);
        startRangeByReceiveTimes.put(currentTime + 3700, 4000);

        MetricQueueReceiveTimeUpdater receiveTimeUpdater = new MetricQueueReceiveTimeUpdater(
            startRangeByReceiveTimes,
            MetricPeriod.ONE_MIN,
            currentTimestamp
        );

        Range<Integer> range = Range.closedOpen(0, 5000);
        RangeSet<Integer> mainRangeSet = TreeRangeSet.create();
        mainRangeSet.add(range);

        receiveTimeUpdater.compact(mainRangeSet);

        Assertions.assertEquals(3, startRangeByReceiveTimes.size());
        Assertions.assertEquals(3000, startRangeByReceiveTimes.get(0));
        Assertions.assertEquals(3600, startRangeByReceiveTimes.get(currentTime + 10));
        Assertions.assertEquals(4000, startRangeByReceiveTimes.get(currentTime + 3600));
    }
}
