package ru.yandex.calendar.logic.event.web;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventUserRoutines;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.EventParticipants;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class EventWebManagerOnetimeOrganizerTest extends AbstractConfTest {

    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventUserRoutines eventUserRoutines;


    private final Instant startTs = MoscowTime.instant(2017, 10, 10, 22, 0);

    private TestUserInfo organizer1;
    private TestUserInfo organizer2;
    private TestUserInfo attendee;

    private Event master;
    private Event recurrence;

    @Before
    public void setup() {
        organizer1 = testManager.prepareRandomYaTeamUser(32213);
        organizer2 = testManager.prepareRandomYaTeamUser(32214);
        attendee = testManager.prepareRandomYaTeamUser(32215);

        master = testManager.createDefaultEvent(organizer1.getUid(), "Master", startTs);
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        testManager.addUserParticipantToEvent(master.getId(), organizer1.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), organizer2.getUid(), Decision.YES, false);

        recurrence = testManager.createDefaultRecurrence(organizer2.getUid(), master.getId(), startTs);

        testManager.addUserParticipantToEvent(recurrence.getId(), organizer2.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer1.getUid(), Decision.YES, false);
    }

    @Test
    public void recurrenceAndFutureTrivialUpdate() {
        EventData data = new EventData();
        data.setEvent(master.copy());
        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        data.getEvent().setId(recurrence.getId());
        data.setInvData(Option.of(organizer1.getEmail()), organizer2.getEmail());

        eventWebManager.update(organizer1.getUserInfo(), data, true, ActionInfo.webTest(startTs));

        Assert.equals(Cf.list(organizer1.getUid(), organizer2.getUid()),
                eventDbManager.getParticipantsByEventIds(Cf.list(master.getId(), recurrence.getId()))
                        .flatMap(ps -> ps.getOrganizerIdWithInconsistent().flatMap(ParticipantId::getUidIfYandexUser)));
    }

    @Test
    public void eventNameAndAttendeesUpdate() {
        EventData data = new EventData();
        data.setEvent(master.copy());

        data.setRepetition(TestManager.createDailyRepetitionTemplate());
        data.setInvData(Option.of(organizer1.getEmail()), organizer2.getEmail(), attendee.getEmail());

        data.getEvent().setName("Updated");

        eventWebManager.update(organizer1.getUserInfo(), data, true, ActionInfo.webTest(startTs));

        Assert.equals(data.getEvent().getName(), eventDbManager.getEventById(master.getId()).getName());
        Assert.notEquals(data.getEvent().getName(), eventDbManager.getEventById(recurrence.getId()).getName());

        ListF<EventParticipants> participants = eventDbManager.getParticipantsByEventIds(
                Cf.list(master.getId(), recurrence.getId()));

        Assert.some(organizer1.getParticipantId(), participants.first().getOrganizerIdWithInconsistent());
        Assert.isTrue(participants.first().userIsAttendeeWithInconsistent(attendee.getUid()));

        Assert.some(organizer2.getParticipantId(), participants.last().getOrganizerIdWithInconsistent());
        Assert.isTrue(participants.last().userIsAttendeeWithInconsistent(attendee.getUid()));
    }

    @Test
    public void recurrenceExcludedAttendeeDoesNotAttaches() {
        eventInvitationManager.removeAttendeeByParticipantId(
                recurrence.getId(), ParticipantId.yandexUid(organizer1.getUid()), ActionInfo.webTest());

        EventData data = new EventData();
        data.setEvent(master.copy());

        data.setRepetition(TestManager.createDailyRepetitionTemplate());
        data.setInvData(Option.of(organizer1.getEmail()), attendee.getEmail());

        Assert.none(eventDbManager.getEventLayerForEventAndUser(recurrence.getId(), organizer1.getUid()));
        Assert.some(Decision.NO, eventUserRoutines.findEventUserDecision(organizer1.getUid(), recurrence.getId()));

        eventWebManager.update(organizer1.getUserInfo(), data, true, ActionInfo.webTest(startTs));

        Assert.none(eventDbManager.getEventLayerForEventAndUser(recurrence.getId(), organizer1.getUid()));
        Assert.some(Decision.NO, eventUserRoutines.findEventUserDecision(organizer1.getUid(), recurrence.getId()));
    }
}
