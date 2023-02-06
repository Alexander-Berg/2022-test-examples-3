package ru.yandex.calendar.logic.event.repetition;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventHelper;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.TimeUtils;

public class RepetitionRoutinesGetIntervalsTest extends AbstractDbDataTest {

    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private GenericBeanDao genericBeanDao;
    @Autowired
    private TestManager testManager;

    private void run(long startMs, DateTime dtSt, DateTime dtEnd, long eventId) {
        Event e = genericBeanDao.loadBeanById(EventHelper.INSTANCE, eventId);
        RepetitionInstanceInfo rii = repetitionRoutines.getRepetitionInstanceInfoByEvent(e);
        ListF<InstantInterval> intervals = RepetitionUtils.getIntervals(
            rii, new Instant(startMs), Option.<Instant>empty(), false, 1
        );
        Assert.A.hasSize(1, intervals);
        Assert.A.equals(dtSt.toInstant(), intervals.single().getStart());
        Assert.A.equals(dtEnd.toInstant(), intervals.single().getEnd());
    }

    @Test
    public void eventWithRepetitionExdateAndLimit() throws Exception {
        Tuple2<Event, Rdate> event = testManager.createEventWithRepetitionAndExdate(TestManager.UID);
        DateTime dtSt = new DateTime(2009, 4, 30, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        DateTime dtEnd = new DateTime(2009, 4, 30, 12, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        run(event._2.getStartTs().getMillis(), dtSt, dtEnd, event._1.getId());
    }

    @Test
    public void eventWithRepetitionRecurAndLimit() throws Exception {
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(TestManager.UID);
        DateTime dt = new DateTime(2009, 4, 28, 0, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        DateTime dtSt = new DateTime(2009, 4, 29, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        DateTime dtEnd = new DateTime(2009, 4, 29, 12, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        run(dt.getMillis(), dtSt, dtEnd, events._1.getId());
    }

    @Test
    public void eventRdateAndLimit() throws Exception {
        Tuple2<Event, Rdate> event = testManager.createEventWithRdate(TestManager.UID);
        DateTime dt = new DateTime(event.get1().getStartTs(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        DateTime dtSt = new DateTime(event.get1().getStartTs(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        DateTime dtEnd = new DateTime(event.get1().getEndTs(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        run(dt.getMillis(), dtSt, dtEnd, event._1.getId());
    }

}
