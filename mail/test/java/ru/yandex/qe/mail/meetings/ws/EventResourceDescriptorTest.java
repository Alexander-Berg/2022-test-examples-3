package ru.yandex.qe.mail.meetings.ws;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.qe.mail.meetings.api.resource.dto.AddResourceRequest;
import ru.yandex.qe.mail.meetings.cron.actions.Contexts;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarWeb;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Events;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.utils.FormConverters;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Galyamichev
 */
public class EventResourceDescriptorTest {

    @Test
    public void fromRequest() throws Exception {
        String url = "https://calendar.yandex-team.ru/event/41291971?applyToFuture=0&event_date=2020-01-30T19%3A00%3A00&layerId=40357&show_date=2020-01-02";
        Events events = mock(Events.class);
        when(events.getEvents()).thenReturn(Collections.emptyList());
        CalendarWeb calendarWeb = mock(CalendarWeb.class);
        when(calendarWeb.getEvents(anyString(), anyInt(), any(), any())).thenReturn(events);
        StaffClient staffClient = mock(StaffClient.class);
        when(staffClient.getByLogin(anyString())).thenReturn(mock(Person.class));
        EventResourceDescriptor descriptor = EventResourceDescriptor.fromRequest(calendarWeb, staffClient, "g-s-v", url);
        assertEquals(41291971, descriptor.getEventId());
        assertEquals("2020-01-30T19:00:00", descriptor.getInstanceTs());
        assertEquals("g-s-v", descriptor.getLogin());
    }

    @Test
    public void contextFromRequest() {
        AddResourceRequest request = new AddResourceRequest();
        request.setOffice("Москва, БЦ Морозов, Санкт-Петербург, БЦ Бенуа");
        request.setManualOffice("да");
        request.setFilter("Конференц-связь, Видеосвязь, ЖК-панель");
        Contexts.Scan scan = FormConverters.toScan(request);
        assertThat(scan.getOffices(), anyOf(containsString("1,2"), containsString("2,1")));
        assertThat(scan.getFilter(), containsString("video"));
        assertThat(scan.getFilter(), containsString("lcd_panel"));
        assertThat(scan.getFilter(), containsString("voice_conferencing"));
        assertFalse(scan.isAuto());
    }

    @Test
    public void contextWithList() {
        AddResourceRequest request = new AddResourceRequest();
        request.setOffice("Москва, БЦ Морозов, Санкт-Петербург, БЦ Бенуа");
        request.setManualOffice("да");
        request.setFilter("видео");
        Contexts.Scan scan = FormConverters.toScan(request);
        String context = scan.toString();
        Contexts.Scan read = Contexts.Scan.parse(context);
        assertEquals(scan, read);
    }

    @Test
    public void contextAuto() {
        AddResourceRequest request = new AddResourceRequest();
        request.setFilter("видео");
        Contexts.Scan scan = FormConverters.toScan(request);
        String context = scan.toString();
        Contexts.Scan read = Contexts.Scan.parse(context);
        assertEquals(scan, read);
    }

    @Test
    public void contextSwap() {
        Contexts.Swap swap = Contexts.swap("org", 1, "source", "target");
        String swapString = swap.toString();
        assertEquals(swap, Contexts.Swap.parse(swapString));
    }

    @Test
    public void contextMove() {
        Contexts.Swap move = Contexts.swap("org", 1, null, "target");
        String moveString = move.toString();
        assertEquals(move, Contexts.Swap.parse(moveString));
    }

    @Test
    public void contextEmptyRequest() {
        AddResourceRequest request = new AddResourceRequest();
        Contexts.Scan scan = FormConverters.toScan(request);
        assertTrue(scan.getOffices().isEmpty());
        assertTrue(scan.isAuto());
    }
}
