package ru.yandex.qe.mail.meetings.ws.booking;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.Test;

import ru.yandex.qe.mail.meetings.booking.impl.TimeTableImpl;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Repetition;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;
import ru.yandex.qe.mail.meetings.services.calendar.dto.suggest.SuggestResponse;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.utils.RepetitionUtils;
import ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper;
import ru.yandex.qe.mail.meetings.ws.booking.util.DatePositionInterval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.SPB;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.personWithSingleFreeIntervalOnWeek;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.roomWithSingleFreeIntervalOnWeek;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePosition.dp;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePosition.week;

public class RegularMeetingTest {
    private static final ThreadLocal<SimpleDateFormat> _SDF = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));

    private final Interval terms = week();
    private final Duration duration = Duration.standardMinutes(30);

    private final BookingTestHelper.ObjectWithTimeTable<Person> person1 = personWithSingleFreeIntervalOnWeek(SPB,1, "12:00-13:00");
    private final BookingTestHelper.ObjectWithTimeTable<Person> person2 = personWithSingleFreeIntervalOnWeek(SPB,1, "12:30-13:00");

    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> spbRoomForRegular = roomWithSingleFreeIntervalOnWeek(SPB, 1, "10:00-18:00");
    private final BookingTestHelper.ObjectWithTimeTable<Resource.Info> spbRoom = roomWithSingleFreeIntervalOnWeek(SPB, 1, "10:00-18:00");

    private final BookingTestHelper.Config config = BookingTestHelper.Config.create()
            .addPersons(person1, person2)
            .addRooms(spbRoom);

    @Test
    public void canBookRegularWithoutAdditionalMeetings() {
        var place = new SuggestResponse.Place();
        place.setOfficeId(SPB.getId());
        place.setResources(List.of(spbRoomForRegular.obj));

        var interval = new SuggestResponse.Interval();
        interval.setStart(_SDF.get().format(dp(1, 10, 0).toDate()));
        interval.setEnd(_SDF.get().format(dp(1, 10, 30).toDate()));
        interval.setPlaces(List.of(place));

        var objects = config.createObjects();
        objects.calendarWeb.setSuggestIntervals(List.of(interval));

        var result = objects.fuzzyBookingService.findTimeSlotAndRoomForRegular(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                person1.obj.getLogin(),
                Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                RepetitionUtils.buildRepetition(Repetition.Type.WEEKLY, 1)
        );

        assertFalse(result.isEmpty());
        assertTrue(result.isRegular());

        assertEquals(1, result.regular().rooms().size());

        var actualRegularRoom = result.regular().rooms().get(0);
        assertEquals(spbRoomForRegular.obj.getId(), actualRegularRoom.getId());

        assertEquals("1::10:00-1::10:30", DatePositionInterval.fromInterval(result.regular().interval()).toString());

        assertEquals(0, result.additional().size());
    }


    @Test
    public void canBookRegularWithAdditionalMeetings() {
        var place = new SuggestResponse.Place();
        place.setOfficeId(SPB.getId());
        place.setResources(List.of(spbRoomForRegular.obj));

        var interval = new SuggestResponse.Interval();
        interval.setStart(_SDF.get().format(dp(9, 10, 0).toDate()));
        interval.setEnd(_SDF.get().format(dp(9, 10, 30).toDate()));
        interval.setPlaces(List.of(place));

        var objects = config.createObjects();
        objects.calendarWeb.setSuggestIntervals(List.of(interval));

        var result = objects.fuzzyBookingService.findTimeSlotAndRoomForRegular(
                TimeTableImpl.fromEventDates(terms, Collections.emptyList()),
                duration,
                person1.obj.getLogin(),
                Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                RepetitionUtils.buildRepetition(Repetition.Type.WEEKLY, 1)
        );

        assertFalse(result.isEmpty());
        assertTrue(result.isRegular());

        assertEquals(1, result.regular().rooms().size());

        var actualRegularRoom = result.regular().rooms().get(0);
        assertEquals(spbRoomForRegular.obj.getId(), actualRegularRoom.getId());

        assertEquals("9::10:00-9::10:30", DatePositionInterval.fromInterval(result.regular().interval()).toString());

        assertEquals(1, result.additional().size());
    }

    @Test
    public void canSetFirstMeetingDateForRegular() {
        var leftBorder = dp(7, 0, 0);
        var rightBorder = dp(14, 0, 0);
        var shiftedTerms = new Interval(leftBorder.toMillis(), rightBorder.toMillis());

        var place = new SuggestResponse.Place();
        place.setOfficeId(SPB.getId());
        place.setResources(List.of(spbRoomForRegular.obj));

        var interval = new SuggestResponse.Interval();
        interval.setStart(_SDF.get().format(dp(9, 10, 0).toDate()));
        interval.setEnd(_SDF.get().format(dp(9, 10, 30).toDate()));
        interval.setPlaces(List.of(place));

        var objects = config.createObjects();
        objects.calendarWeb.setSuggestIntervals(List.of(interval));

        var result = objects.fuzzyBookingService.findTimeSlotAndRoomForRegular(
                TimeTableImpl.fromEventDates(shiftedTerms, Collections.emptyList()),
                duration,
                person1.obj.getLogin(),
                Set.of(person1.obj.getLogin(), person2.obj.getLogin()),
                Collections.emptySet(),
                new Resource.Info(),
                RepetitionUtils.buildRepetition(Repetition.Type.WEEKLY, 1)
        );

        assertFalse(result.isEmpty());
        assertTrue(result.isRegular());

        assertEquals(1, result.regular().rooms().size());

        var actualRegularRoom = result.regular().rooms().get(0);
        assertEquals(spbRoomForRegular.obj.getId(), actualRegularRoom.getId());

        assertEquals("9::10:00-9::10:30", DatePositionInterval.fromInterval(result.regular().interval()).toString());

        var additionalMeetingsInTerms = result.additional().stream().filter(sr -> sr.interval().getStart().isAfter(terms.getStart())).count();
        assertEquals(0, additionalMeetingsInTerms);
    }
}
