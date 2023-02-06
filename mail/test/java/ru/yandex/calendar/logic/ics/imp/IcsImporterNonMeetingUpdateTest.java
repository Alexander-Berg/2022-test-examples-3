package ru.yandex.calendar.logic.ics.imp;

import net.fortuna.ical4j.model.Property;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.repetition.EventAndRepetition;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVAlarm;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsValue;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAction;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsExDate;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsRRule;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsTrigger;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.layer.LayerType;
import ru.yandex.calendar.logic.notification.Channel;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author Stepan Koltsov
 */
public class IcsImporterNonMeetingUpdateTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private UserManager userManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;

    @Test
    public void updateRecurrencePreservesEventRecord() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11201").getUid();

        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(uid);
        Event mainEvent = events._1;
        Event recuEvent = events._2;

        long mainEventId = mainEvent.getMainEventId();

        IcsVEvent veventMain = new IcsVEvent();
        IcsVEvent veventRecu = new IcsVEvent();

        String externalId = mainEventDao.findMainEventById(mainEventId).getExternalId();

        veventMain = veventMain.withUid(externalId);
        veventRecu = veventRecu.withUid(externalId);

        veventMain = veventMain.withSummary(mainEvent.getName() + "x");
        veventRecu = veventRecu.withSummary(recuEvent.getName() + "y");

        veventMain = veventMain.withSequenece(1);
        veventRecu = veventRecu.withSequenece(1);

        veventMain = veventMain.withDtStart(mainEvent.getStartTs());
        veventMain = veventMain.withDtEnd(mainEvent.getEndTs());

        veventRecu = veventRecu.withDtStart(recuEvent.getStartTs());
        veventRecu = veventRecu.withDtEnd(recuEvent.getEndTs());
        veventRecu = veventRecu.withRecurrence(recuEvent.getRecurrenceId().get());

        IcsCalendar calendar = new IcsCalendar();
        calendar = calendar.addComponent(veventMain);
        calendar = calendar.addComponent(veventRecu);

        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest();
        IcsImportStats stats = icsImporter.importIcsStuff(uid, calendar, mode);

        // sanity checks
        ListF<Event> newMainEvent = eventDao.findMasterEventByMainId(mainEventId);
        Event newRecuEvent = eventDao.findRecurrenceEventByMainId(mainEventId, recuEvent.getRecurrenceId().get()).single();
        Assert.A.equals(mainEvent.getName() + "x", newMainEvent.single().getName());
        Assert.A.equals(recuEvent.getName() + "y", newRecuEvent.getName());

        SetF<Long> oldEventIds = Cf.set(mainEvent.getId(), recuEvent.getId());

        // essential
        Assert.A.equals(oldEventIds, eventDao.findEventsByMainId(mainEventId).map(EventFields.ID.getF()).unique());
        Assert.A.equals(oldEventIds, stats.getUpdatedEventIds());
    }

    /**
     * Import ics, which contains only recurrence instance of event
     */
    @Test
    public void updatesSingleInstanceOfEvent() { // variation of test 11 (not a meeting)
        PassportLogin user1 = new PassportLogin("yandex-team-mm-11211");

        PassportUid uid1 = userManager.getUidByLoginForTest(user1);
        testManager.cleanUser(uid1);

        Event event = testManager.createDefaultMeeting(uid1, "Existing repeating event without recurrence-ids");
        testManager.addUserParticipantToEvent(event.getId(), user1, Decision.UNDECIDED, true);
        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withSummary("Incoming recurrence-id for an instance being changed");
        vevent = vevent.withDtStart(event.getStartTs().plus(Duration.standardHours(1)));
        vevent = vevent.withDtEnd(event.getEndTs().plus(Duration.standardHours(1)));
        vevent = vevent.withRecurrence(event.getStartTs());

        icsImporter.importIcsStuff(uid1, vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());

        // check that recurrence instance was added
        Assert.A.lt(event.getStartTs(), eventDbManager.getFirstEventInstanceStart(event.getId()));
    }

    // https://jira.yandex-team.ru/browse/CAL-2784
    @Test
    public void updateEventWithRDate() {
        PassportLogin login = new PassportLogin("yandex-team-mm-11221");
        PassportUid uid = userManager.getUidByLoginForTest(login);
        testManager.cleanUser(uid);

        Tuple2<Event, Rdate> eventAndRDate = testManager.createEventWithRdate(uid);
        Event event = eventAndRDate.get1();
        Rdate rdate = eventAndRDate.get2();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withSummary("testUpdateEventWithRDate()-update");
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(1);
        vevent = vevent.addRDate(rdate.getStartTs());

        icsImporter.importIcsStuff(uid, vevent.makeCalendar(),
                IcsImportMode.importFile(LayerReference.byCategory()));
    }

    @Test
    public void recurrenceIsIgnoredWhenImportedWithStaleMaster() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11006");

        Event master = testManager.createDefaultEvent(user.getUid(), "recurrenceIsIgnoredWhenImportedWithStaleMaster");

        Instant dtstamp = TestDateTimes.moscow(2011, 9, 4, 23, 55);
        master.setDtstamp(dtstamp); // XXX: dtstamp is ignored now
        master.setLastUpdateTs(dtstamp);
        eventDao.updateEvent(master);

        EventUser eventUser = new EventUser();
        eventUser.setEventId(master.getId());
        eventUser.setUid(user.getUid());
        eventUserDao.saveEventUser(eventUser, ActionInfo.webTest());

        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        IcsVEvent masterVevent = new IcsVEvent();
        masterVevent = masterVevent.withUid(eventDao.findExternalIdByEventId(master.getId()));
        masterVevent = masterVevent.withSummary("new event name");
        masterVevent = masterVevent.withDtStart(master.getStartTs());
        masterVevent = masterVevent.withDtEnd(master.getEndTs());
        masterVevent = masterVevent.withSequenece(master.getSequence());
        masterVevent = masterVevent.addProperty(new IcsRRule("FREQ=DAILY"));
        masterVevent = masterVevent.withDtStamp(dtstamp.minus(Duration.standardHours(1)));

        Instant recurrenceStartTs = master.getStartTs().plus(Duration.standardDays(1));
        IcsVEvent recurrenceVevent = masterVevent.withRecurrence(recurrenceStartTs);
        recurrenceVevent = recurrenceVevent.withDtStart(recurrenceStartTs);
        recurrenceVevent = recurrenceVevent.withDtEnd(recurrenceStartTs.plus(Duration.standardHours(1)));
        recurrenceVevent = recurrenceVevent.removeProperties(Property.RRULE);

        IcsCalendar calendar = new IcsCalendar().addComponent(masterVevent).addComponent(recurrenceVevent);
        IcsImportStats stats = icsImporter
                .importIcsStuff(user.getUid(), calendar, IcsImportMode.importFile(LayerReference.defaultLayer()));

        Assert.A.equals(0, stats.getNewEventIds().size());
        Assert.A.equals(0, stats.getUpdatedEventIds().size());

        Event masterAfterImport = eventDao.findMasterEventsWithPossibleIds(Cf.list(master.getId())).single();
        Assert.A.equals(master.getName(), masterAfterImport.getName());
        Assert.A.hasSize(0, eventDao.findRecurrenceEventByMainId(masterAfterImport.getMainEventId(), recurrenceStartTs));
    }

    @Test
    public void updateEventTimeZone() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-12852").getUid();
        settingsRoutines.updateTimezone(uid, "Asia/Yekaterinburg");

        IcsVEvent event = new IcsVEvent();
        event = event.withSummary("updateEventTimeZone");
        event = event.withUid("updateEventTimeZone");

        event = event.withDtStamp(Instant.now());
        event = event.withDtStart(DateTime.now(MoscowTime.TZ));
        event = event.withDtEnd(event.getStart().plus(Period.hours(1)));

        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest();
        long eventId = icsImporter.importIcsStuff(uid, event.makeCalendar(), mode).getNewEventIds().single();
        Assert.equals(MoscowTime.TZ, eventRoutines.getEventTimeZone(eventId));

        event = event.withDtStamp(event.getDtStamp().get().getInstant().plus(Duration.standardSeconds(1)));
        event = event.withDtStart(DateTime.now(DateTimeZone.UTC));
        event = event.withDtEnd(event.getStart().plus(Period.hours(1)));
        icsImporter.importIcsStuff(uid, event.makeCalendar(), mode);
        Assert.equals(MoscowTime.TZ, eventRoutines.getEventTimeZone(eventId));

        settingsRoutines.updateTimezone(uid, "Arctic/Longyearbyen");
        Assert.equals(MoscowTime.TZ, eventRoutines.getEventTimeZone(eventId));
    }

    @Test
    public void updateByFeedKeepsNotification() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(922);

        IcsVEvent vevent = createVeventWithAudioAlarm();
        Notification layerNotification = Notification.display(Duration.standardMinutes(-15));
        ListF<Notification> expectedNotifications = Cf.list(
                new Notification(Channel.XIVA, Duration.ZERO),
                new Notification(Channel.MOBILE, Duration.standardMinutes(-5)),
                layerNotification);
        expectedNotifications = expectedNotifications.sortedBy(Notification::getChannel);

        IcsImportMode importMode = IcsImportMode.updateFeed(createFeedLayerWithNotification(user, layerNotification));

        IcsImportStats stats = icsImporter.importIcsStuff(user.getUid(), vevent.makeCalendar(), importMode);
        Assert.equals(expectedNotifications, getEventNotifications(user, stats.getNewEventIds().single()));

        vevent = vevent.withSequenece(vevent.getSequence().get() + 1);

        stats = icsImporter.importIcsStuff(user.getUid(), vevent.makeCalendar(), importMode);
        Assert.equals(expectedNotifications, getEventNotifications(user, stats.getUpdatedEventIds().single()));
    }

    @Test
    public void defaultLayerIfCreateReference() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(923);

        IcsVEvent vevent = createVeventWithAudioAlarm();
        IcsImportMode importMode = IcsImportMode.importFile(LayerReference.defaultLayerIfCreate());

        IcsImportStats stats = icsImporter.importIcsStuff(user.getUid(), vevent.makeCalendar(), importMode);
        long eventId = stats.getNewEventIds().single();

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, user.getDefaultLayerId()));


        long layerId = layerRoutines.createLayer(user.getUid(), LayerType.USER, new Layer());

        vevent = createVeventWithAudioAlarm();
        importMode = IcsImportMode.importFile(LayerReference.id(layerId));

        stats = icsImporter.importIcsStuff(user.getUid(), vevent.makeCalendar(), importMode);
        eventId = stats.getNewEventIds().single();

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, layerId));

        vevent = vevent.withSequenece(vevent.getSequence().get() + 1);
        importMode = IcsImportMode.importFile(LayerReference.defaultLayerIfCreate());

        icsImporter.importIcsStuff(user.getUid(), vevent.makeCalendar(), importMode);
        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, layerId));
    }

    @Test
    public void reattachByWebIcs() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(923);

        IcsVEvent vevent = createVeventWithAudioAlarm();

        IcsImportMode mode = IcsImportMode.importFileForTest(
                LayerReference.id(creator.getDefaultLayerId()), vevent.getStart(IcsVTimeZones.fallback(MoscowTime.TZ)));

        IcsImportStats stats = icsImporter.importIcsStuff(creator.getUid(), vevent.makeCalendar(), mode);
        Assert.hasSize(1, stats.getNewEventIds());

        long eventId = stats.getNewEventIds().single();

        Assert.some(eventRoutines.detachEventsFromLayerByMainEventId(
                creator.getUserInfo(), mainEventDao.findMainEventByEventId(eventId),
                creator.getDefaultLayerId(), ActionInfo.webTest()).getLayers().getEvents().single().get2().getDetachedId());

        Assert.hasSize(1, icsImporter.importIcsStuff(creator.getUid(), vevent.makeCalendar(), mode).getUpdatedEventIds());

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, creator.getDefaultLayerId()));
        Assert.some(Decision.YES, eventUserDao.findEventUserByEventIdAndUid(eventId, creator.getUid()).map(EventUser::getDecision));
    }

    @Test
    public void createMasterWithExdateViaUpdate() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(923);

        DateTime start = MoscowTime.dateTime(2022, 5, 18, 21, 33);

        ListF<Instant> exdates = Cf.list(start, start.plusDays(1))
                .map(ReadableInstant::toInstant);

        IcsVEvent vMaster = new IcsVEvent()
                .withUid(CalendarUtils.generateExternalId())
                .withDtStart(start)
                .withDtEnd(start.plusHours(1))
                .addProperty(new IcsRRule("FREQ=DAILY"))
                .withExdates(exdates.map(IcsExDate::new));

        IcsVEvent vRecurrence = vMaster
                .withRecurrence(exdates.first())
                .removeProperties(Property.RRULE);

        IcsImportStats recurrenceStats = icsImporter.importIcsStuff(
                creator.getUid(), vRecurrence.makeCalendar(),
                IcsImportMode.caldavPutToDefaultLayerForTest());

        Assert.hasSize(1, recurrenceStats.getNewEventIds());

        IcsImportStats masterStats = icsImporter.importIcsStuff(
                creator.getUid(), vMaster.makeCalendar(),
                IcsImportMode.caldavPutToDefaultLayerForTest());

        Assert.isEmpty(eventDao.findEventsByIdsSafe(recurrenceStats.getNewEventIds().toList()));
        Assert.hasSize(1, masterStats.getNewEventIds());

        EventAndRepetition master = eventDbManager.getEventsAndRepetitionsByEventIds(
                masterStats.getNewEventIds().toList()).single();

        Assert.equals(exdates, master.getRepetitionInfo().getExdateStarts());
    }

    private long createFeedLayerWithNotification(TestUserInfo user, Notification notification) {
        long layerId = layerRoutines.createFeedLayer(user.getUid(), "Feed layer with notification");

        layerRoutines.updateNotification(user.getUid(), layerId,
                NotificationsData.updateFromWeb(Cf.list(notification)), false, ActionInfo.webTest());

        return layerId;
    }

    private ListF<Notification> getEventNotifications(TestUserInfo user, long eventId) {
        return notificationDbManager.getEventUserWithNotificationsByUidAndEventId(user.getUid(), eventId)
                .get().getNotifications().getNotifications().sortedBy(Notification::getChannel);
    }

    private static IcsVEvent createVeventWithAudioAlarm() {
        IcsVEvent event = new IcsVEvent();

        event = event.withUid(CalendarUtils.generateExternalId());
        event = event.withSummary("Event with audio alarm");

        event = event.withDtStart(MoscowTime.instant(2013, 5, 22, 21, 30));
        event = event.withDtEnd(MoscowTime.instant(2013, 5, 22, 22, 30));

        event = event.withSequenece(0);
        event = event.withDtStampNow();

        event = event.addVAlarm(new IcsVAlarm()
                .withAction(IcsAction.AUDIO)
                .withTrigger(new IcsTrigger("-PT30M", Cf.list(IcsValue.DURATION))));

        return event;
    }
} //~
