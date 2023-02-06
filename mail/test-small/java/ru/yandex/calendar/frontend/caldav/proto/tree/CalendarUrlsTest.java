package ru.yandex.calendar.frontend.caldav.proto.tree;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.frontend.caldav.proto.tree.CalendarUrls.Handler;
import ru.yandex.calendar.frontend.caldav.proto.webdav.DavHref;
import ru.yandex.misc.test.Assert;

public class CalendarUrlsTest {
    @SuppressWarnings("unchecked")
    @Test
    public void parse() {
        Handler<Object> handler = EasyMock.createMock(CalendarUrls.Handler.class);

        EasyMock.expect(handler.calendarsUserEvents("abb@yandex-team.ru", "12")).andReturn(1);
        //EasyMock.expect(handler.unknown("abb@yandex-team.ru", "dfgdgdfg")).andReturn(1);
        EasyMock.expect(handler.calendarsUserEventsUser("abb@yandex-team.ru", "12")).andReturn(2);
        EasyMock.expect(handler.calendarsUserEventsUser("levin-matveev@yandex.ru", "14")).andReturn(4);

        EasyMock.expect(handler.calendarsUserTodosTodo("somewho@somewhere.com", "XXX", "todo.ics")).andReturn(8);

        EasyMock.expect(handler.principalsUser("levin-matveev@yandex.ru")).andReturn(5);

        EasyMock.expect(
                handler.addressbooksUserAddressbookCard(
                        "nga@yandex-team.ru",
                        "5389B9C0-6BDA-4400-B7A9-49DE646AB9C9-ABSPlugin.vcf"))
                .andReturn(5);

        EasyMock.expect(handler.directory()).andReturn(5);
        EasyMock.expect(handler.directoryContact("me@here")).andReturn(5);

        EasyMock.expect(handler.root()).andReturn(7);

        EasyMock.replay(handler);

        CalendarUrls.parse(DavHref.fromDecoded("/calendars/abb@yandex-team.ru/events-12/"), handler);
        //CalendarUrls.parse("/calendars/abb@yandex-team.ru/dfgdgdfg/", handler);
        CalendarUrls.parse(DavHref.fromDecoded("/calendars/abb@yandex-team.ru/events-12/user"), handler);
        CalendarUrls.parse(DavHref.fromDecoded("/calendars/levin-matveev@yandex.ru/events-14/user/"), handler);

        CalendarUrls.parse(DavHref.fromDecoded("/calendars/somewho@somewhere.com/todos-XXX/todo.ics"), handler);

        CalendarUrls.parse(DavHref.fromDecoded("/principals/users/levin-matveev@yandex.ru/"), handler);

        CalendarUrls.parse(
                DavHref.fromDecoded(
                        "/addressbook/nga@yandex-team.ru/addressbook/"
                        + "5389B9C0-6BDA-4400-B7A9-49DE646AB9C9-ABSPlugin.vcf"),
                handler);

        CalendarUrls.parse(DavHref.fromDecoded("/directory/"), handler);
        CalendarUrls.parse(DavHref.fromDecoded("/directory/me@here"), handler);

        CalendarUrls.parse(DavHref.fromDecoded("/"), handler);

        EasyMock.verify(handler);
    }

    @Test
    public void serialize() {
        Assert.A.equals(
                "/calendars/abb@yandex-team.ru/events-12/",
                CalendarUrls.events("abb@yandex-team.ru", "12").getDecoded());
        Assert.A.equals(
                "/calendars/abb@yandex-team.ru/events-12/user/",
                CalendarUrls.eventsUser("abb@yandex-team.ru", "12").getDecoded());
    }

    @Test
    public void covertToTagForLog() {
        Function<String, String> c = (uri) -> CalendarUrls.covertToTagForLog(DavHref.fromDecoded(uri));

        Assert.equals("root", c.apply("/"));
        Assert.equals("directoryContact", c.apply("/directory/me@here"));
        Assert.equals("calendarsUserEvents", c.apply("/calendars/levin-matveev@yandex.ru/events-14"));
        Assert.equals("calendarsUserTodosTodo", c.apply("/calendars/somewho@somewhere.com/todos-XXX/todo.ics"));
        Assert.equals("unknown", c.apply("/favicon.ico"));
    }
} //~
