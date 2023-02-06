package ru.yandex.calendar.logic.event.avail;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.beans.generated.ResourceFields;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.layer.LayerUserDao;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.resource.reservation.ResourceReservationManager;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.dates.DayOfWeek;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

public class AvailRoutinesTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private AvailRoutines availRoutines;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private LayerUserDao layerUserDao;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private ResourceReservationManager resourceReservationManager;

    @Test
    public void iSeeResourcesNamesInMyEventsAndOthersPublicEvents() {
        TestUserInfo me = testManager.prepareRandomYaTeamUser(5624);
        TestUserInfo participant = testManager.prepareRandomYaTeamUser(6634);

        Resource threeLittlePigs = testManager.cleanAndCreateThreeLittlePigs();
        Resource smolny = testManager.cleanAndCreateSmolny();

        Instant i = new DateTime(2010, 12, 22, 18, 51, 0, 0, DateTimeZone.UTC).toInstant();

        Event oneInPigs = testManager.createDefaultEventWithEventLayerAndEventUser(participant.getUid(), "one", i.plus(Duration.standardHours(1)), i.plus(Duration.standardHours(2)));
        testManager.addResourceParticipantToEvent(oneInPigs.getId(), threeLittlePigs);

        Event twoWithoutResources = testManager.createDefaultEventWithEventLayerAndEventUser(participant.getUid(), "two", i.plus(Duration.standardHours(3)), i.plus(Duration.standardHours(4)));

        Event threeInSmolny = testManager.createDefaultEventWithEventLayerAndEventUser(participant.getUid(), "three", i.plus(Duration.standardHours(5)), i.plus(Duration.standardHours(6)));
        testManager.addResourceParticipantToEvent(threeInSmolny.getId(), smolny);

        Event fourInBoth = testManager.createDefaultEventWithEventLayerAndEventUser(participant.getUid(), "three", i.plus(Duration.standardHours(7)), i.plus(Duration.standardHours(8)));
        testManager.addResourceParticipantToEvent(fourInBoth.getId(), smolny);
        testManager.addResourceParticipantToEvent(fourInBoth.getId(), threeLittlePigs);

        testManager.updateEventTimeIndents(oneInPigs, twoWithoutResources, threeInSmolny, fourInBoth);

        AvailabilityRequest request = AvailabilityRequest
                .interval(oneInPigs.getStartTs(), fourInBoth.getStartTs().plus(Duration.standardMinutes(10)))
                .includeEventsInfo();

        ListF<AvailabilityInterval> intervals = availRoutines.getAvailabilityIntervals(
                me.getUid(), SubjectId.uid(participant.getUid()), request, ActionInfo.webTest()).getIntervals().merged();

        Assert.A.equals(threeLittlePigs.getId(), intervals.get(0).getResources().single().getResource().getId());
        Assert.A.hasSize(0, intervals.get(1).getResources());
        Assert.A.equals(smolny.getId(), intervals.get(2).getResources().single().getResource().getId());
        Assert.A.equals(Cf.set(threeLittlePigs.getId(), smolny.getId()),
                intervals.get(3).getResources().map(ResourceInfo.resourceF().andThen(ResourceFields.ID.getF())).unique());

    }

    @Test
    public void currentEventExportedToStaff() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-11601");

        Event eventOverrides = new Event();
        eventOverrides.setStartTs(TestDateTimes.moscow(2011, 1, 20, 18, 0));
        eventOverrides.setEndTs(TestDateTimes.moscow(2011, 1, 20, 19, 0));

        EventUser eventUserOverrides = new EventUser();
        eventUserOverrides.setAvailability(Availability.BUSY);

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(
                user1.getUid(), "some event", eventOverrides, eventUserOverrides);

        testManager.updateEventTimeIndents(event);

        AvailabilityIntervals intervals = availRoutines.getAvailabilityIntervalsForStaff(
                user1.getEmail(), user1.getEmail(),
                TestDateTimes.moscow(2011, 1, 20, 0, 0), TestDateTimes.moscow(2011, 1, 21, 0, 0), ActionInfo.webTest());
        Assert.A.hasSize(1, intervals.merged());
        AvailabilityInterval interval = intervals.merged().single();
        Assert.A.equals("some event", interval.getEventName().get());
    }

    @Test
    public void repeatedEventExportedToStaff() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-11611");

        Event eventOverrides = new Event();
        eventOverrides.setStartTs(TestDateTimes.moscow(2011, 1, 20, 18, 0));
        eventOverrides.setEndTs(TestDateTimes.moscow(2011, 1, 20, 19, 0));
        eventOverrides.setRepetitionId(testManager.createWeeklyRepetition(DayOfWeek.fromDay(new LocalDate(2011, 1, 20))));

        EventUser eventUserOverrides = new EventUser();
        eventUserOverrides.setAvailability(Availability.BUSY);

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(
                user1.getUid(), "repeated event", eventOverrides, eventUserOverrides);

        testManager.updateEventTimeIndents(event);

        AvailabilityIntervals intervals = availRoutines.getAvailabilityIntervalsForStaff(
                user1.getEmail(), user1.getEmail(),
                TestDateTimes.moscow(2011, 1, 27, 0, 0), TestDateTimes.moscow(2011, 1, 28, 0, 0), ActionInfo.webTest());
        Assert.A.hasSize(1, intervals.merged());
        AvailabilityInterval interval = intervals.merged().single();
        Assert.A.equals("repeated event", interval.getEventName().get());
    }

    @Test
    public void emptyRequestIntervalForUser() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(22222);

        EventUser eventUserOverrides = new EventUser();
        eventUserOverrides.setAvailability(Availability.BUSY);

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(
                user.getUid(), "event", new Event(), eventUserOverrides);

        testManager.updateEventTimeIndents(event);

        Assert.notEmpty(findIntervals(user, event.getStartTs(), event.getStartTs(), ActionInfo.webTest()));
        Assert.isEmpty(findIntervals(user, event.getEndTs(), event.getEndTs(), ActionInfo.webTest()));

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());
        testManager.updateEventTimeIndents(event);

        Instant nextStart = event.getStartTs().plus(Duration.standardDays(1));
        Assert.notEmpty(findIntervals(user, nextStart, nextStart, ActionInfo.webTest()));
    }

    @Test
    public void emptyRequestIntervalForResource() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(22227);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(user.getUid(), "event");
        testManager.addResourceParticipantToEvent(event.getId(), resource);
        testManager.updateEventTimeIndents(event);

        AvailabilityRequest request = AvailabilityRequest.interval(event.getStartTs(), event.getStartTs());

        request = request.withUseResourceCheduleCache(false);
        Assert.notEmpty(findIntervals(user, resource, request, ActionInfo.webTest()));

        request = request.withUseResourceCheduleCache(true);
        Assert.notEmpty(findIntervals(user, resource, request, ActionInfo.webTest()));

        request = AvailabilityRequest.interval(event.getEndTs(), event.getEndTs());

        request = request.withUseResourceCheduleCache(false);
        Assert.isEmpty(findIntervals(user, resource, request, ActionInfo.webTest()));

        request = request.withUseResourceCheduleCache(true);
        Assert.isEmpty(findIntervals(user, resource, request, ActionInfo.webTest()));
    }

    @Test
    public void emptyRequestOverlap() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(33333);

        EventUser eventUserOverrides = new EventUser();
        eventUserOverrides.setAvailability(Availability.BUSY);

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(
                user.getUid(), "event", new Event(), eventUserOverrides);

        testManager.updateEventTimeIndents(event);

        Assert.some(findOverlap(user, event.getStartTs(), event.getStartTs(), ActionInfo.webTest()));
        Assert.none(findOverlap(user, event.getEndTs(), event.getEndTs(), ActionInfo.webTest()));

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());
        testManager.updateEventTimeIndents(event);

        Instant nextStart = event.getStartTs().plus(Duration.standardDays(1));
        Assert.some(findOverlap(user, nextStart, nextStart, ActionInfo.webTest()));
    }

    @Test
    public void busyOverlappingEventsInFuture() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11641");

        Instant start = TestDateTimes.moscow(2011, 1, 30, 19, 0);
        Instant end = TestDateTimes.moscow(2011, 1, 30, 20, 0);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                TestManager.testExchangeThreeLittlePigsEmail, start, end);

        Instant before = start.minus(Duration.standardMinutes(30));
        Instant between = start.plus(Duration.standardMinutes(30));
        Instant after = end.plus(Duration.standardMinutes(30));

        EventData first = testManager.createDefaultEventData(user.getUid(), "busyOverlappingEventsInFuture");
        first.getEvent().setStartTs(start);
        first.getEvent().setEndTs(end);
        first.setInvData(testManager.createParticipantsData(user.getEmail(), ResourceRoutines.getResourceEmail(r)));

        eventRoutines.createUserOrFeedEvent(UidOrResourceId.user(user.getUid()), EventType.USER,
                eventRoutines.createMainEvent(user.getUid(), first, ActionInfo.webTest(between)), first,
                NotificationsData.create(first.getEventUserData().getNotifications()),
                InvitationProcessingMode.SAVE_ATTACH, ActionInfo.webTest(between));

        for (boolean useResourceScheduleCache : Cf.list(false, true)) {
            Instant now = before;
            Option<AvailabilityOverlap> overlapping = availRoutines.busyOverlappingEventsInFuture(
                    Option.empty(), UidOrResourceId.resource(r.getId()), now,
                    RepetitionInstanceInfo.noRepetition(new InstantInterval(start, end)), Option.<Long>empty(),
                    useResourceScheduleCache, ActionInfo.webTest());
            Assert.some(overlapping, "useResourceScheduleCache: " + useResourceScheduleCache);

            now = between;
            overlapping = availRoutines.busyOverlappingEventsInFuture(
                    Option.empty(), UidOrResourceId.resource(r.getId()), now,
                    RepetitionInstanceInfo.noRepetition(new InstantInterval(start, end)), Option.<Long>empty(),
                    useResourceScheduleCache, ActionInfo.webTest());
            Assert.some(overlapping, "useResourceScheduleCache: " + useResourceScheduleCache);

            now = after;
            overlapping = availRoutines.busyOverlappingEventsInFuture(
                    Option.empty(), UidOrResourceId.resource(r.getId()), now,
                    RepetitionInstanceInfo.noRepetition(new InstantInterval(start, end)), Option.<Long>empty(),
                    useResourceScheduleCache, ActionInfo.webTest());
            Assert.none(overlapping, "useResourceScheduleCache: " + useResourceScheduleCache);
        }
    }

    @Test
    public void eventFromSharedLayerInBatch() {
        TestUserInfo layerOwner = testManager.prepareUser("yandex-team-mm-11631");
        TestUserInfo layerSharer = testManager.prepareUser("yandex-team-mm-11632");

        Instant start = TestDateTimes.moscow(2011, 6, 26, 18, 0);
        Instant end = TestDateTimes.moscow(2011, 6, 26, 19, 0);

        Event eventOverrides = new Event();
        eventOverrides.setStartTs(start);
        eventOverrides.setEndTs(end);
        EventUser eventUserOverrides = new EventUser();
        eventUserOverrides.setAvailability(Availability.BUSY);

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(layerOwner.getUid(), "some event", eventOverrides, eventUserOverrides);

        long layerId = layerRoutines.getOrCreateDefaultLayer(layerOwner.getUid());

        LayerUser layerUser = new LayerUser();
        layerUser.setLayerId(layerId);
        layerUser.setUid(layerSharer.getUid());
        layerUser.setPerm(LayerActionClass.VIEW);
        layerUser.setFieldDefaults(layerRoutines.createDefaultLayerUserOverrides(layerSharer.getUid().getDomain()));
        layerUserDao.saveLayerUser(layerUser);

        testManager.createEventUser(layerSharer.getUid(), event.getId(), Decision.YES, Option.empty());
        testManager.updateEventTimeIndents(event);

        ListF<UidOrResourceId> subjectIds = Cf.list(layerOwner.getUid(), layerSharer.getUid()).map(UidOrResourceId.userF());
        ListF<UserOrResourceAvailabilityIntervals> availabilityIntervalss = availRoutines.getAvailabilityIntervalss(
                layerOwner.getUid(), subjectIds, AvailabilityRequest.interval(start, end), ActionInfo.webTest());

        Assert.A.hasSize(2, availabilityIntervalss);

        UserOrResourceAvailabilityIntervals ownerIntervals = availabilityIntervalss.find(UserOrResourceAvailabilityIntervals.getUidF().andThenEquals(layerOwner.getUid())).get();
        UserOrResourceAvailabilityIntervals sharerIntervals = availabilityIntervalss.find(UserOrResourceAvailabilityIntervals.getUidF().andThenEquals(layerSharer.getUid())).get();

        Assert.A.equals(new InstantInterval(start, end), ownerIntervals.getIntervalsO().get().merged().single().getInterval());
        Assert.A.equals(new InstantInterval(start, end), sharerIntervals.getIntervalsO().get().merged().single().getInterval());
    }

    @Test
    public void availableOnRejectedRecurrence() {
        TestUserInfo performer = testManager.prepareRandomYaTeamUser(8080);

        TestUserInfo user = testManager.prepareRandomYaTeamUser(8181);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        DateTime start = MoscowTime.dateTime(2012, 7, 27, 18, 10);

        long masterEventId = testManager.createDefaultEvent(performer.getUid(),
                "availableOnRejectedRecurrence", start.toInstant(), start.plusHours(1).toInstant()).getId();
        testManager.createDailyRepetitionAndLinkToEvent(masterEventId);
        testManager.addUserParticipantToEvent(masterEventId, performer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(masterEventId, user.getUid(), Decision.YES, false);
        testManager.addResourceParticipantToEvent(masterEventId, resource);

        Instant recurrenceId = start.plusDays(2).toInstant();
        long recurEventId = testManager.createDefaultRecurrence(performer.getUid(), masterEventId, recurrenceId).getId();
        testManager.addUserParticipantToEvent(recurEventId, performer.getUid(), Decision.YES, true);

        testManager.updateEventTimeIndents(masterEventId, recurEventId);

        Instant from = start.toInstant();
        Instant to = start.plusDays(4).toInstant();

        AvailabilityRequest req = AvailabilityRequest.interval(from, to);

        AvailabilityIntervals userIntervals = availRoutines.getAvailabilityIntervals(
                performer.getUid(), SubjectId.uid(user.getUid()), req, ActionInfo.webTest()).getIntervals();

        AvailabilityIntervals resourceIntervals = availRoutines.getAvailabilityIntervals(
                performer.getUid(), SubjectId.resourceId(resource.getId()), req, ActionInfo.webTest()).getIntervals();

        Assert.hasSize(3, userIntervals.merged());
        Assert.hasSize(3, resourceIntervals.merged());

        Assert.exists(userIntervals.merged(), AvailabilityInterval.startF.andThenEquals(start.toInstant()));
        Assert.exists(resourceIntervals.merged(), AvailabilityInterval.startF.andThenEquals(start.toInstant()));

        Assert.none(userIntervals.merged().find(AvailabilityInterval.startF.andThenEquals(recurrenceId)));
        Assert.none(resourceIntervals.merged().find(AvailabilityInterval.startF.andThenEquals(recurrenceId)));
    }

    // CAL-7184
    @Test
    public void layerAffectAvailabilityLeak() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(22226);
        TestUserInfo user = testManager.prepareRandomYaTeamUser(22227);

        DateTime start = MoscowTime.dateTime(2015, 4, 8, 16, 30);

        long eventId = testManager.createDefaultEvent(creator.getUid(), "Leak", start.toInstant()).getId();
        testManager.addUserParticipantToEvent(eventId, creator.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(eventId, user.getUid(), Decision.YES, true);

        testManager.updateEventTimeIndents(eventId);

        AvailabilityRequest req = AvailabilityRequest.interval(start, start.plusHours(1));

        Function<ListF<TestUserInfo>, ListF<InstantInterval>> getIntervalsF = users -> {
            ListF<UserOrResourceAvailabilityIntervals> intervalss = availRoutines.getAvailabilityIntervalss(
                    user.getUid(), users.map(TestUserInfo.getUidF()), Cf.list(), req, ActionInfo.webTest());
            return intervalss
                    .find(UserOrResourceAvailabilityIntervals.getUidF().andThenEquals(user.getUid()))
                    .get().getIntervalsO().get().getIntervals();
        };

        Assert.notEmpty(getIntervalsF.apply(Cf.list(user)));

        LayerUser data = new LayerUser();
        data.setAffectsAvailability(false);
        layerRoutines.updateLayerUser(user.getDefaultLayerId(), user.getUid(), data, ActionInfo.webTest());

        Assert.isEmpty(getIntervalsF.apply(Cf.list(user)));
        Assert.isEmpty(getIntervalsF.apply(Cf.list(user, creator)));
    }

    @Test
    public void requestFields() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(8080);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(user.getUid(), "Occupation", true);
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        testManager.updateEventTimeIndents(event);

        AvailabilityRequest request = AvailabilityRequest.interval(event.getStartTs(), event.getEndTs());

        Assert.isTrue(findSingleAvailEventInfo(user, request).isEmpty());
        Assert.some(event.getName(), findSingleAvailEventInfo(user, request.includeEventsNames()).getName());
        Assert.some(user.getUid(), findSingleAvailEventInfo(user, request.includeEventsOrganizers()).getOrganizerUid());
        Assert.in(resource.getId(), findSingleAvailEventInfo(user, request.includeEventsResources()).getResourceIds());
        Assert.some(findSingleAvailEventInfo(user, request.includeEventsParticipants()).getParticipants());

        request = request.withoutPermsCheck();

        Assert.isTrue(findSingleAvailEventInfo(user, request).isEmpty());
        Assert.some(event.getName(), findSingleAvailEventInfo(user, request.includeEventsNames()).getName());
        Assert.some(user.getUid(), findSingleAvailEventInfo(user, request.includeEventsOrganizers()).getOrganizerUid());
        Assert.in(resource.getId(), findSingleAvailEventInfo(user, request.includeEventsResources()).getResourceIds());
        Assert.some(findSingleAvailEventInfo(user, request.includeEventsParticipants()).getParticipants());

        TestUserInfo someGuy = testManager.prepareRandomYaTeamUser(8081);
        request = AvailabilityRequest.interval(event.getStartTs(), event.getEndTs());

        Assert.none(findSingleAvailEventInfo(someGuy, user, request.includeEventsNames()).getName());
        Assert.none(findSingleAvailEventInfo(someGuy, user, request.includeEventsParticipants()).getParticipants());

        request = request.withoutPermsCheck();
        Assert.some(findSingleAvailEventInfo(someGuy, user, request.includeEventsNames()).getName());
        Assert.some(findSingleAvailEventInfo(someGuy, user, request.includeEventsParticipants()).getParticipants());
    }

    @Test
    public void resourceReservationAvailability() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(22222);
        TestUserInfo user = testManager.prepareRandomYaTeamUser(22223);
        Resource resource = testManager.cleanAndCreateResource("resource_rra", "RRA");

        InstantInterval interval = new InstantInterval(
                MoscowTime.instant(2018, 6, 6, 23, 30),
                MoscowTime.instant(2018, 6, 7, 0, 30));

        ActionInfo actionInfo = ActionInfo.webTest(interval.getStart());

        resourceReservationManager.createOrUpdateReservations(
                creator.getUid(), 22222, Cf.list(resource.getId()),
                RepetitionInstanceInfo.noRepetition(interval), actionInfo);

        AvailabilityRequest request = AvailabilityRequest.interval(interval);
        AvailabilityInterval availInterval = findIntervals(user, resource, request, actionInfo).single();

        Assert.none(availInterval.getEventId());
        Assert.some(creator.getUid(), availInterval.eventInfo.getOrganizerUid());

        Assert.isEmpty(findIntervals(creator, resource, request, actionInfo));                         // self
        Assert.isEmpty(findIntervals(user, resource, request, ActionInfo.webTest(interval.getEnd()))); // expired
    }

    @Test
    public void resourceReservationOverlap() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(22222);
        TestUserInfo user = testManager.prepareRandomYaTeamUser(22223);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        InstantInterval interval = new InstantInterval(MoscowTime.now(), MoscowTime.now().plusHours(1));
        ActionInfo actionInfo = ActionInfo.webTest(interval.getStart());

        resourceReservationManager.createOrUpdateReservations(
                creator.getUid(), 22223, Cf.list(resource.getId()),
                RepetitionInstanceInfo.noRepetition(interval), actionInfo);

        AvailabilityOverlap overlap = findOverlap(user, resource, interval, actionInfo).get();
        Assert.isTrue(overlap.getEventIdOrReservation().isRight());

        Assert.none(findOverlap(creator, resource, interval, actionInfo));                         // self
        Assert.none(findOverlap(user, resource, interval, ActionInfo.webTest(interval.getEnd()))); // expired
    }

    private AvailabilityEventInfo findSingleAvailEventInfo(TestUserInfo user, AvailabilityRequest request) {
        return findSingleAvailEventInfo(user, user, request);
    }

    private AvailabilityEventInfo findSingleAvailEventInfo(
            TestUserInfo client, TestUserInfo subject, AvailabilityRequest request)
    {
        return availRoutines
                .getAvailabilityIntervals(client.getUid(), SubjectId.uid(subject.getUid()), request, ActionInfo.webTest())
                .getIntervals().unmerged().single().eventInfo;
    }

    private ListF<AvailabilityInterval> findIntervals(
            TestUserInfo client, Resource resource, AvailabilityRequest request, ActionInfo actionInfo)
    {
        return availRoutines
                .getAvailabilityIntervals(client.getUid(), SubjectId.resourceId(resource.getId()), request, actionInfo)
                .getIntervals().unmerged();
    }

    private ListF<AvailabilityInterval> findIntervals(
            TestUserInfo user, Instant start, Instant end, ActionInfo actionInfo)
    {
        AvailabilityRequest request = AvailabilityRequest.interval(start, end);
        return availRoutines
                .getAvailabilityIntervals(user.getUid(), SubjectId.uid(user.getUid()), request, actionInfo)
                .getIntervals().unmerged();
    }

    private Option<AvailabilityOverlap> findOverlap(
            TestUserInfo client, Resource resource, InstantInterval interval, ActionInfo actionInfo)
    {
        return availRoutines.busyOverlappingEventsInFuture(
                Option.of(client.getUid()), UidOrResourceId.resource(resource.getId()),
                interval.getStart(), RepetitionInstanceInfo.noRepetition(interval), Option.empty(), true, actionInfo);
    }

    private Option<AvailabilityOverlap> findOverlap(
            TestUserInfo user, Instant start, Instant end, ActionInfo actionInfo)
    {
        return availRoutines.busyOverlappingEventsInFuture(
                Option.of(user.getUid()), UidOrResourceId.user(user.getUid()),
                start, RepetitionInstanceInfo.noRepetition(new InstantInterval(start, end)),
                Option.<Long>empty(), false, actionInfo);
    }
}
