package ru.yandex.calendar.frontend.ews.imp;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.exp.EventToCalendarItemConverter;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.hook.EwsFirewallTestConfiguration;
import ru.yandex.calendar.frontend.ews.hook.EwsNtfContextConfiguration;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.resource.RejectedResources;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.resource.SpecialResources;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.user.NameI18n;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.io.IoFunction0V;
import ru.yandex.misc.io.IoFunction1V;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;

@ContextConfiguration(classes = {
        EwsNtfContextConfiguration.class,
        EwsFirewallTestConfiguration.class
})
public class EwsImporterResourceAccessTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsImporter ewsImporter;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private EventToCalendarItemConverter eventToCalendarItemConverter;

    private final EwsExportRoutines mockEwsExportRoutines = Mockito.mock(EwsExportRoutines.class);

    private TestUserInfo organizer;

    private DateTime dateTime = DateTime.now(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).plusDays(10);
    private ActionInfo actionInfo = new ActionInfo(ActionSource.EXCHANGE, "tryToBook", Instant.now());

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        organizer = testManager.prepareRandomYaTeamUser(476);
        eventRoutines.setEwsExportRoutinesForTest(mockEwsExportRoutines);
    }

    @After
    public void restore() {
        eventRoutines.setEwsExportRoutinesForTest(ewsExportRoutines);
    }

    @Test
    public void bookInaccessibleResourceByOrganizer() throws Exception {
        commonCheck(RejectedResources.Reason.INACCESSIBLE, false, true);
    }

    @Test
    public void bookInaccessibleResourceByResource() throws Exception {
        commonCheck(RejectedResources.Reason.INACCESSIBLE, false, false);
    }

    @Test
    public void bookBusyResourceByOrganizer() throws Exception {
        commonCheck(RejectedResources.Reason.BUSY, false, true);
    }

    @Test
    public void bookBusyResourceByResource() throws Exception {
        commonCheck(RejectedResources.Reason.BUSY, false, false);
    }

    @Test
    public void bookNotPermittedResourceByOrganizer() throws Exception {
        commonCheck(RejectedResources.Reason.NOT_PERMITTED, false, true);
    }

    @Test
    public void bookNotPermittedResourceByResource() throws Exception {
        commonCheck(RejectedResources.Reason.NOT_PERMITTED, false, false);
    }

    @Test
    public void bookUpdateInaccessibleResourceByOrganizer() throws Exception {
        commonCheck(RejectedResources.Reason.INACCESSIBLE, true, true);
    }

    @Test
    public void bookUpdateInaccessibleResourceByResource() throws Exception {
        commonCheck(RejectedResources.Reason.INACCESSIBLE, true, false);
    }

    @Test
    public void bookUpdateBusyResourceByOrganizer() throws Exception {
        commonCheck(RejectedResources.Reason.BUSY, true, true);
    }

    @Test
    public void bookUpdateBusyResourceByResource() throws Exception {
        commonCheck(RejectedResources.Reason.BUSY, true, false);
    }

    @Test
    public void bookUpdateNotPermittedResourceByOrganizer() throws Exception {
        commonCheck(RejectedResources.Reason.NOT_PERMITTED, true, true);
    }

    @Test
    public void bookUpdateNotPermittedResourceByResource() throws Exception {
        commonCheck(RejectedResources.Reason.NOT_PERMITTED, true, false);
    }

    @Test
    public void successfulBooking() throws Exception {
        Resource resource = getSimpleResource();

        String externalId = getRandomExternalId();

        CalendarItemType calItem = createMeetingWithResource(resource, externalId);
        ewsImporter.createOrUpdateEventForTest(
                UidOrResourceId.user(organizer.getUid()), calItem, actionInfo, false);

        assertEventResources(externalId, Cf.list(resource.getId()));

        assertReplyWasSent(() -> ewsImporter.createOrUpdateEventForTest(
                UidOrResourceId.resource(resource.getId()), calItem, actionInfo, false), Decision.YES, Option.empty());

    }

    @Test
    public void keepInaccessibleResourceByOrganizer() throws Exception {
        keepNonBusyResourceIfTimeOrRepetitionNotChanged(true, getInaccessibleResource());
    }

    @Test
    public void keepInaccessibleResourceByResource() throws Exception {
        keepNonBusyResourceIfTimeOrRepetitionNotChanged(false, getInaccessibleResource());
    }

    @Test
    public void keepNotPermittedResourceByOrganizer() throws Exception {
        keepNonBusyResourceIfTimeOrRepetitionNotChanged(true, getNotPermittedResource());
    }

    @Test
    public void keepNotPermittedResourceByResource() throws Exception {
        keepNonBusyResourceIfTimeOrRepetitionNotChanged(false, getNotPermittedResource());
    }

    private void keepNonBusyResourceIfTimeOrRepetitionNotChanged(boolean byOrganizer, Resource resource) throws Exception {
        Event event = testManager.createDefaultEventWithDailyRepetitionInFuture(
                organizer.getUid(), "Calendar event");

        testManager.addUserParticipantToEvent(event.getId(), organizer, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        EventWithRelations eventWR = eventDbManager.getEventWithRelationsByEvent(event);
        RepetitionInstanceInfo repetitionInfo = repetitionRoutines.getRepetitionInstanceInfo(eventWR);

        CalendarItemType calItem = eventToCalendarItemConverter.convertToCalendarItem(eventWR, repetitionInfo);

        CalendarItemType defaultItemForImport = createBlankMeeting(eventWR.getExternalId());

        calItem.setItemId(defaultItemForImport.getItemId());
        calItem.setLastModifiedTime(defaultItemForImport.getLastModifiedTime());
        calItem.setDateTimeStamp(defaultItemForImport.getDateTimeStamp());

        Resource simpleResource = getSimpleResource();

        TestCalItemFactory.addAttendee(calItem,
                resourceRoutines.getResourceEmailByResourceId(simpleResource.getId()), ResponseTypeType.UNKNOWN);

        UidOrResourceId subjectId = byOrganizer
                ? UidOrResourceId.user(organizer.getUid()) : UidOrResourceId.resource(resource.getId());

        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, actionInfo, false);

        assertEventResources(eventWR.getExternalId(), Cf.list(resource.getId(), simpleResource.getId()));
    }

    private void commonCheck(RejectedResources.Reason reason, boolean doUpdate, boolean byOrganizer) throws Exception {
        IoFunction1V<String> action = (externalId) -> {
            switch (reason) {
                case INACCESSIBLE:
                    bookInaccessibleResource(byOrganizer, externalId);
                    break;
                case BUSY:
                    bookBusyResource(byOrganizer, externalId);
                    break;
                case NOT_PERMITTED:
                    bookNotPermittedResource(byOrganizer, externalId);
                    break;
                default:
                    throw new IllegalStateException("unknown reason");
            }
        };

        String externalId = getRandomExternalId();

        if (doUpdate) {
            CalendarItemType calItem = createBlankMeeting(externalId);
            ewsImporter.createOrUpdateEventForTest(
                    UidOrResourceId.user(organizer.getUid()), calItem, actionInfo, false);
        }

        ArgumentCaptor<EventWithRelations> eventCaptor = ArgumentCaptor.forClass(EventWithRelations.class);

        Mockito.doNothing().when(mockEwsExportRoutines).forceUpdateEventForOrganizer(
                eventCaptor.capture(), Mockito.any(), Mockito.any());

        action.apply(externalId);
        Assert.equals(externalId, eventCaptor.getValue().getExternalId());
    }

    private Resource getSimpleResource() {
        return testManager.cleanAndCreateResource("simple_resource", "simpleResource");
    }

    private Resource getInaccessibleResource() {
        String resourceName = SpecialResources.repetitionUnacceptableRooms.get().first();
        return testManager.cleanAndCreateResource(resourceName, resourceName);
    }

    private Resource getNotPermittedResource() {
        Resource resource =
                testManager.cleanAndCreateResource("not_permitted_resource", "not_permitted_resource").copy();
        resource.setType(ResourceType.PRIVATE_ROOM);
        resourceDao.updateResource(resource);
        return resource;
    }

    private void bookInaccessibleResource(boolean byOrganizer, String externalId) throws Exception {
        Resource resource = getInaccessibleResource();

        UidOrResourceId subjectId = byOrganizer
                ? UidOrResourceId.user(organizer.getUid()) : UidOrResourceId.resource(resource.getId());

        CalendarItemType calItem = createMeetingWithResource(resource, externalId);
        TestCalItemFactory.addDailyRecurrence(calItem);

        importItem(subjectId, calItem, RejectedResources.Reason.INACCESSIBLE);

        assertNoResource(externalId);
    }

    private void bookBusyResource(boolean byOrganizer, String externalId) throws Exception {
        TestUserInfo firstOrganizer = testManager.prepareRandomYaTeamUser(477);

        Resource resource = testManager.cleanAndCreateResource("busy_resource", "busy_resource");
        Event event = testManager.createDefaultEvent(firstOrganizer.getUid(), "blocking event", dateTime.toInstant());
        testManager.addUserParticipantToEvent(event.getId(), firstOrganizer, Decision.YES, true);
        testManager.createEventResource(resource.getId(), event.getId(), event.getStartTs(), event.getEndTs());

        CalendarItemType calItem = createMeetingWithResource(resource, externalId);

        UidOrResourceId subjectId = byOrganizer
                ? UidOrResourceId.user(organizer.getUid()) : UidOrResourceId.resource(resource.getId());

        importItem(subjectId, calItem, RejectedResources.Reason.BUSY);

        assertNoResource(externalId);
    }

    private void bookNotPermittedResource(boolean byOrganizer, String externalId) throws Exception {
        Resource resource = getNotPermittedResource();

        UidOrResourceId subjectId = byOrganizer
                ? UidOrResourceId.user(organizer.getUid()) : UidOrResourceId.resource(resource.getId());

        CalendarItemType calItem = createMeetingWithResource(resource, externalId);

        importItem(subjectId, calItem, RejectedResources.Reason.NOT_PERMITTED);

        assertNoResource(externalId);
    }

    private void importItem(UidOrResourceId subjectId, CalendarItemType calItem, RejectedResources.Reason reason) {
        IoFunction0V action = () -> ewsImporter.createOrUpdateEventForTest(subjectId, calItem, actionInfo, false);

        if (subjectId.isUser()) {
            action.apply();
        } else {
            assertRejectWasSent(action, reason.getExchangeReason());
        }
    }

    private void assertNoResource(String externalId) {
        assertEventResources(externalId, Cf.list());
    }

    private void assertEventResources(String externalId, ListF<Long> resourceIds) {
        Option<Event> eventO = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(organizer.getUid()), externalId);
        Assert.some(eventO, "Event was not imported");
        EventWithRelations event = eventDbManager.getEventWithRelationsByEvent(eventO.get());
        Assert.equals(resourceIds.unique(), event.getResourceIds().unique());
    }

    @Captor
    private ArgumentCaptor<Option<NameI18n>> reasonCaptor;

    private void assertRejectWasSent(IoFunction0V action, NameI18n reason) {
        assertReplyWasSent(action, Decision.NO, Option.of(reason));
    }

    private void assertReplyWasSent(IoFunction0V action, Decision decision, Option<NameI18n> reason) {
        ArgumentCaptor<Decision> decisionCaptor = ArgumentCaptor.forClass(Decision.class);

        Mockito.doNothing().when(mockEwsExportRoutines).setResourceDecisionIfNeeded(
                Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(),
                decisionCaptor.capture(), reasonCaptor.capture(), Mockito.any());

        action.apply();

        Assert.equals(decision, decisionCaptor.getValue());
        Assert.equals(reason, reasonCaptor.getValue());
    }

    private CalendarItemType createBlankMeeting(String externalId) throws Exception {
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(dateTime, "Exchange event");
        calItem.setIsMeeting(true);
        calItem.setUID(externalId);
        TestCalItemFactory.setOrganizer(calItem, organizer.getEmail());
        return calItem;
    }

    private CalendarItemType createMeetingWithResource(Resource resource, String externalId) throws Exception {
        CalendarItemType calItem = createBlankMeeting(externalId);
        TestCalItemFactory.addAttendee(calItem,
                resourceRoutines.getResourceEmailByResourceId(resource.getId()), ResponseTypeType.UNKNOWN);
        return calItem;
    }

    private String getRandomExternalId() {
        return Random2.R.nextAlnum(8);
    }
}
