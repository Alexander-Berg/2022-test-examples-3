package ru.yandex.calendar.logic.ics.imp;

import lombok.val;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.domain.PassportAuthDomainsHolder;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.logic.sharing.perm.Authorizer;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.property.PropertiesHolder;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * CAL-7933
 */
public class IcsImporterPseudoLocalCopyTest extends AbstractConfTest {

    @Autowired
    private PassportAuthDomainsHolder passportAuthDomainsHolder;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private Authorizer authorizer;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private EventDao eventDao;

    private final Email externalEmail = new Email("someone@somewhere.com");

    private TestUserInfo attendee1;
    private TestUserInfo attendee2;
    private TestUserInfo attendee3;

    @Before
    public void setup() {
        passportAuthDomainsHolder.setDomains("public");
        attendee1 = testManager.prepareUser("yandex-team-mm-11311");
        attendee2 = testManager.prepareUser("yandex-team-mm-11312");
        attendee3 = testManager.prepareUser("yandex-team-mm-11313");
    }

    @After
    public void teardown() {
        passportAuthDomainsHolder.setDomains(PropertiesHolder.properties().getProperty("auth.domains"));
    }

    @Test
    public void create() {
        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withSummary("Pseudo local copied");
        vevent = vevent.withDtStart(MoscowTime.now());
        vevent = vevent.withDtEnd(MoscowTime.now().plusHours(1));

        vevent = vevent.withOrganizer(externalEmail);
        vevent = vevent.addAttendee(attendee1.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(attendee2.getEmail(), IcsPartStat.DECLINED);

        IcsCalendar calendar = vevent.makeCalendar();
        Function<TestUserInfo, Long> importF = user -> icsImporter.importIcsStuff(
                user.getUid(), calendar, IcsImportMode.incomingEmailFromMailhook()).getNewEventIds().single();

        long attendee1EventId = importF.apply(attendee1);
        long attendee2EventId = importF.apply(attendee2);

        Assert.notEquals(attendee1EventId, attendee2EventId);

        EventWithRelations event = eventDbManager.getEventWithRelationsById(attendee1EventId);
        val eventAuthInfo = authorizer.loadEventInfoForPermsCheck(attendee1.getUserInfo(), event);

        Assert.isFalse(authorizer.canEditEvent(attendee1.getUserInfo(), eventAuthInfo, ActionSource.WEB));
        Assert.isFalse(authorizer.canInviteToEvent(attendee1.getUserInfo(), eventAuthInfo, ActionSource.WEB));

        assertIsYandexUserParticipantWithDecision(attendee1EventId, attendee1, Decision.UNDECIDED);
        assertIsExternalUserParticipantWithDecision(attendee1EventId, attendee2, Decision.NO);

        assertIsYandexUserParticipantWithDecision(attendee2EventId, attendee2, Decision.UNDECIDED);
        assertIsExternalUserParticipantWithDecision(attendee2EventId, attendee1, Decision.YES);
    }

    @Test
    public void updateExisting() {
        Event event = testManager.createDefaultEvent(attendee1.getUid(), "Pseudo local copied");
        long eventId = event.getId();

        testManager.addExternalUserParticipantToEvent(eventId, externalEmail, Decision.YES, true);
        testManager.addUserParticipantToEvent(eventId, attendee1.getUid(), Decision.UNDECIDED, false);
        testManager.addUserParticipantToEvent(eventId, attendee2.getUid(), Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());

        vevent = vevent.withOrganizer(externalEmail);
        vevent = vevent.addAttendee(attendee1.getEmail(), IcsPartStat.NEEDS_ACTION);
        vevent = vevent.addAttendee(attendee2.getEmail(), IcsPartStat.NEEDS_ACTION);
        vevent = vevent.addAttendee(attendee3.getEmail(), IcsPartStat.NEEDS_ACTION);

        vevent = vevent.withSequenece(1);

        IcsImportMode importMode = IcsImportMode.incomingEmailFromMailhook();

        Assert.equals(event.getId(), icsImporter.importIcsStuff(
                attendee1.getUid(), vevent.makeCalendar(), importMode).getUpdatedEventIds().single());

        assertIsYandexUserParticipantWithDecision(eventId, attendee1, Decision.UNDECIDED);
        assertIsYandexUserParticipantWithDecision(eventId, attendee2, Decision.UNDECIDED);
        assertIsExternalUserParticipantWithDecision(eventId, attendee3, Decision.UNDECIDED);

        vevent = vevent.removeAttendee(attendee3.getEmail());
        vevent = vevent.addAttendee(attendee3.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.withSequenece(2);

        Assert.equals(eventId, icsImporter.importIcsStuff(
                attendee2.getUid(), vevent.makeCalendar(), importMode).getUpdatedEventIds().single());

        assertIsExternalUserParticipantWithDecision(eventId, attendee3, Decision.YES);

        eventId = icsImporter.importIcsStuff(
                attendee3.getUid(), vevent.makeCalendar(), importMode).getNewEventIds().single();

        assertIsExternalUserParticipantWithDecision(eventId, attendee1, Decision.UNDECIDED);
        assertIsExternalUserParticipantWithDecision(eventId, attendee2, Decision.UNDECIDED);
        assertIsYandexUserParticipantWithDecision(eventId, attendee3, Decision.UNDECIDED);
    }

    @Test
    public void cancel() {
        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withSummary("Pseudo local copied");
        vevent = vevent.withDtStart(MoscowTime.now());
        vevent = vevent.withDtEnd(MoscowTime.now().plusHours(1));

        vevent = vevent.withOrganizer(externalEmail);
        vevent = vevent.addAttendee(attendee1.getEmail(), IcsPartStat.TENTATIVE);
        vevent = vevent.addAttendee(attendee2.getEmail(), IcsPartStat.TENTATIVE);

        IcsImportMode importMode = IcsImportMode.mailWidget(
                attendee1.getUid(), new ActionInfo(ActionSource.MAIL, "", Instant.now()));

        long eventId = icsImporter.importIcsStuff(
                attendee1.getUid(), vevent.makeCalendar(), importMode).getNewEventIds().single();

        Assert.some(findParticipant(eventId, attendee1));

        icsImporter.importIcsStuff(
                attendee1.getUid(), vevent.withSequenece(1).makeCalendar().withMethod(IcsMethod.CANCEL), importMode);

        Assert.none(findParticipant(eventId, attendee1));
    }

    private void assertIsYandexUserParticipantWithDecision(long eventId, TestUserInfo user, Decision decision) {
        assertIsParticipantWithDecision(eventId, user, decision);
        Assert.isTrue(findParticipant(eventId, user).get().getId().isYandexUserWithUid(user.getUid()));
    }

    private void assertIsExternalUserParticipantWithDecision(long eventId, TestUserInfo user, Decision decision) {
        assertIsParticipantWithDecision(eventId, user, decision);
        Assert.isTrue(findParticipant(eventId, user).get().getId().isExternalUser());
    }

    private void assertIsParticipantWithDecision(long eventId, TestUserInfo user, Decision decision) {
        Option<ParticipantInfo> participant = findParticipant(eventId, user);
        Assert.some(participant, "Not a participant");
        Assert.equals(decision, participant.get().getDecision());
    }

    private Option<ParticipantInfo> findParticipant(long eventId, TestUserInfo user) {
        Participants ps = eventInvitationManager.getParticipantsByEventId(eventId);

        ListF<ParticipantId> ids = Cf.list(
                ParticipantId.yandexUid(user.getUid()),
                ParticipantId.invitationIdForExternalUser(user.getEmail()));

        return ids.foldLeft(Option.empty(), (acc, id) -> acc.orElse(() -> ps.getByIdSafe(id)));
    }
}
