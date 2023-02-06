package ru.yandex.chemodan.app.docviewer.graphite;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.commune.graphite.GraphiteClient;
import ru.yandex.misc.net.HostnameUtils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author akirakozov
 */
public class GraphiteEventStatisticPusherTest {
    private static final String NAME_PREFIX =
            "media.docviewer.stage.DEVELOPMENT.unknown_dc."
            + HostnameUtils.localHostname().replace(".", "_");

    @Mock
    private GraphiteClient graphiteClient;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void pushEventStatisticByMinute() {
        Instant now = new DateTime(2015, 10, 2, 14, 20).toInstant();
        EventMinuteMeterMap meterMap = createMeterMap(now);

        GraphiteEventStatisticPusher pusher = new GraphiteEventStatisticPusher(
                graphiteClient, meterMap, NAME_PREFIX);

        pusher.pushEventStatisticByMinute(now.plus(Duration.standardMinutes(2)));

        verify(graphiteClient).spool(NAME_PREFIX + ".aaa", 3L, now);
        verify(graphiteClient).spool(NAME_PREFIX + ".bbb", 1L, now);
    }

    @Test
    public void pushEventStatisticInHourBounds() {
        Instant now = new DateTime(2015, 10, 2, 14, 59).toInstant();
        EventMinuteMeterMap meterMap = new EventMinuteMeterMap();
        meterMap.incEvent("aaa", now.plus(Duration.standardSeconds(4)));

        GraphiteEventStatisticPusher pusher = new GraphiteEventStatisticPusher(
                graphiteClient, meterMap, NAME_PREFIX);

        pusher.pushEventStatisticByMinute(now.plus(Duration.standardMinutes(2)));

        verify(graphiteClient).spool(NAME_PREFIX + ".aaa", 1L, now);
    }

    @Test
    public void checkCurrentMinuteNotPushed() {
        Instant now = new DateTime(2015, 10, 2, 14, 20).toInstant();
        EventMinuteMeterMap meterMap = createMeterMap(now);

        GraphiteEventStatisticPusher pusher = new GraphiteEventStatisticPusher(
                graphiteClient, meterMap, NAME_PREFIX);
        pusher.pushEventStatisticByMinute(now);

        verify(graphiteClient, never()).spool(any(), any(), any());
    }

    private EventMinuteMeterMap createMeterMap(Instant now) {
        EventMinuteMeterMap map = new EventMinuteMeterMap();

        String event1 = "aaa";
        String event2 = "bbb";

        map.incEvent(event1, now);
        map.incEvent(event1, now.plus(Duration.standardSeconds(4)));
        map.incEvent(event1, now.plus(Duration.standardSeconds(23)));

        map.incEvent(event2, now.plus(Duration.standardSeconds(10)));
        return map;
    }
}
