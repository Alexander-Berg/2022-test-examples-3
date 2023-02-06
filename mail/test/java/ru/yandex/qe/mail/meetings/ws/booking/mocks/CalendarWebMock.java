package ru.yandex.qe.mail.meetings.ws.booking.mocks;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.NotImplementedException;

import ru.yandex.qe.mail.meetings.services.calendar.CalendarWeb;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Availability;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Decision;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Event;
import ru.yandex.qe.mail.meetings.services.calendar.dto.EventSchedule;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Events;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Layer;
import ru.yandex.qe.mail.meetings.services.calendar.dto.MoveResult;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Office;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Offices;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resources;
import ru.yandex.qe.mail.meetings.services.calendar.dto.User;
import ru.yandex.qe.mail.meetings.services.calendar.dto.WebEventUserData;
import ru.yandex.qe.mail.meetings.services.calendar.dto.suggest.SuggestBody;
import ru.yandex.qe.mail.meetings.services.calendar.dto.suggest.SuggestResponse;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper;

import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.eventDates;
import static ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper.officeMock;


public class CalendarWebMock implements CalendarWeb {
    private static final ThreadLocal<DateFormat> _DF = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    private final List<BookingTestHelper.ObjectWithTimeTable<Resource.Info>> resources;
    private final List<BookingTestHelper.ObjectWithTimeTable<Person>> persons;
    private final Map<String, Resource.Info> unvisibleResourceForUser;

    private List<SuggestResponse.Interval> suggestIntervals = null;

    private final List<Office> offices;
    private final AtomicInteger counter = new AtomicInteger();

    public CalendarWebMock(List<BookingTestHelper.ObjectWithTimeTable<Resource.Info>> resources, List<BookingTestHelper.ObjectWithTimeTable<Person>> persons, Map<String, Resource.Info> unvisibleResourceForUser) {
        this.resources = resources;
        this.persons = persons;
        this.unvisibleResourceForUser = unvisibleResourceForUser;

        this.offices = resources
                .stream()
                .map(owt -> owt.obj.getOfficeId())
                .sorted()
                .distinct()
                .map(BookingTestHelper.ID_TO_OFFICE::get)
                .collect(Collectors.toList());
    }

    @Override
    public Offices getOffices() {
        return new Offices(offices);
    }

    @Override
    public Offices getResourceSchedule(int officeId, Date date) {
        throw new NotImplementedException();
    }

    @Override
    public Offices getResourceSchedule(List<Integer> officeIds, Date from, Date to) {
        return getResourceSchedule(officeIds, from, to, "#unexistinguid");
    }

    @Override
    public Events getEvents(String uid, String fromStr, String toStr, boolean hideAbsences) {
        try {
            var from = _DF.get().parse(fromStr);

            var to = _DF.get().parse(toStr);

            var personInfo = persons.stream()
                    .filter(p -> p.obj.getUid().equals(uid))
                    .findFirst()
                    .get();

            return new Events(
                    personInfo.busyTimes.stream()
                            .filter(i -> i.isAfter(from.getTime() - 1))
                            .filter(i -> i.isBefore(to.getTime() + 1))
                            .map(i -> {
                                var webEventUserData = new WebEventUserData();
                                webEventUserData.setAvailability(Availability.BUSY);
                                webEventUserData.setLayerId(1L);
                                webEventUserData.setNotifications(Collections.emptyList());

                                var e = new Event(
                                                counter.incrementAndGet(),
                                                counter.get(),
                                                i.getStart().toDate(),
                                                i.getEnd().toDate());
                                e.setDecision(Decision.YES);
                                e.setWebEventUserData(webEventUserData);
                                return e;
                            })
                            .collect(Collectors.toUnmodifiableList())
            );
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Offices getResourceSchedule(List<Integer> officeIds, Date from, Date to, String uid) {
        var result = officeIds
                .stream()
                .map(officeId -> BookingTestHelper.ID_TO_OFFICE.get(officeId))
                .map(stub -> officeMock(stub, resources
                        .stream()
                        .filter(r -> r.obj.getOfficeId() == stub.getId())
                        .filter(r -> !r.obj.getId().equals(
                                Optional.ofNullable(unvisibleResourceForUser.get(uid)).map(Resource.Info::getId).orElse(-1))
                        )
                        .map(r -> new Resource(
                                r.obj,
                                Collections.emptyList(), // TODO ?
                                eventDates(r.busyTimes.stream()
                                        .filter(i ->
                                                i.isAfter(from.getTime()) && i.isBefore(to.getTime())
                                        ).collect(Collectors.toList()))
                        ))
                        .collect(Collectors.toUnmodifiableList())))
                .collect(Collectors.toUnmodifiableList());
        return new Offices(result);
    }

    @Override
    public Resource.Info getResourceDescription(String email) {
        return resources.stream().map(r -> r.obj).filter(r -> r.getEmail().equals(email)).findFirst().get();
    }

    @Override
    public Event getEvent(int eventId) {
        throw new NotImplementedException();
    }

    @Override
    public Event getEvent(int eventId, String uid) {
        throw new NotImplementedException();
    }

    @Override
    public Event getEvent(int eventId, String instanceStartTs, Boolean recurrenceAsOccurrence) {
        throw new NotImplementedException();
    }

    @Override
    public JsonNode getEventJson(int eventId) {
        throw new NotImplementedException();
    }

    @Override
    public Resource.Info getResourceInfo(String email) {
        throw new NotImplementedException();
    }

    @Override
    public User getUser(String email) {
        throw new NotImplementedException();
    }

    @Override
    public Events getEventsBrief(String eventIds, boolean forResource) {
        throw new NotImplementedException();
    }

    @Override
    public Resources findAvailableResources(String uid, Integer officeId, String filter, String tz, EventSchedule schedule) {
        throw new NotImplementedException();
    }

    @Override
    public Events getEvents(String uid, Date from, Date to) {
        throw new NotImplementedException();
    }

    @Override
    public Events getEvents(String uid, Integer eventId, String from, String to) {
        throw new NotImplementedException();
    }

    @Override
    public Events getEvents(String uid, String from, String to) {
        throw new NotImplementedException();
    }

    @Override
    public SuggestResponse suggestMeetingsTimes(SuggestBody body) {
        throw new NotImplementedException();
    }

    @Override
    public SuggestResponse suggestMeetingsTimes(@Nonnull String uid, SuggestBody body) {
        if (suggestIntervals == null) {
            throw new RuntimeException("suggest intervals are missing");
        }
        var result = new SuggestResponse();
        result.setIntervals(suggestIntervals);
        return result;
    }

    @Override
    public Layer getLayer(@Nonnull String uid, @Nonnull Long layerId) {
        var l = new Layer();
        l.setAffectAvailability(true);
        return l;
    }

    @Override
    public MoveResult moveResource(String sourceUid, String targetUid, int sourceId, int targetId, String resource, String instanceStartTs) {
        throw new NotImplementedException();
    }

    public void setSuggestIntervals(List<SuggestResponse.Interval> suggestIntervals) {
        this.suggestIntervals = suggestIntervals;
    }
}
