package ru.yandex.chemodan.app.docviewer.graphite;

import java.util.Collections;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class GraphiteEventStatisticManagerTest {

    @Test
    public void pushEventStatistic() {
        String event = "aaa";
        int second = 20;
        Instant now = new DateTime(2013, 10, 2, 14, second).toInstant();

        GraphiteEventStatisticManager.getEventMinuteMeterMap().reset();

        GraphiteEventStatisticManager.pushEventStatistic(event, Duration.standardSeconds(23), "ok", now);
        GraphiteEventStatisticManager.pushEventStatistic(event, Duration.standardSeconds(27), "ok", now);
        GraphiteEventStatisticManager.pushEventStatistic(event, Duration.standardSeconds(4), "fail", now);
        GraphiteEventStatisticManager.pushEventStatistic(event, Duration.standardSeconds(55), "ok", now);
        GraphiteEventStatisticManager.pushEventStatistic(event, Duration.standardSeconds(37), "fail_other", now);
        EventMinuteMeterMap map = GraphiteEventStatisticManager.getEventMinuteMeterMap();

        Assert.assertListsEqual(Cf.list(second), Collections.list(map.getMinuteKeys()));

        EventMeterMap m = map.getEventMeterMap(second);
        Assert.assertEquals(3, m.getCounter(event + ".ok"));
        Assert.assertEquals(1, m.getCounter(event + ".fail"));
        Assert.assertEquals(1, m.getCounter(event + ".fail_other"));
        Assert.assertEquals(2, m.getCounter(event + ".20_30"));
        Assert.assertEquals(1, m.getCounter(event + ".30_40"));
        Assert.assertEquals(1, m.getCounter(event + ".0_10"));
        Assert.assertEquals(1, m.getCounter(event + ".40_inf"));
    }
}
