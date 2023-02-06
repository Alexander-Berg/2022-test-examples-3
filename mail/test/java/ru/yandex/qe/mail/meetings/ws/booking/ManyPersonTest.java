package ru.yandex.qe.mail.meetings.ws.booking;

import java.util.Collections;
import java.util.Set;

import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.Test;

import ru.yandex.qe.mail.meetings.booking.dto.FullSearchResult;
import ru.yandex.qe.mail.meetings.booking.impl.TimeTableImpl;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper;
import ru.yandex.qe.mail.meetings.ws.booking.util.DatePositionInterval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.MSK;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.SPB;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.basicInfoFor;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.personWithSingleFreeIntervalOnWeek;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.roomWithSingleFreeIntervalOnWeek;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePosition.week;

public class ManyPersonTest {
    private final Interval terms = week();
    private final Duration duration = Duration.standardMinutes(30);

    private final BookingTestHelper.ObjectWithTimeTable<Person> person1 = personWithSingleFreeIntervalOnWeek(SPB,1, "12:00-13:00");
    private final BookingTestHelper.ObjectWithTimeTable<Person> person2 = personWithSingleFreeIntervalOnWeek(SPB,1, "12:30-13:00");
    private final BookingTestHelper.ObjectWithTimeTable<Person> person3 = personWithSingleFreeIntervalOnWeek(MSK,1, "12:00-13:00");
    private final BookingTestHelper.ObjectWithTimeTable<Person> person4 = personWithSingleFreeIntervalOnWeek(MSK,1, "13:00-14:00");
    private final BookingTestHelper.ObjectWithTimeTable<Person> person5 = personWithSingleFreeIntervalOnWeek(SPB,2, "12:00-13:00");
    private final BookingTestHelper.ObjectWithTimeTable<Person> person6 = personWithSingleFreeIntervalOnWeek(SPB,2, "12:30-13:00");

    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> spbRoom = roomWithSingleFreeIntervalOnWeek(SPB, 1, "10:00-18:00");
    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> mskRoom = roomWithSingleFreeIntervalOnWeek(MSK, 1, "10:00-18:00");


    private final BookingTestHelper.BookingObjects defaultObjects = BookingTestHelper.Config.create()
            .addPersons(person1, person2, person3, person4, person5, person6)
            .addRooms(spbRoom, mskRoom)
            .createObjects();

    /**
     * 2 пользователя могут забронировать встречу, которая подойдет обоим
     */
    @Test
    public void twoPersonCanBookMeeting() {
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
        assertEquals(1, suggested.rooms().size());
        assertEquals(spbRoom.obj.getId(), suggested.rooms().get(0).getId());
        assertEquals("1::12:30-1::13:00", DatePositionInterval.fromInterval(suggested.interval()).toString());
    }

    @Test
    public void twoPersonsWithEmptyIntersectionCantBookMeeting() {
        var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                person1.obj.getLogin(),
                Set.of(person1.obj.getLogin(), person4.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                false
        );

        assertTrue(result.isEmpty());
        assertEquals(FullSearchResult.MissingReason.PERSON_HAVE_NO_INTERSECTION, result.missingReason());
    }


    @Test
    public void additionalAttendeeIsNotMondatory() {
        var result = defaultObjects.fuzzyBookingService.bookMeeting(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                basicInfoFor(person1.obj.getLogin()),
                Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                Set.of(person4.obj.getLogin()),
                new Resource.Info(),
                false,
                null,
                0
        );

        assertTrue(result.isOk());
    }

    @Test
    public void twoPersonFromDifferentCityCanBookMeeting() {
        var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                person1.obj.getLogin(),
                Set.of(person3.obj.getLogin(), person2.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                false
        );

        assertFalse(result.isEmpty());
        assertEquals(result.additional().size(), 1);

        var suggested = result.additional().get(0);
        assertEquals(2, suggested.rooms().size());
        assertEquals("1::12:30-1::13:00", DatePositionInterval.fromInterval(suggested.interval()).toString());
    }


    @Test
    public void canOrCantCreateMeetingWithoutRoom() {
        var p1 = person5;
        var p2 = person6;

        var result1 = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                p1.obj.getLogin(),
                Set.of(p1.obj.getLogin(), p2.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                false
        );

        assertTrue(result1.isEmpty());
        assertEquals(FullSearchResult.MissingReason.NO_FREE_ROOMS, result1.missingReason());

        var result2 = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                p1.obj.getLogin(),
                Set.of(p1.obj.getLogin(), p2.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                true
        );

        assertFalse(result2.isEmpty());
        assertEquals(0, result2.additional().get(0).rooms().size());

    }
}
