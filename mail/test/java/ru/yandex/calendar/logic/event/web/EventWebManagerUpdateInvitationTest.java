package ru.yandex.calendar.logic.event.web;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationUpdateData;
import ru.yandex.calendar.logic.event.model.EventInvitationsData;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class EventWebManagerUpdateInvitationTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventInvitationManager eventInvitationManager;

    @Test
    public void singleEvent() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-14001");
        TestUserInfo user2 = testManager.prepareUser("yandex-team-mm-14002");

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(user1.getUid(), "singleEvent");
        EventData eventData = new EventData();
        eventData.getEvent().setId(event.getId());
        EventInvitationUpdateData invitationsData = new EventInvitationUpdateData(
                Cf.list(user2.getEmail()), Cf.<Email>list());
        eventData.setInvData(invitationsData);
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(user1.getUserInfo(), eventData, true, ActionInfo.webTest());

        ParticipantInfo participant =
                eventInvitationManager.getParticipantsByEventId(event.getId())
                .getAllAttendeesButNotOrganizer().single();
        Assert.A.equals(invitationsData.getNewEmails().first(), participant.getEmail());
        Assert.A.equals(Decision.UNDECIDED, participant.getDecision());
    }

    @Test
    public void eventWithRepetitionUpdateOccurrence() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-14011");
        TestUserInfo user2 = testManager.prepareUser("yandex-team-mm-14012");

        Event event = testManager.createEventWithDailyRepetition(user1.getUid());
        EventData eventData = new EventData();
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.getEvent().setId(event.getId());
        EventInvitationUpdateData invitationsData = new EventInvitationUpdateData(
                Cf.list(user2.getEmail()), Cf.<Email>list());
        eventData.setInvData(invitationsData);
        eventData.setTimeZone(MoscowTime.TZ);

        // new recurrence event is created
        long newEventId = eventWebManager.update(user1.getUserInfo(), eventData, false, ActionInfo.webTest()).get();

        Assert.A.isTrue(!eventInvitationManager.getParticipantsByEventId(event.getId()).isMeeting());

        ParticipantInfo participant =
                eventInvitationManager.getParticipantsByEventId(newEventId)
                .getAllAttendeesButNotOrganizer().single();
        Assert.A.equals(invitationsData.getNewEmails().first(), participant.getEmail());
        Assert.A.equals(Decision.UNDECIDED, participant.getDecision());
    }

    @Test
    public void eventWithRepetitionUpdateThisAndFuture() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-14021");
        TestUserInfo user2 = testManager.prepareUser("yandex-team-mm-14022");

        Event event = testManager.createEventWithDailyRepetition(user1.getUid());

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(TestDateTimes.addDaysMoscow(event.getStartTs(), 1));
        eventData.getEvent().setId(event.getId());
        EventInvitationsData invitationsData = new EventInvitationsData(Cf.list(user2.getEmail()));
        eventData.setInvData(invitationsData);
        eventData.setTimeZone(MoscowTime.TZ);

        Assert.none(eventWebManager.update(user1.getUserInfo(), eventData, true, ActionInfo.webTest()));

        Assert.isTrue(eventInvitationManager.getParticipantsByEventId(event.getId()).isMeeting());

        ParticipantInfo participant = eventInvitationManager.getParticipantsByEventId(event.getId())
                .getAllAttendeesButNotOrganizer().single();
        Assert.equals(invitationsData.getNowEmails().first(), participant.getEmail());
        Assert.equals(Decision.UNDECIDED, participant.getDecision());
    }

    @Test
    public void eventWithRepetitionAndRecurInst() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-14031");
        TestUserInfo user2 = testManager.prepareUser("yandex-team-mm-14032");

        Event recurrenceEvent = testManager.createEventWithRepetitionAndRecurrence(user1.getUid()).get2();

        EventData eventData = new EventData();
        eventData.getEvent().setId(recurrenceEvent.getId());
        EventInvitationUpdateData invitationsData = new EventInvitationUpdateData(
                Cf.list(user2.getEmail()), Cf.<Email>list());
        eventData.setInvData(invitationsData);
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(user1.getUserInfo(), eventData, false, ActionInfo.webTest());

        ParticipantInfo participant =
                eventInvitationManager.getParticipantsByEventId(recurrenceEvent.getId())
                .getAllAttendeesButNotOrganizer().single();
        Assert.A.equals(invitationsData.getNewEmails().first(), participant.getEmail());
        Assert.A.equals(Decision.UNDECIDED, participant.getDecision());
    }

    @Test
    public void remInvEventWithRepetition() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-14041");
        Email attendeeEmail = new Email("testRemInvEventWithRepetition@mail.ru");

        Event event = testManager.createDefaultMeeting(user1.getUid(), "Event name");
        testManager.createDailyRepetitionAndLinkToEvent(event.getId());
        testManager.addUserParticipantToEvent(event.getId(), user1.getUid(), Decision.UNDECIDED, true);
        testManager.addExternalUserParticipantToEvent(event.getId(), attendeeEmail, Decision.YES, false);

        EventData eventData = new EventData();
        eventData.getEvent().setId(event.getId());
        eventData.setInstanceStartTs(TestDateTimes.addDaysMoscow(event.getStartTs(), 1));
        EventInvitationUpdateData invitationsData = new EventInvitationUpdateData(
                Cf.<Email>list(), Cf.list(attendeeEmail));
        eventData.setInvData(invitationsData);
        eventData.setTimeZone(MoscowTime.TZ);

        Assert.none(eventWebManager.update(user1.getUserInfo(), eventData, true, ActionInfo.webTest()));

        Participants newEventParticipants = eventInvitationManager.getParticipantsByEventId(event.getId());
        Assert.isTrue(newEventParticipants.isNotMeetingStrict(), newEventParticipants.toString());
    }

    @Test
    public void eventWithRepetitionAndInvitation() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-14051");
        Email attendeeEmail = new Email("testEventWithRepetitionAndInvitation@mail.ru");

        Event event = testManager.createDefaultMeeting(user1.getUid(), "Event name");
        testManager.createDailyRepetitionAndLinkToEvent(event.getId());
        testManager.addUserParticipantToEvent(event.getId(), user1.getUid(), Decision.UNDECIDED, true);
        testManager.addExternalUserParticipantToEvent(event.getId(), attendeeEmail, Decision.YES, false);

        EventData eventData = new EventData();
        eventData.getEvent().setId(event.getId());
        eventData.setInvData(EventInvitationUpdateData.EMPTY);
        eventData.setInstanceStartTs(TestDateTimes.addDaysMoscow(event.getStartTs(), 1));
        eventData.setTimeZone(MoscowTime.TZ);

        Assert.none(eventWebManager.update(user1.getUserInfo(), eventData, true, ActionInfo.webTest()));

        ListF<ParticipantInfo> oldParticipants =
                eventInvitationManager.getParticipantsByEventId(event.getId()).getAllAttendeesButNotOrganizer();
        Assert.A.hasSize(1, oldParticipants);
    }
}
