package ru.yandex.chemodan.app.telemost.calendar;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.calendar.model.CalendarEvent;
import ru.yandex.chemodan.app.telemost.exceptions.ConferenceNotFoundTelemostException;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;

public class CalendarClientStub implements CalendarClient {

    MapF<String, CalendarEvent> events = Cf.hashMap();

    public void reset() {
        events.clear();
    }

    public void addEvent(CalendarEvent calendarEvent) {
        events.put(calendarEvent.getEventId(), calendarEvent);
    }

    @Override
    public CalendarEvent getEvent(String eventId, Option<PassportOrYaTeamUid> uid, Option<String> tvmUserTicketO) {
        events.getO(eventId).orElseThrow(ConferenceNotFoundTelemostException::new);

        return events.getO(eventId).get();
    }

}
