package ru.yandex.parser.mail.received;

import org.junit.Test;

import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ReceivedParserTest extends TestBase {
    @Test
    public void test() throws Exception {
        String received =
            "from mxfront4h.mail.yandex.net "
            + "(mxfront4h.mail.yandex.net [84.201.187.136]) "
            + "by service5h.mail.yandex.net (Yandex) with ESMTP id 6576D40013 "
            + "for <service@services.mail.yandex.net>; "
            + "Wed, 21 Mar 2012 17:04:17 +0400 (MSK)";
        YandexAssert.check(
            new StringChecker(
                "Received("
                + "ExtendedDomain(mxfront4h.mail.yandex.net,"
                + "TcpInfo(mxfront4h.mail.yandex.net,84.201.187.136),null),"
                + "ExtendedDomain(service5h.mail.yandex.net,null,null),"
                + "ESMTP,6576D40013,service@services.mail.yandex.net,"
                + "1332335057000)"),
            new ReceivedParser().parse(received).toString());
    }

    @Test
    public void testNwsmtp() throws Exception {
        String received =
            "from support-love.com (support-love.com [193.0.170.20]) "
            + "by mxfront3j.mail.yandex.net (nwsmtp/Yandex) "
            + "with ESMTP id 6C3qy5eIKU-oIs0hd7H; "
            + "Mon,  8 Jul 2013 19:50:18 +0400";
        YandexAssert.check(
            new StringChecker(
                "Received("
                + "ExtendedDomain(support-love.com,"
                + "TcpInfo(support-love.com,193.0.170.20),null),"
                + "ExtendedDomain(mxfront3j.mail.yandex.net,null,null),"
                + "ESMTP,6C3qy5eIKU-oIs0hd7H,null,"
                + "1373298618000)"),
            new ReceivedParser().parse(received).toString());
    }

    @Test
    public void testNestedComment() throws Exception {
        String received =
            "from post.bigcom.ru (post.bigcom.ru [78.107.2.210]) "
            + "by mxcorp1g.mail.yandex.net (nwsmtp/Yandex) "
            + "with ESMTPS id bMXpFcwHxM-3MROvImP; "
            + "Tue, 12 Sep 2017 03:03:22 +0300 "
            + "(using TLSv1.2 with cipher ECDHE-RSA-AES128-SHA256 "
            + "(128/128 bits))"
            + "(Client certificate not present)";
        YandexAssert.check(
            new StringChecker(
                "Received("
                + "ExtendedDomain(post.bigcom.ru,"
                + "TcpInfo(post.bigcom.ru,78.107.2.210),null),"
                + "ExtendedDomain(mxcorp1g.mail.yandex.net,null,null),"
                + "ESMTPS,bMXpFcwHxM-3MROvImP,null,"
                + "1505174602000)"),
            new ReceivedParser().parse(received.toCharArray()).toString());
    }
}

