package ru.yandex.calendar.logic.event.web;

import java.util.List;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.DeletedOccurrenceInfoType;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ResourceParticipantInfo;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author akirakozov
 */
public class EventWithResourceWebRemoverTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EventWebRemover eventWebRemover;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private EventResourceDao eventResourceDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private DateTimeManager dateTimeManager;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;

    @Test
    public void removeOccurrence() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11011");

        Instant now = TestDateTimes.moscow(2011, 11, 21, 21, 28);
        long layer = testManager.createDefaultLayerForUser(user.getUid(), now.minus(Duration.standardDays(1)));

        Event event = testManager.createDefaultEvent(user.getUid(), "removeOccurrence");
        Instant dueTs = TestDateTimes.plusDays(event.getStartTs(), 2);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(event.getId(), dueTs);
        testManager.addUserParticipantToEvent(event.getId(), user.getLogin(), Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        InstantInterval interval = new InstantInterval(event.getStartTs(), dueTs);
        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                TestManager.testExchangeThreeLittlePigsEmail, interval);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest());
        ResourceParticipantInfo resourceParticipant =
            resourceDao.findResourceParticipants(Cf.list(event.getId())).single();
        Assert.A.isTrue(resourceParticipant.getExchangeId().isPresent());

        // remove first occurrence of event
        eventWebRemover.remove(
                user.getUserInfo(), event.getId(), Option.of(event.getStartTs()),
                false, ActionInfo.webTest(now));
        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();

        CalendarItemType updatedItem = ewsProxyWrapper.getEvent(exchangeId).get();
        List<DeletedOccurrenceInfoType> deletedOccurences =
                updatedItem.getDeletedOccurrences().getDeletedOccurrence();
        Assert.A.hasSize(1, deletedOccurences);

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(layer), now);
    }

    // CAL-6208
    @Test
    public void removeRecurrenceWithoutExchangeId() {
        Resource pigs = testManager.cleanAndCreateThreeLittlePigs();
        Email pigsEmail = TestManager.testExchangeThreeLittlePigsEmail;

        TestUserInfo user = testManager.prepareRandomYaTeamUser(2013);

        Event master = testManager.createDefaultEvent(user.getUid(), "removeRecurrenceWithoutExchangeId");
        Instant dueTs = master.getStartTs().plus(Duration.standardDays(5));

        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(master.getId(), dueTs);
        testManager.addUserParticipantToEvent(master.getId(), user, Decision.YES, true);
        testManager.addResourceParticipantToEvent(master.getId(), pigs);

        String externalId = eventDao.findExternalIdByEventId(master.getId());

        Event recurrence = testManager.createDefaultRecurrence(user.getUid(),
                master.getId(), master.getStartTs().plus(Duration.standardDays(3)));
        InstantInterval recurrenceInterval = EventRoutines.getInstantInterval(recurrence);

        InstantInterval interval = new InstantInterval(master.getStartTs(), dueTs);
        ewsProxyWrapper.cancelMasterAndSingleMeetings(pigsEmail, interval);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(master.getId(), ActionInfo.webTest());

        Assert.isEmpty(eventResourceDao.findExchangeIds(Cf.list(recurrence.getId())));
        Assert.notEmpty(ewsProxyWrapper.findInstanceEventIdsByExternalId(pigsEmail, recurrenceInterval, externalId));

        eventWebRemover.remove(user.getUserInfo(), recurrence.getId(), Option.empty(), false, ActionInfo.webTest());

        Assert.isEmpty(ewsProxyWrapper.findInstanceEventIdsByExternalId(pigsEmail, recurrenceInterval, externalId));
    }
}
