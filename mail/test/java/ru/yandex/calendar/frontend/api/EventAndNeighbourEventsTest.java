package ru.yandex.calendar.frontend.api;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author gutman
 */
@ContextConfiguration(classes = ApiContextConfiguration.class)
public class EventAndNeighbourEventsTest extends AbstractConfTest {

    @Autowired
    TestManager testManager;

    @Autowired
    ApiManager apiManager;


    @Test
    public void twoNeighbours() {
        Instant start = TestDateTimes.moscow(2012, 3, 7, 12, 0);

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10001");

        Event event1 = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventAndNeighbourEvents" + 1, start, start.plus(Duration.standardHours(1)));
        Event event2 = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventAndNeighbourEvents" + 2, start.plus(Duration.standardHours(1)), start.plus(Duration.standardHours(2)));
        Event event3 = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventAndNeighbourEvents" + 3, start.plus(Duration.standardHours(2)), start.plus(Duration.standardHours(3)));

        testManager.updateEventTimeIndents(event1, event2, event3);

        EventAndNeighbourEvents x = apiManager
                .getEventAndNeighbourEventsByEvent(event2, user.getUid(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        Assert.equals(event1.getName(), x.getPreviousEvent().get().getName());
        Assert.equals(event3.getName(), x.getNextEvent().get().getName());
    }

    @Test
    public void oneNeighbour() {
        Instant start = TestDateTimes.moscow(2012, 3, 7, 12, 0);

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10002");

        Event event1 = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventAndNeighbourEvents" + 1, start, start.plus(Duration.standardHours(1)));
        Event event2 = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventAndNeighbourEvents" + 2, start.plus(Duration.standardHours(1)), start.plus(Duration.standardHours(2)));

        testManager.updateEventTimeIndents(event1, event2);

        EventAndNeighbourEvents x = apiManager
                .getEventAndNeighbourEventsByEvent(event2, user.getUid(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        Assert.equals(event1.getName(), x.getPreviousEvent().get().getName());
        Assert.none(x.getNextEvent());
    }

    @Test
    public void noNeighbours() {
        Instant start = TestDateTimes.moscow(2012, 3, 7, 12, 0);

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10003");

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventAndNeighbourEvents" + 2, start.plus(Duration.standardHours(1)), start.plus(Duration.standardHours(2)));

        testManager.updateEventTimeIndents(event);

        EventAndNeighbourEvents x = apiManager
                .getEventAndNeighbourEventsByEvent(event, user.getUid(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        Assert.none(x.getPreviousEvent());
        Assert.none(x.getNextEvent());
    }

    @Test
    public void neighbourAtTheSameTime() {
        Instant start = TestDateTimes.moscow(2012, 3, 7, 12, 0);
        Instant end = start.plus(Duration.standardHours(1));

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10004");

        Event event1 = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventAndNeighbourEvents" + 1, start, end);
        Event event2 = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventAndNeighbourEvents" + 2, start, end);

        testManager.updateEventTimeIndents(event1, event2);

        EventAndNeighbourEvents x = apiManager
                .getEventAndNeighbourEventsByEvent(event1, user.getUid(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        Assert.notEquals(x.getPreviousEvent().isPresent(), x.getNextEvent().isPresent());
        Assert.equals(event2.getName(), x.getPreviousEvent().orElse(x.getNextEvent()).get().getName());
    }

}
