package ru.yandex.calendar.logic.event.meeting;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ModificationInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationUpdateData;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.event.web.EventWebRemover;
import ru.yandex.calendar.logic.event.web.EventWebUpdater;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sending.param.EventMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class RecurrenceOnlyAttendeesTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private EventWebUpdater eventWebUpdater;
    @Autowired
    private EventWebRemover eventWebRemover;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private EventDao eventDao;

    private TestUserInfo organizer;
    private TestUserInfo masterAttendee;
    private TestUserInfo instancesAttendee;

    private Event masterEvent;
    private Repetition masterRepetition;
    private ListF<Event> recurrences;

    @Before
    public void prepare() {
        organizer = testManager.prepareRandomYaTeamUser(154);
        masterAttendee = testManager.prepareRandomYaTeamUser(155);
        instancesAttendee = testManager.prepareRandomYaTeamUser(156);

        masterEvent = testManager.createDefaultEventWithDailyRepetition(organizer.getUid(), "master");
        masterRepetition = repetitionRoutines.getRepetitionById(masterEvent.getRepetitionId().get());

        testManager.addUserParticipantToEvent(masterEvent.getId(), masterAttendee, Decision.YES, false);

        int startOffset = ((int) new Duration(masterEvent.getStartTs(), DateTime.now(TestManager.chrono).toInstant())
                .getStandardDays()) + 1;

        recurrences = Cf.range(0, 2).map(offset -> {
            Instant recurrenceId = masterEvent.getStartTs().toDateTime(TestManager.chrono).plusDays(startOffset + offset).toInstant();
            Event recurrence = testManager.createDefaultRecurrence(
                    organizer.getUid(), masterEvent.getId(), recurrenceId);
            testManager.addUserParticipantToEvent(recurrence.getId(), organizer, Decision.YES, true);
            testManager.addUserParticipantToEvent(recurrence.getId(), masterAttendee, Decision.YES, false);
            testManager.addUserParticipantToEvent(recurrence.getId(), instancesAttendee, Decision.YES, false);
            return recurrence;
        });
    }

    @Test
    public void deleteMaster() {
        mailSender.clear();

        eventWebRemover.remove(
                organizer.getUserInfo(), masterEvent.getId(),
                Option.empty(), true, ActionInfo.webTest(masterEvent.getStartTs()));

        Assert.hasSize(4, mailSender.getEventMessageParameters());

        assertMail(organizer, masterEvent, MailType.EVENT_CANCEL);
        assertMail(masterAttendee, masterEvent, MailType.EVENT_CANCEL);

        recurrences.forEach(recurrence -> assertMail(instancesAttendee, recurrence, MailType.EVENT_CANCEL));
    }

    @Test
    public void updateMasterEndTime() {
        EventData eventData = new EventData();
        eventData.setEvent(masterEvent.copy());

        eventData.getEvent().setEndTs(masterEvent.getEndTs().plus(Duration.standardHours(1)));

        eventData.setRepetition(masterRepetition);
        eventData.setInvData(EventInvitationUpdateData.EMPTY);

        update(eventData);

        Assert.hasSize(4, mailSender.getEventMessageParameters());

        assertMail(organizer, masterEvent, MailType.EVENT_UPDATE);
        assertMail(masterAttendee, masterEvent, MailType.EVENT_UPDATE);
        recurrences.forEach(recurrence -> assertMail(instancesAttendee, recurrence, MailType.EVENT_CANCEL));
    }

    @Test
    public void updateMasterDescription() {
        EventData eventData = new EventData();

        eventData.setEvent(masterEvent.copy());
        eventData.setRepetition(masterRepetition);

        eventData.setInvData(EventInvitationUpdateData.EMPTY);
        eventData.getEvent().setDescription("Updated");

        update(eventData);

        Assert.hasSize(4, mailSender.getEventMessageParameters());

        assertMail(organizer, masterEvent, MailType.EVENT_UPDATE);
        assertMail(masterAttendee, masterEvent, MailType.EVENT_UPDATE);
        recurrences.forEach(recurrence -> assertMail(instancesAttendee, recurrence, MailType.EVENT_UPDATE));
    }

    @Test
    public void ignoreMasterDescriptionUpdate() {
        EventData eventData = new EventData();

        eventData.setEvent(masterEvent.copy());
        eventData.setRepetition(masterRepetition);

        eventData.setInvData(EventInvitationUpdateData.EMPTY);
        eventData.getEvent().setDescription("Updated");

        mailSender.clear();

        eventWebUpdater.update(
                masterAttendee.getUserInfo(), eventData, NotificationsData.notChanged(),
                true, ActionInfo.webTest(masterEvent.getStartTs()));

        Assert.isEmpty(mailSender.getEventMessageParameters());
    }

    @Test
    public void updateMasterDueTs() {
        EventData eventData = new EventData();
        eventData.setEvent(masterEvent);

        Repetition repetition = masterRepetition.copy();
        repetition.setDueTs(recurrences.last().getStartTs());

        eventData.setRepetition(repetition);
        eventData.setInvData(EventInvitationUpdateData.EMPTY);

        update(eventData);

        eventDao.findEventById(recurrences.first().getId());
        Assert.isEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrences.last().getId())));

        Assert.hasSize(3, mailSender.getEventMessageParameters());

        assertMail(organizer, masterEvent, MailType.EVENT_UPDATE);
        assertMail(masterAttendee, masterEvent, MailType.EVENT_UPDATE);
        assertMail(instancesAttendee, recurrences.last(), MailType.EVENT_CANCEL);
    }

    @Test
    public void masterSubscribedOnetimeParticipant() {
        testManager.addSubscriberToEvent(masterEvent.getId(), instancesAttendee.getUid());

        EventData eventData = new EventData();
        eventData.setEvent(masterEvent.copy());
        eventData.setRepetition(masterRepetition);

        eventData.setInvData(Option.empty());

        update(eventData);

        Assert.hasSize(5, mailSender.getEventMessageParameters());

        assertMail(organizer, masterEvent, MailType.EVENT_UPDATE);
        assertMail(masterAttendee, masterEvent, MailType.EVENT_CANCEL);

        ListF<EventMessageParameters> instanceAttendeeMails = Cf.list(masterEvent).plus(recurrences)
                .map(e -> mailSender.findEventMail(e.getId(), instancesAttendee.getEmail())
                        .getOrThrow("Missing mail for event ", + e.getId()));

        Assert.forAll(instanceAttendeeMails, m -> m.mailType() == MailType.EVENT_UPDATE);
        Assert.equals(Cf.list(false, true, true), instanceAttendeeMails.map(m -> m.getIcs().isPresent()));
    }

    private ModificationInfo update(EventData data) {
        mailSender.clear();

        return eventWebUpdater.update(
                organizer.getUserInfo(), data, NotificationsData.notChanged(),
                true, ActionInfo.webTest(masterEvent.getStartTs()));
    }

    private void assertMail(TestUserInfo user, Event event, MailType mailType) {
        Assert.some(mailType, mailSender.findEventMailType(event.getId(), user.getEmail()));
    }
}
