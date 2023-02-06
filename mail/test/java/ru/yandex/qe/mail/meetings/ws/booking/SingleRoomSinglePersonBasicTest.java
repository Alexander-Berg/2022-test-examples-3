package ru.yandex.qe.mail.meetings.ws.booking;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import ru.yandex.qe.mail.meetings.booking.dto.FullSearchResult;
import ru.yandex.qe.mail.meetings.booking.impl.TimeTableImpl;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;
import ru.yandex.qe.mail.meetings.services.calendar.dto.WebEventCreateData;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper;
import ru.yandex.qe.mail.meetings.ws.booking.util.DatePositionInterval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.AURORA_CAL_ID;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.AURORA_STAFF_ID;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.Config;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.SPB;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.basicInfoFor;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.personWithSingleFreeIntervalOnWeek;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.roomWithSingleFreeIntervalOnWeek;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePosition.week;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePositionInterval.dpInterval;


public class SingleRoomSinglePersonBasicTest {
    private final Interval terms = week();
    private final Duration duration = Duration.standardMinutes(30);

    private final BookingTestHelper.ObjectWithTimeTable<Person> person = personWithSingleFreeIntervalOnWeek(SPB, 1, "12:00-13:00");
    private final BookingTestHelper.ObjectWithTimeTable<Person> auroraPerson = personWithSingleFreeIntervalOnWeek(AURORA_STAFF_ID, 1, "12:00-13:00");
//    746, 181

    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> spbRoom = roomWithSingleFreeIntervalOnWeek(SPB,1, "12:00-13:30");
    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> auroraRoom = roomWithSingleFreeIntervalOnWeek(AURORA_CAL_ID,1, "12:00-13:30");

    private final BookingTestHelper.BookingObjects defaultObjects = Config.create()
            .addPerson(person)
            .addPerson(auroraPerson)
            .addRoom(spbRoom)
            .addRoom(auroraRoom)
            .createObjects();

    @Before
    public void setup() {
        reset(defaultObjects.calendarUpdate);
        BookingTestHelper.doMock(defaultObjects.calendarUpdate);
    }
    /**
     * Проверяем, что простое бронирование на одного человека работает
     */
    @Test
    public void checkSimpleSingleBookingIsOk() {
        var result = defaultObjects.fuzzyBookingService.bookMeeting(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                basicInfoFor(person.obj.getLogin()),
                Collections.emptySet(),
                Collections.emptySet(),
                new Resource.Info(),
                false,
                null,
                0
        );
        assertTrue("Booking must be success", result.isOk());

        verify(defaultObjects.calendarUpdate, times(1)).createEvent(Matchers.argThat(new BaseMatcher<>() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public boolean matches(Object item) {
                WebEventCreateData data = (WebEventCreateData) item;
                var attendees = new HashSet<>(data.getAttendees());
                var expectedLogins = Set.of(
                        person.obj.getLogin() + "@yandex-team.ru",
                        spbRoom.obj.getName() + "@yandex-team.ru"
                );
                assertEquals("name", "name", data.getName());
                assertEquals("desc", "desc", data.getDescription());
                assertEquals("duration", duration.getMillis(), (data.getEndTs().getTime() - data.getStartTs().getTime()));
                assertEquals("attendees must be equal", expectedLogins, attendees);
                return true;
            }
        }));
    }

    /**
     * Проверяем все параметры брони: переговорку, время
     */
    @Test
    public void meetingParamsMustBeCorrect() {
        var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                person.obj.getLogin(),
                Set.of(person.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                false
        );
        assertFalse(result.isEmpty());
        assertEquals(result.additional().size(), 1);

        var suggested = result.additional().get(0);
        assertEquals(1, suggested.rooms().size());
        assertEquals(SPB.getId(), suggested.offices().stream().findFirst().get().intValue());
        assertEquals(spbRoom.obj.getId(), suggested.rooms().get(0).getId());
        assertEquals("1::12:00-1::12:30", DatePositionInterval.fromInterval(suggested.interval()).toString());
    }


    /**
     * Встреча на 45 минут не может быть забронирована
     */
    @Test
    public void longMeetingCantBeBooked() {
        var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                Duration.standardMinutes(145),
                person.obj.getLogin(),
                Set.of(person.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                false
        );
        assertTrue(result.isEmpty());
        assertEquals(FullSearchResult.MissingReason.PERSON_HAVE_NO_INTERSECTION, result.missingReason());
    }

    /**
     * Проверяем, что запрет на проведение встреч с 12:15 до 12:25 будет работать
     */
    @Test
    public void timeRestrictionsMustBeNoticed() {
        var result = defaultObjects.fuzzyBookingService.findTimeSlotAndRoom(
                // встреча возможна в течении недели кроме указанного временного участка в пн
                TimeTableImpl.fromEventDates(terms, List.of(dpInterval(1, "12:15-12:25"))),
                duration,
                person.obj.getLogin(),
                Set.of(person.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                false
        );
        assertFalse(result.isEmpty());
        assertEquals(result.additional().size(), 1);

        var suggested = result.additional().get(0);
        assertEquals(1, suggested.rooms().size());
        assertEquals(spbRoom.obj.getId(), suggested.rooms().get(0).getId());
        assertEquals("1::12:25-1::12:55", DatePositionInterval.fromInterval(suggested.interval()).toString());
    }

    /**
     * пароверяем работу отображения staff_office_id в calendar_office_id
     */
    @Test
    public void userFromAuroraCanBook() {
        var result = defaultObjects.fuzzyBookingService.bookMeeting(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                basicInfoFor(auroraPerson.obj.getLogin()),
                Collections.emptySet(),
                Collections.emptySet(),
                new Resource.Info(),
                false,
                null,
                0
        );
        assertTrue("Booking must be success", result.isOk());

        verify(defaultObjects.calendarUpdate, times(1)).createEvent(Matchers.argThat(new BaseMatcher<>() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public boolean matches(Object item) {
                WebEventCreateData data = (WebEventCreateData) item;
                var attendees = new HashSet<>(data.getAttendees());
                var expectedLogins = Set.of(
                        auroraPerson.obj.getLogin() + "@yandex-team.ru",
                        auroraRoom.obj.getName() + "@yandex-team.ru"
                );
                assertEquals("name", "name", data.getName());
                assertEquals("desc", "desc", data.getDescription());
                assertEquals("duration", duration.getMillis(), (data.getEndTs().getTime() - data.getStartTs().getTime()));
                assertEquals("attendees must be equal", expectedLogins, attendees);
                return true;
            }
        }));
    }
}
