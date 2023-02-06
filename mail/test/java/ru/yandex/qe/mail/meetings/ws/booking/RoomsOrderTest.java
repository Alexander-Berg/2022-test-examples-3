package ru.yandex.qe.mail.meetings.ws.booking;

import java.util.Collections;
import java.util.Set;

import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.Test;

import ru.yandex.qe.mail.meetings.booking.impl.TimeTableImpl;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper;
import ru.yandex.qe.mail.meetings.ws.booking.util.DatePositionInterval;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.SPB;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.locationOf;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.personWithSingleFreeIntervalOnWeek;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.room;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePosition.week;

public class RoomsOrderTest {
    private final Interval terms = week();
    private final Duration duration = Duration.standardMinutes(30);

    private final BookingTestHelper.ObjectWithTimeTable<Person> person1 = personWithSingleFreeIntervalOnWeek(SPB,1, "12:00-13:00");
    private final BookingTestHelper.ObjectWithTimeTable<Person> person2 = personWithSingleFreeIntervalOnWeek(SPB,1, "12:00-13:00");

    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> roomLarge = room(
            locationOf(SPB),
            10,
            BookingTestHelper.RoomParams.NONE
    );

    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> roomSmall = room(
            locationOf(SPB),
            3,
            BookingTestHelper.RoomParams.NONE
    );

    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> roomForSingle = room(
            locationOf(SPB),
            1,
            BookingTestHelper.RoomParams.NONE
    );

    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> roomWithLcd = room(
            locationOf(SPB),
            10,
            BookingTestHelper.RoomParams.LCD
    );

    private final BookingTestHelper.BookingObjects defaultObjects = BookingTestHelper.Config.create()
            .addPersons(person1, person2)
            .addRooms(roomSmall, roomForSingle, roomLarge, roomWithLcd)
            .makeResourceUnvisible(person2.obj, roomSmall.obj)
            .createObjects();

    @Test
    public void mustChooseSmallRoom() {
        var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                person1.obj.getLogin(),
                Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                false
        );

        assertFalse(result.isEmpty());
        assertEquals(result.additional().size(), 1);

        var suggested = result.additional().get(0);

        assertEquals(roomSmall.obj.getId(), suggested.rooms().get(0).getId());
        assertEquals("1::12:00-1::12:30", DatePositionInterval.fromInterval(suggested.interval()).toString());
    }

    @Test
    public void cantBookUnavailableResourceForPerson() {
        {
            // person1 has access to smallRoom
            var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                    TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                    duration,
                    person1.obj.getLogin(),
                    Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                    Collections.emptySet(),
                    new Resource.Info(),
                    false
            );

            assertFalse(result.isEmpty());
            assertEquals(result.additional().size(), 1);

            var suggested = result.additional().get(0);

            assertEquals(roomSmall.obj.getId(), suggested.rooms().get(0).getId());
            assertEquals("1::12:00-1::12:30", DatePositionInterval.fromInterval(suggested.interval()).toString());
        }

        {
            // person2 does NOT have access to smallRoom
            var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                    TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                    duration,
                    person2.obj.getLogin(),
                    Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                    Collections.emptySet(),
                    new Resource.Info(),
                    false
            );

            assertFalse(result.isEmpty());
            assertEquals(result.additional().size(), 1);

            var suggested = result.additional().get(0);

            // важно, что выбирается large, а не lcd, т.к. у них одинаковый capacity, но large имеет меньше спецц ресурсов
            assertEquals(roomLarge.obj.getId(), suggested.rooms().get(0).getId());
            assertEquals("1::12:00-1::12:30", DatePositionInterval.fromInterval(suggested.interval()).toString());
        }
    }

    @Test
    public void additionalRoomParamsMustBeNoticed() {
        var roomRestrictions = new Resource.Info();
        roomRestrictions.setLcdPanel(1);

        var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                person1.obj.getLogin(),
                Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                Collections.emptySet(),
                roomRestrictions,
                false
        );

        assertFalse(result.isEmpty());
        var suggested = result.additional().get(0);
        assertEquals(roomWithLcd.obj.getId(), suggested.rooms().get(0).getId());
    }

    @Test
    public void additionalParamsCanBeMissing() {
        var roomRestrictions = new Resource.Info();
        roomRestrictions.setVoiceConferencing(true);

        var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                person1.obj.getLogin(),
                Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                Collections.emptySet(),
                roomRestrictions,
                false
        );

        assertFalse(result.isEmpty());
        var suggested = result.additional().get(0);
        assertEquals(roomSmall.obj.getId(), suggested.rooms().get(0).getId());
    }
}
