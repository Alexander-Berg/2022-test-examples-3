package ru.yandex.reminders.logic.panel.old;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.reminders.api.reminder.Source;
import ru.yandex.reminders.logic.event.Event;
import ru.yandex.reminders.logic.event.EventData;
import ru.yandex.reminders.logic.event.EventId;
import ru.yandex.reminders.logic.event.EventManagerTestCommon;
import ru.yandex.reminders.logic.reminder.Reminder;

public class PanelBenderTest {
    @Test
    public void serializeFlightNote() {
        FlightNote flightNote =
                NoteDataConverter.toFlightNote(EventManagerTestCommon.createFlightEventMeta(), System.currentTimeMillis());
        byte[] bytes = PanelBender.mapper.serializeJson(flightNote);
        FlightNote deserialized = PanelBender.mapper.parseJson(FlightNote.class, bytes);
        Assert.notNull(deserialized);
    }

    @Test
    public void serializeUniversalNote() {
        EventData eventData = new EventData(
                Option.some(Source.INTERNAL),
                Option.some("name"), Option.some("descr"),
                Option.some(EventManagerTestCommon.createEventJsonData()), Cf.<Reminder>list());
        Event event = new Event(
                new EventId(PassportUid.cons(1), "cid1", "ext1"), eventData,
                Option.<String>none(), Instant.now(), "req1");
        UniversalNote universalNote =
                NoteDataConverter.toUniversalNote(event,
                        Reminder.panel(
                                DateTime.now(), Option.some("http://ya.ru/"), Option.some("subj"), Option.some("msg")),
                        System.currentTimeMillis());
        byte[] bytes = PanelBender.mapper.serializeJson(universalNote);
        UniversalNote deserialized = PanelBender.mapper.parseJson(UniversalNote.class, bytes);
        Assert.notNull(deserialized);
    }
}
