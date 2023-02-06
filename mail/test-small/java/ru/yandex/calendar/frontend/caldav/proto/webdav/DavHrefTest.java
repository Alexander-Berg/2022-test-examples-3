package ru.yandex.calendar.frontend.caldav.proto.webdav;

import org.junit.jupiter.api.Test;

import ru.yandex.misc.test.Assert;

public class DavHrefTest {
    @Test
    public void encode() {
        String encoded = "/calendars/ivanov%40yandex-team.ru/events-871/%7B432AA244-286B-4AB9-8E24-8E74E1A418C0%7D.ics";
        String decoded = "/calendars/ivanov@yandex-team.ru/events-871/{432AA244-286B-4AB9-8E24-8E74E1A418C0}.ics";
        Assert.A.equals(encoded, DavHref.fromDecoded(decoded).getEncoded());

        Assert.A.equals("/calendars/abb%40yandex-team.ru/events-12/",
                DavHref.fromDecoded("/calendars/abb@yandex-team.ru/events-12/").getEncoded());
    }

    @Test
    public void decode() {
        String decoded = "/calendars/ivanov@yandex-team.ru/events-871/{432AA244-286B-4AB9-8E24-8E74E1A418C0}.ics";
        String encoded = "/calendars/ivanov%40yandex-team.ru/events-871/%7B432AA244-286B-4AB9-8E24-8E74E1A418C0%7D.ics";
        Assert.A.equals(decoded,
                DavHref.fromEncoded(encoded).getDecoded());

        Assert.A.equals("/calendars/abb@yandex-team.ru/events-12/",
                DavHref.fromEncoded("/calendars/abb%40yandex-team.ru/events-12/").getDecoded());
    }

    @Test
    public void root() {
        Assert.A.equals("/", DavHref.fromEncoded("/").getDecoded());
        Assert.A.equals("/", DavHref.fromDecoded("/").getEncoded());
    }
}
