package ru.yandex.reminders.logic.callmeback.out;

import java.util.Optional;

import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import ru.yandex.commune.a3.action.HttpMethod;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.reminders.api.ApiBender;
import ru.yandex.reminders.logic.callmeback.in.CallbackRequest;
import ru.yandex.reminders.logic.event.EventData;
import ru.yandex.reminders.logic.event.EventId;
import ru.yandex.reminders.logic.reminder.Reminder;

import static org.assertj.core.api.Assertions.assertThat;

public class SerializeTest {
    private static String serializeJson(CallmebackRequest request) {
        return new String(ApiBender.mapper.serializeJson(request));
    }

    @Test
    public void verifyJsonHttpElement() {
        val date = new DateTime().withZone(DateTimeZone.UTC).withDate(1984, 3,11).withTime(6,0,0,0);
        val eventId = new EventId(PassportUid.cons(13), "yandex-tv");
        val context = new Context(new HttpContext(
                HttpMethod.POST,
                new CallbackRequest(eventId, Reminder.callback(date, "http://narod.ru"), EventData.empty(), Optional.empty())
        ));
        val request = new CallmebackRequest(date, "http://localhost:81", NotifyScheme.HTTP, context);
        assertThat(serializeJson(request)).contains("_http");
    }
}