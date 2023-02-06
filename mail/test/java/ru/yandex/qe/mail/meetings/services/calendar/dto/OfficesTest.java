package ru.yandex.qe.mail.meetings.services.calendar.dto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static ru.yandex.qe.mail.meetings.utils.StringUtils.trim;

public class OfficesTest {
    @Test
    public void serialization() throws IOException {
        String requestString = trim(IOUtils.readLines(
                OfficesTest.class.getResourceAsStream("/calendar/offices.json"), StandardCharsets.UTF_8));
        ObjectMapper mapper = new ObjectMapper();
        final Offices offices = mapper.readValue(requestString, Offices.class);
        System.out.println(offices);
//        assertEquals(6, events.getEvents().size());
//        final Event first = events.getEvents().get(0);
//        assertNull(first.getOrganizer());
//
//        final Event second = events.getEvents().get(1);
//        assertNotNull(second.getOrganizer());
    }


}
