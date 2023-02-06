package ru.yandex.calendar.logic.event;

import org.joda.time.Duration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function0;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.repetition.EventIndentAndRepetition;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author dbrylev
 */
public class EventInfoDbLoaderTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventInfoDbLoader eventInfoDbLoader;

    // CAL-7202
    @Test
    public void excludedInstance() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(1);

        Event master = testManager.createDefaultEvent(user.getUid(), "Repeating");
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());
        testManager.addUserParticipantToEvent(master.getId(), user.getUid(), Decision.YES, false);
        testManager.updateEventTimeIndents(master);

        InstantInterval interval = new InstantInterval(
                master.getStartTs().plus(Duration.standardMinutes(30)),
                master.getEndTs().plus(Duration.standardMinutes(30)));

        Function0<ListF<EventIndentAndRepetition>> loadIndents = () -> eventInfoDbLoader.getEventIndentsOnLayers(
                Cf.list(user.getDefaultLayerId()), EventLoadLimits.intersectsInterval(interval));

        Assert.equals(master.getId(), loadIndents.apply().single().getEventId());

        Event recurrence = testManager.createDefaultRecurrence(user.getUid(), master.getId(), master.getStartTs());
        testManager.addUserParticipantToEvent(recurrence.getId(), user.getUid(), Decision.YES, false);
        testManager.updateEventTimeIndents(recurrence);

        Assert.equals(recurrence.getId(), loadIndents.apply().single().getEventId());
    }
}
