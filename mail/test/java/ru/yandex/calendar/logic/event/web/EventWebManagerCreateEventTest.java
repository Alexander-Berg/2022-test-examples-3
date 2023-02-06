package ru.yandex.calendar.logic.event.web;

import lombok.val;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.WindowsTimeZones;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.domain.PassportAuthDomainsHolder;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationsData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sending.param.EventLocation;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.perm.Authorizer;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractEwsExportedLoginsTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class EventWebManagerCreateEventTest extends AbstractEwsExportedLoginsTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private MailSenderMock mailSenderMock;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private Authorizer authorizer;
    @Autowired
    private PassportAuthDomainsHolder passportAuthDomainsHolder;

    public EventWebManagerCreateEventTest(EwsUsage ewsUsage) {
        super(ewsUsage);
    }

    @Test
    public void createEventFromWebWithUndefinedLayerId() {
        createEventFromWeb(false);
    }

    @Test
    public void createEventFromWebWithDefineLayerId() {
        createEventFromWeb(true);
    }

    private void createEventFromWeb(boolean defineLayerId) {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-14400");

        ActionInfo actionInfo = ActionInfo.webTest();

        long layerId =
                testManager.createDefaultLayerForUser(user.getUid(), actionInfo.getNow().minus(Duration.standardDays(1)));

        EventData eventData = new EventData();
        eventData.getEvent().setName("createEventFromWeb");
        eventData.getEvent().setStartTs(TestDateTimes.moscow(2010, 11, 30, 10, 0));
        eventData.getEvent().setEndTs(TestDateTimes.moscow(2010, 11, 30, 10, 0));
        eventData.setTimeZone(MoscowTime.TZ);

        if (defineLayerId) {
            eventData.setLayerId(layerId);
        }

        CreateInfo info = createEvent(user, eventData, actionInfo);

        if (!defineLayerId) {
            layerId = info.getLayerId().get();
        }

        EventLayer eventLayer = eventLayerDao.findEventLayerByEventIdAndLayerId(
                info.getEvent().getId(), layerId).get();

        Assert.A.isTrue(eventLayer.getIsPrimaryInst());

        testLayerCollLastUpdateChecker.assertUpdated(layerId, actionInfo.getNow());
    }

    @Test
    public void createEventFromWebInSharedLayer() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-14410");
        TestUserInfo layerOwner = testManager.prepareUser("yandex-team-mm-14411");

        long layerId = layerRoutines.createUserLayer(layerOwner.getUid());
        layerRoutines.startNewSharing(creator.getUid(), layerId, LayerActionClass.CREATE);

        EventData eventData = new EventData();
        eventData.getEvent().setName("createEventFromWebInSharedLayer");
        eventData.getEvent().setStartTs(TestDateTimes.moscow(2010, 11, 30, 10, 0));
        eventData.getEvent().setEndTs(TestDateTimes.moscow(2010, 11, 30, 10, 0));
        eventData.setLayerId(layerId);
        eventData.setTimeZone(MoscowTime.TZ);

        CreateInfo info = createEvent(creator, eventData, ActionInfo.webTest());

        EventLayer eventLayer = eventLayerDao.findEventLayerByEventIdAndLayerId(
                info.getEvent().getId(), layerId).get();

        Assert.A.isTrue(eventLayer.getIsPrimaryInst());
    }

    @Test
    public void createMeetingWithSpecifiedAvailability() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(127);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(172);

        EventData eventData = testManager.createDefaultEventData(organizer.getUid(), "createWithAvailability");
        eventData.setInvData(new EventInvitationsData(Cf.list(organizer.getEmail(), attendee.getEmail())));

        EventUser eventUser = new EventUser();
        eventUser.setAvailability(Availability.AVAILABLE);

        eventData.setEventUserData(eventData.getEventUserData().withEventUser(eventUser));
        CreateInfo info = createEvent(organizer, eventData, ActionInfo.webTest());

        EventUser organizerEventUser = eventRoutines.findEventUser(organizer.getUid(), info.getEvent().getId()).get();
        Assert.equals(Availability.AVAILABLE, organizerEventUser.getAvailability());
    }

    // CAL-6298
    @Test
    @WantsEws
    public void occupyParkingRoomForUser() {
        Resource parking = testManager.cleanAndCreateResourceWithNoExchSync("pp_137", "Парковка", ResourceType.PARKING);

        TestUserInfo customer = testManager.prepareYandexUser(TestManager.createSsytnik());
        TestUserInfo superUser = testManager.prepareYandexUser(TestManager.createHelga(), Group.SUPER_USER);

        setIsEwserIfNeeded(superUser);

        EventData eventData = testManager.createDefaultEventData(superUser.getUid(), "occupyParking");
        eventData.setInvData(Option.of(customer.getEmail()), ResourceRoutines.getResourceEmail(parking));

        mailSenderMock.clear();

        CreateInfo created = createEvent(superUser, eventData, ActionInfo.webTest());
        ListF<EventUser> createdEventUsers = eventUserDao.findEventUsersByEventId(created.getEvent().getId());

        Assert.isEmpty(mailSenderMock.getEventMessageParameters());
        Assert.equals(Cf.list(customer.getUid()), createdEventUsers.map(EventUser::getUid));
        Assert.equals(Cf.list(Decision.NO), createdEventUsers.map(EventUser.getDecisionF()));
        Assert.isEmpty(eventLayerDao.findEventLayersByEventId(created.getEvent().getId()));
    }

    // CAL-6382
    @Test
    public void exchangeExportingIgnoredForEventWithoutResourcesInPublicDomain() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-14400");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-19200");

        DateTimeZone tz = DateTimeZone.forID("Asia/Nicosia");

        EventData data = testManager.createDefaultEventData(organizer.getUid(), "Event from Nicosia");
        data.setInvData(new EventInvitationsData(Cf.list(attendee.getEmail())));
        data.setTimeZone(tz);

        Assert.none(WindowsTimeZones.getWinNameByZone(tz));

        CreateInfo created = createEvent(organizer, data, ActionInfo.webTest());
        Assert.equals(tz, created.getRepetitionInfo().getTz());
    }

    // CAL-7996
    @Test
    public void ownerAttendingEventOnSharedLayer() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(887);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(788);

        long layerId = attendee.getDefaultLayerId();

        layerRoutines.startNewSharing(organizer.getUid(), layerId, LayerActionClass.EDIT);

        EventData data = testManager.createDefaultEventData(organizer.getUid(), "Event to share");
        data.setInvData(new EventInvitationsData(Cf.list(attendee.getEmail())));
        data.setLayerId(layerId);

        CreateInfo created = createEvent(organizer, data, ActionInfo.webTest());

        Assert.exists(
                eventLayerDao.findEventLayerByEventIdAndLayerId(created.getEventId(), layerId),
                EventLayer::getIsPrimaryInst);
    }

    // CAL-10264
    @Test
    public void eventGift() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(687);
        TestUserInfo recipient = testManager.prepareRandomYaTeamUser(786);

        EventData data = testManager.createDefaultEventData(creator.getUid(), "Event to gift");
        data.setInvData(Option.of(recipient.getEmail()));

        CreateInfo created = createEvent(creator, data, ActionInfo.webTest());

        var userInfo = recipient.getUserInfo();
        val eventAuthInfo = authorizer.loadEventInfoForPermsCheck(userInfo, eventDbManager.getEventWithRelationsByEvent(created.getEvent()));
        Assert.isTrue(authorizer.canEditEvent(userInfo, eventAuthInfo, ActionSource.WEB));
    }

    @Test
    public void createEventWithResourceAndLocation() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(787);
        TestUserInfo invitee = testManager.prepareRandomYaTeamUser(788);

        Resource resource = testManager.cleanAndCreateResourceWithNoExchSync(
                "resource", "Resource", ResourceType.ROOM);

        EventData data = testManager.createDefaultEventData(creator.getUid(), "Located");
        data.getEvent().setLocation("Location");
        data.setInvData(ResourceRoutines.getResourceEmail(resource), invitee.getEmail());

        mailSenderMock.clear();

        CreateInfo created = passportAuthDomainsHolder.withDomainsForTest("public",
                () -> createEvent(creator, data, ActionInfo.webTest()));

        Assert.equals(data.getEvent().getLocation(), created.getEvent().getLocation());

        EventLocation location = mailSenderMock.getEventMessageParameters().single().getEventMessageInfo().getLocation();
        Assert.equals("Resource, Location", location.asTextForMailSubject(Language.RUSSIAN));
    }

    private CreateInfo createEvent(TestUserInfo creator, EventData eventData, ActionInfo actionInfo) {
        return eventRoutines.createUserOrFeedEvent(
                UidOrResourceId.user(creator.getUid()), EventType.USER,
                eventRoutines.createMainEvent(creator.getUid(), eventData, actionInfo), eventData,
                NotificationsData.createEmpty(),
                InvitationProcessingMode.SAVE_ATTACH_SEND, actionInfo);
    }
}
