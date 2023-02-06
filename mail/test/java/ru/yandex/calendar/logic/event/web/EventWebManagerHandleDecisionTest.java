package ru.yandex.calendar.logic.event.web;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.model.WebReplyData;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class EventWebManagerHandleDecisionTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventDao eventDao;

    private final Instant masterEventStart = MoscowTime.instant(2012, 10, 30, 22, 0);
    private final Instant recurrenceEventStart = masterEventStart.plus(Duration.standardDays(2));

    private final ActionInfo actionInfo = ActionInfo.webTest(masterEventStart.minus(77777));

    private TestUserInfo organizer;
    private TestUserInfo attendee1;
    private TestUserInfo attendee2;

    @Before
    public void cleanBeforeTest() {
        organizer = testManager.prepareRandomYaTeamUser(450);
        attendee1 = testManager.prepareRandomYaTeamUser(451);
        attendee2 = testManager.prepareRandomYaTeamUser(452);
    }

    @Test
    public void acceptSingle() {
        Event master = createSingleMeeting("acceptSingle", organizer,
                attendee1, Decision.UNDECIDED, attendee2, Decision.UNDECIDED);

        eventWebManager.handleEventInvitationDecision(
                attendee1.getUid(), master.getId(), new WebReplyData(Decision.YES), actionInfo);

        Assert.equals(Decision.YES, getUserDecision(master, attendee1));
        Assert.equals(Decision.UNDECIDED, getUserDecision(master, attendee2));
    }

    @Test
    public void acceptRecurrence() {
        Event master = createRepeatingMeeting("acceptRecurrence", organizer, attendee1, Decision.UNDECIDED);
        Event recurrence = createRecurrence(master, attendee1, Decision.UNDECIDED);

        eventWebManager.handleEventInvitationDecision(
                attendee1.getUid(), recurrence.getId(), new WebReplyData(Decision.YES), actionInfo);

        Assert.equals(Decision.UNDECIDED, getUserDecision(master, attendee1));
        Assert.equals(Decision.YES, getUserDecision(recurrence, attendee1));
    }

    @Test
    public void acceptMasterHasRecurrence() {
        Event master = createRepeatingMeeting("acceptMasterHasRecurrence", organizer, attendee1, Decision.UNDECIDED);
        Event recurrence = createRecurrence(master, attendee1, Decision.UNDECIDED);

        eventWebManager.handleEventInvitationDecision(
                attendee1.getUid(), master.getId(), new WebReplyData(Decision.YES), actionInfo);

        Assert.equals(Decision.YES, getUserDecision(master, attendee1));
        Assert.equals(Decision.YES, getUserDecision(recurrence, attendee1));
    }

    @Test
    public void acceptInstance() {
        Event master = createRepeatingMeeting("acceptOneInstance", organizer, attendee1, Decision.UNDECIDED);
        Assert.isEmpty(findRecurrenceEvents(master));

        eventWebManager.handleEventInvitationDecision(
                attendee1.getUid(), master.getId(), recurrenceEventStart, new WebReplyData(Decision.MAYBE), actionInfo);

        Assert.equals(Decision.MAYBE, getUserDecision(master, attendee1));
        Assert.isEmpty(findRecurrenceEvents(master));
    }

    @Test
    public void rejectInstance() {
        Event master = createRepeatingMeeting("rejectInstance", organizer,
                attendee1, Decision.UNDECIDED, attendee2, Decision.YES);

        Assert.isEmpty(findRecurrenceEvents(master));

        eventWebManager.handleEventInvitationDecision(
                attendee1.getUid(), master.getId(), recurrenceEventStart, new WebReplyData(Decision.NO), actionInfo);

        ListF<Event> recurrences = findRecurrenceEvents(master);

        Assert.hasSize(1, recurrences);

        Assert.equals(Decision.UNDECIDED, getUserDecision(master, attendee1));
        Assert.equals(Decision.YES, getUserDecision(master, attendee2));

        Assert.equals(Decision.NO, getUserDecision(recurrences.single(), attendee1));
        Assert.equals(Decision.YES, getUserDecision(recurrences.single(), attendee2));
    }


    private ListF<Event> findRecurrenceEvents(Event master) {
        return eventDao.findEventsByMainId(master.getMainEventId()).filter(e -> e.getRecurrenceId().isPresent());
    }

    private Decision getUserDecision(Event event, TestUserInfo user) {
        return eventUserDao.findEventUserByEventIdAndUid(event.getId(), user.getUid()).get().getDecision();
    }

    private Event createSingleMeeting(String eventName, TestUserInfo organizer,
            TestUserInfo attendee1, Decision decision1, Object ... attendees)
    {
        Event e = testManager.createDefaultEvent(organizer.getUid(), eventName, masterEventStart);
        addParticipantsToMeeting(e, organizer, attendee1, decision1, attendees);
        return e;
    }

    private Event createRepeatingMeeting(String eventName, TestUserInfo organizer,
            TestUserInfo attendee1, Decision decision1, Object ... attendees)
    {
        Event e = testManager.createDefaultEvent(organizer.getUid(), eventName, masterEventStart);
        testManager.createDailyRepetitionAndLinkToEvent(e.getId());
        addParticipantsToMeeting(e, organizer, attendee1, decision1, attendees);
        return e;
    }

    private Event createRecurrence(Event masterMeeting,
            TestUserInfo attendee1, Decision decision1, Object ... attendees)
    {
        Event e = testManager.createDefaultRecurrence(organizer.getUid(), masterMeeting.getId(), recurrenceEventStart);
        addParticipantsToMeeting(e, organizer, attendee1, decision1, attendees);
        return e;
    }

    private void addParticipantsToMeeting(Event meeting, TestUserInfo organizer,
            TestUserInfo attendee1, Decision decision1, Object ... attendees)
    {
        testManager.addUserParticipantToEvent(meeting.getId(), organizer.getUid(), Decision.YES, true);

        Tuple2List<TestUserInfo, Decision> otherAttendees = Tuple2List.fromPairs(attendees);
        for (Tuple2<TestUserInfo, Decision> attendee : Tuple2List.fromPairs(attendee1, decision1).plus(otherAttendees)) {
            testManager.addUserParticipantToEvent(meeting.getId(), attendee._1.getUid(), attendee._2, false);
        }
    }
}
