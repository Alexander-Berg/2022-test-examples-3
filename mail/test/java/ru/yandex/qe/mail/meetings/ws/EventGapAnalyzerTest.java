package ru.yandex.qe.mail.meetings.ws;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.qe.mail.meetings.analisys.EventGapAnalyzer;
import ru.yandex.qe.mail.meetings.analisys.EventGapsInspectionsBuilder;
import ru.yandex.qe.mail.meetings.analisys.InspectionsResult;
import ru.yandex.qe.mail.meetings.analisys.UserCheck;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarWeb;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Decision;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Event;
import ru.yandex.qe.mail.meetings.services.calendar.dto.EventUser;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Office;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;
import ru.yandex.qe.mail.meetings.services.gaps.dto.Gap;
import ru.yandex.qe.mail.meetings.services.staff.MockStaff;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.utils.CalendarHelper;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventGapAnalyzerTest {

    private static final String ORGANIZER = "petya";
    private static final String USER = "serega";
    private static final String SECOND_USER = "vitalya";

    private static final int OFFICE_ID = 1;
    private static final int SECOND_OFFICE_ID = 2;
    private static final int RESOURCE_ID = 3;
    private static final int CAMPUS_ID = 4;
    private static final int SECOND_RESOURCE_ID = 5;
    private static final int CAMPUS_EVENT_ID = 6;
    private static final int SINGLE_ROOM_EVENT_ID = 7;
    private static final int DOUBLE_ROOM_EVENT_ID = 8;

    private static final String RESOURCE = "Kolpino";
    private static final String SECOND_RESOURCE = "Gatchina";
    private static final String CAMPUS = "Campus";

    private EventUser organizer;
    private EventUser attendee;
    private EventUser secondAttendee;

    private Resource resource;
    private Resource secondResource;

    private EventObject singleRoomEvent;
    private EventObject doubleRoomEvent;

    private Office office;
    private CalendarWeb calendarWeb;

    @Before
    public void setUp() {
        calendarWeb = mock(CalendarWeb.class);
        office = office(OFFICE_ID);

        resource = resource(RESOURCE_ID, OFFICE_ID,  RESOURCE, Resource.Type.ROOM);
        secondResource = resource(SECOND_RESOURCE_ID, SECOND_OFFICE_ID, SECOND_RESOURCE, Resource.Type.ROOM);

        organizer = user(ORGANIZER, OFFICE_ID);
        attendee = user(USER, OFFICE_ID);
        secondAttendee = user(SECOND_USER, SECOND_OFFICE_ID);

        Event event = event(SINGLE_ROOM_EVENT_ID, organizer, Collections.singletonList(attendee),
                Collections.singletonList(resource.getResourceInfo()));
        singleRoomEvent = new EventObject(office, resource, event);


        Event other = event(DOUBLE_ROOM_EVENT_ID, organizer, Collections.singletonList(secondAttendee),
                Arrays.asList(resource.getResourceInfo(), secondResource.getResourceInfo()));
        doubleRoomEvent = new EventObject(office, resource, other);
    }

    private Office office(int officeId) {
        Office office = mock(Office.class);
        when(office.getId()).thenReturn(officeId);
        return office;
    }

    private Resource resource(int id, int officeId, String resourceName, Resource.Type type) {
        Resource resource = mock(Resource.class);
        Resource.Info info = mock(Resource.Info.class);
        when(resource.getResourceInfo()).thenReturn(info);
        when(info.getId()).thenReturn(id);
        when(info.getEmail()).thenReturn(resourceName);
        when(info.getOfficeId()).thenReturn(officeId);
        when(info.getResourceType()).thenReturn(type);
        when(calendarWeb.getResourceInfo(resourceName)).thenReturn(info);
        return resource;
    }

    private EventUser user(String login, int officeId) {
        EventUser user = mock(EventUser.class);
        when(user.getLogin()).thenReturn(login);
        when(user.getDecision()).thenReturn(Decision.YES);
        when(user.getOfficeId()).thenReturn(officeId);
        return user;
    }

    private Event event(int eventId, EventUser orginaizer, List<EventUser> attendees, List<Resource.Info> resources) {
        Event event = mock(Event.class);
        when(event.getEventId()).thenReturn(eventId);
        when(event.getOrganizer()).thenReturn(orginaizer);
        when(event.getAttendees()).thenReturn(attendees);
        when(event.getResources()).thenReturn(resources);

        when(event.getStart()).thenReturn(Date.from(Instant.parse("2019-08-09T11:00:00.00Z")));
        when(event.getEnd()).thenReturn(Date.from(Instant.parse("2019-08-09T11:30:00.00Z")));

        when(calendarWeb.getEvent(eventId)).thenReturn(event);
        return event;
    }

    private EventGapAnalyzer<InspectionsResult> eventGapAnalyzer(Map<String, List<Gap>> gaps) {
        return eventGapAnalyzer(Collections.emptySet(), gaps);
    }

    private EventGapAnalyzer<InspectionsResult> eventGapAnalyzer(Set<String> dismissed, Map<String, List<Gap>> gaps) {
        CalendarHelper calendar = mock(CalendarHelper.class);
        when(calendar.getBaseEvent((Event)any())).then(i -> i.getArguments()[0]);
        UserCheck userCheck = new UserCheck.Bulk(new StaffClient(new MockStaff(dismissed)));
        return new EventGapAnalyzer<>(calendar, calendarWeb, userCheck, gaps, EventGapsInspectionsBuilder::new);
    }

    @Test
    public void checkOkSingleRoomEvent() {
        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(Collections.emptyMap());
        Assert.assertNull(eventGapAnalyzer.check(singleRoomEvent));
    }

    @Test
    public void checkOkDoubleRoomEvent() {
        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(Collections.emptyMap());
        Assert.assertNull(eventGapAnalyzer.check(doubleRoomEvent));
    }

    @Test
    public void checkOkNoRoomEvent() {
        Event other = event(1000, organizer, Collections.singletonList(secondAttendee),
                Collections.singletonList(resource.getResourceInfo()));
        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(Collections.emptyMap());
        Assert.assertNull(eventGapAnalyzer.check(new EventObject(office, resource, other)));
    }

    @Test
    public void checkOneDeclined() {
        when(attendee.getDecision()).thenReturn(Decision.NO);
        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(Collections.emptyMap());
        Assert.assertNotNull(eventGapAnalyzer.check(singleRoomEvent));
    }

    @Test
    public void checkOneUserWithGap() {
        Instant gapStart = Instant.parse("2019-08-09T00:00:00.00Z");
        Instant gapEnd = Instant.parse("2019-08-10T00:00:00.00Z");
        Gap gap = new Gap(1, "absence", Date.from(gapStart), Date.from(gapEnd), "", false, false);
        Map<String, List<Gap>> gaps = Collections.singletonMap(USER, Collections.singletonList(gap));

        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(gaps);
        Assert.assertNotNull(eventGapAnalyzer.check(singleRoomEvent));
    }

    @Test
    public void checkGapOnCampus() {
        Resource campus = resource(CAMPUS_ID, OFFICE_ID, CAMPUS, Resource.Type.CAMPUS);
        Event picnic = event(CAMPUS_EVENT_ID, organizer, Collections.singletonList(attendee),
                Collections.singletonList(campus.getResourceInfo()));

        Instant gapStart = Instant.parse("2019-08-09T00:00:00.00Z");
        Instant gapEnd = Instant.parse("2019-08-10T00:00:00.00Z");
        Gap gap = new Gap(1, "absence", Date.from(gapStart), Date.from(gapEnd), "", false, false);
        Map<String, List<Gap>> gaps = Collections.singletonMap(USER, Collections.singletonList(gap));

        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(gaps);
        Assert.assertNull(eventGapAnalyzer.check(new EventObject(office, campus, picnic)));
    }

    @Test
    public void checkDismissed() {
        Set<String> dismissed = Collections.singleton(USER);
        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(dismissed, Collections.emptyMap());
        Assert.assertNotNull(eventGapAnalyzer.check(singleRoomEvent));
    }

    @Test
    public void checkOrganizerDismissed() {
        Event other = event(1001, organizer, Arrays.asList(attendee, secondAttendee),
                Arrays.asList(resource.getResourceInfo(), secondResource.getResourceInfo()));
        Set<String> dismissed = Collections.singleton(ORGANIZER);
        EventGapAnalyzer<InspectionsResult>  eventGapAnalyzer = eventGapAnalyzer(dismissed, Collections.emptyMap());
        Assert.assertNotNull(eventGapAnalyzer.check(singleRoomEvent));
    }

    @Test
    public void checkTrip() {
        Instant gapStart = Instant.parse("2019-08-09T00:00:00.00Z");
        Instant gapEnd = Instant.parse("2019-08-10T00:00:00.00Z");
        Gap gap = new Gap(1, "trip", Date.from(gapStart), Date.from(gapEnd), "", false, false);
        Map<String, List<Gap>> gaps = Collections.singletonMap(USER, Collections.singletonList(gap));
        EventGapAnalyzer<InspectionsResult>  eventGapAnalyzer = eventGapAnalyzer(gaps);
        Assert.assertNull(eventGapAnalyzer.check(singleRoomEvent));
    }

    @Test
    public void checkTwoFromOneOfficeAndTrip() {
        Event other = event(1002, organizer, Collections.singletonList(attendee),
                Arrays.asList(resource.getResourceInfo(), secondResource.getResourceInfo()));

        Instant gapStart = Instant.parse("2019-08-09T00:00:00.00Z");
        Instant gapEnd = Instant.parse("2019-08-10T00:00:00.00Z");
        Gap gap = new Gap(1, "trip", Date.from(gapStart), Date.from(gapEnd), "", false, false);
        Map<String, List<Gap>> gaps = Collections.singletonMap(USER, Collections.singletonList(gap));
        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(gaps);
        Assert.assertTrue(eventGapAnalyzer.check(new EventObject(office, resource, other)).getCriticals().isEmpty());
    }

    @Test
    public void checkOrganizerOutAttendeeInTrip() {
        Instant gapStart = Instant.parse("2019-08-09T00:00:00.00Z");
        Instant gapEnd = Instant.parse("2019-08-10T00:00:00.00Z");
        Gap gap = new Gap(1, "absence", Date.from(gapStart), Date.from(gapEnd), "", false, false);
        Gap gap2 = new Gap(2, "trip", Date.from(gapStart), Date.from(gapEnd), "", false, false);
        Map<String, List<Gap>> gaps = new HashMap<>();
        gaps.put(ORGANIZER, Collections.singletonList(gap));
        gaps.put(USER, Collections.singletonList(gap2));

        EventGapAnalyzer<InspectionsResult> eventGapAnalyzer = eventGapAnalyzer(gaps);
        Assert.assertFalse(eventGapAnalyzer.check(singleRoomEvent).getCriticals().isEmpty());
    }
}
