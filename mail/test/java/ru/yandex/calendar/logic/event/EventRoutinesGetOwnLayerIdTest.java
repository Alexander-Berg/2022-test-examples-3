package ru.yandex.calendar.logic.event;

import ru.yandex.misc.test.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;

public class EventRoutinesGetOwnLayerIdTest extends AbstractDbDataTest {

    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private TestManager testManager;

    private void checkLayerId(long eId, long layerId) {
        Option<Long> lId = eventRoutines.getOwnLayerId(TestManager.UID, eId);
        Assert.A.equals(lId.get().longValue(), layerId, "Incorrect layer Id");
    }

    // test event with rrule and recurrence_id
    @Test
    public void test() throws Exception {
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(TestManager.UID);
        long layerId = eventLayerDao.findEventLayersByEventId(events._1.getId()).single().getLayerId();
        checkLayerId(events._1.getId(), layerId);
        checkLayerId(events._2.getId(), layerId);
    }
}
