package ru.yandex.calendar.logic.ics.imp;

import net.fortuna.ical4j.model.Property;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.domain.PassportAuthDomainsHolder;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsRRule;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.property.PropertiesHolder;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * CAL-7856, GREG-1056
 */
public class IcsImporterPublicMailTest extends AbstractConfTest {

    @Autowired
    private PassportAuthDomainsHolder passportAuthDomainsHolder;
    @Autowired
    private TestManager testManager;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventUserDao eventUserDao;

    private Email externalEmail = new Email("someone@somewhere.com");
    private TestUserInfo organizer;
    private TestUserInfo attendee;

    @Before
    public void setup() {
        passportAuthDomainsHolder.setDomains("public");
        organizer = testManager.prepareUser("yandex-team-mm-11333");
        attendee = testManager.prepareUser("yandex-team-mm-11334");
    }

    @After
    public void teardown() {
        passportAuthDomainsHolder.setDomains(PropertiesHolder.properties().getProperty("auth.domains"));
    }

    @Test
    public void newEvent() {
        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withSummary("Event");
        vevent = vevent.withDtStart(MoscowTime.now());
        vevent = vevent.withDtEnd(MoscowTime.now().plusHours(1));

        long eventId = icsImporter.importIcsStuff(attendee.getUid(), vevent.makeCalendar(),
                mailImportMode(attendee)).getNewEventIds().single();

        assertIsAttachedWithDecision(eventId, attendee, Decision.UNDECIDED);
    }

    @Test
    public void newMeeting() {
        IcsVEvent vevent = createDefaultVEvent();

        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        long attendeeEventId = icsImporter.importIcsStuff(attendee.getUid(), vevent.makeCalendar(),
                mailImportMode(attendee)).getNewEventIds().single();

        assertIsExternalParticipantWithDecision(attendeeEventId, organizer, Decision.YES);
        assertIsParticipantWithDecision(attendeeEventId, attendee, Decision.UNDECIDED);

        long organizerEventId = icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(),
                mailImportMode(organizer)).getNewEventIds().single();

        assertIsExternalParticipantWithDecision(organizerEventId, organizer, Decision.YES);
        assertIsExternalParticipantWithDecision(organizerEventId, attendee, Decision.YES);
        assertIsAttachedWithDecision(organizerEventId, organizer, Decision.UNDECIDED);
    }

    @Test
    public void externalMeetingUpdated() {
        IcsVEvent vevent = createDefaultVEvent();

        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        long eventId = icsImporter.importIcsStuff(attendee.getUid(),
                vevent.makeCalendar(), mailImportMode(attendee)).getNewEventIds().single();

        assertIsExternalParticipantWithDecision(eventId, organizer, Decision.YES);
        assertIsParticipantWithDecision(eventId, attendee, Decision.UNDECIDED);

        vevent = vevent.withSummary("Updated");
        vevent = vevent.withSequenece(1);

        icsImporter.importIcsStuff(attendee.getUid(), vevent.makeCalendar(), mailImportMode(attendee));

        Assert.equals(vevent.getSummary().get(), eventDbManager.getEventById(eventId).getName());
        assertIsParticipantWithDecision(eventId, attendee, Decision.UNDECIDED);
    }

    @Test
    public void internalMeetingUpdateRestricted() {
        IcsVEvent vevent = createDefaultVEvent();

        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        long eventId = icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar(), IcsImportMode.caldavPutToDefaultLayerForTest()).getNewEventIds().single();

        assertIsParticipantWithDecision(eventId, organizer, Decision.YES);
        assertIsParticipantWithDecision(eventId, attendee, Decision.UNDECIDED);

        vevent = vevent.withSummary("Updated");
        vevent = vevent.withSequenece(1);

        icsImporter.importIcsStuff(attendee.getUid(), vevent.makeCalendar(), mailImportMode(attendee));

        Assert.notEquals(vevent.getSummary().get(), eventDbManager.getEventById(eventId).getName());
    }

    @Test
    public void externalRecurrenceUpdated() {
        IcsVEvent vevent = createDefaultVEvent();

        vevent = vevent.addProperty(new IcsRRule("FREQ=DAILY"));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        long masterId = icsImporter.importIcsStuff(attendee.getUid(),
                vevent.makeCalendar(), mailImportMode(attendee)).getNewEventIds().single();

        assertIsExternalParticipantWithDecision(masterId, organizer, Decision.YES);
        assertIsParticipantWithDecision(masterId, attendee, Decision.UNDECIDED);

        vevent = vevent.withRecurrence(vevent.getStart(IcsVTimeZones.fallback(MoscowTime.TZ)));
        vevent = vevent.removeProperties(Property.RRULE);

        vevent = vevent.withSummary("Updated");
        vevent = vevent.withSequenece(1);

        long recurrenceId = icsImporter.importIcsStuff(
                attendee.getUid(), vevent.makeCalendar(), mailImportMode(attendee)).getNewEventIds().single();

        Assert.equals(vevent.getSummary().get(), eventDbManager.getEventById(recurrenceId).getName());

        assertIsParticipantWithDecision(recurrenceId, attendee, Decision.UNDECIDED);
        assertIsParticipantWithDecision(masterId, attendee, Decision.UNDECIDED);
    }

    @Test
    public void internalRecurrenceUpdateRestricted() {
        IcsVEvent vevent = createDefaultVEvent();

        vevent = vevent.addProperty(new IcsRRule("FREQ=DAILY"));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        long eventId = icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar(), IcsImportMode.caldavPutToDefaultLayerForTest()).getNewEventIds().single();

        assertIsParticipantWithDecision(eventId, organizer, Decision.YES);
        assertIsParticipantWithDecision(eventId, attendee, Decision.UNDECIDED);

        vevent = vevent.withRecurrence(vevent.getStart(IcsVTimeZones.fallback(MoscowTime.TZ)));
        vevent = vevent.removeProperties(Property.RRULE);
        vevent = vevent.withSequenece(1);

        Assert.isEmpty(icsImporter.importIcsStuff(
                attendee.getUid(), vevent.makeCalendar(), mailImportMode(attendee)).getNewEventIds());

        vevent = vevent.withSummary("Updated");
        vevent = vevent.withSequenece(2);

        Assert.isEmpty(icsImporter.importIcsStuff(
                attendee.getUid(), vevent.makeCalendar(), mailImportMode(attendee)).getNewEventIds());
    }

    @Test
    public void internalEventUpdateRestricted() {
        IcsVEvent vevent = createDefaultVEvent();

        long eventId = icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar(), IcsImportMode.caldavPutToDefaultLayerForTest()).getNewEventIds().single();

        vevent = vevent.withSummary("Updated");
        vevent = vevent.withSequenece(1);

        icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(), mailImportMode(organizer));

        Assert.notEquals(vevent.getSummary().get(), eventDbManager.getEventById(eventId).getName());
    }

    @Test
    public void externalMeetingCancelled() {
        IcsVEvent vevent = createDefaultVEvent();

        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        long eventId = icsImporter.importIcsStuff(attendee.getUid(),
                vevent.makeCalendar(), mailImportMode(attendee)).getNewEventIds().single();

        assertIsParticipantWithDecision(eventId, attendee, Decision.UNDECIDED);

        vevent = vevent.withSequenece(1);

        icsImporter.importIcsStuff(attendee.getUid(),
                vevent.makeCalendar().withMethod(IcsMethod.CANCEL), mailImportMode(attendee));

        Assert.none(findParticipant(eventId, attendee.getParticipantId()));
    }

    @Test
    public void internalMeetingCancellationRestricted() {
        IcsVEvent vevent = createDefaultVEvent();

        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        long eventId = icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar(), IcsImportMode.caldavPutToDefaultLayerForTest()).getNewEventIds().single();

        assertIsParticipantWithDecision(eventId, attendee, Decision.UNDECIDED);

        vevent = vevent.withSequenece(1);

        icsImporter.importIcsStuff(attendee.getUid(),
                vevent.makeCalendar().withMethod(IcsMethod.CANCEL), mailImportMode(attendee));

        assertIsParticipantWithDecision(eventId, attendee, Decision.UNDECIDED);
    }

    @Test
    public void internalEventCancellationRestricted() {
        IcsVEvent vevent = createDefaultVEvent();

        long eventId = icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar(), IcsImportMode.caldavPutToDefaultLayerForTest()).getNewEventIds().single();

        assertIsAttachedWithDecision(eventId, organizer, Decision.YES);

        vevent = vevent.withSequenece(1);

        icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar().withMethod(IcsMethod.CANCEL), mailImportMode(organizer));

        assertIsAttachedWithDecision(eventId, organizer, Decision.YES);
    }

    @Test
    public void meetingReply() {
        IcsVEvent vevent = createDefaultVEvent();

        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.NEEDS_ACTION);
        vevent = vevent.addAttendee(externalEmail, IcsPartStat.NEEDS_ACTION);

        long eventId = icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar(), IcsImportMode.caldavPutToDefaultLayerForTest()).getNewEventIds().single();

        vevent = vevent.removeProperties(Property.ATTENDEE);
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED).withSequenece(1);

        icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar().withMethod(IcsMethod.REPLY), mailImportMode(organizer));

        assertIsParticipantWithDecision(eventId, attendee, Decision.UNDECIDED);

        vevent = vevent.removeProperties(Property.ATTENDEE);
        vevent = vevent.addAttendee(externalEmail, IcsPartStat.ACCEPTED).withSequenece(2);

        icsImporter.importIcsStuff(organizer.getUid(),
                vevent.makeCalendar().withMethod(IcsMethod.REPLY), mailImportMode(organizer));

        assertIsParticipantWithDecision(eventId, externalEmail, Decision.YES);
    }

    private static IcsVEvent createDefaultVEvent() {
        IcsVEvent vevent = new IcsVEvent();

        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withSummary("Meeting");
        vevent = vevent.withDtStart(MoscowTime.now());

        return vevent.withDtEnd(MoscowTime.now().plusHours(1));
    }

    private static IcsImportMode mailImportMode(TestUserInfo recipient) {
        return IcsImportMode.mailWidget(recipient.getUid(), new ActionInfo(ActionSource.MAIL, "", Instant.now()));
    }

    private void assertIsExternalParticipantWithDecision(long eventId, TestUserInfo user, Decision decision) {
        assertIsParticipantWithDecision(eventId, ParticipantId.invitationIdForExternalUser(user.getEmail()), decision);
    }

    private void assertIsParticipantWithDecision(long eventId, TestUserInfo user, Decision decision) {
        assertIsParticipantWithDecision(eventId, ParticipantId.yandexUid(user.getUid()), decision);
    }

    private void assertIsParticipantWithDecision(long eventId, ParticipantId id, Decision decision) {
        Option<ParticipantInfo> participant = findParticipant(eventId, id);

        Assert.some(participant, "Not a participant");
        Assert.equals(decision, participant.get().getDecision());
    }

    private void assertIsParticipantWithDecision(long eventId, Email email, Decision decision) {
        Option<ParticipantInfo> participant = findParticipant(eventId, ParticipantId.invitationIdForExternalUser(email));

        Assert.some(participant, "Not a participant");
        Assert.equals(decision, participant.get().getDecision());
    }

    private void assertIsAttachedWithDecision(long eventId, TestUserInfo user, Decision decision) {
        Option<EventUser> eventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, user.getUid());

        Assert.some(eventUser, "Not attached to event");
        Assert.isFalse(eventUser.get().getIsAttendee(), "Should not be attendee");
        Assert.isFalse(eventUser.get().getIsOrganizer(), "Should not be organizer");
        Assert.equals(decision, eventUser.get().getDecision());
    }

    private Option<ParticipantInfo> findParticipant(long eventId, ParticipantId id) {
        return eventInvitationManager.getParticipantByEventIdAndParticipantId(eventId, id);
    }
}
