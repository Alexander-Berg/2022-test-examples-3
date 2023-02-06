package ru.yandex.calendar.logic.resource.schedule;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.beans.generated.ResourceSchedule;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.web.EventWebRemover;
import ru.yandex.calendar.logic.resource.ResourceFilter;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.reservation.ResourceReservationManager;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.user.TestUsers;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

public class ResourceScheduleManagerTest extends AbstractConfTest {
    @Autowired
    private ResourceScheduleManager resourceScheduleManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebRemover eventWebRemover;
    @Autowired
    private ResourceScheduleDao resourceScheduleDao;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private ResourceReservationManager resourceReservationManager;

    @Test
    public void invalidateCacheOnNonRepeatingEventDelete() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(100);
        Resource pigs = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(user.getUid(), "invalidateCacheOnNonRepeatingEventDelete");

        testManager.addResourceParticipantToEvent(event.getId(), pigs);
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);

        testManager.updateEventTimeIndents(event);

        DateTime instanceStart = event.getStartTs().toDateTime(MoscowTime.TZ);
        invalidateCacheOnEventDelete(user.getUserInfo(), pigs, event.getId(), instanceStart, false);
    }

    @Test
    public void invalidateCacheOnRepeatingEventDelete() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(101);
        Resource pigs = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(user.getUid(), "invalidateCacheOnRepeatingEventDelete");

        testManager.addResourceParticipantToEvent(event.getId(), pigs);
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());
        testManager.updateEventTimeIndents(event);

        DateTime instanceStart = event.getStartTs().toDateTime(MoscowTime.TZ);
        invalidateCacheOnEventDelete(user.getUserInfo(), pigs, event.getId(), instanceStart, true);
    }

    @Test
    public void invalidateCacheOnTailOfRepeatingEventDelete() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(102);
        Resource pigs = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(user.getUid(), "invalidateCacheOnTailOfRepeatingEventDelete");

        testManager.addResourceParticipantToEvent(event.getId(), pigs);
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());
        testManager.updateEventTimeIndents(event);

        DateTime instanceStart = event.getStartTs().toDateTime(MoscowTime.TZ).plusWeeks(1);
        invalidateCacheOnEventDelete(user.getUserInfo(), pigs, event.getId(), instanceStart, true);
    }

    @Test
    public void invalidateCacheOnInstanceOfRepeatingEventDelete() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(103);
        Resource pigs = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(user.getUid(), "invalidateCacheOnInstanceOfRepeatingEventDelete");

        testManager.addResourceParticipantToEvent(event.getId(), pigs);
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());
        testManager.updateEventTimeIndents(event);

        DateTime instanceStart = event.getStartTs().toDateTime(MoscowTime.TZ).plusWeeks(1);
        invalidateCacheOnEventDelete(user.getUserInfo(), pigs, event.getId(), instanceStart, false);
    }

    public void invalidateCacheOnEventDelete(
            UserInfo user, Resource resource, long eventId,
            DateTime instanceStart, boolean applyToFutureEvent)
    {
        ResourceSchedule schedule = new ResourceSchedule();
        schedule.setResourceId(resource.getId());
        schedule.setEventIntervals("");
        schedule.setDayStart(instanceStart.withTimeAtStartOfDay().toInstant());
        schedule.setIsValid(true);

        resourceScheduleDao.insertResourceSchedulesIgnoreDuplicates(Cf.list(schedule));

        eventWebRemover.remove(user, eventId, Option.of(instanceStart.toInstant()), applyToFutureEvent, ActionInfo.webTest());

        Assert.some(false, findResourceSchedule(resource, instanceStart.withTimeAtStartOfDay()).map(ResourceSchedule.getIsValidF()));
    }

    private Option<ResourceSchedule> findResourceSchedule(Resource resource, DateTime dayStart) {
        return resourceScheduleDao.findResourceSchedulesByResourceIdsAndDays(
                Cf.list(resource.getId()), Cf.list(dayStart.toInstant())).singleO();
    }

    @Test
    public void getResourceScheduleDataForInterval() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(101).getUid();
        Resource pigs = testManager.cleanAndCreateThreeLittlePigs();

        DateTime start = MoscowTime.dateTime(2012, 6, 5, 19, 37);

        Event event = testManager.createDefaultEvent(
                uid, "scheduleDataForIntervalEvent", start.toInstant(), start.plusHours(1).toInstant());
        testManager.addResourceParticipantToEvent(event.getId(), pigs);
        testManager.addUserParticipantToEvent(event.getId(), uid, Decision.YES, true);
        testManager.updateEventTimeIndents(event);

        start = start.plusDays(1);

        PassportUid reservationCreatorUid = TestUsers.DBRYLEV;
        InstantInterval reservationInterval = new InstantInterval(start.plusDays(1), start.plusDays(2));

        resourceReservationManager.createOrUpdateReservations(
                TestUsers.DBRYLEV, 101, Cf.list(pigs.getId()),
                RepetitionInstanceInfo.noRepetition(reservationInterval), ActionInfo.webTest());

        start = start.plusDays(3);

        Event nextEvent = testManager.createDefaultEvent(
                uid, "scheduleDataForIntervalNext", start.toInstant(), start.plusHours(1).toInstant());
        testManager.addResourceParticipantToEvent(nextEvent.getId(), pigs);
        testManager.addUserParticipantToEvent(nextEvent.getId(), uid, Decision.YES, true);
        testManager.updateEventTimeIndents(nextEvent);

        InstantInterval interval = new InstantInterval(event.getStartTs(), nextEvent.getEndTs());
        Tuple2List<Long, ResourceFilter> filters = Tuple2List.fromPairs(pigs.getOfficeId(), ResourceFilter.any());

        ResourceInfo pigsInfo = resourceRoutines.getDomainResourcesCanBookWithLayersAndOffices(uid, filters).single();

        Tuple2List<ResourceInfo, ResourceEventsAndReservations> schedules;

        schedules = resourceScheduleManager.getResourceScheduleDataForInterval(
                Option.empty(), Cf.list(pigsInfo), interval, MoscowTime.TZ, Option.empty(), ActionInfo.webTest());

        Tuple2List<Long, InstantInterval> intervals = schedules.single().get2().getEventIntervals();
        Assert.hasSize(2, intervals);
        Assert.equals(Cf.set(event.getId(), nextEvent.getId()), intervals.get1().unique());

        Assert.some(reservationCreatorUid, schedules.single().get2().getReservationCreatorUids().singleO());

        schedules = resourceScheduleManager.getResourceScheduleDataForInterval(
                Option.empty(), Cf.list(pigsInfo), interval, MoscowTime.TZ,
                Option.of(nextEvent.getId()), ActionInfo.webTest());

        intervals = schedules.single().get2().getEventIntervals();
        Assert.hasSize(1, intervals);
        Assert.equals(event.getId(), intervals.single().get1());
    }

    @Test
    public void getIncompatibleResourceScheduleDataForInterval() {
        DateTime start = MoscowTime.dateTime(2020, 5, 5, 10, 0);
        DateTime end = start.plusDays(1);
        PassportUid uid = testManager.prepareRandomYaTeamUser(101).getUid();
        InstantInterval interval = new InstantInterval(start, end);

        Tuple2List<ResourceInfo, ResourceEventsAndReservations> resourceScheduleDataForInterval = resourceScheduleManager.getResourceScheduleDataForInterval(
                Option.of(uid),
                Cf.list(), // here shall be an empty list due to the filter
                interval, MoscowTime.TZ, Option.empty(), ActionInfo.webTest());
        Assert.isEmpty(resourceScheduleDataForInterval);
    }
}
