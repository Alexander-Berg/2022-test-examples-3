package ru.yandex.parser.email;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.james.mime4j.dom.address.DomainList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.LenientAddressParser;
import org.junit.Test;

import ru.yandex.parser.rfc2047.DefaultRfc2047DecodersProvider;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class EmailParserTest extends TestBase {
    public EmailParserTest() {
        super(false, 0L);
    }

    private static String toString(final List<Mailbox> mailboxes) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Mailbox mailbox: mailboxes) {
            if (sb.length() > 1) {
                sb.append(',');
                sb.append(' ');
            }
            sb.append('{');
            boolean empty = true;
            String name = mailbox.getName();
            if (name != null) {
                sb.append("name=");
                sb.append(name);
                empty = false;
            }
            String local = mailbox.getLocalPart();
            if (local != null) {
                if (empty) {
                    empty = false;
                } else {
                    sb.append(',');
                }
                sb.append("local=");
                sb.append(local);
            }
            String domain = mailbox.getDomain();
            if (domain != null) {
                if (empty) {
                    empty = false;
                } else {
                    sb.append(',');
                }
                sb.append("domain=");
                sb.append(domain);
            }
            DomainList route = mailbox.getRoute();
            if (!route.isEmpty()) {
                if (!empty) {
                    sb.append(',');
                }
                sb.append("route=");
                sb.append(route.toRouteString());
            }
            sb.append('}');
        }
        sb.append(']');
        return new String(sb);
    }

    private static void check(
        final List<Mailbox> expected,
        final List<Mailbox> actual)
    {
        YandexAssert.check(
            new StringChecker(toString(expected)),
            toString(actual));
    }

    private static List<Mailbox> parse(final String str) {
        return new EmailParser()
            .parse(str, DefaultRfc2047DecodersProvider.INSTANCE);
    }

    private static List<Mailbox> parseMime4j(final String str) {
        return LenientAddressParser.DEFAULT.parseAddressList(str).flatten();
    }

    private static void check(
        final List<Mailbox> expected,
        final String str)
    {
        check(expected, parse(str));
        check(expected, parseMime4j(str));
    }

    @Test
    public void test() {
        check(
            Collections.singletonList(
                new Mailbox("", null, "suggest.user2", "yandex.ru")),
            "\"\" <suggest.user2@yandex.ru>");
    }

    @Test
    public void testBare() {
        // XXX: mime4j can't parse this properly
        check(
            Collections.singletonList(
                new Mailbox("", null, "потапов", "почта.рф")),
            parse("потапов@почта.рф"));
    }

    @Test
    public void testBareDisplayName() {
        // XXX: mime4j can't parse this properly
        check(
            Collections.singletonList(
                new Mailbox("Дмитрий Потапов", null, "потапов", "почта.рф")),
            parse("Дмитрий Потапов <потапов@почта.рф>"));
    }

    @Test
    public void testQPDisplayNameEmptyEmail() {
        Mailbox expected = new Mailbox("Дмитрий Потапов", null, "", null);
        String str =
            "=?koi8-r?q?=e4=cd=c9=d4=d2=c9=ca_=f0=cf=d4=c1=d0=cf=d7?= <>";
        // XXX: Deviation from mime4j, we parse only one address here
        check(Collections.singletonList(expected), parse(str));
        check(
            Arrays.asList(expected, new Mailbox(null, null, ">", null)),
            parseMime4j(str));
    }

    @Test
    public void testComment() {
        check(
            Collections.singletonList(
                new Mailbox("Dmitry \"Di\\ma\" Potapov", null, "me", "ya.ru")),
            " \"Dmitry \\\"Di\\\\ma\\\" Potapov\" (Once (again)) <me@ya.ru>");
    }

    @Test
    public void testMultyByteBroken() {
        // XXX: mime4j breaks symbol in the middle because of rfc2047 violation
        check(
            Collections.singletonList(
                new Mailbox("Дима", null, "", null)),
            parse("=?utf-8?b?0JTQuNA=?= =?utf-8?B?vNCw?= <>"));
    }

    @Test
    public void testGroup() {
        check(
            Arrays.asList(
                new Mailbox("First on", null, "me", "ya.ru"),
                new Mailbox("Second on", null, "you", "ya.ru"),
                new Mailbox("", null, "he", "ya.ru"),
                new Mailbox("Dmitry Potapov", null, "dpotapov", "ya.ru")),
            "Group name:First on <me@ya.ru>,\"Second on\"<you@ya.ru>,he@ya.ru;"
            + "(Comment),Dmitry Potapov<dpotapov@ya.ru>");
    }

    @Test
    public void testRoute() {
        check(
            Collections.singletonList(
                new Mailbox(
                    "Name here",
                    new DomainList(Arrays.asList("ya.ru", "yandex.ru")),
                    "me",
                    "ya.ru")),
            "Name here<\t((comment) here)\t@ya.ru,@yandex.ru:me@ya.ru>");
    }

    @Test
    public void testBareStrings() {
        check(
            Arrays.asList(
                new Mailbox("", null, "hello", null),
                new Mailbox("", null, "world", null)),
            "hello, world");
    }

    @Test
    public void testEmptyString() {
        check(
            Collections.emptyList(),
            "");
    }

    @Test
    public void testBareStringInGroup() {
        check(
            Arrays.asList(
                new Mailbox("", null, "hello", null),
                new Mailbox("", null, "world", null)),
            "\"Group\" \"name\":hello,world");
    }

    @Test
    public void testBadDelimiter() {
        check(
            Collections.singletonList(
                new Mailbox(
                    "",
                    null,
                    "mailsearchtest",
                    "yandex.ru;IvanPupkin<pukin.user2@yandex.kz>")),
            "mailsearchtest@yandex.ru; Ivan Pupkin <pukin.user2@yandex.kz>");
    }

    @Test
    public void testRfc2047Comma() {
        // Insistent with Yandex.Mail, consistent with Thunderbird and Gmail
        check(
            Collections.singletonList(
                new Mailbox(
                    "Potapov, Dmitry; The: @nd",
                    null,
                    "me",
                    "ya.ru")),
            parse("=?utf-8?Q?Potapov,_Dmitry;_The:_@nd?= <me@ya.ru>"));
    }

    @Test
    public void testRfc2047AddrSpec() {
        check(
            Collections.singletonList(
                new Mailbox(
                    "",
                    null,
                    "potapov.d",
                    "gmail.com")),
            parse("=?utf-8?Q?<potapov.d@gmail.com>?="));
        check(
            Collections.singletonList(
                new Mailbox(
                    "",
                    null,
                    "potapov.d",
                    "gmail.com")),
            parse("=?utf-8?Q?potapov.d@gmail.com?="));
    }
}

