package ru.yandex.calendar.logic.resource.reservation;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.repetition.RecurrenceTimeInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionUtils;
import ru.yandex.calendar.logic.user.TestUsers;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class ResourceReservationManagerTest extends AbstractConfTest {

    @Autowired
    private ResourceReservationManager resourceReservationManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private ResourceReservationDao resourceReservationDao;

    @Test
    public void create() {
        resourceReservationDao.deleteByCreatorUid(TestUsers.DBRYLEV);

        InstantInterval interval = new InstantInterval(MoscowTime.now(), MoscowTime.now().plusHours(1));

        RepetitionInstanceInfo repetitionInfo = RepetitionInstanceInfo.create(
                interval, MoscowTime.TZ, Option.of(TestManager.createDailyRepetitionTemplate()));

        ListF<Long> resourceIds = Cf.list(
                testManager.cleanAndCreateConfRr21().getId(),
                testManager.cleanAndCreateSmolny().getId(),
                testManager.cleanAndCreateThreeLittlePigs().getId());

        long reservationId = 127000;

        resourceReservationManager.createOrUpdateReservations(
                TestUsers.DBRYLEV, reservationId, resourceIds,
                repetitionInfo, ActionInfo.webTest(interval.getStart()));

        ListF<ResourceReservationInfo> reservations = resourceReservationManager.findReservations(
                resourceIds, interval, Option.<PassportUid>empty(), ActionInfo.webTest(interval.getStart()));

        Assert.hasSize(3, reservations);
        Assert.equals(resourceIds.unique(), reservations.map(ResourceReservationInfo::getResourceId).unique());
        Assert.forAll(reservations, r -> r.getReservationId() == reservationId);
    }

    @Test
    public void update() {
        resourceReservationDao.deleteByCreatorUid(TestUsers.DBRYLEV);

        InstantInterval interval = new InstantInterval(MoscowTime.now(), MoscowTime.now().plusHours(1));

        RepetitionInstanceInfo repetitionInfo = RepetitionInstanceInfo.create(
                interval, MoscowTime.TZ, Option.of(TestManager.createDailyRepetitionTemplate()));

        long survivedResourceId = testManager.cleanAndCreateConfRr21().getId();
        long deletedResourceId = testManager.cleanAndCreateSmolny().getId();
        long addedResourceId = testManager.cleanAndCreateThreeLittlePigs().getId();

        ListF<Long> resourceIds = Cf.list(survivedResourceId, deletedResourceId, addedResourceId);

        long reservationId = 640023;

        resourceReservationManager.createOrUpdateReservations(
                TestUsers.DBRYLEV, reservationId, Cf.list(survivedResourceId, deletedResourceId),
                repetitionInfo, ActionInfo.webTest(interval.getStart()));

        resourceReservationManager.createOrUpdateReservations(
                TestUsers.DBRYLEV, reservationId, Cf.list(survivedResourceId, addedResourceId),
                RepetitionInstanceInfo.noRepetition(interval), ActionInfo.webTest(interval.getStart()));

        ListF<ResourceReservationInfo> reservations = resourceReservationManager.findReservations(
                resourceIds, interval, Option.<PassportUid>empty(), ActionInfo.webTest(interval.getStart()));

        Assert.hasSize(2, reservations);
        Assert.equals(Cf.set(survivedResourceId, addedResourceId), reservations.map(r -> r.getResourceId()).unique());

        Assert.forAll(reservations, r -> r.getReservationId() == reservationId);
        Assert.forAll(reservations, r -> r.getRepetitionInfo().isEmpty());
    }

    @Test
    public void keepExcluded() {
        resourceReservationDao.deleteByCreatorUid(TestUsers.DBRYLEV);

        long resourceId = testManager.cleanAndCreateConfRr21().getId();

        long reservationId = 8_800_2000_600L;

        InstantInterval interval = new InstantInterval(MoscowTime.now(), MoscowTime.now().plusHours(1));

        ListF<Instant> exdates = Cf.list(interval.getStart().plus(Duration.standardDays(1)));
        ListF<Instant> recurrenceIds = Cf.list(interval.getStart().plus(Duration.standardDays(2)));

        RepetitionInstanceInfo repetitionInfo = new RepetitionInstanceInfo(
                interval, MoscowTime.TZ, Option.of(TestManager.createDailyRepetitionTemplate()),
                Cf.list(), exdates.map(RepetitionUtils::consExdate),
                recurrenceIds.map(id -> new RecurrenceTimeInfo(id, interval)));

        resourceReservationManager.createOrUpdateReservations(
                TestUsers.DBRYLEV, reservationId, Cf.list(resourceId),
                repetitionInfo, ActionInfo.webTest(interval.getStart()));

        ListF<ResourceReservationInfo> reservations = resourceReservationManager.findReservations(
                Cf.list(resourceId), interval, Option.empty(), ActionInfo.webTest(interval.getStart()));

        Assert.equals(exdates.plus(recurrenceIds).unique(),
                reservations.single().getRepetitionInfo().getExcludeInstants(false));
    }
}
