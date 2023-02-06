package ru.yandex.calendar.logic.event;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.ReadableInstant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.resource.ResourceInaccessibility;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.SpecialResources;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.dates.TimeField;
import ru.yandex.calendar.util.dates.TimesInUnit;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class EventRoutinesInaccessibleResourcesTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private ResourceRoutines resourceRoutines;

    @Test
    public void eventsLimited() {
        TestUserInfo user1 = testManager.prepareRandomYaTeamUser(1123);
        TestUserInfo user2 = testManager.prepareRandomYaTeamUser(2234);

        Resource resource = testManager.cleanAndCreateResource("resource_events_limited", "Resource");

        DateTime start = MoscowTime.dateTime(2018, 1, 11, 20, 0);

        InaccessibleResourcesRequest request = InaccessibleResourcesRequest.verify(
                Cf.list(resource.getId()).map(resourceRoutines.getResourceInfoByIdF()),
                Cf.list(), RepetitionInstanceInfo.noRepetition(new InstantInterval(start, Duration.millis(100500))),
                Option.empty(), start.toInstant());

        Function<TestUserInfo, Option<ResourceInaccessibility.Reason>> find = user ->
                eventRoutines.findInaccessibleResources(Option.of(user.getUserInfo()), request).singleO()
                        .map(ResourceInaccessibility::getReason);

        testManager.setValue(SpecialResources.eventsLimitedRooms, Cf.list(new SpecialResources.RoomsEvents(
                SpecialResources.Rooms.of(resource), new TimesInUnit(2, TimeField.MONTH))));

        testManager.setValue(SpecialResources.eventsByUserLimitedRooms, Cf.list(new SpecialResources.RoomsEvents(
                SpecialResources.Rooms.of(resource), new TimesInUnit(1, TimeField.WEEK))));

        createEvent(user1, resource, start.plusDays(1));

        Assert.some(ResourceInaccessibility.Reason.TOO_MANY_EVENTS_BY_USER, find.apply(user1));
        Assert.none(find.apply(user2));

        createEvent(user2, resource, start.plusWeeks(1));

        Assert.some(ResourceInaccessibility.Reason.TOO_MANY_EVENTS, find.apply(user1));
        Assert.some(ResourceInaccessibility.Reason.TOO_MANY_EVENTS, find.apply(user2));
    }

    private void createEvent(TestUserInfo user, Resource resource, ReadableInstant start) {
        Event event = testManager.createDefaultEvent(user.getUid(), "X", start);

        testManager.addUserParticipantToEvent(event.getId(), user, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        testManager.updateEventTimeIndents(event);
    }
}
