package ru.yandex.calendar.frontend.ews.exp.async;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemTypeType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.SetItemFieldType;
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
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.beans.generated.YtEwsExportingEvent;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventInfoDbLoader;
import ru.yandex.calendar.logic.event.MainEventInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

public class EwsResourceMeetingExporterTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsResourceMeetingExporter ewsResourceMeetingExporter;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EwsProxy ewsProxy;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private EventInfoDbLoader eventInfoDbLoader;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventResourceDao eventResourceDao;

    private final Instant masterEventStart = MoscowTime.instant(2016, 1, 29, 20, 0);
    private final Instant masterEventDue = masterEventStart.plus(Duration.standardDays(4));

    private final ActionInfo actionInfo = ActionInfo.webTest(masterEventStart.minus(77777));

    private TestUserInfo user;
    private Resource resource;

    @Before
    public void cleanBeforeTest() {
        user = testManager.prepareYandexUser(TestManager.createAkirakozov());
        resource = testManager.cleanAndCreateThreeLittlePigs();
        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                resourceRoutines.getExchangeEmail(resource), masterEventStart, masterEventDue);
    }

    @Test
    public void createRepeatingMeetingWithRecurrenceAndExdate() {
        Event masterEvent = testManager.createDefaultEvent(user.getUid(), "Repeating", masterEventStart);

        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(masterEvent.getId(), masterEventDue);
        testManager.addResourceParticipantToEvent(masterEvent.getId(), resource);

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(1));
        Event recurrenceEvent = testManager.createDefaultRecurrence(user.getUid(), masterEvent.getId(), recurrenceId);
        moveEventStartEnd(recurrenceEvent, Duration.standardMinutes(5));
        testManager.addResourceParticipantToEvent(recurrenceEvent.getId(), resource);

        Instant exdate = masterEventStart.plus(Duration.standardDays(2));
        testManager.createExdate(exdate, masterEvent.getId());

        Assert.isFalse(existsItemStartingIn(resource, masterEvent.getStartTs()));

        ewsResourceMeetingExporter.createMeeting(resource.getId(), loadMainEventInfo(masterEvent), actionInfo);

        Assert.isTrue(existsItemStartingIn(resource, masterEvent.getStartTs()));
        Assert.isTrue(existsExceptionItemStartingIn(resource, recurrenceEvent.getStartTs()));
        Assert.isFalse(existsItemStartingIn(resource, exdate));
    }

    @Test
    public void skipRecurrenceWithNotChangedTime() {
        Event masterEvent = testManager.createDefaultEvent(user.getUid(), "Repeating", masterEventStart);

        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(masterEvent.getId(), masterEventDue);
        testManager.addResourceParticipantToEvent(masterEvent.getId(), resource);

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(1));
        Event recurrenceEvent = testManager.createDefaultRecurrence(user.getUid(), masterEvent.getId(), recurrenceId);
        testManager.addResourceParticipantToEvent(recurrenceEvent.getId(), resource);

        Assert.isFalse(existsItemStartingIn(resource, recurrenceId));
        Assert.isFalse(existsItemStartingIn(resource, masterEventStart));

        ewsResourceMeetingExporter.createMeeting(resource.getId(), loadMainEventInfo(masterEvent), actionInfo);

        Assert.isFalse(existsExceptionItemStartingIn(resource, recurrenceId));
        Assert.isTrue(existsItemStartingIn(resource, recurrenceId));
        Assert.isTrue(existsItemStartingIn(resource, masterEventStart));
    }

    @Test
    public void skipPastOccurrences() {
        Event masterEvent = testManager.createDefaultEvent(user.getUid(), "Repeating", masterEventStart);

        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(masterEvent.getId(), masterEventDue);
        testManager.addResourceParticipantToEvent(masterEvent.getId(), resource);

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(1));
        Event recurrenceEvent = testManager.createDefaultRecurrence(user.getUid(), masterEvent.getId(), recurrenceId);
        testManager.addResourceParticipantToEvent(recurrenceEvent.getId(), resource);

        Instant exdateStart = masterEventStart.plus(Duration.standardDays(2));
        testManager.createExdate(exdateStart, masterEvent.getId());

        Assert.isFalse(existsItemStartingIn(resource, masterEventStart));
        Assert.isFalse(existsItemStartingIn(resource, recurrenceId));
        Assert.isFalse(existsItemStartingIn(resource, exdateStart));

        ActionInfo actionInfo = ActionInfo.webTest(masterEventDue);
        ewsResourceMeetingExporter.createMeeting(resource.getId(), loadMainEventInfo(masterEvent), actionInfo);

        Assert.isTrue(existsOccurrenceItemStartingIn(resource, masterEventStart));
        Assert.isTrue(existsOccurrenceItemStartingIn(resource, recurrenceId));
        Assert.isTrue(existsOccurrenceItemStartingIn(resource, exdateStart));
    }

    @Test
    public void createOneTimeParticipation() {
        Event masterEvent = testManager.createDefaultEvent(user.getUid(), "Repeating", masterEventStart);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(masterEvent.getId(), masterEventDue);

        Instant recurrenceId1 = masterEventStart.plus(Duration.standardDays(1));
        Event recurrenceEvent1 = testManager.createDefaultRecurrence(user.getUid(), masterEvent.getId(), recurrenceId1);
        testManager.addResourceParticipantToEvent(recurrenceEvent1.getId(), resource);

        Instant recurrenceId2 = masterEventStart.plus(Duration.standardDays(3));
        Event recurrenceEvent2 = testManager.createDefaultRecurrence(user.getUid(), masterEvent.getId(), recurrenceId2);
        testManager.addResourceParticipantToEvent(recurrenceEvent2.getId(), resource);

        Assert.isFalse(existsItemStartingIn(resource, recurrenceId1));
        Assert.isFalse(existsItemStartingIn(resource, recurrenceId2));

        ewsResourceMeetingExporter.createMeeting(resource.getId(), loadMainEventInfo(masterEvent), actionInfo);

        Assert.isTrue(existsSingleItemStartingIn(resource, recurrenceId1));
        Assert.isTrue(existsSingleItemStartingIn(resource, recurrenceId2));
    }

    @Test
    public void exportNoOccurrencesParticipation() {
        Event masterEvent = testManager.createDefaultEvent(user.getUid(), "Empty", masterEventStart);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(
                masterEvent.getId(), masterEventStart.plus(Duration.standardDays(1)));

        testManager.addResourceParticipantToEvent(masterEvent.getId(), resource);
        testManager.createExdate(masterEventStart, masterEvent.getId());

        exportMeeting(getExternalId(masterEvent));

        Assert.isFalse(existsItemStartingIn(resource, masterEventStart));
    }

    @Test
    public void exportCancelsConflicts() {
        Event singleConflict = testManager.createDefaultEvent(user.getUid(), "Single conflict", masterEventStart);
        addParticipants(singleConflict);
        exportMeetingFromNotYaCalendar(singleConflict);

        Instant repeatingStart = masterEventStart.plus(Duration.standardDays(1));
        Event repeatingConflict = testManager.createDefaultEvent(user.getUid(), "Repeating conflict", repeatingStart);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(repeatingConflict.getId(), masterEventDue);

        addParticipants(repeatingConflict);
        exportMeetingFromNotYaCalendar(repeatingConflict);

        String singleConflictExchangeId = findResourceExchangeId(singleConflict);
        String repeatingConflictExchangeId = findResourceExchangeId(repeatingConflict);

        Assert.hasSize(2, ewsProxyWrapper.getEvents(Cf.list(singleConflictExchangeId, repeatingConflictExchangeId)));

        Event masterEvent = testManager.createDefaultEvent(user.getUid(), "Master", masterEventStart);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(masterEvent.getId(), masterEventDue);
        addParticipants(masterEvent);

        exportMeeting(getExternalId(masterEvent));

        Assert.isEmpty(ewsProxyWrapper.getEvents(Cf.list(singleConflictExchangeId, repeatingConflictExchangeId)));
        Assert.isTrue(existsItemStartingIn(resource, masterEventStart));
    }

    // CAL-6923
    @Test
    public void exportCancelsSameUidRepeatingConflicts() {
        Event conflict = testManager.createDefaultEvent(user.getUid(), "Repeating conflict", masterEventStart);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(conflict.getId(), masterEventDue);

        addParticipants(conflict);
        exportMeeting(getExternalId(conflict));

        String conflictExchangeId = findResourceExchangeId(conflict);

        eventResourceDao.saveUpdateExchangeId(resource.getId(), conflict.getId(), null, ActionInfo.webTest());
        exportMeeting(getExternalId(conflict));

        Assert.isEmpty(ewsProxyWrapper.getEvents(Cf.list(conflictExchangeId)));
        Assert.isTrue(existsItemStartingIn(resource, masterEventStart));
    }

    private void exportMeeting(String externalId) {
        YtEwsExportingEvent exporting = new YtEwsExportingEvent();
        exporting.setExternalId(externalId);
        exporting.setLastSubmitTs(actionInfo.getNow());
        exporting.setFailureReasonNull();

        updateResourceSetAsyncWithExchange(true);
        ewsResourceMeetingExporter.exportMeeting(exporting, actionInfo.withActionSource(ActionSource.EXCHANGE_ASYNCH));
    }

    private void addParticipants(Event event) {
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);
    }

    private void exportMeetingFromNotYaCalendar(Event event) {
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), actionInfo);

        CalendarItemType i = new CalendarItemType();
        i.getExtendedProperty().add(EwsUtils.createExtendedProperty(EwsUtils.EXTENDED_PROPERTY_SOURCE, "XXX"));
        ListF<SetItemFieldType> changes = Cf.list(EwsUtils.createSetItemField(i, EwsUtils.EXTENDED_PROPERTY_SOURCE));

        String exchangeId = findResourceExchangeId(event);
        ewsProxyWrapper.updateItem(EwsModifyingItemId.fromExchangeId(exchangeId), changes, EwsActionLogData.test());
    }

    private String findResourceExchangeId(Event event) {
        return eventResourceDao.findEventResourceByEventIdAndResourceId(event.getId(), resource.getId())
                .get().getExchangeId().get();
    }

    private String getExternalId(Event event) {
        return eventDao.findExternalIdByEventId(event.getId());
    }

    private void updateResourceSetAsyncWithExchange(boolean async) {
        Resource data = new Resource();
        data.setId(resource.getId());
        data.setAsyncWithExchange(async);

        resourceRoutines.updateResource(data);
    }

    private MainEventInfo loadMainEventInfo(Event event) {
        return eventInfoDbLoader.getMainEventInfoById(
                Option.<PassportUid>empty(), event.getMainEventId(), ActionSource.UNKNOWN);
    }

    private boolean existsItemStartingIn(Resource resource, Instant expectedStart) {
        return findOccurrenceIdStartingIn(resource, expectedStart).isPresent();
    }

    private boolean existsOccurrenceItemStartingIn(Resource resource, Instant expectedStart) {
        Option<CalendarItemType> item = findOccurrenceStartingIn(resource, expectedStart);
        return item.isPresent() && item.get().getCalendarItemType() == CalendarItemTypeType.OCCURRENCE;
    }

    private boolean existsSingleItemStartingIn(Resource resource, Instant expectedStart) {
        Option<CalendarItemType> item = findOccurrenceStartingIn(resource, expectedStart);
        return item.isPresent() && item.get().getCalendarItemType() == CalendarItemTypeType.SINGLE;
    }

    private boolean existsExceptionItemStartingIn(Resource resource, Instant expectedStart) {
        Option<CalendarItemType> item = findOccurrenceStartingIn(resource, expectedStart);
        return item.isPresent() && item.get().getCalendarItemType() == CalendarItemTypeType.EXCEPTION;
    }

    private Option<CalendarItemType> findOccurrenceStartingIn(Resource resource, Instant expectedStart) {
        Option<ItemIdType> itemId = findOccurrenceIdStartingIn(resource, expectedStart);

        if (!itemId.isPresent()) return Option.empty();

        return ewsProxy.getEvents(itemId, false).singleO();
    }

    private Option<ItemIdType> findOccurrenceIdStartingIn(Resource resource, Instant expectedStart) {
        ListF<CalendarItemType> occurrences = ewsProxy.findInstanceEvents(
                resourceRoutines.getExchangeEmail(resource), new InstantInterval(expectedStart, expectedStart), true);
        Assert.isFalse(occurrences.size() > 1, "more than one item starting in " + expectedStart + " found");

        return occurrences.singleO().map(EwsUtils.calendarItemItemIdF());
    }

    private void moveEventStartEnd(Event event, Duration offset) {
        event.setId(event.getId());
        event.setStartTs(event.getStartTs().plus(offset));
        event.setEndTs(event.getEndTs().plus(offset));
        eventDao.updateEvent(event);
    }
}
