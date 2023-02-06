package ru.yandex.calendar.logic.ics.imp;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.logic.event.MainEventWithRelations;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVEventGroup;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsRecurrenceId;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author gutman
 */
public class IcsEventChangesFinderTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private MainEventDao mainEventDao;

    @Test
    public void updateTimeOfRecurrence() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-13811");

        Tuple2<Event,Event> t = testManager.createEventWithRepetitionAndRecurrence(organizer.getUid());
        Event master = t._1;
        Event recurrence = t._2;

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(master.getId()));
        vevent = vevent.withSummary("");
        vevent = vevent.withDtStart(master.getStartTs());
        vevent = vevent.withDtEnd(master.getEndTs());
        vevent = vevent.withSequenece(1);

        IcsVEvent recurrenceVevent = vevent.withRecurrence(recurrence.getRecurrenceId().get());
        recurrenceVevent = recurrenceVevent.withDtStart(recurrence.getStartTs().plus(Duration.standardHours(1)));
        recurrenceVevent = recurrenceVevent.withDtEnd(recurrence.getEndTs().plus(Duration.standardHours(1)));

        MainEventWithRelations mainEvent = eventDbManager.getMainEventWithRelationsByMainEvent(mainEventDao.findMainEventByEventId(master.getId()));
        IcsEventChangesInfo changes = IcsEventChangesFinder.changes(mainEvent, new IcsVEventGroup(
                        Option.of(mainEvent.getMainEvent().getExternalId()), Cf.list(vevent, recurrenceVevent)),
                IcsVTimeZones.fallback(MoscowTime.TZ));

        Assert.isEmpty(changes.getDeleteEvents());
        Assert.assertContains(changes.getUpdateEvents().get1().map(EventWithRelations::getId), recurrence.getId());
    }

    @Test
    public void updateRecurrenceOfAllDayEvent() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-13812").getUid();

        DateTimeZone eventTz = MoscowTime.TZ;
        LocalDate startDay = new LocalDate(2012, 8, 5);
        DateTime start = startDay.toDateTimeAtStartOfDay(eventTz);

        Event data = new Event();
        data.setStartTs(start.toInstant());
        data.setEndTs(start.plusDays(1).toInstant());

        Event master = testManager.createDefaultEvent(uid, "allDayMaster", data);
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        data.setRecurrenceId(start.plusDays(3).toInstant());
        data.setStartTs(start.plusDays(3).toInstant());
        data.setEndTs(start.plusDays(4).toInstant());
        Event recurrence = testManager.createDefaultEvent(uid, "allDayRecurrence", data, master.getMainEventId());

        testManager.updateEventTimezone(master.getId(), eventTz);
        MainEventWithRelations mainEvent = eventDbManager.getMainEventWithRelationsById(master.getMainEventId());

        IcsVEvent masterVevent = new IcsVEvent();
        masterVevent = masterVevent.withUid(mainEvent.getMasterEvents().single().getExternalId());
        masterVevent = masterVevent.withDtStart(startDay);
        masterVevent = masterVevent.withDtEnd(startDay.plusDays(1));

        IcsVEvent recurrenceVevent = masterVevent.withRecurrence(new IcsRecurrenceId(startDay.plusDays(3)));
        recurrenceVevent = recurrenceVevent.withDtStart(startDay.plusDays(3));
        recurrenceVevent = recurrenceVevent.withDtEnd(startDay.plusDays(4));

        IcsEventChangesInfo changes = IcsEventChangesFinder.changes(mainEvent, new IcsVEventGroup(
                        Option.of(mainEvent.getMainEvent().getExternalId()), Cf.list(masterVevent, recurrenceVevent)),
                IcsVTimeZones.fallback(MoscowTime.TZ));

        Assert.isEmpty(changes.getDeleteEvents());
        Assert.in(recurrence.getId(), changes.getUpdateEvents().get1().map(EventWithRelations::getId));
    }

}
