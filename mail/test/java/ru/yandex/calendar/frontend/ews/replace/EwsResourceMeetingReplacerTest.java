package ru.yandex.calendar.frontend.ews.replace;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.SetItemFieldType;
import com.microsoft.schemas.exchange.services._2006.types.UnindexedFieldURIType;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.exp.EwsModifyingItemId;
import ru.yandex.calendar.frontend.ews.proxy.EwsActionLogData;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxy;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.frontend.ews.proxy.ExchangeIdLogData;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author Daniel Brylev
 */
public class EwsResourceMeetingReplacerTest extends AbstractConfTest {

    @Autowired
    private EwsResourceMeetingReplacer ewsResourceMeetingReplacer;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EwsProxy ewsProxy;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EventResourceDao eventResourceDao;

    private final Instant masterEventStart = MoscowTime.instant(2012, 8, 29, 20, 0);
    private final Instant masterEventDue = masterEventStart.plus(Duration.standardDays(3));

    private final ActionInfo actionInfo = ActionInfo.webTest(masterEventStart.minus(77777));

    private TestUserInfo user;
    private Resource resource1;
    private Resource resource2;

    @Before
    public void cleanBeforeTest() {
        user = testManager.prepareYandexUser(TestManager.createAkirakozov());
        resource1 = testManager.cleanAndCreateThreeLittlePigs();
        resource2 = testManager.cleanAndCreateSmolny();

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                resourceRoutines.getExchangeEmail(resource1), masterEventStart, masterEventDue);
        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                resourceRoutines.getExchangeEmail(resource2), masterEventStart, masterEventDue);
    }

    @Test
    public void replaceSingleMeetings() {
        Assert.isFalse(existsMeetingStartingIn(resource1, masterEventStart));
        Assert.isFalse(existsMeetingStartingIn(resource2, masterEventStart));

        Event master = createSingleFromExchangeAndExport(Cf.list(resource1, resource2), "replaceSingleMeetings");

        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource1, masterEventStart));
        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource2, masterEventStart));

        ewsResourceMeetingReplacer.replaceByMainEventId(master.getMainEventId());

        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource1, masterEventStart));
        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource2, masterEventStart));
    }

    @Test
    public void ignoreNotFoundMeetings() {
        Event master = createSingleFromExchangeAndExport(Cf.list(resource1, resource2), "ignoreNotFoundMeetings");

        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource1, masterEventStart));
        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource2, masterEventStart));

        ewsProxyWrapper.cancelMeetingsSafe(Cf.list(ExchangeIdLogData.test(findExchangeId(resource1, master))), ActionInfo.exchangeTest());
        Assert.isFalse(existsMeetingStartingIn(resource1, masterEventStart));

        ewsResourceMeetingReplacer.replaceByMainEventId(master.getMainEventId());
        Assert.isFalse(existsMeetingStartingIn(resource1, masterEventStart));
        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource2, masterEventStart));
    }

    @Test
    public void ignoreAlreadyReplaced() {
        Event master = createSingleFromExchangeAndExport(Cf.list(resource1), "ignoreAlreadyReplaced");
        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource1, masterEventStart));

        ewsResourceMeetingReplacer.replaceByMainEventId(master.getMainEventId());
        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource1, masterEventStart));

        String exchangeId = findExchangeId(resource1, master);

        ewsResourceMeetingReplacer.replaceByMainEventId(master.getMainEventId());
        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource1, masterEventStart));

        Assert.equals(exchangeId, findExchangeId(resource1, master));
    }

    @Test
    public void foundByExternalId() {
        Event master = createRepeatingFromExchangeAndExport(Cf.list(resource1), "foundByExternalId");

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(1));
        Event recurrence = createRecurrenceFromExchangeAndExport(Cf.list(resource1, resource2), master, recurrenceId);

        String masterExchangeId = findExchangeId(resource1, master);
        String recurrenceExchangeId = findExchangeId(resource1, recurrence);
        String singleExchangeId = findExchangeId(resource2, recurrence);

        eventResourceDao.saveUpdateExchangeId(resource1.getId(), master.getId(), null, ActionInfo.webTest());
        eventResourceDao.saveUpdateExchangeId(resource1.getId(), recurrence.getId(), null, ActionInfo.webTest());
        eventResourceDao.saveUpdateExchangeId(resource2.getId(), recurrence.getId(), null, ActionInfo.webTest());

        ListF<String> foundIds = ewsResourceMeetingReplacer
                .findResourcesMeetingsCreatedByExchangeByMainEventId(master.getMainEventId())
                .map(ExchangeResourceMeeting.getExchangeIdF());

        Assert.unique(foundIds);
        Assert.equals(Cf.set(masterExchangeId, recurrenceExchangeId, singleExchangeId), foundIds.unique());
    }

    @Test
    public void replaceOneTimeParticipation() {
        Event master = createRepeatingFromExchangeAndExport(Cf.list(resource1), "replaceOneTimeParticipation");

        Instant recurrence1Id = masterEventStart.plus(Duration.standardDays(1));
        createRecurrenceFromExchangeAndExport(Cf.list(resource1, resource2), master, recurrence1Id);

        Instant recurrence2Id = masterEventStart.plus(Duration.standardDays(2));
        createRecurrenceFromExchangeAndExport(Cf.list(resource2), master, recurrence2Id);

        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource1, masterEventStart));
        Assert.isFalse(existsMeetingStartingIn(resource2, masterEventStart));

        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource1, recurrence1Id));
        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource2, recurrence1Id));

        Assert.isFalse(existsMeetingStartingIn(resource1, recurrence2Id));
        Assert.isTrue(meetingStartingInWasCreatedFromExchange(resource2, recurrence2Id));

        ewsResourceMeetingReplacer.replaceByMainEventId(master.getMainEventId());

        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource1, masterEventStart));
        Assert.isFalse(existsMeetingStartingIn(resource2, masterEventStart));

        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource1, recurrence1Id));
        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource2, recurrence1Id));

        Assert.isFalse(existsMeetingStartingIn(resource1, recurrence2Id));
        Assert.isTrue(meetingStartingInWasCreatedFromCalendar(resource2, recurrence2Id));
    }

    private Event createSingleFromExchangeAndExport(ListF<Resource> resources, String eventName) {
        return createMasterAndExport(resources, eventName, false, false);
    }

    private Event createRepeatingFromExchangeAndExport(ListF<Resource> resources, String eventName) {
        return createMasterAndExport(resources, eventName, true, false);
    }

    private Event createRecurrenceFromExchangeAndExport(
            ListF<Resource> resources, Event masterEvent, Instant recurrenceId)
    {
        Event recurrenceEvent = testManager.createDefaultRecurrence(user.getUid(), masterEvent.getId(), recurrenceId);
        addParticipantsAndExport(recurrenceEvent, resources, false);

        return recurrenceEvent;
    }

    private boolean meetingStartingInWasCreatedFromExchange(Resource resource, Instant expectedStart) {
        return findInstanceStartingInWasCreatedFromYaTeamCal(resource, expectedStart)
                .getOrThrow("no meetings found starting in ", expectedStart).equals(false);
    }

    private boolean meetingStartingInWasCreatedFromCalendar(Resource resource, Instant expectedStart) {
        return findInstanceStartingInWasCreatedFromYaTeamCal(resource, expectedStart)
                .getOrThrow("no meetings found starting in ", expectedStart).equals(true);
    }

    private boolean existsMeetingStartingIn(Resource resource, Instant expectedStart) {
        return findInstanceStartingInWasCreatedFromYaTeamCal(resource, expectedStart).isPresent();
    }

    private Option<Boolean> findInstanceStartingInWasCreatedFromYaTeamCal(Resource resource, Instant expectedStart) {
        ListF<CalendarItemType> occurrences = ewsProxy.findInstanceEvents(
                resourceRoutines.getExchangeEmail(resource), new InstantInterval(expectedStart, expectedStart),
                Cf.<UnindexedFieldURIType>list(), Cf.list(EwsUtils.EXTENDED_PROPERTY_SOURCE));

        Assert.isFalse(occurrences.size() > 1, "more than one item found starting in ", expectedStart);

        if (occurrences.isEmpty()) {
            return Option.empty();
        }
        return Option.of(EwsUtils.convertExtendedProperties(occurrences.single()).getWasCreatedFromYaTeamCalendar());
    }

    private Event createMasterAndExport(
            ListF<Resource> resources, String eventName, boolean isRepeating, boolean fromYaTeamCalendar)
    {
        Event masterEvent = testManager.createDefaultEvent(
                user.getUid(), eventName, masterEventStart, masterEventStart.plus(Duration.standardHours(1)));

        if (isRepeating) {
            testManager.createDailyRepetitionWithDueTsAndLinkToEvent(masterEvent.getId(), masterEventDue);
        }

        addParticipantsAndExport(masterEvent, resources, fromYaTeamCalendar);

        return masterEvent;
    }

    private void addParticipantsAndExport(Event event, ListF<Resource> resources, boolean fromYaTeamCalendar) {
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);
        for (Resource resource : resources) {
            testManager.addResourceParticipantToEvent(event.getId(), resource);
        }
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), actionInfo);

        if (!fromYaTeamCalendar) {
            CalendarItemType i = new CalendarItemType();
            i.getExtendedProperty().add(EwsUtils.createExtendedProperty(EwsUtils.EXTENDED_PROPERTY_SOURCE, "XXX"));
            ListF<SetItemFieldType> changes = Cf.list(EwsUtils.createSetItemField(i, EwsUtils.EXTENDED_PROPERTY_SOURCE));

            for (Resource resource : resources) {
                String exchangeId = findExchangeId(resource, event);
                ewsProxyWrapper.updateItem(EwsModifyingItemId.fromExchangeId(exchangeId), changes, EwsActionLogData.test());
            }
        }
    }

    private String findExchangeId(Resource resource, Event event) {
        return eventResourceDao.findEventResourceByEventIdAndResourceId(event.getId(), resource.getId())
                .get().getExchangeId().get();
    }
}
