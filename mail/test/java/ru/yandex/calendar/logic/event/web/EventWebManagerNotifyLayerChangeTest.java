package ru.yandex.calendar.logic.event.web;

import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Instant;
import org.joda.time.ReadablePeriod;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.ModificationInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventUserData;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.layer.LayerUserDao;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sending.param.EventMessageParameters;
import ru.yandex.calendar.logic.sending.param.EventOnLayerChangeMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class EventWebManagerNotifyLayerChangeTest extends AbstractConfTest {
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private LayerUserDao layerUserDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private DateTimeManager dateTimeManager;


    private TestUserInfo organizer;
    private TestUserInfo attendee;

    private Email attendeeEmail;
    private final Email externalEmail = new Email("top@yandex-team.ru");

    private long organizerLayer1Id;
    private long organizerLayer2Id;
    private long attendeeLayerId;

    private TestUserInfo organizerLayer1User;
    private TestUserInfo organizerLayer2User;
    private TestUserInfo attendeeLayerUser;

    @Before
    public void cleanBeforeTest() {
        organizer = testManager.prepareRandomYaTeamUser(2220);
        attendee = testManager.prepareRandomYaTeamUser(2221);

        attendeeEmail = attendee.getEmail();

        organizerLayer1User = testManager.prepareRandomYaTeamUser(2222);
        organizerLayer2User = testManager.prepareRandomYaTeamUser(2223);
        attendeeLayerUser = testManager.prepareRandomYaTeamUser(2224);

        organizerLayer1Id = organizer.getDefaultLayerId();
        organizerLayer2Id = layerRoutines.createUserLayer(organizer.getUid());
        attendeeLayerId = attendee.getDefaultLayerId();

        startNotifiedLayerSharing(organizerLayer1User, organizerLayer1Id);
        startNotifiedLayerSharing(organizerLayer2User, organizerLayer2Id);
        startNotifiedLayerSharing(attendeeLayerUser, attendeeLayerId);

        mailSender.clear();
    }

    @Test
    public void singleMeeting() {
        EventData data = testManager.createDefaultEventData(organizer.getUid(), "Single");
        data.setInvData(attendeeEmail);
        data.setLayerId(organizerLayer1Id);

        CreateInfo created = createEvent(organizer, data);
        assertNotifiedOf(created.getLayerNotifyMails(), organizerLayer1User, MailType.EVENT_ON_LAYER_ADDED);
        assertNotifiedOf(created.getLayerNotifyMails(), attendeeLayerUser, MailType.EVENT_ON_LAYER_ADDED);

        data.setLayerId(organizerLayer2Id);
        ModificationInfo updated = updateEvent(organizer, created.getEventId(), data, false);

        assertNotifiedOf(updated.getEventMails(), organizerLayer1User, MailType.EVENT_ON_LAYER_REMOVED);
        assertNotifiedOf(updated.getEventMails(), organizerLayer2User, MailType.EVENT_ON_LAYER_ADDED);
        assertNotNotified(updated.getEventMails(), attendeeLayerUser);

        data.getEvent().setName(data.getEvent().getName() + "2");
        updated = updateEvent(organizer, created.getEventId(), data, false);

        assertNotifiedOf(updated.getEventMails(), attendeeLayerUser, MailType.EVENT_ON_LAYER_UPDATED);
        assertNotifiedOf(updated.getEventMails(), organizerLayer2User, MailType.EVENT_ON_LAYER_UPDATED);
    }

    @Test
    public void tailOfMeetingWithRecurrence() {
        EventData data = testManager.createDefaultEventData(organizer.getUid(), "Repeating");
        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        Event master = createEvent(organizer, data).getEvent();
        Event recurrence = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), master.getStartTs());
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);

        data.setInstanceStartTs(master.getStartTs().plus(Duration.standardDays(1)));
        data.setEvent(moveStartEnd(master.copy(), Hours.hours(28)));
        data.setLayerId(organizerLayer2Id);
        data.setInvData(attendeeEmail);

        data.getEvent().setId(master.getId());
        data.setExternalId(Option.empty());

        ModificationInfo updated = eventWebManager.update(
                organizer.getUserInfo(), data, Option.empty(), true, ActionInfo.webTest(master.getStartTs()));
        Assert.some(updated.getNewEventId());

        ListF<EventMessageParameters> mails = updated.getEventMails();
        assertNotifiedOf(mails, organizerLayer1User, MailType.EVENT_ON_LAYER_UPDATED, Option.of(master.getId()));
        assertNotifiedOf(mails, organizerLayer2User, MailType.EVENT_ON_LAYER_ADDED, updated.getNewEventId());
        assertNotifiedOf(mails, attendeeLayerUser, MailType.EVENT_ON_LAYER_ADDED, updated.getNewEventId());
    }

    @Test
    public void changedAndUnchangedTailOfRepeatingMeeting() {
        EventData data = testManager.createDefaultEventData(organizer.getUid(), "Repeating");
        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        Event master = createEvent(organizer, data).getEvent();

        data.setEvent(moveStartEnd(master.<Event>copy(), Days.THREE));
        ModificationInfo unchanged = updateEvent(organizer, master.getId(), data, true);

        data.setEvent(moveStartEnd(master.<Event>copy(), Days.TWO));
        data.getEvent().setName(data.getEvent().getName() + "2");
        ModificationInfo changed = updateEvent(organizer, master.getId(), data, true);

        assertNotifiedOf(changed.getEventMails(), organizerLayer1User, MailType.EVENT_ON_LAYER_UPDATED);
        assertNotNotified(unchanged.getEventMails(), organizerLayer1User);
    }

    @Test
    public void recurrenceMeeting() {
        Event master = testManager.createEventWithDailyRepetition(organizer.getUid());
        testManager.createEventUser(organizer.getUid(), master.getId(), Decision.YES, Option.of(true));
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.YES, false);

        EventData data = new EventData();
        data.setEvent(moveStartEnd(master.<Event>copy(), Days.ONE));
        data.setEventUserData(new EventUserData(new EventUser(), Cf.list(Notification.email(Duration.ZERO))));
        data.setInvData(attendeeEmail);

        ModificationInfo updated = updateEvent(organizer, master.getId(), data, false);
        Event recurrence = updated.getNewEvent().get().getEvent();

        ListF<EventMessageParameters> mails = updated.getEventMails();
        assertNotNotified(mails, organizerLayer1User);
        assertNotNotified(mails, attendeeLayerUser);

        data.setLayerId(organizerLayer2Id);
        data.getEvent().setName(data.getEvent().getName() + "2");

        updated = updateEvent(organizer, recurrence.getId(), data, false);
        mails = updated.getEventMails();

        assertNotifiedOf(mails, organizerLayer1User, MailType.EVENT_ON_LAYER_REMOVED, Option.of(master.getId()));
        assertNotifiedOf(mails, organizerLayer2User, MailType.EVENT_ON_LAYER_ADDED, Option.of(master.getId()));
        assertNotifiedOf(mails, attendeeLayerUser, MailType.EVENT_ON_LAYER_UPDATED, Option.of(recurrence.getId()));
    }

    @Test
    public void deleteSingleMeeting() {
        Event event = testManager.createDefaultEvent(organizer.getUid(), "Single");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        ModificationInfo deleted = eventWebManager.deleteUserEvent(
                organizer.getUserInfo(), event.getId(), Option.<Instant>empty(), false, ActionInfo.webTest());

        assertNotifiedOf(deleted.getEventMails(), organizerLayer1User, MailType.EVENT_ON_LAYER_REMOVED);
        assertNotifiedOf(deleted.getEventMails(), attendeeLayerUser, MailType.EVENT_ON_LAYER_REMOVED);
    }

    @Test
    public void deleteOccurrence() {
        Event event = testManager.createEventWithDailyRepetition(organizer.getUid());
        testManager.createEventUser(organizer.getUid(), event.getId(), Decision.YES, Option.of(true));
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        ModificationInfo deleted = eventWebManager.deleteUserEvent(
                organizer.getUserInfo(), event.getId(), Option.of(event.getStartTs()), false, ActionInfo.webTest());

        assertNotifiedOf(deleted.getEventMails(), organizerLayer1User, MailType.EVENT_ON_LAYER_REMOVED);
        assertNotifiedOf(deleted.getEventMails(), attendeeLayerUser, MailType.EVENT_ON_LAYER_REMOVED);
    }

    @Test
    public void attachDetachRepeatingEventWithRecurrence() {
        Event master = testManager.createEventWithDailyRepetition(organizer.getUid());
        Event recurrence = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), master.getStartTs());

        testManager.createEventUser(organizer.getUid(), master.getId(), Decision.YES, Option.of(true));
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);

        openLayerAndEvents(organizerLayer1Id);

        eventWebManager.attachEvent(
                attendee.getUserInfo(), master.getId(), Option.empty(), new EventUser(),
                NotificationsData.createEmpty(), ActionInfo.webTest());

        ListF<EventMessageParameters> mails = mailSender.getEventMessageParameters();
        assertNotifiedOf(mails, attendeeLayerUser, MailType.EVENT_ON_LAYER_ADDED, Option.of(master.getId()));

        mailSender.clear();
        eventWebManager.detachEvent(attendee.getUserInfo(), master.getId(), Option.of(attendeeLayerId), ActionInfo.webTest());

        mails = mailSender.getEventMessageParameters();
        assertNotifiedOf(mails, attendeeLayerUser, MailType.EVENT_ON_LAYER_REMOVED, Option.of(master.getId()));
    }

    @Test
    public void preattachedRepeatingMeetingWithRecurrence() {
        Event master = testManager.createEventWithDailyRepetition(organizer.getUid());
        Event recurrence = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), master.getStartTs());

        testManager.createEventUser(organizer.getUid(), master.getId(), Decision.YES, Option.of(true));
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);

        testManager.addExternalUserParticipantToEvent(master.getId(), externalEmail, Decision.YES, false);
        testManager.addExternalUserParticipantToEvent(recurrence.getId(), externalEmail, Decision.YES, false);

        EventData data = new EventData();
        data.setEvent(master.<Event>copy());
        data.setTimeZone(dateTimeManager.getTimeZoneForUid(organizer.getUid()));
        data.setRepetition(TestManager.createDailyRepetitionTemplate());
        data.setInvData(externalEmail);

        Participants participants = eventInvitationManager.getParticipantsByEventId(master.getId());
        String token = participants.getExternalParticipantByEmail(externalEmail).single().getPrivateToken().get();
        Assert.none(participants.getParticipantByUid(attendee.getUid()));

        ModificationInfo updated = eventWebManager.update(
                attendee.getUserInfo(), data, NotificationsData.notChanged(),
                Option.of(token), Option.empty(), true, Option.empty(), ActionInfo.webTest());

        ListF<EventMessageParameters> mails = updated.getEventMails();
        assertNotifiedOf(mails, attendeeLayerUser, MailType.EVENT_ON_LAYER_ADDED, Option.of(master.getId()));
    }


    private Event moveStartEnd(Event event, ReadablePeriod period) {
        event.setStartTs(event.getStartTs().plus(period.toPeriod().toStandardDuration()));
        event.setEndTs(event.getEndTs().plus(period.toPeriod().toStandardDuration()));

        return event;
    }

    private void startNotifiedLayerSharing(TestUserInfo listener, long layerId) {
        LayerUser layerUser = layerRoutines.createDefaultLayerUserOverrides(listener.getUid().getDomain());
        layerUser.setLayerId(layerId);
        layerUser.setUid(listener.getUid());
        layerUser.setPerm(LayerActionClass.EDIT);
        layerUser.setIsNotifyChanges(true);

        layerUserDao.saveLayerUser(layerUser);
    }

    private void openLayerAndEvents(long layerId) {
        eventDao.updateEventsPermAll(eventLayerDao.findEventLayerEventIdsByLayerId(layerId), EventActionClass.VIEW);
    }

    private void assertNotifiedOf(
            ListF<? extends EventMessageParameters> mails, TestUserInfo user, MailType mailType)
    {
        assertNotifiedOf(mails, user, mailType, Option.<Long>empty());
    }

    private void assertNotifiedOf(
            ListF<? extends EventMessageParameters> mails, TestUserInfo user, MailType mailType, Option<Long> eventId)
    {
        ListF<EventOnLayerChangeMessageParameters> found = findNotifyMails(mails, user);

        Assert.isTrue(found.size() < 2,
                "More than one notify email for user " + user.getEmail());
        Assert.isTrue(found.singleO().map(EventMessageParameters.mailTypeF()).isSome(mailType),
                "No notify email of type " + mailType + " for user " + user.getEmail());
        Assert.isTrue(!eventId.isPresent() || found.singleO().map(EventMessageParameters.getEventIdF()).equals(eventId),
                "No notify email for event " + eventId + " and user " + user.getEmail());
    }

    private void assertNotNotified(ListF<? extends EventMessageParameters> mails, TestUserInfo user) {
        Assert.isTrue(findNotifyMails(mails, user).isEmpty(), "Got notify email for " + user.getEmail());
    }

    private ListF<EventOnLayerChangeMessageParameters> findNotifyMails(
            ListF<? extends EventMessageParameters> mails, TestUserInfo recipient)
    {
        return mails.<EventMessageParameters>uncheckedCast()
                .filter(EventMessageParameters.getRecipientEmailF().andThen(recipient.getEmail().equalsIgnoreCaseF()))
                .filterByType(EventOnLayerChangeMessageParameters.class);
    }

    private CreateInfo createEvent(TestUserInfo actor, EventData eventData) {
        eventData.setTimeZone(MoscowTime.TZ);
        return eventWebManager.createUserEvent(
                actor.getUid(), eventData, InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest());
    }

    private ModificationInfo updateEvent(TestUserInfo actor, long eventId, EventData updateData, boolean applyToFuture) {
        updateData.getEvent().setId(eventId);
        updateData.setExternalId(Option.empty());

        updateData.setInstanceStartTs(updateData.getEvent().getStartTs());

        return eventWebManager.update(
                actor.getUserInfo(), updateData, Option.<Integer>empty(), applyToFuture, ActionInfo.webTest());
    }
}
