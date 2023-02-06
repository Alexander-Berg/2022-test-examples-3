package ru.yandex.chemodan.app.docviewer.graphite;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class EventMinuteMeterMapTest {

    @Test
    public void incEvent() {
        EventMinuteMeterMap map = new EventMinuteMeterMap();
        Instant now = new DateTime(2015, 10, 2, 14, 20).toInstant();

        String event1 = "aaa";
        String event2 = "bbb";

        map.incEvent(event1, now);
        map.incEvent(event1, now.plus(Duration.standardSeconds(4)));
        map.incEvent(event1, now.plus(Duration.standardSeconds(23)));
        map.incEvent(event1, now.plus(Duration.standardSeconds(81)));

        map.incEvent(event2, now);
        map.incEvent(event2, now.plus(Duration.standardSeconds(4)));
        map.incEvent(event2, now.plus(Duration.standardSeconds(183)));

        List<Integer> minutes = Collections.list(map.getMinuteKeys());
        Assert.assertListsEqual(minutes, Cf.list(20, 21, 23));

        Assert.equals(map.getEventMeterMap(20).getCounter(event1), 3L);
        Assert.equals(map.getEventMeterMap(20).getCounter(event2), 2L);
        Assert.equals(map.getEventMeterMap(21).getCounter(event1), 1L);
        Assert.equals(map.getEventMeterMap(23).getCounter(event2), 1L);
    }
}
