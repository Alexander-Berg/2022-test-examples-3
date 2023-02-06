package ru.yandex.calendar.logic.event;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.ics.EventInstanceStatusInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.misc.test.Assert;

public class EventRoutinesGetEventStatusTest extends AbstractDbDataTest {

    @Autowired
    private EventInstanceStatusChecker eventInstanceStatusChecker;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private TestManager testManager;

    @Test
    public void notFound() throws Exception {
        IcsEventSynchData si = new IcsEventSynchData(Option.of("sfsfksjdflsjfkljs121213131"), Option.<Instant>empty(), 0, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.CALDAV);
        Assert.A.isTrue(status.isNotFound());
        si = new IcsEventSynchData(Option.of("sdfkjslkfjsjf1239871837913"), Option.of(new Instant()), 0, new Instant(), new Instant());
        status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.CALDAV);
        Assert.A.isTrue(status.isNotFound());
    }

    @Test
    public void alreadyUpdated() throws Exception {
        Event event = testManager.createSingleEventWithSeq();
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(event.getId())), Option.<Instant>empty(), TestManager.SEQUENCE-1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.WEB_ICS);
        Assert.A.isTrue(status.isAlreadyUpdated());
    }

    @Test
    public void fromCaldavAlreadyUpdated() throws Exception {
        Event event = testManager.createSingleEventWithSeq();
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(event.getId())), Option.<Instant>empty(), TestManager.SEQUENCE-1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.CALDAV);
        Assert.A.isTrue(status.isFromCaldavAlreadyUpdated());
    }

    @Test
    public void needToUpdate() throws Exception {
        Event event = testManager.createSingleEventWithSeq();
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(event.getId())), Option.<Instant>empty(), TestManager.SEQUENCE+1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.CALDAV);
        Assert.A.isTrue(status.isNeedToUpdate());
    }

    @Test
    public void updated2() throws Exception {
        Event event = testManager.createSingleEventWithSeq();
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(event.getId())), Option.of(TestDateTimes.moscow(2010, 4, 20, 12, 30)), TestManager.SEQUENCE-1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.WEB_ICS);
        Assert.A.isTrue(status.isNotFound());
    }

    @Test
    public void needToUpdate2() throws Exception {
        Event event = testManager.createSingleEventWithSeq();
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(event.getId())), Option.of(TestDateTimes.moscow(2010, 4, 20, 12, 30)), TestManager.SEQUENCE+1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.CALDAV);
        Assert.A.isTrue(status.isNotFound());
    }

    @Test
    public void updatedRecur() throws Exception {
        Event recurrence = testManager.createEventWithRepetitionAndRecurInstWithSeq()._2;
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(recurrence.getId())), recurrence.getRecurrenceId(), TestManager.SEQUENCE-1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.WEB_ICS);
        Assert.A.isTrue(status.isAlreadyUpdated());
    }

    @Test
    public void needToUpdateRecur() throws Exception {
        Event recurrence = testManager.createEventWithRepetitionAndRecurInstWithSeq()._2;
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(recurrence.getId())), recurrence.getRecurrenceId(), TestManager.SEQUENCE+1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.WEB_ICS);
        Assert.A.isTrue(status.isNeedToUpdate());
    }

    @Test
    public void updatedRecur2() throws Exception {
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurInstWithSeq();
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(events._1.getId())), Option.<Instant>empty(), TestManager.SEQUENCE-1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.WEB_ICS);
        Assert.A.isTrue(status.isAlreadyUpdated());
    }

    @Test
    public void needToUpdateRecur2() throws Exception {
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurInstWithSeq();
        IcsEventSynchData si = new IcsEventSynchData(Option.of(eventDao.findExternalIdByEventId(events._1.getId())), Option.<Instant>empty(), TestManager.SEQUENCE+1, new Instant(), new Instant());
        EventInstanceStatusInfo status = eventInstanceStatusChecker.getStatusByParticipantsAndIcsSyncData(UidOrResourceId.user(TestManager.UID), si, Cf.<UidOrResourceId>list(), ActionSource.CALDAV);
        Assert.A.isTrue(status.isNeedToUpdate());
    }
}
