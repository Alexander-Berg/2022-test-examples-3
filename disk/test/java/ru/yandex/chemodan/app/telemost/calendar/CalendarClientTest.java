package ru.yandex.chemodan.app.telemost.calendar;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.calendar.model.CalendarEvent;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.misc.test.Assert;

public class CalendarClientTest extends TelemostBaseContextTest {

    @Autowired
    private CalendarClient calendarClient;

    private final String uid = "141815581";
    private final String eventId = "4271660";

    @Test
    public void testGetEvent() {
        CalendarEvent expectedEvent = new CalendarEvent(eventId, "Event caption", "Event description",
                Instant.now(), Instant.now().plus(Duration.standardHours(1)));
        ((CalendarClientStub)calendarClient).addEvent(expectedEvent);

        CalendarEvent actualEvent = calendarClient.getEvent(eventId, Option.of(PassportOrYaTeamUid.parseUid(uid)), Option.empty());

        Assert.equals(expectedEvent, actualEvent);
    }
}
