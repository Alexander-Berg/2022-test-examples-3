package ru.yandex.calendar.frontend.kiosk;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

/**
 * @author gutman
 */
@ContextConfiguration(classes = KioskContextConfiguration.class)
public class KioskManagerTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private KioskManager kioskManager;
    @Autowired
    private EventResourceDao eventResourceDao;

    @Test
    public void getRoomStates() {
        Instant now = new DateTime(2011, 3, 28, 10, 0, 0, 0, DateTimeZone.UTC).toInstant();

        Resource threeLittlePigs = testManager.cleanAndCreateResourceWithNoSyncWithExchange("conf_rr_3_1", "Три поросенка");
        Resource smolny = testManager.cleanAndCreateResourceWithNoSyncWithExchange("conf_spb_bkz", "Смольный");

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-13001");

        Event beforeNow = createEventWithResource(threeLittlePigs, user, now.minus(60*60*1000), now.minus(30*60*1000), "beforeNow");
        Event afterNow = createEventWithResource(threeLittlePigs, user, now.plus(30*60*1000), now.plus(60*60*1000), "afterNow");

        ListF<RoomState> roomStates = kioskManager.getRoomStates(Cf.toList(threeLittlePigs.getExchangeName()), now);
        Assert.A.isFalse(roomStates.single().isBusy());
        Assert.A.equals(roomStates.single().getUntilStateChange(), new Duration(now, afterNow.getStartTs()));

        Event rightNow = createEventWithResource(threeLittlePigs, user, now.minus(1*60*1000), now.plus(1*60*1000), "rightNow");
        roomStates = kioskManager.getRoomStates(Cf.toList(threeLittlePigs.getExchangeName()), now);
        Assert.A.isTrue(roomStates.single().isBusy());
        Assert.A.equals(roomStates.single().getUntilStateChange(), new Duration(now, rightNow.getEndTs()));

        Event rightAfterRightNow = createEventWithResource(threeLittlePigs, user, rightNow.getEndTs().plus(4*60*1000), rightNow.getEndTs().plus(10*60*1000), "rightAfterRightNow");
        roomStates = kioskManager.getRoomStates(Cf.toList(threeLittlePigs.getExchangeName()), now);
        Assert.A.isTrue(roomStates.single().isBusy());
        // merged with rightNow interval
        Assert.A.equals(roomStates.single().getUntilStateChange(), new Duration(now, rightAfterRightNow.getEndTs()));

        ListF<RoomState> rooomStatesForSmolniy = kioskManager.getRoomStates(Cf.toList(smolny.getExchangeName()), now);
        Assert.A.isFalse(rooomStatesForSmolniy.single().isBusy());
    }

    @Test
    public void createMeetingInRoom() {
        TestUserInfo akirakozov = testManager.prepareYandexUser(TestManager.createAkirakozov());

        Instant now = new DateTime(2011, 3, 28, 10, 0, 0, 0, DateTimeZone.UTC).toInstant();

        Resource threeLittlePigs = testManager.cleanAndCreateResourceWithNoSyncWithExchange("conf_rr_3_1", "Три поросенка");
        long eventId = kioskManager.createMeetingInRoom(
                threeLittlePigs.getExchangeName().get(), now, Duration.standardMinutes(10));

        Assert.A.some(eventResourceDao.findEventResourceByEventIdAndResourceId(eventId, threeLittlePigs.getId()));
    }

    private Event createEventWithResource(Resource threeLittlePigs, TestUserInfo user, Instant startTs, Instant endTs, String name) {
        Event event = testManager.createDefaultEvent(user.getUid(), name);
        event.setStartTs(startTs);
        event.setEndTs(endTs);
        eventDao.updateEvent(event);
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), threeLittlePigs);

        testManager.updateEventTimeIndents(event);
        return event;
    }

}
