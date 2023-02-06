package ru.yandex.calendar.frontend.ews.exp;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemCreateOrDeleteOperationType;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.*;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.event.*;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.repetition.EventAndRepetition;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class EwsExportRoutinesDetachEventsTest extends AbstractConfTest {
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private EventUserRoutines eventUserRoutines;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventRoutines eventRoutines;

    private static final Instant pastRecurrenceTs = new DateTime(2017, 11, 25, 10, 0, 0, 0, TestManager.chrono).toInstant();
    private static final Instant futureRecurrenceTs = new DateTime(2017, 11, 28, 10, 0, 0, 0, TestManager.chrono).toInstant();
    private static final Instant anotherFutureRecurrenceTs = new DateTime(2017, 11, 30, 10, 0, 0, 0, TestManager.chrono).toInstant();

    private Event masterEvent;
    private Event pastRecurrence;
    private Event futureRecurrence;
    private Event anotherFutureRecurrence;

    private MainEvent mainEvent;
    private ListF<EventAndRepetition> eventAndRepetitions;

    private DateTime nowDt = new DateTime(2017, 11, 27, 10, 0, 0, 0, TestManager.chrono);
    private Instant nowTs = nowDt.toInstant();

    private ActionInfo actionInfo = new ActionInfo(ActionSource.WEB, "detach", nowTs);

    private TestUserInfo organizer;
    private TestUserInfo subscriber;

    @Before
    public void setup() {
        organizer = testManager.prepareRandomYaTeamUser(934);
        subscriber = testManager.prepareRandomYaTeamUser(935);
        testManager.updateIsEwser(subscriber);

        masterEvent = testManager.createEventWithDailyRepetition(organizer.getUid());

        pastRecurrence = testManager.createDefaultRecurrence(
                organizer.getUid(), masterEvent.getId(), pastRecurrenceTs);
        futureRecurrence = testManager.createDefaultRecurrence(
                organizer.getUid(), masterEvent.getId(), futureRecurrenceTs);
        anotherFutureRecurrence = testManager.createDefaultRecurrence(
                masterEvent.getCreatorUid(), masterEvent.getId(), anotherFutureRecurrenceTs);

        mainEvent = mainEventDao.findMainEventById(futureRecurrence.getMainEventId());
        eventAndRepetitions = eventDbManager.getEventsAndRepetitionsByMainEventId(mainEvent.getId());
    }

    @Test
    public void detachMaster() {
        attachEventToSubscriber(masterEvent);
        attachEventToSubscriber(pastRecurrence);
        attachEventToSubscriber(futureRecurrence);

        MapF<String, Boolean> detachEwsCalls = detach(subscriber.getUid());

        Assert.hasSize(1, detachEwsCalls);
        Assert.isFalse(detachEwsCalls.getTs(getUserExchangeId(masterEvent)));
    }

    @Test
    public void detachRecurrences() {
        testManager.addUserParticipantToEvent(anotherFutureRecurrence.getId(), subscriber, Decision.YES, false);

        attachEventToSubscriber(pastRecurrence);
        attachEventToSubscriber(futureRecurrence);
        attachEventToSubscriber(anotherFutureRecurrence);

        MapF<String, Boolean> detachEwsCalls = detach(subscriber.getUid());

        Assert.hasSize(2, detachEwsCalls);
        Assert.isFalse(detachEwsCalls.getTs(getUserExchangeId(futureRecurrence)));
        Assert.isTrue(detachEwsCalls.getTs(getUserExchangeId(anotherFutureRecurrence)));
    }

    @Test
    public void detachMasterWithAttendeeRecurrence() {
        testManager.addUserParticipantToEvent(anotherFutureRecurrence.getId(), subscriber, Decision.YES, false);

        attachEventToSubscriber(masterEvent);
        attachEventToSubscriber(futureRecurrence);
        attachEventToSubscriber(anotherFutureRecurrence);

        MapF<String, Boolean> detachEwsCalls = detach(subscriber.getUid());

        Assert.hasSize(1, detachEwsCalls);
        Assert.isTrue(detachEwsCalls.getTs(getUserExchangeId(masterEvent)));
    }

    private void addParticipant(Event event, TestUserInfo user, Decision decision, boolean isOrganizer) {
        testManager.addUserParticipantToEvent(event.getId(), user, decision, isOrganizer);
    }

    @Test
    public void updateDecisionForAttendeeInMasterAndRecurrenceViaEws() {
        val master = testManager.createMeetingWithRepetitionDueAndRecurrenceIdO(organizer.getUid(), "test-meeting",
            nowDt, RegularRepetitionRule.DAILY, 1,
            Cf.list(Either.left(subscriber.getUid())), Option.empty())._1.get(0);

        val pastRec = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), pastRecurrenceTs);
        val futureRec = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), futureRecurrenceTs);

        addParticipant(pastRec, organizer, Decision.YES, true);
        addParticipant(futureRec, organizer, Decision.YES, true);

        addParticipant(pastRec, subscriber, Decision.UNDECIDED, false);
        addParticipant(futureRec, subscriber, Decision.UNDECIDED, false);

        val ids = Cf.list(master.getId(), pastRec.getId(), futureRec.getId());
        eventRoutines.updateDecisionViaEwsOrElseCreateMailIfNeeded(ids, subscriber.getUid(), Decision.YES, Option.empty(), actionInfo);
    }

    private MapF<String, Boolean> detach(PassportUid uid) {
        EwsProxyWrapper mockEwsProxyWrapper = Mockito.mock(EwsProxyWrapper.class);

        ArgumentCaptor<String> exchangeIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CalendarItemCreateOrDeleteOperationType> operationTypeCaptor =
                ArgumentCaptor.forClass(CalendarItemCreateOrDeleteOperationType.class);

        Mockito.when(mockEwsProxyWrapper.setUserDecision(exchangeIdCaptor.capture(), Mockito.any(),
                Mockito.any(), Mockito.any(), operationTypeCaptor.capture(), Mockito.any())).thenReturn(true);

        try {
            ewsExportRoutines.setEwsProxyWrapperForTest(mockEwsProxyWrapper);
            ewsExportRoutines.detachEvents(eventAndRepetitions, mainEvent, uid, actionInfo);
        } finally {
            ewsExportRoutines.setEwsProxyWrapperForTest(ewsProxyWrapper);
        }

        return Cf.x(exchangeIdCaptor.getAllValues())
                .zip(Cf.x(operationTypeCaptor.getAllValues())
                        .map(op -> !op.equals(CalendarItemCreateOrDeleteOperationType.SEND_TO_NONE))).toMap();
    }

    private void attachEventToSubscriber(Event event) {
        EventUser eventUser = new EventUser();
        eventUser.setExchangeId(getUserExchangeId(event));
        eventUserRoutines.createOrUpdateEventUserAndCreateNotifications(
                subscriber.getUid(), eventDbManager.getEventAndRepetitionByEvent(event),
                eventUser, NotificationsData.useLayerDefaultIfCreate(),
                subscriber.getDefaultLayerId(), ActionInfo.webTest());
    }

    private String getUserExchangeId(Event event) {
        return "mockExchangeId_" + event.getId();
    }
}
