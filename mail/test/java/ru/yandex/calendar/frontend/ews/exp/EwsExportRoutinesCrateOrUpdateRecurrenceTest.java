package ru.yandex.calendar.frontend.ews.exp;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemTypeType;
import com.microsoft.schemas.exchange.services._2006.types.DeletedOccurrenceInfoType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.OccurrenceInfoType;
import com.microsoft.schemas.exchange.services._2006.types.RecurringMasterItemIdType;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxy;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.SequenceAndDtStamp;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationsData;
import ru.yandex.calendar.logic.ics.EventInstanceStatusInfo;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

public class EwsExportRoutinesCrateOrUpdateRecurrenceTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EwsProxy ewsProxy;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventDao eventDao;

    private final Instant masterEventStart = MoscowTime.instant(2012, 8, 19, 14, 0);
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
    public void createRecurrence() {
        long masterEventId = createMasterEventAndExport(Cf.list(resource1), "createRecurrence");

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(2));
        createRecurrenceEventAndExport(Cf.list(resource1, resource2), masterEventId, recurrenceId);

        Assert.isTrue(masterHasOneModifiedOccurrenceStartingIn(resource1, recurrenceId));
        Assert.isFalse(existsItemStartingIn(resource2, masterEventStart));

        Assert.isTrue(existsExceptionItemStartingIn(resource1, recurrenceId));
        Assert.isTrue(existsSingleItemStartingIn(resource2, recurrenceId));
    }

    @Test
    public void createRecurrenceWithRemovedResource() {
        long masterEventId = createMasterEventAndExport(
                Cf.list(resource1, resource2), "createRecurrenceWithRemovedResource");

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(2));
        createRecurrenceEventAndExport(Cf.list(resource2), masterEventId, recurrenceId);

        Assert.isTrue(masterHasOneDeletedOccurrenceStartingIn(resource1, recurrenceId));
        Assert.isTrue(masterHasOneModifiedOccurrenceStartingIn(resource2, recurrenceId));

        Assert.isFalse(existsItemStartingIn(resource1, recurrenceId));
        Assert.isTrue(existsExceptionItemStartingIn(resource2, recurrenceId));
    }

    @Test
    public void createRecurrenceWithRemovedResourceUsingUpdate() {
        long masterEventId = createMasterEventAndExport(
                Cf.list(resource1), "createRecurrenceWithRemovedResourceUsingUpdate");

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(2));
        long recurrenceEventId = createRecurrenceEvent(Cf.list(resource1), masterEventId, recurrenceId);

        EventData eventData = new EventData();
        eventData.setInvData(createInvData(Cf.list()));
        updateEventAndExport(recurrenceEventId, eventData);

        Assert.isTrue(masterHasOneDeletedOccurrenceStartingIn(resource1, recurrenceId));
        Assert.isFalse(existsItemStartingIn(resource1, recurrenceId));
    }

    @Test
    public void createMovedRecurrence() {
        long masterEventId = createMasterEventAndExport(Cf.list(resource1), "createMovedRecurrence");

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(2));
        long recurrenceEventId = createRecurrenceEvent(Cf.list(resource1, resource2), masterEventId, recurrenceId);

        Instant recurrenceStart = recurrenceId.plus(Duration.standardHours(4));
        moveEventStartEnd(recurrenceEventId, Duration.standardHours(4));
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(recurrenceEventId, actionInfo);

        Assert.isTrue(masterHasOneModifiedOccurrenceStartingIn(resource1, recurrenceStart));
        Assert.isFalse(existsItemStartingIn(resource2, masterEventStart));

        Assert.isFalse(existsItemStartingIn(resource1, recurrenceId));
        Assert.isFalse(existsItemStartingIn(resource2, recurrenceId));

        Assert.isTrue(existsExceptionItemStartingIn(resource1, recurrenceStart));
        Assert.isTrue(existsSingleItemStartingIn(resource2, recurrenceStart));
    }

    @Test
    public void updateRecurrenceMoveStartEnd() {
        long masterEventId = createMasterEventAndExport(Cf.list(resource1), "updateRecurrenceMoveStartEnd");

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(2));
        long recurrenceEventId = createRecurrenceEventAndExport(
                Cf.list(resource1, resource2), masterEventId, recurrenceId);

        Instant updatedRecurrenceStart = recurrenceId.minus(Duration.standardHours(4));

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(recurrenceId);
        eventData.getEvent().setStartTs(updatedRecurrenceStart);
        eventData.getEvent().setEndTs(updatedRecurrenceStart.plus(Duration.standardHours(1)));

        eventData.setInvData(createInvData(Cf.list(resource1, resource2)));
        updateEventAndExport(recurrenceEventId, eventData);

        Assert.isTrue(masterHasOneModifiedOccurrenceStartingIn(resource1, updatedRecurrenceStart));
        Assert.isFalse(existsItemStartingIn(resource2, masterEventStart));

        Assert.isFalse(existsItemStartingIn(resource1, recurrenceId));
        Assert.isFalse(existsItemStartingIn(resource2, recurrenceId));

        Assert.isTrue(existsExceptionItemStartingIn(resource1, updatedRecurrenceStart));
        Assert.isTrue(existsSingleItemStartingIn(resource2, updatedRecurrenceStart));
    }

    @Test
    public void updateRecurrenceSwapResources() {
        long masterEventId = createMasterEventAndExport(Cf.list(resource1), "updateRecurrenceSwapResources");

        Instant recurrenceId = masterEventStart.plus(Duration.standardDays(2));
        long recurrenceEventId = createRecurrenceEventAndExport(Cf.list(resource1), masterEventId, recurrenceId);

        EventData eventData = new EventData();
        eventData.setInvData(createInvData(Cf.list(resource2)));
        updateEventAndExport(recurrenceEventId, eventData);

        Assert.isFalse(existsItemStartingIn(resource1, recurrenceId));
        Assert.isTrue(existsSingleItemStartingIn(resource2, recurrenceId));

        eventData.setInvData(createInvData(Cf.list(resource1)));
        updateEventAndExport(recurrenceEventId, eventData);

        Assert.isTrue(existsSingleItemStartingIn(resource1, recurrenceId));
        Assert.isFalse(existsSingleItemStartingIn(resource2, recurrenceId));

        eventData.setInvData(createInvData(Cf.list()));
        updateEventAndExport(recurrenceEventId, eventData);

        Assert.isFalse(existsItemStartingIn(resource1, recurrenceId));
        Assert.isFalse(existsItemStartingIn(resource2, recurrenceId));
    }

    private long createMasterEventAndExport(ListF<Resource> resources, String eventName) {
        long masterEventId = testManager.createDefaultEvent(
                user.getUid(), eventName, masterEventStart, masterEventStart.plus(Duration.standardHours(1))).getId();

        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(masterEventId, masterEventDue.toInstant());
        testManager.addUserParticipantToEvent(masterEventId, user.getUid(), Decision.YES, true);

        for (Resource resource : resources) {
            testManager.addResourceParticipantToEvent(masterEventId, resource);
        }

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(masterEventId, actionInfo);

        return masterEventId;
    }

    private long createRecurrenceEventAndExport(ListF<Resource> resources, long masterEventId, Instant recurrenceId) {
        long recurrenceEventId = createRecurrenceEvent(resources, masterEventId, recurrenceId);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(recurrenceEventId, actionInfo);

        return recurrenceEventId;
    }

    private long createRecurrenceEvent(ListF<Resource> resources, long masterEventId, Instant recurrenceId) {
        long recurrenceEventId = testManager.createDefaultRecurrence(user.getUid(), masterEventId, recurrenceId).getId();
        testManager.addUserParticipantToEvent(recurrenceEventId, user.getUid(), Decision.YES, true);

        for (Resource resource : resources) {
            testManager.addResourceParticipantToEvent(recurrenceEventId, resource);
        }
        return recurrenceEventId;
    }

    private void moveEventStartEnd(long evenId, Duration offset) {
        Event event = eventDao.findEventById(evenId);

        Event updateData = new Event();
        updateData.setId(evenId);
        updateData.setStartTs(event.getStartTs().plus(offset));
        updateData.setEndTs(event.getEndTs().plus(offset));
        eventDao.updateEvent(updateData);
    }

    private void updateEventAndExport(long eventId, EventData eventData) {
        Event event = eventDao.findEventById(eventId);
        eventData.getEvent().setId(eventId);

        eventRoutines.updateEventFromIcsOrExchange(
                UidOrResourceId.user(user.getUid()), eventData, NotificationsData.turnedOff(),
                EventInstanceStatusInfo.needToUpdate(eventId), SequenceAndDtStamp.web(event, actionInfo), actionInfo);
    }

    private EventInvitationsData createInvData(ListF<Resource> resources) {
        ListF<Email> resourceEmails = resources.map(ResourceRoutines::getResourceEmail);
        return new EventInvitationsData(resourceEmails.plus1(user.getEmail()));
    }

    private boolean existsItemStartingIn(Resource resource, Instant expectedStart) {
        return findOccurrenceIdStartingIn(resource, expectedStart).isPresent();
    }

    private boolean existsSingleItemStartingIn(Resource resource, Instant expectedStart) {
        Option<CalendarItemType> item = findOccurrenceStartingIn(resource, expectedStart);
        return item.isPresent() && item.get().getCalendarItemType() == CalendarItemTypeType.SINGLE;
    }

    private boolean existsExceptionItemStartingIn(Resource resource, Instant expectedStart) {
        Option<CalendarItemType> item = findOccurrenceStartingIn(resource, expectedStart);
        return item.isPresent() && item.get().getCalendarItemType() == CalendarItemTypeType.EXCEPTION;
    }

    private boolean masterHasOneModifiedOccurrenceStartingIn(Resource resource, Instant expectedStart) {
        CalendarItemType item = findMasterEvent(resource);

        ListF<OccurrenceInfoType> os = item.getModifiedOccurrences() != null
                ? Cf.x(item.getModifiedOccurrences().getOccurrence())
                : Cf.list();

        ListF<Instant> modifiedOccurrences = os.map(o -> EwsUtils.xmlGregorianCalendarInstantToInstant(o.getStart()));
        return modifiedOccurrences.equals(Cf.list(expectedStart));
    }

    private boolean masterHasOneDeletedOccurrenceStartingIn(Resource resource, Instant expectedStart) {
        CalendarItemType item = findMasterEvent(resource);

        ListF<DeletedOccurrenceInfoType> os = item.getDeletedOccurrences() != null
                ? Cf.x(item.getDeletedOccurrences().getDeletedOccurrence())
                : Cf.list();

        ListF<Instant> deletedOccurrences = os.map(o -> EwsUtils.xmlGregorianCalendarInstantToInstant(o.getStart()));
        return deletedOccurrences.equals(Cf.list(expectedStart));
    }

    private CalendarItemType findMasterEvent(Resource resource) {
        Option<ItemIdType> itemId = findOccurrenceIdStartingIn(resource, masterEventStart.toInstant());
        Assert.some(itemId, "master event was not found");

        RecurringMasterItemIdType masterId = new RecurringMasterItemIdType();
        masterId.setOccurrenceId(itemId.get().getId());
        masterId.setChangeKey(itemId.get().getChangeKey());

        CalendarItemType master = ewsProxy.getEvents(Cf.list(masterId), false)
                .singleO().getOrThrow("master event was not found by id");

        Assert.equals(CalendarItemTypeType.RECURRING_MASTER, master.getCalendarItemType());
        return master;
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
}
