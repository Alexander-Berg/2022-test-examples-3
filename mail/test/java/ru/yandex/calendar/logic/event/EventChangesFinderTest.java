package ru.yandex.calendar.logic.event;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sharing.participant.EventParticipants;
import ru.yandex.calendar.logic.sharing.perm.EventInfoForPermsCheck;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.time.InstantInterval;

public class EventChangesFinderTest extends AbstractConfTest {
    @Autowired
    private EventChangesFinder eventChangesFinder;

    @Test
    public void eventInstanceIntervalChanges() {
        DateTime eventStart = TestDateTimes.moscowDateTime(2010, 12, 9, 10, 0);
        DateTime eventEnd = TestDateTimes.moscowDateTime(2010, 12, 9, 10, 0);
        InstantInterval eventInterval = new InstantInterval(
                eventStart.toInstant(),
                eventEnd.toInstant());

        Event event = new Event();
        event.setId(1L);
        event.setStartTs(eventStart.minusDays(1).toInstant());
        event.setEndTs(eventEnd.toDateTime().minusDays(1).toInstant());
        event.setIsAllDay(false);

        MainEvent mainEvent = new MainEvent();
        mainEvent.setTimezoneId("UTC");

        EventWithRelations eventWithRelations = new EventWithRelations(
                event, mainEvent,
                new EventParticipants(1, Cf.list(), Cf.list(), Cf.list(), Cf.list()), new EventLayers(Cf.list()));

        EventLayer eventLayer = new EventLayer();
        eventLayer.setLayerId(2L);

        EventInfoForPermsCheck infoForPermsCheck = null; // XXX

        EventInstanceForUpdate eventInstance = new EventInstanceForUpdate(
                Option.of(eventInterval), infoForPermsCheck, eventWithRelations,
                RepetitionInstanceInfo.noRepetition(eventInterval),
                Option.empty(),
                Option.of(eventLayer),
                Cf.list());

        EventData newEventData = new EventData();
        newEventData.getEvent().setStartTs(eventInterval.getStart());
        newEventData.getEvent().setEndTs(eventInterval.getEnd());
        EventChangesInfo changes = eventChangesFinder.getEventChangesInfo(
                eventInstance, newEventData, NotificationsData.notChanged(), Option.empty(), true, false);
        Assert.assertTrue(!changes.wasChange());
    }
}
