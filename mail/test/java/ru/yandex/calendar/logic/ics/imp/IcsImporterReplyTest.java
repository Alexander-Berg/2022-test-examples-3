package ru.yandex.calendar.logic.ics.imp;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.EventInvitationDao;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsComment;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.YandexUserParticipantInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsImporterReplyTest extends AbstractConfTest {

    @Autowired
    private UserManager userManager;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventInvitationDao eventInvitationDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;

    @Test
    public void replyIcsNotChangeEvent() {
        TestUserInfo user1 = testManager.prepareRandomYaTeamUser(11401);
        TestUserInfo user2 = testManager.prepareRandomYaTeamUser(11402);

        PassportUid uid1 = user1.getUid();
        PassportUid uid2 = user2.getUid();

        Email email1 = user1.getEmail();
        Email email2 = user2.getEmail();

        testManager.cleanUser(uid1);
        testManager.cleanUser(uid2);

        final String oldEventName = "Old event name";
        Event event = testManager.createDefaultMeeting(uid1, oldEventName);
        testManager.addUserParticipantToEvent(event.getId(), user1, Decision.UNDECIDED, true);
        testManager.addUserParticipantToEvent(event.getId(), user2, Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(email1);
        vevent = vevent.addProperty(new IcsAttendee(email2, IcsPartStat.ACCEPTED));
        vevent = vevent.withSummary("New event name");
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());

        IcsCalendar calendar = vevent.makeCalendar().withMethod(IcsMethod.REPLY);
        icsImporter.importIcsStuff(uid2, calendar, IcsImportMode.incomingEmailFromMailhook());

        Event event1 = eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(uid2), vevent.getUid().get()).get();
        Assert.A.equals(Decision.YES, eventUserDao.findEventUserByEventIdAndUid(event1.getId(), uid2).get().getDecision());
        Assert.A.equals(oldEventName, event1.getName());
    }

    @Test
    public void acceptInvitationWithNewAttendeeWithReply() { // test 13
        TestUserInfo user1 = testManager.prepareRandomYaTeamUser(11411);
        TestUserInfo user2 = testManager.prepareRandomYaTeamUser(11412);
        TestUserInfo user3 = testManager.prepareRandomYaTeamUser(11413);

        PassportUid uid1 = user1.getUid();
        PassportUid uid2 = user2.getUid();
        PassportUid uid3 = user3.getUid();

        Event event = testManager.createDefaultMeeting(uid1, "incomplete invitation");

        testManager.addUserParticipantToEvent(event.getId(), user1, Decision.UNDECIDED, true);
        testManager.addUserParticipantToEvent(event.getId(), user2, Decision.UNDECIDED, false);
        testManager.addUserParticipantToEvent(event.getId(), user3, Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withDtStart(event.getStartTs().plus(Duration.standardHours(1)));
        vevent = vevent.withDtEnd(event.getEndTs().plus(Duration.standardHours(1)));
        vevent = vevent.withSequenece(1);
        vevent = vevent.addProperty(new IcsAttendee(user3.getEmail(), IcsPartStat.ACCEPTED));

        IcsCalendar calendar = vevent.makeCalendar().withMethod(IcsMethod.REPLY);
        icsImporter.importIcsStuff(uid3, calendar, IcsImportMode.incomingEmailFromMailhook());

        ParticipantInfo participant3 = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(uid3)).get();
        Assert.equals(Decision.YES, participant3.getDecision());
        Assert.equals(Availability.BUSY, ((YandexUserParticipantInfo) participant3).getEventUser().getAvailability());

        // make sure other users weren't uninvited
        ParticipantInfo participant2 = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(uid2)).get();
        Assert.A.equals(Decision.UNDECIDED, participant2.getDecision());
    }

    // CAL-5683
    @Test
    public void acceptByNotAttendee() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(117);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(118);
        TestUserInfo notAttendee = testManager.prepareRandomYaTeamUser(119);

        Event e = testManager.createDefaultEvent(organizer.getUid(), "acceptByNotAttendee");
        testManager.addUserParticipantToEvent(e.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(e.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(e.getId()));
        vevent = vevent.withSummary(e.getName());
        vevent = vevent.withDtStart(e.getStartTs());
        vevent = vevent.withDtEnd(e.getEndTs());

        String comment = "I wanna attend!";
        vevent = vevent.addProperty(new IcsComment(comment));
        vevent = vevent.addAttendee(notAttendee.getEmail(), IcsPartStat.ACCEPTED);

        IcsCalendar calendar = vevent.makeCalendar().withMethod(IcsMethod.REPLY);
        icsImporter.importIcsStuff(organizer.getUid(), calendar, IcsImportMode.incomingEmailFromMailhook());

        YandexUserParticipantInfo notAttendeeParticipant =
                (YandexUserParticipantInfo) findParticipant(e.getId(), notAttendee).get();

        Assert.equals(Decision.YES, notAttendeeParticipant.getDecision());
        Assert.equals(Availability.BUSY, notAttendeeParticipant.getEventUser().getAvailability());
        Assert.equals(comment, notAttendeeParticipant.getReason());
    }

    @Test
    public void acceptOneInstance() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(171);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(172);

        Event master = testManager.createDefaultEvent(organizer.getUid(), "acceptOneInstance");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(master.getId()));
        vevent = vevent.withSummary(master.getName());

        Instant recurrenceId = master.getStartTs().plus(Duration.standardDays(2));
        vevent = vevent.withRecurrence(recurrenceId);
        vevent = vevent.withDtStart(recurrenceId);
        vevent = vevent.withDtEnd(recurrenceId.plus(1331));
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        Assert.isEmpty(eventDao.findRecurrenceEventByMainId(master.getMainEventId(), recurrenceId));

        IcsCalendar calendar = vevent.makeCalendar().withMethod(IcsMethod.REPLY);
        icsImporter.importIcsStuff(organizer.getUid(), calendar, IcsImportMode.incomingEmailFromMailhook());

        Event recurrence = eventDao.findRecurrenceEventByMainId(master.getMainEventId(), recurrenceId).single();

        Assert.some(Decision.UNDECIDED, findParticipant(master.getId(), attendee).map(ParticipantInfo.getDecisionF()));
        Assert.some(Decision.YES, findParticipant(recurrence.getId(), attendee).map(ParticipantInfo.getDecisionF()));
    }

    // CAL-10015
    @Test
    public void acceptRecurrenceWithoutRecurrenceId() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(173);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(174);

        Event master = testManager.createDefaultEvent(organizer.getUid(), "acceptRecurrence");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        Event recurrence = testManager.createDefaultRecurrence(
                organizer.getUid(), master.getId(), master.getStartTs().plus(Duration.standardDays(3)));
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(recurrence.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(master.getId()));
        vevent = vevent.withSummary(master.getName());

        vevent = vevent.withDtStart(recurrence.getStartTs());
        vevent = vevent.withDtEnd(recurrence.getEndTs());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        IcsCalendar calendar = vevent.makeCalendar().withMethod(IcsMethod.REPLY);
        icsImporter.importIcsStuff(organizer.getUid(), calendar, IcsImportMode.incomingEmailFromMailhook());

        Assert.none(findParticipant(master.getId(), attendee));
        Assert.some(Decision.YES, findParticipant(recurrence.getId(), attendee).map(ParticipantInfo.getDecisionF()));

    }

    private Option<ParticipantInfo> findParticipant(long eventId, TestUserInfo user) {
        return eventInvitationManager.getParticipantByEventIdAndParticipantId(
                eventId, ParticipantId.yandexUid(user.getUid()));
    }

} //~
