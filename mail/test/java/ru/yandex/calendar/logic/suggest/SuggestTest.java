package ru.yandex.calendar.logic.suggest;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author gutman
 */
public class SuggestTest extends CalendarTestBase {

    @Test
    public void fromOfficeFreeIntervals() {
        TestOffice testOffice1 = SuggestTestHelper.createTestOffice(1, 11, 12, 13);
        TestOffice testOffice2 = SuggestTestHelper.createTestOffice(2, 21, 22, 23);

        Tuple2List<ResourceInfo, ListF<InstantInterval>> office1FreeIntervals = Tuple2List.<ResourceInfo, ListF<InstantInterval>>tuple2List()
                .plus1(testOffice1.getResource1(), Cf.list(new InstantInterval(minutes(0), minutes(120))))
                .plus1(testOffice1.getResource2(), Cf.list(new InstantInterval(minutes(170), minutes(225))))
                .plus1(testOffice1.getResource3(), Cf.list(new InstantInterval(minutes(170), minutes(225)), new InstantInterval(minutes(240), minutes(300))));

        IntervalSet usersFreeIntervals = IntervalSet.cons(Cf.list(new InstantInterval(minutes(0), minutes(400))));

        UsersAndResourcesAvailability availability = UsersAndResourcesAvailability.onlyUsers(usersFreeIntervals)
                .plusOffice(testOffice1.getOffice(), office1FreeIntervals, 1);

        AvailableResourcesFinder finder = new AvailableResourcesFinder(
                availability, minutes(0), durationMinutes(35), new InstantInterval(minutes(0), minutes(400)));

        ListF<AvailableResourcesInOffices> suggest = finder.findAvailableResourcesInOffices();

        SuggestTestHelper.assertSuggestEquals(0, 35, Cf.map(1L, Cf.list(11L)), suggest.get(0));
        SuggestTestHelper.assertSuggestEquals(30, 65, Cf.map(1L, Cf.list(11L)), suggest.get(1));
        SuggestTestHelper.assertSuggestEquals(60, 95, Cf.map(1L, Cf.list(11L)), suggest.get(2));
        SuggestTestHelper.assertSuggestEquals(180, 215, Cf.map(1L, Cf.list(12L, 13L)), suggest.get(3));
        SuggestTestHelper.assertSuggestEquals(240, 275, Cf.map(1L, Cf.list(13L)), suggest.get(4));
        Assert.hasSize(5, suggest);

        Tuple2List<ResourceInfo, ListF<InstantInterval>> office2FreeIntervals = Tuple2List.<ResourceInfo, ListF<InstantInterval>>tuple2List()
                .plus1(testOffice2.getResource1(), Cf.list(new InstantInterval(minutes(25), minutes(100))))
                .plus1(testOffice2.getResource2(), Cf.list(new InstantInterval(minutes(100), minutes(350))))
                .plus1(testOffice2.getResource3(), Cf.list(new InstantInterval(minutes(100), minutes(350))));

        availability = availability.plusOffice(testOffice2.getOffice(), office2FreeIntervals, 1);

        finder = new AvailableResourcesFinder(
                availability, minutes(0), durationMinutes(35), new InstantInterval(minutes(0), minutes(400)));

        suggest = finder.findAvailableResourcesInOffices();

        SuggestTestHelper.assertSuggestEquals(30, 65, Cf.map(1L, Cf.list(11L), 2L, Cf.list(21L)), suggest.get(0));
        SuggestTestHelper.assertSuggestEquals(60, 95, Cf.map(1L, Cf.list(11L), 2L, Cf.list(21L)), suggest.get(1));
        SuggestTestHelper.assertSuggestEquals(180, 215, Cf.map(1L, Cf.list(12L, 13L), 2L, Cf.list(22L, 23L)), suggest.get(2));
        SuggestTestHelper.assertSuggestEquals(240, 275, Cf.map(1L, Cf.list(13L), 2L, Cf.list(22L, 23L)), suggest.get(3));
        Assert.hasSize(4, suggest);
    }

    @Test
    public void fromFreeIntervals() {
        TestOffice testOffice1 = SuggestTestHelper.createTestOffice(1, 11, 12, 13);

        Tuple2List<ResourceInfo, ListF<InstantInterval>> office1FreeIntervals = Tuple2List.<ResourceInfo, ListF<InstantInterval>>tuple2List()
                .plus1(testOffice1.getResource1(), Cf.list(new InstantInterval(minutes(0), minutes(120))))
                .plus1(testOffice1.getResource2(), Cf.list(new InstantInterval(minutes(170), minutes(225))))
                .plus1(testOffice1.getResource3(), Cf.list(new InstantInterval(minutes(170), minutes(225)), new InstantInterval(minutes(240), minutes(300))));


        IntervalSet usersFreeIntervals = IntervalSet.cons(Cf.<InstantInterval>list());

        UsersAndResourcesAvailability availability = UsersAndResourcesAvailability.onlyUsers(usersFreeIntervals)
                .plusOffice(testOffice1.getOffice(), office1FreeIntervals, 1);

        AvailableResourcesFinder finder = new AvailableResourcesFinder(
                availability, minutes(0), durationMinutes(35), new InstantInterval(minutes(0), minutes(400)));

        ListF<AvailableResourcesInOffices> suggest = finder.findAvailableResourcesInOffices();

        Assert.hasSize(0, suggest);

        usersFreeIntervals = IntervalSet.cons(Cf.list(new InstantInterval(minutes(25), minutes(100)), new InstantInterval(minutes(150), minutes(300))));

        availability = UsersAndResourcesAvailability.onlyUsers(usersFreeIntervals)
                .plusOffice(testOffice1.getOffice(), office1FreeIntervals, 1);

        finder = new AvailableResourcesFinder(
                availability, minutes(0), durationMinutes(35), new InstantInterval(minutes(0), minutes(400)));

        suggest = finder.findAvailableResourcesInOffices();

        SuggestTestHelper.assertSuggestEquals(30, 65, Cf.map(1L, Cf.list(11L)), suggest.get(0));
        SuggestTestHelper.assertSuggestEquals(60, 95, Cf.map(1L, Cf.list(11L)), suggest.get(1));
        SuggestTestHelper.assertSuggestEquals(180, 215, Cf.map(1L, Cf.list(12L, 13L)), suggest.get(2));
        SuggestTestHelper.assertSuggestEquals(240, 275, Cf.map(1L, Cf.list(13L)), suggest.get(3));
        Assert.hasSize(4, suggest);
    }

    @Test
    public void mixedEventInterval() {
        TestOffice office = SuggestTestHelper.createTestOffice(1, 11, 12, 13);

        InstantInterval freeInterval = new InstantInterval(minutes(0), minutes(60));
        UsersAndResourcesAvailability availability = UsersAndResourcesAvailability
                .onlyUsers(IntervalSet.cons(Cf.list(freeInterval)))
                .plusOffice(office.getOffice(), Tuple2List.fromPairs(office.getResource1(), Cf.list(freeInterval)), 1);

        ListF<AvailableResourcesInOffices> suggest = new AvailableResourcesFinder(
                availability, minutes(13), durationMinutes(30), freeInterval).findAvailableResourcesInOffices();

        SuggestTestHelper.assertSuggestEquals(0, 30, Cf.map(1L, Cf.list(11L)), suggest.get(0));
        SuggestTestHelper.assertSuggestEquals(13, 43, Cf.map(1L, Cf.list(11L)), suggest.get(1));
        SuggestTestHelper.assertSuggestEquals(30, 60, Cf.map(1L, Cf.list(11L)), suggest.get(2));
    }

    private static Instant minutes(int i) {
        return new Instant(i * 60 * 1000);
    }

    private static Duration durationMinutes(int m) {
        return Duration.standardMinutes(m);
    }

}
