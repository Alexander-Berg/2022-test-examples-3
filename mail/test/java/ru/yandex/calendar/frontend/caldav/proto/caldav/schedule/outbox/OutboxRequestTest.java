package ru.yandex.calendar.frontend.caldav.proto.caldav.schedule.outbox;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxRequestTest {
    @Test
    public void vfreeBusy1() {
        val text = new ClassPathResourceInputStreamSource(OutboxRequestTest.class, "OutboxRequestTest-1.ics").readText();
        val calendar = IcsCalendar.parse(new StringReader(text));
        val req = OutboxRequest.parse(calendar);
        val freeBusy = (OutboxRequestVFreeBusy) req;

        assertThat(freeBusy.getEmails()).containsExactly(new Email("nga@yandex-team.ru"));
    }

    @Test
    public void vevent() {
        val text = new ClassPathResourceInputStreamSource(OutboxRequestTest.class, "OutboxRequestTest-2.ics").readBytes();
        val calendar = IcsCalendar.parse(new ByteArrayInputStream(text));
        val req = OutboxRequest.parse(calendar);
        val vevent = (OutboxRequestVEvent) req;

        assertThat(vevent.getUid()).isEqualTo("B5E38A19-E08F-439A-A1B3-47B8D9335CDA");
        assertThat(vevent.getAttendees()).containsExactly(new Email("nga@yandex-team.ru"), new Email("levin-matveev@yandex.ru"));
    }

    @Test
    public void veventCancel() {
        val calendar = IcsCalendar.parse(new ClassPathResourceInputStreamSource(OutboxRequestTest.class, "OutboxRequestTest-cancel.ics"));

        OutboxRequest.parse(calendar);
    }

    @Test
    public void vfreebusy() {
        val calendar = IcsCalendar.parse(new ClassPathResourceInputStreamSource(OutboxRequestTest.class, "OutboxRequestTest-vfreebusy-1.ics"));

        val req = (OutboxRequestVFreeBusy) OutboxRequest.parse(calendar);
        assertThat(req.getEmails()).containsExactly(new Email("nga@yandex-team.ru"));

        assertThat(req.getStart().toOptional()).hasValue(new DateTime(2010, 4, 21, 20, 0, 0, 0, DateTimeZone.UTC).toInstant());
        assertThat(req.getEnd().toOptional()).hasValue(new DateTime(2010, 4, 22, 20, 0, 0, 0, DateTimeZone.UTC).toInstant());
    }
}
