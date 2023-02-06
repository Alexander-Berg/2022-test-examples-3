package ru.yandex.calendar.frontend.webNew;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function3;
import ru.yandex.calendar.frontend.bender.WebDate;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.actions.AvailabilityActions;
import ru.yandex.calendar.frontend.webNew.dto.in.AvailParameters;
import ru.yandex.calendar.frontend.webNew.dto.in.AvailabilitiesData;
import ru.yandex.calendar.frontend.webNew.dto.in.IntervalAndRepetitionData;
import ru.yandex.calendar.frontend.webNew.dto.in.SuggestData;
import ru.yandex.calendar.frontend.webNew.dto.out.HolidaysInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.LocalDateTimeInterval;
import ru.yandex.calendar.frontend.webNew.dto.out.ReservationInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.ResourcesSchedule;
import ru.yandex.calendar.frontend.webNew.dto.out.SubjectAvailability;
import ru.yandex.calendar.frontend.webNew.dto.out.SubjectAvailabilityIntervals.AvailabilityInterval;
import ru.yandex.calendar.frontend.webNew.dto.out.SubjectsAvailabilities;
import ru.yandex.calendar.frontend.webNew.dto.out.SubjectsAvailabilityIntervals;
import ru.yandex.calendar.frontend.webNew.dto.out.SuggestDatesInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.SuggestInfo;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.reservation.ResourceReservationManager;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.suggest.SuggestManager;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.util.base.Cf2;
import ru.yandex.calendar.util.dates.WeekdayConv;
import ru.yandex.commune.holidays.DayType;
import ru.yandex.commune.holidays.OutputMode;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

import static ru.yandex.calendar.logic.event.EventRoutines.getInstantInterval;

/**
 * @author dbrylev
 */
public class WebNewAvailabilityManagerTest extends WebNewTestWithResourcesBase {

    @Autowired
    private AvailabilityActions availabilityActions;
    @Autowired
    private WebNewAvailabilityManager webNewAvailabilityManager;
    @Autowired
    private ResourceReservationManager resourceReservationManager;
    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private SettingsRoutines settingsRoutines;


    private final DateTime now = MoscowTime.dateTime(2017, 12, 19, 16, 0);

    private final long reservationId = 127000828;

    @Override
    protected ReadableInstant now() {
        return now;
    }

    @Before
    public void setup() {
        super.setup();

        resourceReservationManager.deleteReservationsDeadlinedBefore(now.plusDays(1).toInstant());
    }


    @Test
    public void suggestMeetingTimesSingle() {
        DateTime userStart = now.plusHours(1);
        DateTime resourceStart = now.plusHours(2);

        createUserEvent(userStart, user);
        createResourceEvent(resourceStart, resource1);

        SuggestData request = createSuggestRequest();
        SuggestInfo.WithPlaces suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.equals(Cf.set(resource1Email, resource2Email),
                suggest.findResourceEmailsByStart(request.getEventStart()).get().unique());

        Assert.equals(Cf.set(resource2Email),
                suggest.findResourceEmailsByStart(resourceStart.toLocalDateTime()).get().unique());


        Assert.none(suggest.findByStart(userStart.toLocalDateTime()));

        request.setIgnoreUsersEvents(Option.of(true));

        Assert.some(suggestMeetingTimes(request).asWithPlaces().findByStart(userStart.toLocalDateTime()));
    }

    @Test
    public void suggestMeetingTimesRepeating() {
        LocalDate today = now.toLocalDate();

        DateTime userStart = now.plusHours(1).plusDays(1);
        DateTime resource1Start = now.plusHours(2).plusDays(2);
        DateTime resource2Start = now.plusHours(3).plusDays(10);

        createUserEvent(userStart, user);
        createResourceEvent(resource1Start, resource1);
        createResourceEvent(resource2Start, resource2);

        SuggestData request = createSuggestRequest();
        request.setRepetition(Option.of(consDailyRepetitionData()));

        SuggestInfo.WithPlaces suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.equals(Cf.set(resource1Email, resource2Email),
                suggest.findResourceEmailsByStart(request.getEventStart()).get().unique());

        Assert.none(suggest.findByStart(today.toLocalDateTime(userStart.toLocalTime())));

        Assert.equals(Cf.set(resource2Email),
                suggest.findResourceEmailsByStart(today.toLocalDateTime(resource1Start.toLocalTime())).get().unique());

        SuggestInfo.IntervalAndPlaces interval = suggest.findByStart(
                today.toLocalDateTime(resource2Start.toLocalTime())).get();

        Assert.equals(Cf.set(resource1Email, resource2Email), interval.getResourceEmails().unique());

        Assert.none(interval.findResource(resource1Email).get().getDueDate());
        Assert.some(resource2Start.toLocalDate().minusDays(1), interval.findResource(resource2Email).get().getDueDate());
    }

    @Test
    public void suggestMeetingTimesSingleSameRoom() {
        DateTime userStart = now.plusHours(1);

        DateTime resource1Start = now.plusHours(2);
        DateTime resource2Start = now.plusHours(3);

        createUserEvent(userStart, user);
        createResourceEvent(resource1Start, resource1);
        createResourceEvent(resource2Start, nextOfficeResource);

        SuggestData request = createSuggestRequest();
        request.setNumberOfOptions(Option.of(3));

        request.setMode(Option.of(SuggestData.Mode.SAME_ROOM));
        request.setOffices(Cf.list(
                new SuggestData.Office(office.getId(), Cf.list(resource1Email), Option.empty(), Option.empty()),
                new SuggestData.Office(nextOfficeResource.getOfficeId(), Cf.list(), Option.empty(), Option.empty())));

        SuggestInfo.WithNoPlaces suggest = suggestMeetingTimes(request).asWithNoPlaces();

        Assert.some(suggest.findByStart(request.getEventStart()));
        Assert.none(suggest.findByStart(userStart.toLocalDateTime()));

        Assert.none(suggest.findByStart(resource1Start.toLocalDateTime()));
        Assert.none(suggest.findByStart(resource2Start.toLocalDateTime()));

        request.setSearchStart(suggest.getNextSearchStart());
        request.setSearchBackward(Option.of(true));

        suggest = suggestMeetingTimes(request).asWithNoPlaces();

        Assert.some(suggest.findByStart(request.getEventStart()));
    }

    @Test
    public void suggestMeetingTimesRepeatingSameRoom() {
        LocalDate today = now.toLocalDate();

        DateTime resource11Start = now.plusHours(2).plusDays(1);
        DateTime resource21Start = now.plusHours(1);
        DateTime resource22Start = now.plusHours(3).plusDays(10);

        createResourceEvent(resource11Start, resource1);
        createResourceEvent(resource21Start, nextOfficeResource);
        createResourceEvent(resource22Start, nextOfficeResource);

        SuggestData request = createSuggestRequest();
        request.setNumberOfOptions(Option.of(5));

        request.setMode(Option.of(SuggestData.Mode.SAME_ROOM));
        request.setRepetition(Option.of(consDailyRepetitionData()));

        request.setOffices(Cf.list(
                new SuggestData.Office(office.getId(), Cf.list(resource1Email), Option.empty(), Option.empty()),
                new SuggestData.Office(nextOfficeResource.getOfficeId(), Cf.list(nextOfficeResourceEmail),
                        Option.empty(), Option.empty())));

        SuggestInfo.WithNoPlaces suggest = suggestMeetingTimes(request).asWithNoPlaces();

        Assert.some(suggest.findByStart(request.getEventStart()));

        Assert.none(suggest.findByStart(today.toLocalDateTime(resource11Start.toLocalTime())));

        Assert.none(suggest.findByStart(today.toLocalDateTime(resource21Start.toLocalTime())));

        Assert.some(resource22Start.toLocalDate().minusDays(1), suggest.findByStart(
                today.toLocalDateTime(resource22Start.toLocalTime())).flatMapO(SuggestInfo.IntervalWithDue::getDueDate));
    }

    @Test
    public void suggestMeetingTimesEventInterval() {
        SuggestData request = createSuggestRequest();

        request.setEventStart(now.withTime(23, 0, 0, 0).toLocalDateTime());
        request.setEventEnd(now.plusDays(1).withTime(1, 0, 0, 0).toLocalDateTime());

        SuggestInfo.WithPlaces suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.equals(request.getEventEnd(), suggest.findByStart(request.getEventStart()).get().getInterval().getEnd());

        request.setEventStart(now.toLocalDateTime());
        request.setEventEnd(now.plusHours(25).toLocalDateTime());

        Assert.isEmpty(suggestMeetingTimes(request).asWithPlaces().getIntervals());

        request.setEventStart(now.toLocalDateTime());
        request.setEventEnd(now.toLocalDateTime());

        Assert.isEmpty(suggestMeetingTimes(request).asWithPlaces().getIntervals());
    }

    @Test
    public void suggestMeetingsTimesNavigation() {
        SuggestData request = createSuggestRequest();
        SuggestInfo.WithPlaces suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.none(suggest.getBackwardSearchStart());
        Assert.some(suggest.getNextSearchStart());

        request.setSearchStart(suggest.getNextSearchStart());
        suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.isTrue(suggest.getIntervals().first().getInterval().getStart().isAfter(request.getSearchStart().get()));

        Assert.some(suggest.getBackwardSearchStart());
        Assert.some(suggest.getNextSearchStart());

        request.setSearchStart(suggest.getBackwardSearchStart());
        request.setSearchBackward(Option.of(true));

        suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.isTrue(suggest.getIntervals().last().getInterval().getStart().isBefore(request.getSearchStart().get()));

        Assert.none(suggest.getBackwardSearchStart());
        Assert.some(suggest.getNextSearchStart());
    }

    @Test
    public void suggestMeetingTimesSearchStartGap() {
        SuggestData request = createSuggestRequest();

        request.setEventStart(now.minusMinutes(10).toLocalDateTime());
        SuggestInfo.WithPlaces suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.equals(now.minusMinutes(10).toLocalDateTime(), suggest.getIntervals().first().getInterval().getStart());

        request.setEventStart(now.minusMinutes(40).toLocalDateTime());
        suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.equals(now.toLocalDateTime(), suggest.getIntervals().first().getInterval().getStart());

        request.setEventStart(now.plusMinutes(40).toLocalDateTime());
        request.setEventEnd(now.plusMinutes(70).toLocalDateTime());
        suggest = suggestMeetingTimes(request).asWithPlaces();

        Assert.equals(now.toLocalDateTime(), suggest.getIntervals().first().getInterval().getStart());
    }

    private SuggestInfo suggestMeetingTimes(SuggestData request) {
        return availabilityActions.suggestMeetingTimes(uid, request, AvailParameters.empty(), Option.empty());
    }

    @Test
    public void suggestMeetingsDates() {
        Function<Integer, LocalDate> date = dayOfMonth -> new LocalDate(2017, 12, dayOfMonth);
        Function<Integer, DateTime> midnight = date.andThen(Cf2.f(d -> d.toDateTimeAtStartOfDay(MoscowTime.TZ)));

        Event user1Event = createUserEvent(midnight.apply(21), Duration.standardDays(2), user);

        createResourceEvent(midnight.apply(26),  Duration.standardDays(1), resource1, resource2);

        SuggestData request = createSuggestRequest();

        request.setDate(Option.of(date.apply(22)));
        request.setDirection(Option.of(SuggestData.Direction.BOTH));

        SuggestDatesInfo suggest = availabilityActions.suggestMeetingDates(uid, request, AvailParameters.empty());
        Assert.equals(Cf.list(19, 20, 25, 27).map(date), suggest.getLocalDates());

        Assert.none(suggest.getBackwardDate());
        Assert.some(date.apply(27), suggest.getForwardDate());

        Assert.equals(Cf.list(19, 20, 21, 25, 27).map(date), availabilityActions.suggestMeetingDates(
                uid, request, AvailParameters.eventId(user1Event.getId())).getLocalDates());

        request.setIgnoreUsersEvents(Option.of(true));
        suggest = availabilityActions.suggestMeetingDates(uid, request, AvailParameters.empty());

        Assert.equals(Cf.list(19, 20, 21, 25, 27).map(date), suggest.getLocalDates());

        request.setDirection(Option.of(SuggestData.Direction.FORWARD));
        suggest = availabilityActions.suggestMeetingDates(uid, request, AvailParameters.empty());

        Assert.equals(Cf.list(25, 27).map(date), suggest.getLocalDates());

        request.setDate(Option.of(date.apply(25)));
        request.setDirection(Option.of(SuggestData.Direction.BACKWARD));

        suggest = availabilityActions.suggestMeetingDates(uid, request, AvailParameters.empty());

        Assert.equals(Cf.list(20, 21, 22).map(date), suggest.getLocalDates());
        Assert.some(date.apply(20), suggest.getBackwardDate());
    }

    @Test
    public void suggestMeetingResources() {
        DateTime nextDay = now.plusDays(1).withFields(SuggestManager.PREFERRED_TIME);

        Event resource1Event = createResourceEvent(nextDay.plusHours(1), Duration.standardHours(2), resource1);
        Event resource2Event = createResourceEvent(nextDay.minusHours(1), Duration.standardHours(1), resource2);

        LocalDateTimeInterval interval1 = new LocalDateTimeInterval(getInstantInterval(resource1Event), MoscowTime.TZ);
        LocalDateTimeInterval interval2 = new LocalDateTimeInterval(getInstantInterval(resource2Event), MoscowTime.TZ);

        SuggestData request = createSuggestRequest();
        request.setDate(Option.of(nextDay.toLocalDate()));

        ListF<ResourcesSchedule.Resource> suggest = suggestMeetingResources(request).getResources();

        Assert.equals(Cf.list(resource2.getId(), resource1.getId()), suggest.map(rs -> rs.getInfo().getId()));

        Assert.equals(interval1, suggest.last().getEvents().single().getInterval());
        Assert.equals(interval2, suggest.first().getEvents().single().getInterval());

        request.setSelectedStart(Option.of(interval2.getStart()));
        suggest = suggestMeetingResources(request).getResources();

        Assert.equals(Cf.list(resource1.getId(), resource2.getId()), suggest.map(rs -> rs.getInfo().getId()));
    }

    private ResourcesSchedule suggestMeetingResources(SuggestData request) {
        return availabilityActions.suggestMeetingResources(uid, request, AvailParameters.empty(), Option.empty());
    }

    private SuggestData createSuggestRequest() {
        SuggestData request = SuggestData.interval(now.toLocalDateTime(), now.plusMinutes(30).toLocalDateTime());

        request.setUsers(Cf.list(user.getEmail()));
        request.setOffices(Cf.list(new SuggestData.Office(office.getId(), Cf.list(), Option.empty(), Option.empty())));

        return request;
    }

    @Test
    public void getAvailabilityIntervalsRanges() {
        createResourceEvent(now, resource1);
        createResourceEvent(now.plusHours(1), resource1);

        Function3<Option<LocalDate>, Option<WebDate>, Option<WebDate>, ListF<AvailabilityInterval>> findIntervals =
                (date, from, to) -> getAvailabilityIntervals(
                        uid, Cf.list(resource1Email), date, from, to, Option.empty(), Option.empty())
                        .findIntervals(resource1Email).get().getIntervals().get();

        Assert.hasSize(2, findIntervals.apply(Option.of(now.toLocalDate()),
                Option.empty(), Option.empty()));

        Assert.hasSize(2, findIntervals.apply(Option.empty(),
                Option.of(WebDate.localDate(now.minusDays(1).toLocalDate())),
                Option.of(WebDate.localDate(now.toLocalDate()))));

        Assert.hasSize(1, findIntervals.apply(Option.empty(),
                Option.of(WebDate.dateTime(WebDateTime.dateTime(now))),
                Option.of(WebDate.dateTime(WebDateTime.dateTime(now.plusHours(1))))));

        Assert.hasSize(1, findIntervals.apply(Option.empty(),
                Option.of(WebDate.dateTime(WebDateTime.localDateTime(now.plusHours(1).toLocalDateTime()))),
                Option.of(WebDate.dateTime(WebDateTime.localDateTime(now.plusHours(2).toLocalDateTime())))));
    }

    @Test
    public void getAvailabilityIntervalsData() {
        Event event = testManager.createDefaultEvent(uid, "User event", now);

        testManager.addUserParticipantToEvent(event.getId(), uid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), uid2, Decision.MAYBE, false);
        testManager.addResourceParticipantToEvent(event.getId(), resource2);

        testManager.updateEventTimeIndents(event);

        Email externalEmail = new Email("xxx@yyy");

        SubjectsAvailabilityIntervals intervals = getAvailabilityIntervals(
                uid, Cf.list(user.getEmail(), resource1Email, externalEmail), Option.of(now.toLocalDate()),
                Option.empty(), Option.empty(), Option.of("events"), Option.empty());

        Assert.equals("ok", intervals.findIntervals(user.getEmail()).get().getStatus());
        Assert.equals("ok", intervals.findIntervals(resource1Email).get().getStatus());
        Assert.equals("unknown-user", intervals.findIntervals(externalEmail).get().getStatus());

        AvailabilityInterval interval = intervals.findIntervals(user.getEmail()).get().getIntervals().get().single();

        Assert.equals(event.getStartTs(), interval.getStart().toInstant(MoscowTime.TZ));
        Assert.equals(event.getEndTs(), interval.getEnd().toInstant(MoscowTime.TZ));

        Assert.some(false, interval.getIsAllDay());
        Assert.some(event.getId(), interval.getEventId());

        Assert.equals(Availability.MAYBE, interval.getAvailability());
        Assert.some(event.getName(), interval.getEventName());

        Assert.equals(user.getEmail(), interval.getOrganizer().get().getUserInfo().getEmail());
        Assert.equals(user2.getEmail(), interval.getAttendees().get().single().getUserInfo().getEmail());
        Assert.equals(resource2Email, interval.getResources().get().single().email);
    }

    @Test
    public void getAvailabilityIntervalsIdsOnly() {
        Event event = createResourceEvent(now, resource1);

        SubjectsAvailabilityIntervals intervals = getAvailabilityIntervals(
                uid, Cf.list(resource1Email), Option.of(now.toLocalDate()),
                Option.empty(), Option.empty(), Option.of("events"), Option.of("ids-only"));

        AvailabilityInterval interval = intervals.findIntervals(resource1Email).get().getIntervals().get().single();

        Assert.equals(event.getStartTs(), interval.getStart().toInstant(MoscowTime.TZ));
        Assert.equals(event.getEndTs(), interval.getEnd().toInstant(MoscowTime.TZ));

        Assert.some(event.getId(), interval.getEventId());

        Assert.equals(Availability.BUSY, interval.getAvailability());
        Assert.none(interval.getEventName());

        Assert.none(interval.getOrganizer());
        Assert.none(interval.getAttendees());
        Assert.none(interval.getResources());
    }

    @Test
    public void getAvailabilityIntervalsNoParticipants() {
        Event event = testManager.createDefaultEvent(uid, "User event", now);

        testManager.addUserParticipantToEvent(event.getId(), uid, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource1);

        testManager.updateEventTimeIndents(event);

        SubjectsAvailabilityIntervals intervals = getAvailabilityIntervals(
                uid, Cf.list(user.getEmail()), Option.of(now.toLocalDate()),
                Option.empty(), Option.empty(), Option.of("events"), Option.of("omit-participants"));

        AvailabilityInterval interval = intervals.findIntervals(user.getEmail()).get().getIntervals().get().single();

        Assert.equals(event.getStartTs(), interval.getStart().toInstant(MoscowTime.TZ));
        Assert.equals(event.getEndTs(), interval.getEnd().toInstant(MoscowTime.TZ));

        Assert.some(event.getId(), interval.getEventId());
        Assert.some(event.getName(), interval.getEventName());
        Assert.equals(resource1Email, interval.getResources().get().single().email);

        Assert.none(interval.getOrganizer());
        Assert.none(interval.getAttendees());
    }

    @Test
    public void getAvailabilityIntervalsRangesWithActions() {
        createResourceEvent(now, resource1);
        createResourceEvent(now.plusHours(1), resource1);
        Function3<Option<LocalDate>, Option<WebDate>, Option<WebDate>, ListF<AvailabilityInterval>> findIntervals =
                (date, from, to) -> getAvailabilityIntervals(
                        uid, Cf.list(resource1Email), date, from, to, Option.empty(), Option.of("full"))
                        .findIntervals(resource1Email).get().getIntervals().get();
        ListF<AvailabilityInterval> availIntervals = findIntervals.apply(Option.of(now.toLocalDate()),
                Option.empty(), Option.empty());
        Assert.hasSize(2, availIntervals);
        Assert.notNull(availIntervals.get(0).getActions());
        Assert.notNull(availIntervals.get(1).getActions());
    }

    private SubjectsAvailabilityIntervals getAvailabilityIntervals(
            PassportUid uid, ListF<Email> emails,
            Option<LocalDate> date, Option<WebDate> from, Option<WebDate> to,
            Option<String> display, Option<String> shape)
    {
        return availabilityActions.getAvailabilityIntervals(
                uid, emails, date, from, to, display, shape, Option.empty(),
                AvailParameters.empty(), Option.empty(), Option.empty(), Option.empty()).getBendable();
    }

    @Test
    public void getAvailabilitiesAvailableDue() {
        Event event = createResourceEvent(now.plusWeeks(1), resource1);

        AvailabilitiesData request = new AvailabilitiesData(Cf.list(resource1Email), new IntervalAndRepetitionData(
                now.toLocalDateTime(), now.plusHours(1).toLocalDateTime(), Option.of(consDailyRepetitionData())));

        SubjectsAvailabilities avails = availabilityActions.getAvailabilities(uid, request, AvailParameters.empty());

        SubjectAvailability availability = avails.findAvailability(resource1Email).get();

        Assert.equals("available", availability.getAvailability());
        Assert.some(new LocalDate(event.getStartTs(), MoscowTime.TZ).minusDays(1), availability.getDueDate());
    }

    @Test
    public void inactiveResourceAvailabilityAndReservation() {
        Resource data = new Resource();
        data.setId(resource1.getId());
        data.setIsActive(false);

        resourceRoutines.updateResource(data);

        createResourceEvent(now, resource1);

        AvailabilitiesData request = new AvailabilitiesData(Cf.list(resource1Email),
                new IntervalAndRepetitionData(now.toLocalDateTime(), now.plusHours(1).toLocalDateTime(), Option.empty()));

        ReservationInfo reservations = availabilityActions.reserveResources(
                uid, request, reservationId, Option.empty(), Option.empty(), AvailParameters.empty());

        SubjectsAvailabilities avails = availabilityActions.getAvailabilities(uid, request, AvailParameters.empty());

        Assert.some("not-active", reservations.findAvailabilityValue(resource1Email));
        Assert.some("not-active", avails.findAvailabilityValue(resource1Email));
    }

    @Test
    public void reserveResourcesAlreadyReserved() {
        AvailabilitiesData request = new AvailabilitiesData(Cf.list(resource1Email),
                new IntervalAndRepetitionData(now.toLocalDateTime(), now.plusHours(1).toLocalDateTime(), Option.empty()));

        ReservationInfo reservations = availabilityActions.reserveResources(
                uid, request, reservationId, Option.empty(), Option.empty(), AvailParameters.empty());

        SubjectsAvailabilities avails = availabilityActions.getAvailabilities(uid, request, AvailParameters.empty());

        Assert.some("available", reservations.findAvailabilityValue(resource1Email));
        Assert.some("available", avails.findAvailabilityValue(resource1Email));

        reservations = availabilityActions.reserveResources(
                uid2, request, reservationId, Option.empty(), Option.empty(), AvailParameters.empty());

        avails = availabilityActions.getAvailabilities(uid2, request, AvailParameters.empty());

        Assert.some("busy", reservations.findAvailabilityValue(resource1Email));
        Assert.some("busy", avails.findAvailabilityValue(resource1Email));
    }

    @Test
    public void reserveResourcesAlreadyBooked() {
        Event event = createResourceEvent(now, resource1);

        AvailabilitiesData request = new AvailabilitiesData(Cf.list(resource1Email), new IntervalAndRepetitionData(
                now.minusWeeks(1).toLocalDateTime(), now.minusWeeks(1).plusHours(1).toLocalDateTime(),
                Option.of(consDailyRepetitionData())));

        ReservationInfo reservations = availabilityActions.reserveResources(
                uid, request, reservationId, Option.empty(), Option.empty(), AvailParameters.empty());

        SubjectsAvailabilities avails = availabilityActions.getAvailabilities(uid, request, AvailParameters.empty());

        Assert.some("busy", reservations.findAvailabilityValue(resource1Email));
        Assert.some("busy", avails.findAvailabilityValue(resource1Email));

        reservations = availabilityActions.reserveResources(
                uid, request, reservationId, Option.empty(), Option.empty(), AvailParameters.eventId(event.getId()));

        avails = availabilityActions.getAvailabilities(uid, request, AvailParameters.eventId(event.getId()));

        Assert.some("available", reservations.findAvailabilityValue(resource1Email));
        Assert.some("available", avails.findAvailabilityValue(resource1Email));
    }

    @Test
    public void reserveResourcesKeepIfBusy() {
        DateTime start = now;

        Assert.some("available", reserveResources(uid, Cf.list(resource1Email),
                start, Option.empty(), Option.empty()).findAvailabilityValue(resource1Email));

        Assert.isTrue(isBusy(uid2, resource1Email, start));


        DateTime nextStart = now.plusHours(1);
        createResourceEvent(nextStart, resource2);

        Assert.some("busy", reserveResources(uid, Cf.list(resource1Email, resource2Email),
                nextStart, Option.of(true), Option.empty()).findAvailabilityValue(resource2Email));

        Assert.isTrue(isBusy(uid2, resource1Email, start));
        Assert.isFalse(isBusy(uid2, resource1Email, nextStart));


        Assert.some("busy", reserveResources(uid, Cf.list(resource1Email, resource2Email),
                nextStart, Option.of(false), Option.of(resource2Email)).findAvailabilityValue(resource2Email));

        Assert.isTrue(isBusy(uid2, resource1Email, start));
        Assert.isFalse(isBusy(uid2, resource1Email, nextStart));


        Assert.some("busy", reserveResources(uid, Cf.list(resource1Email, resource2Email),
                nextStart, Option.of(false), Option.of(resource1Email)).findAvailabilityValue(resource2Email));

        Assert.isFalse(isBusy(uid2, resource1Email, start));
        Assert.isTrue(isBusy(uid2, resource1Email, nextStart));
    }

    @Test
    public void cancelResourceReservation() {
        Assert.some("available", reserveResources(uid, Cf.list(resource1Email),
                now, Option.empty(), Option.empty()).findAvailabilityValue(resource1Email));

        Assert.isTrue(isBusy(uid2, resource1Email, now));

        availabilityActions.cancelResourcesReservation(uid2, reservationId);

        Assert.isTrue(isBusy(uid2, resource1Email, now));

        availabilityActions.cancelResourcesReservation(uid, reservationId);

        Assert.isFalse(isBusy(uid2, resource1Email, now));
    }

    private boolean isBusy(PassportUid uid, Email email, DateTime start) {
        AvailabilitiesData request = new AvailabilitiesData(Cf.list(email), new IntervalAndRepetitionData(
                start.toLocalDateTime(), start.plusHours(1).toLocalDateTime(), Option.empty()));

        SubjectsAvailabilities avails = availabilityActions.getAvailabilities(
                uid, request, new AvailParameters(Option.empty(), Option.empty(), Option.of(start.getZone())));

        return avails.findAvailabilityValue(email).isSome("busy");
    }

    private ReservationInfo reserveResources(
            PassportUid uid, ListF<Email> emails, DateTime start,
            Option<Boolean> keepIfAnyBusy, Option<Email> keepIfResourceBusy)
    {
        return reserveResources(availabilityActions, reservationId, uid, emails, start, keepIfAnyBusy, keepIfResourceBusy);
    }

    static ReservationInfo reserveResources(
            AvailabilityActions availabilityActions, long reservationId,
            PassportUid uid, ListF<Email> emails, DateTime start,
            Option<Boolean> keepIfAnyBusy, Option<Email> keepIfResourceBusy)
    {
        AvailabilitiesData request = new AvailabilitiesData(emails, new IntervalAndRepetitionData(
                start.toLocalDateTime(), start.plusHours(1).toLocalDateTime(), Option.empty()));

        return availabilityActions.reserveResources(
                uid, request, reservationId, keepIfAnyBusy, keepIfResourceBusy,
                new AvailParameters(Option.empty(), Option.empty(), Option.of(start.getZone())));
    }

    @Test
    public void getHolidays() {
        SettingsYt data = new SettingsYt();
        data.setTableOfficeId(office.getId());

        settingsRoutines.updateSettingsYtByUid(data, uid);


        HolidaysInfo holidays = availabilityActions.getHolidays(Option.of(uid),
                new LocalDate(2018, 4, 28), new LocalDate(2018, 6, 1), "", Option.of(OutputMode.ALL));

        Assert.equals(DayType.WEEKDAY, holidays.find(new LocalDate(2018, 4, 28)).get().getType());
        Assert.some(new LocalDate(2018, 4, 30), holidays.find(new LocalDate(2018, 4, 28)).get().getTransferDate());

        Assert.equals(DayType.WEEKEND, holidays.find(new LocalDate(2018, 4, 29)).get().getType());

        Assert.equals(DayType.WEEKEND, holidays.find(new LocalDate(2018, 4, 30)).get().getType());
        Assert.some(new LocalDate(2018, 4, 28), holidays.find(new LocalDate(2018, 4, 30)).get().getTransferDate());

        Assert.equals(DayType.HOLIDAY, holidays.find(new LocalDate(2018, 5, 1)).get().getType());


        Assert.equals(DayType.WEEKDAY, holidays.find(new LocalDate(2018, 5, 8)).get().getType());

        holidays = availabilityActions.getHolidays(Option.empty(),
                new LocalDate(2018, 4, 28), new LocalDate(2018, 6, 1), "RU", Option.empty());

        Assert.none(holidays.find(new LocalDate(2018, 5, 8)));


        Assert.assertContains(holidays.find(new LocalDate(2018, 6, 1)).get().getName().get(), "защиты детей");

        holidays = availabilityActions.getHolidays(Option.empty(),
                new LocalDate(2018, 4, 28), new LocalDate(2018, 6, 1), "RU", Option.of(OutputMode.OVERRIDES));

        Assert.none(holidays.find(new LocalDate(2018, 6, 1)));
    }

    @Test
    public void findExistingResourcesRepetitionInfoForSingleNewEvent() {
        RepetitionInstanceInfo repData = RepetitionInstanceInfo.noRepetition(
                new InstantInterval(now, now.plusHours(1)));

        ExistingResourcesRepetitionInfo newEventInfo = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource1.getId()), AvailParameters.empty());

        Assert.isEmpty(newEventInfo.getTimeUnchangedResourceIds());
        Assert.equals(repData.getEventInterval(), newEventInfo.getRepetitionInfo().getEventInterval());
    }

    @Test
    public void findExistingResourcesRepetitionInfoForSingleExistingEvent() {
        Event event = testManager.createDefaultEvent(uid, "Event", now);
        testManager.addResourceParticipantToEvent(event.getId(), resource1);

        RepetitionInstanceInfo repData = repetitionRoutines.getRepetitionInstanceInfoByEvent(event);

        ExistingResourcesRepetitionInfo roomChangedInfo = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource2.getId()), AvailParameters.eventId(event.getId()));

        Assert.equals(Cf.list(resource1.getId()), roomChangedInfo.getTimeUnchangedResourceIds());
        Assert.equals(repData.getEventInterval(), roomChangedInfo.getRepetitionInfo().getEventInterval());


        repData = repData.withInterval(repData.getEventInterval().withStart(now.plusMinutes(30).getMillis()));

        ExistingResourcesRepetitionInfo timeChangedInfo = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource2.getId()), AvailParameters.eventId(event.getId()));

        Assert.isEmpty(timeChangedInfo.getTimeUnchangedResourceIds());
        Assert.equals(repData.getEventInterval(), timeChangedInfo.getRepetitionInfo().getEventInterval());
    }

    @Test
    public void findExistingResourcesRepetitionInfoForRepeatingNewEvent() {
        RepetitionInstanceInfo repData = new RepetitionInstanceInfo(
                new InstantInterval(now, now.plusHours(1)), MoscowTime.TZ,
                Option.of(TestManager.createDailyRepetitionTemplate()));

        ExistingResourcesRepetitionInfo info = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource1.getId()), AvailParameters.empty());

        Assert.isEmpty(info.getTimeUnchangedResourceIds());
        Assert.equals(repData, info.getRepetitionInfo());
    }

    @Test
    public void findExistingResourcesRepetitionInfoForRepeatingRoomChange() {
        EventsForExistingRepetition events = createRepeatingEventWithResource1AndRecurrences();

        RepetitionInstanceInfo repData = events.getRepetition().withoutExdatesAndRdatesAndRecurrences();

        ExistingResourcesRepetitionInfo whole = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource2.getId()), events.paramsForWhole());

        Assert.equals(Cf.list(resource1.getId()), whole.getTimeUnchangedResourceIds());
        Assert.equals(events.getRepetition().getRecurIds(), whole.getRepetitionInfo().getRecurIds());


        ExistingResourcesRepetitionInfo future = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData.withInterval(events.getFutureInstance()), Cf.list(resource2.getId()), events.paramsForFuture());

        Assert.isEmpty(future.getTimeUnchangedResourceIds());
        Assert.isEmpty(future.getRepetitionInfo().getRecurIds());
    }

    @Test
    public void findExistingResourcesRepetitionInfoForRepeatingDueTsChange() {
        EventsForExistingRepetition events = createRepeatingEventWithResource1AndRecurrences();

        Repetition rep = TestManager.createDailyRepetitionTemplate();
        rep.setDueTs(events.getRecurrence2().getStartTs());

        RepetitionInstanceInfo repData = new RepetitionInstanceInfo(
                getInstantInterval(events.getMaster()), MoscowTime.TZ, Option.of(rep));

        ExistingResourcesRepetitionInfo whole = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource1.getId()), events.paramsForWhole());

        Assert.isEmpty(whole.getTimeUnchangedResourceIds());
        Assert.equals(Cf.list(events.getRecurrence1().getStartTs()), whole.getRepetitionInfo().getRecurIds());


        ExistingResourcesRepetitionInfo future = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData.withInterval(events.getFutureInstance()), Cf.list(resource1.getId()), events.paramsForFuture());

        Assert.isEmpty(future.getTimeUnchangedResourceIds());
        Assert.equals(whole.getRepetitionInfo().getRecurIds(), future.getRepetitionInfo().getRecurIds());
    }

    @Test
    public void findExistingResourcesRepetitionInfoForRepeatingTimeChange() {
        EventsForExistingRepetition events = createRepeatingEventWithResource1AndRecurrences();

        RepetitionInstanceInfo repData = events.getRepetition()
                .withInterval(new InstantInterval(events.getMaster().getStartTs(), Duration.standardMinutes(10)))
                .withoutExdatesAndRdatesAndRecurrences();

        ExistingResourcesRepetitionInfo whole = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource1.getId()), events.paramsForWhole());

        Assert.isEmpty(whole.getTimeUnchangedResourceIds());
        Assert.isEmpty(whole.getRepetitionInfo().getRecurIds());
        Assert.equals(repData.getEventInterval(), whole.getRepetitionInfo().getEventInterval());

        repData = repData
                .withInterval(new InstantInterval(events.getFutureInstance().getStart(), Duration.standardMinutes(10)));

        ExistingResourcesRepetitionInfo future = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource1.getId()), events.paramsForFuture());

        Assert.isEmpty(future.getTimeUnchangedResourceIds());
        Assert.isEmpty(future.getRepetitionInfo().getRecurIds());
        Assert.equals(repData.getEventInterval(), future.getRepetitionInfo().getEventInterval());
    }

    @Test
    public void findExistingResourcesRepetitionInfoForRepeatingRuleChange() {
        EventsForExistingRepetition events = createRepeatingEventWithResource1AndRecurrences();

        Repetition rep = TestManager.createDailyRepetitionTemplate();
        rep.setType(RegularRepetitionRule.WEEKLY);
        rep.setRWeeklyDays(WeekdayConv.jodaToCals(now.getDayOfWeek()));

        RepetitionInstanceInfo repData = new RepetitionInstanceInfo(
                getInstantInterval(events.getMaster()), MoscowTime.TZ, Option.of(rep));

        ExistingResourcesRepetitionInfo whole = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource1.getId()), events.paramsForFuture());

        Assert.isEmpty(whole.getTimeUnchangedResourceIds());
        Assert.isEmpty(whole.getRepetitionInfo().getRecurIds());
        Assert.equals(rep.getType(), whole.getRepetitionInfo().getRepetition().get().getType());

        ExistingResourcesRepetitionInfo future = webNewAvailabilityManager.findExistingResourcesRepetitionInfo(
                repData, Cf.list(resource1.getId()), events.paramsForFuture());

        Assert.isEmpty(future.getTimeUnchangedResourceIds());
        Assert.isEmpty(future.getRepetitionInfo().getRecurIds());
        Assert.equals(rep.getType(), future.getRepetitionInfo().getRepetition().get().getType());
    }

    private EventsForExistingRepetition createRepeatingEventWithResource1AndRecurrences() {
        Event master = testManager.createDefaultEvent(uid, "Repeating", now);

        master.setRepetitionId(testManager.createDailyRepetitionAndLinkToEvent(master.getId()));
        testManager.addResourceParticipantToEvent(master.getId(), resource1);

        Event firstRecurrence = testManager.createDefaultRecurrence(uid, master.getId(), now.plusDays(1).toInstant());
        testManager.addResourceParticipantToEvent(firstRecurrence.getId(), resource1);

        InstantInterval futureInstance = new InstantInterval(
                now.plusDays(1).toInstant(), EventRoutines.getPeriod(master, MoscowTime.TZ).toStandardDuration());

        Event nextRecurrence = testManager.createDefaultRecurrence(uid, master.getId(), now.plusWeeks(1).toInstant());
        testManager.addResourceParticipantToEvent(nextRecurrence.getId(), resource1);

        RepetitionInstanceInfo repetition = repetitionRoutines.getRepetitionInstanceInfoByEvent(master);

        return new EventsForExistingRepetition(master, repetition, firstRecurrence, nextRecurrence, futureInstance);
    }

    @AllArgsConstructor
    @Getter
    private static final class EventsForExistingRepetition {
        private final Event master;
        private final RepetitionInstanceInfo repetition;
        private final Event recurrence1;
        private final Event recurrence2;
        private final InstantInterval futureInstance;

        public AvailParameters paramsForWhole() {
            return AvailParameters.eventId(master.getId());
        }

        public AvailParameters paramsForFuture() {
            return new AvailParameters(Option.of(futureInstance.getStart()), Option.of(master.getId()), Option.empty());
        }
    }
}
