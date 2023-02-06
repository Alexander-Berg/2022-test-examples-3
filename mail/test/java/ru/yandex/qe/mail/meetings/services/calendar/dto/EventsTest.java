package ru.yandex.qe.mail.meetings.services.calendar.dto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.qe.mail.meetings.utils.StringUtils.trim;

public class EventsTest {

    @Test
    public void serialization() throws IOException {
        String requestString = trim(IOUtils.readLines(
                EventsTest.class.getResourceAsStream("/calendar/get-events.json"), StandardCharsets.UTF_8));
        ObjectMapper mapper = new ObjectMapper();
        final Events events = mapper.readValue(requestString, Events.class);
        assertEquals(6, events.getEvents().size());
        final Event first = events.getEvents().get(0);
        assertNull(first.getOrganizer());

        final Event second = events.getEvents().get(1);
        assertNotNull(second.getOrganizer());
    }

}
