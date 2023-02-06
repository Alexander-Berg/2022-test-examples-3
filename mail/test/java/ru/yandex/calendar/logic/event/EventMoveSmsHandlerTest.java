package ru.yandex.calendar.logic.event;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.repetition.EventAndRepetition;
import ru.yandex.calendar.logic.event.repetition.EventInstanceInterval;
import ru.yandex.calendar.logic.notification.SmsNotificationManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class EventMoveSmsHandlerTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventMoveSmsHandler handler;
    @Autowired
    private SmsNotificationManager smsNotificationManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private MainEventDao mainEventDao;

    private SmsNotificationManager senderMock = Mockito.mock(SmsNotificationManager.class);

    @Test
    public void unchangedRecurrence() {
        DateTime now = MoscowTime.dateTime(2017, 6, 14, 14, 0);
        PassportUid uid = testManager.prepareRandomYaTeamUser(2).getUid();

        Event master = testManager.createDefaultEvent(uid, "Event", now.minusDays(1));
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        EventAndRepetition masterAndRepetition = eventDbManager.getEventAndRepetitionById(master.getId());
        MainEvent mainEvent = mainEventDao.findMainEventById(master.getMainEventId());

        Event recurrence = testManager.createDefaultRecurrence(uid, master.getId(), now);

        Mockito.doThrow(new AssertionError("Unexpected")).when(senderMock).submitEventMovingSmss(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        handler.handleEventUpdate(uid,
                masterAndRepetition.getClosestInterval(recurrence.getRecurrenceId().get()),
                Option.of(mainEvent), ActionInfo.webTest(now));
    }

    @Test
    public void movedRecurrenceCreation() {
        DateTime now = MoscowTime.dateTime(2017, 6, 14, 14, 0);
        PassportUid uid = testManager.prepareRandomYaTeamUser(2).getUid();

        Event master = testManager.createDefaultEvent(uid, "Event", now.minusDays(1));
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        EventAndRepetition masterAndRepetition = eventDbManager.getEventAndRepetitionById(master.getId());
        MainEvent mainEvent = mainEventDao.findMainEventById(master.getMainEventId());

        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.doNothing().when(senderMock).submitEventMovingSmss(
                Mockito.any(), Mockito.any(), Mockito.any(), startCaptor.capture(), Mockito.any(), Mockito.any());

        Event recurrence = testManager.createDefaultRecurrence(uid, master.getId(), now, Duration.standardHours(1));

        handler.handleEventUpdate(uid,
                masterAndRepetition.getClosestInterval(recurrence.getRecurrenceId().get()),
                Option.of(mainEvent), ActionInfo.webTest(now));

        Assert.equals(recurrence.getStartTs(), startCaptor.getValue());

        now = now.plusDays(1);
        recurrence = testManager.createDefaultRecurrence(uid, master.getId(), now, Duration.standardHours(-2));

        handler.handleEventUpdate(uid,
                masterAndRepetition.getClosestInterval(recurrence.getRecurrenceId().get()),
                Option.of(mainEvent), ActionInfo.webTest(now));

        Assert.equals(now.plusDays(1).toInstant(), startCaptor.getValue());
    }

    @Test
    public void movedTail() {
        DateTime now = MoscowTime.dateTime(2017, 6, 14, 14, 0);
        PassportUid uid = testManager.prepareRandomYaTeamUser(2).getUid();

        Event master = testManager.createDefaultEvent(uid, "Master", now.minusDays(2));
        testManager.linkRepetitionToEvent(master.getId(), testManager.createDailyRepetitionWithDueTs(now.toInstant()));

        Event tail = testManager.createDefaultEvent(uid, "Tail", now.plus(Duration.standardHours(1)));
        testManager.createDailyRepetitionAndLinkToEvent(tail.getId());

        ListF<EventInstanceInterval> instance = Cf.list(new EventInstanceInterval(
                eventDbManager.getEventAndRepetitionById(master.getId()),
                new InstantInterval(now.toInstant(), Duration.standardHours(1))));

        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.doNothing().when(senderMock).submitEventMovingSmss(
                Mockito.any(), Mockito.any(), Mockito.any(), startCaptor.capture(), Mockito.any(), Mockito.any());

        handler.handleEventUpdate(uid, instance, mainEventDao.findMainEventsByIds(
                Cf.list(master.getMainEventId(), tail.getMainEventId())), ActionInfo.webTest(now));

        Assert.equals(tail.getStartTs(), startCaptor.getValue());
    }

    @Before
    public void setup() {
        handler.setSmsNotificationManagerForTest(senderMock);
    }

    @After
    public void teardown() {
        handler.setSmsNotificationManagerForTest(smsNotificationManager);
    }
}
