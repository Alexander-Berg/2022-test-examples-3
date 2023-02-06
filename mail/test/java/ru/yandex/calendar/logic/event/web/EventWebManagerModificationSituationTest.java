package ru.yandex.calendar.logic.event.web;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class EventWebManagerModificationSituationTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebManager eventWebManager;

    @Test
    public void thisAndFutureForActualInstance() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(112);
        DateTime start = MoscowTime.dateTime(2017, 6, 2, 22, 0);

        Event event = testManager.createDefaultEvent(user.getUid(), "Future", start);
        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        ModificationItem item = eventWebManager.getModificationItem(
                event.getId(), Option.of(start.plusDays(2).toInstant()),
                Option.of(user.getUid()), Option.empty(), true, ActionInfo.webTest(start.toInstant()));

        Assert.equals(ModificationSituation.THIS_AND_FUTURE, eventWebManager.chooseModificationSituation(
                item, true, true, ActionInfo.webTest(start.toInstant())));

        Assert.equals(ModificationSituation.THIS_AND_FUTURE, eventWebManager.chooseModificationSituation(
                item, true, true, ActionInfo.webTest(start.plusDays(1).plusMinutes(30).toInstant())));

        Assert.equals(ModificationSituation.MAIN_INST_AND_FUTURE, eventWebManager.chooseModificationSituation(
                item, true, true, ActionInfo.webTest(start.plusDays(1).plusHours(1).toInstant())));

        Assert.equals(ModificationSituation.MAIN_INST_AND_FUTURE, eventWebManager.chooseModificationSituation(
                item, true, true, ActionInfo.webTest(start.plusDays(2).toInstant())));
    }

    @Test
    public void thisAndFutureForActualRecurrence() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(112);
        DateTime start = MoscowTime.dateTime(2017, 6, 2, 12, 0);

        Event master = testManager.createDefaultEvent(user.getUid(), "Future", start);
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        Event newEvent = master.copy();
        newEvent.setStartTs(start.plusDays(1).plusHours(3).toInstant());
        newEvent.setEndTs(start.plusDays(1).plusHours(4).toInstant());

        EventData eventData = new EventData();
        eventData.setEvent(newEvent);
        eventData.setInstanceStartTs(start.plusDays(1).toInstant());

        eventWebManager.updateLite(
                user.getUserInfo(), eventData, Option.empty(), ActionInfo.webTest(start.toInstant()));

        ModificationItem item = eventWebManager.getModificationItem(
                master.getId(), Option.of(start.plusDays(2).toInstant()),
                Option.of(user.getUid()), Option.empty(), true, ActionInfo.webTest(start.toInstant()));

        Assert.equals(ModificationSituation.THIS_AND_FUTURE, eventWebManager.chooseModificationSituation(
                item, true, true, ActionInfo.webTest(start.toInstant())));

        Assert.equals(ModificationSituation.THIS_AND_FUTURE, eventWebManager.chooseModificationSituation(
                item, true, true, ActionInfo.webTest(start.plusDays(1).toInstant())));

        Assert.equals(ModificationSituation.THIS_AND_FUTURE, eventWebManager.chooseModificationSituation(
                item, true, true, ActionInfo.webTest(start.plusDays(1).plusHours(1).toInstant())));

        Assert.equals(ModificationSituation.MAIN_INST_AND_FUTURE, eventWebManager.chooseModificationSituation(
                item, true, true, ActionInfo.webTest(start.plusDays(1).plusHours(4).toInstant())));
    }
}
