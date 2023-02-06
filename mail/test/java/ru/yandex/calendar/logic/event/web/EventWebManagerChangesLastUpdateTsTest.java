package ru.yandex.calendar.logic.event.web;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationUpdateData;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class EventWebManagerChangesLastUpdateTsTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private EventWebManager eventWebManager;

    @Test
    public void changeSingleEvent() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-14600").getUserInfo();
        PassportUid uid = user.getUid();
        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);
        long eventId = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "changeSingleEvent").getId();

        Instant layerLastUpdateTs = layerDao.findLayerById(layerId).getCollLastUpdateTs();
        Instant mainEventLastUpdateTs = mainEventDao.findMainEventByEventId(eventId).getLastUpdateTs();

        Instant now = Instant.now().plus(Duration.standardSeconds(1));

        EventData eventData = new EventData();
        eventData.getEvent().setId(eventId);
        eventData.getEvent().setName("updateSingleEventFromWeb#1");
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(user, eventData, false, ActionInfo.webTest(now));

        Assert.notEquals(layerLastUpdateTs, layerDao.findLayerById(layerId).getCollLastUpdateTs());
        Assert.notEquals(mainEventLastUpdateTs, mainEventDao.findMainEventByEventId(eventId).getLastUpdateTs());
    }

    private static Duration THREE_DAYS = Duration.standardDays(3);

    @Test
    public void changeRecurringEventInstanceParticipants() {
        UserInfo organizer = testManager.prepareUser("yandex-team-mm-14620").getUserInfo();
        PassportUid organizerUid = organizer.getUid();
        PassportUid attendeeUid = testManager.prepareUser("yandex-team-mm-14621").getUid();

        long layer1Id = layerRoutines.getOrCreateDefaultLayer(organizerUid);
        long layer2Id = layerRoutines.getOrCreateDefaultLayer(attendeeUid);
        long eventId = testManager.createDefaultEvent(organizerUid, "changeRecurringEventInstanceParticipants").getId();
        testManager.addUserParticipantToEvent(eventId, organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(eventId, attendeeUid, Decision.YES, false);

        Instant layer1LastUpdateTs = layerDao.findLayerById(layer1Id).getCollLastUpdateTs();
        Instant layer2LastUpdateTs = layerDao.findLayerById(layer2Id).getCollLastUpdateTs();
        Instant mainEventLastUpdateTs = mainEventDao.findMainEventByEventId(eventId).getLastUpdateTs();

        Instant now = Instant.now().plus(Duration.standardSeconds(1));

        EventData eventData = new EventData();
        eventData.getEvent().setId(eventId);
        eventData.setInvData(new EventInvitationUpdateData(Cf.list(new Email("x@y.z")), Cf.<Email>list()));
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(organizer, eventData, true, ActionInfo.webTest(now));

        Assert.notEquals(layer1LastUpdateTs, layerDao.findLayerById(layer1Id).getCollLastUpdateTs());
        Assert.notEquals(layer2LastUpdateTs, layerDao.findLayerById(layer2Id).getCollLastUpdateTs());
        Assert.notEquals(mainEventLastUpdateTs, mainEventDao.findMainEventByEventId(eventId).getLastUpdateTs());
    }

    @Test
    public void deleteRecurringEventTail() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-14630").getUserInfo();
        PassportUid uid = user.getUid();
        Instant eventStartTs = new DateTime(2011, 11, 11, 11, 11, 11, 0, DateTimeZone.UTC).toInstant();
        Instant eventEndTs = new DateTime(2011, 11, 11, 12, 12, 12, 0, DateTimeZone.UTC).toInstant();

        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);
        long eventId = testManager.createDefaultEventWithEventLayerAndEventUser(
                uid, "deleteRecurringEventTail", eventStartTs, eventEndTs).getId();
        testManager.createDailyRepetitionAndLinkToEvent(eventId);

        Instant layerLastUpdateTs = layerDao.findLayerById(layerId).getCollLastUpdateTs();
        Instant mainEventLastUpdateTs = mainEventDao.findMainEventByEventId(eventId).getLastUpdateTs();

        Instant now = Instant.now().plus(Duration.standardSeconds(1));

        Instant tailStartTs = eventStartTs.plus(THREE_DAYS);
        eventWebManager.deleteEvent(user, eventId, Option.of(tailStartTs), true, ActionInfo.webTest(now));

        Assert.notEquals(layerLastUpdateTs, layerDao.findLayerById(layerId).getCollLastUpdateTs());
        Assert.assertEmpty(mainEventDao.findMainEventsByEventIds(Cf.list(eventId)));
    }
}
