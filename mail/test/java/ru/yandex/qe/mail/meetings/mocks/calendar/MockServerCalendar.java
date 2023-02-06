package ru.yandex.qe.mail.meetings.mocks.calendar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.qe.mail.meetings.services.calendar.dto.EventSchedule;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Events;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;

/**
 * @author Sergey Galyamichev
 */
public class MockServerCalendar {
    private static final Logger LOG = LoggerFactory.getLogger(MockServerCalendar.class);
    private static final String CALENDAR = "/calendar";
    private static final String CALENDAR_OFFICES_TXT = CALENDAR + "/offices.txt";
    private static final String CALENDAR_EVENTS_TXT = CALENDAR + "/events.txt";
    private static final String CALENDAR_AVAILABILITIES_TXT = CALENDAR + "/availabilities.txt";
    private static final String CALENDAR_SCHEDULES_TXT = CALENDAR + "/schedules.txt";

    private static final int REQUEST = 0;
    private static final int RESPONSE = 1;

    private final Map<String, String> schedules = new HashMap<>();
    private final Map<String, String> events = new HashMap<>();
    private final Map<String, String> availabilities = new HashMap<>();

    @GET
    @Path("get-offices")
    public String getOffices() {
        try {
            return IOUtils.toString(MockServerCalendar.class.getResourceAsStream(CALENDAR_OFFICES_TXT), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("failed to read file {}", CALENDAR_OFFICES_TXT, e);
            return "{\"offices\":[]}";
        }
    }

    @GET
    @Path("get-resources-schedule")
    public String getResourceSchedule(@QueryParam("office-id") int officeId,
                                      @QueryParam("from") Date date,
                                      @QueryParam("to") Date to) {
        if (schedules.isEmpty()) {
            LOG.info("init all schedules");
            schedules.putAll(getRecords(CALENDAR_SCHEDULES_TXT));
        }
        return schedules.get(String.valueOf(officeId));
    }

    @GET
    @Path("get-event")
    public String getEvent(@QueryParam("eventId") int eventId, @QueryParam("uid") String uid) {
        if (events.isEmpty()) {
            LOG.info("init all events");
            events.putAll(getRecords(CALENDAR_EVENTS_TXT));
        }
        return events.get(String.valueOf(eventId));
    }


    @GET
    @Path("get-event")
    public String getEvent(@QueryParam("eventId") int eventId) {
        if (events.isEmpty()) {
            LOG.info("init all events");
            events.putAll(getRecords(CALENDAR_EVENTS_TXT));
        }
        return events.get(String.valueOf(eventId));
    }

    @GET
    @Path("get-events")
    public Events getEvents(@QueryParam("eventId") Integer eventId,
                     @QueryParam("from") String from,
                     @QueryParam("to") String to) {
        return new Events(Collections.emptyList());
    }

    @POST
    @Path("find-available-resources")
    public String findAvailableResources(@QueryParam("office-id") String officeId,
                                     @QueryParam("filter") String filter,
                                     EventSchedule schedule) {
        if (availabilities.isEmpty()) {
            LOG.info("init all availabilities");
            availabilities.putAll(getRecords(CALENDAR_AVAILABILITIES_TXT));
        }
        return availabilities.get(officeId);
    }

    @GET
    @Path("get-user-or-resource-info")
    public Resource.Info getResourceInfo(String email) {
        Resource.Info info =  new Resource.Info();
        info.setSeats(10);
        info.setEmail(email);
        return info;
    }


    private Map<String, String> getRecords(String fileName) {
        Map<String, String> result = new HashMap<>();
        try {
            List<String> strings = IOUtils.readLines(MockServerCalendar.class.getResourceAsStream(fileName), StandardCharsets.UTF_8);
            for (String schedule : strings) {
                String[] data = schedule.split(":", 2);
                result.put(data[REQUEST], data[RESPONSE]);
            }
        } catch (IOException e) {
            LOG.error("failed to read file {}", fileName, e);
        }
        return result;
    }
}
