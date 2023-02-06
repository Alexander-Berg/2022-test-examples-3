package ru.yandex.parser.mail.received;

import java.net.InetAddress;
import java.util.Collections;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.parser.mail.errors.ErrorInfo;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.ip.CidrSet;
import ru.yandex.util.ip.IpSetChecker;

public class ReceivedChainParserTest extends TestBase {
    private static final String RECEIVED_YNDX_TO_YNDX =
        "from mxfront4h.mail.yandex.net "
        + "(mxfront4h.mail.yandex.net [5.255.224.195]) "
        + "by service5h.mail.yandex.net (Yandex) with ESMTP id "
        + "bZ8gVjiq44-8j08Wnw7 for <service@services.mail.yandex.net>; "
        + "Wed, 21 Mar 2012 17:04:17 +0400 (MSK)";

    private static final String RECEIVED_EXT_TO_YNDX =
        "from mail.google.com  "
        + "(mail-wr1-x42c.google.com [2a00:1450:4864:20::42c]) "
        + "by mxfront4h.mail.yandex.net (Yandex) with ESMTP id "
        + "bZ8gVjiq44-8j08Wnw7 for <service@services.mail.yandex.net>; "
        + "Wed, 21 Mar 2012 17:04:17 +0400 (MSK)";

    private static final String RECEIVED_SUSPICIOUS_TO_YNDX =
        "from example.com  "
        + "([2a00:1450:4864:21::]) "
        + "by mxfront4h.mail.yandex.net (Yandex) with ESMTP id "
        + "bZ8gVjiq44-8j08Wnw7 for <service@services.mail.yandex.net>; "
        + "Wed, 21 Mar 2012 17:04:17 +0400 (MSK)";

    private static final String RECEIVED_FAKE_YNDX_TO_YNDX =
        "from mxfront4h.mail.yandex.net "
        + "(mxfront4h.mail.yandex.net [134.121.1.137]) "
        + "by mxfront4h.mail.yandex.net (Yandex) with ESMTP id "
        + "bZ8gVjiq44-8j08Wnw7 for <service@services.mail.yandex.net>; "
        + "Wed, 21 Mar 2012 17:04:17 +0400 (MSK)";


    private static final String RECEIVED_YNDX_TO_YNDX_2 =
        "from service5h.mail.yandex.net "
        + "(service5h.mail.yandex.net [77.88.10.49]) "
        + "by mxfront1o.mail.yandex.net (Yandex) with ESMTP id wrong_id "
        + "for <service@services.mail.yandex.net>; "
        + "Wed, 21 Mar 2012 17:04:17 +0400 (MSK)";

    private static final String RECEIVED_INTERNAL_NO_FROM =
        "by service5h.mail.yandex.net (Yandex) with ESMTP id wrong_id "
        + "for <service@services.mail.yandex.net>; "
        + "Wed, 21 Mar 2012 17:04:17 +0400 (MSK)";

    private static final String SODEV_1975_FROM_YANDEX_TO_YANDEX =
        "from mxfront10q.mail.yandex.net (localhost [127.0.0.1]) "
        + "by mxfront10q.mail.yandex.net with LMTP id "
        + "piTSnnq8V6-mc.us1_39026.439867.5e676cfd8304a.full_000001 "
        + "for <rudenkoag@ya.ru>; Tue, 10 Mar 2020 13:34:16 +0300";

    private static final String SODEV_1975_FROM_EXTERNAL_TO_YANDEX =
        "from mail158.suw161.rsgsv.net (mail158.suw161.rsgsv.net "
        + "[198.2.175.158]) "
        + "by mxfront10q.mail.yandex.net (mxfront/Yandex) with ESMTPS id "
        + "2tDHvNZpVr-YF14BaWt; "
        + "Tue, 10 Mar 2020 13:34:15 +0300 "
        + "(using TLSv1.2 with cipher ECDHE-RSA-AES128-GCM-SHA256 "
        + "(128/128 bits)) "
        + "(Client certificate not present)";

    private static final String MICROSOFT_RECEIVED_1 =
        "from AMBER22.ld.yandex.ru (5.255.224.195)"
        + " by PRINCE22-N2.ld.yandex.ru (5.255.224.195)"
        + " with Microsoft SMTP Server"
        + " (TLS) id 15.0.1497.2; Mon, 21 Sep 2020 17:37:44 +0300";
    private static final String MICROSOFT_RECEIVED_2 =
        "from mail-eopbgr50042.outbound.protection.outlook.com"
        + " (mail-eopbgr50042.outbound.protection.outlook.com [40.107.5.42])"
        + " by amber22.ld.yandex.ru (5.255.224.195)"
        + " with Microsoft SMTP Server (TLS)"
        + " id 15.0.1497.2 via Frontend Transport;"
        + " Mon, 21 Sep 2020 17:37:44 +0300";

    private final Predicate<InetAddress> yandexNets;

    public ReceivedChainParserTest() throws Exception {
        super(false, 0L);
        yandexNets =
            new IpSetChecker<>(
                resource("yandex-nets.txt").toFile(),
                CidrSet.INSTANCE);
    }

    private void assertState(
        final ReceivedChainParser parser,
        final String yandexSmtpId,
        final String sourceDomain)
    {
        Assert.assertNull(parser.errorInfo());
        Assert.assertEquals("SmtpId:", yandexSmtpId, parser.yandexSmtpId());
        Assert.assertEquals("Source domain:",
            sourceDomain, parser.sourceDomain());
    }

    private void assertState(
        final ReceivedChainParser parser,
        final String yandexSmtpId,
        final String sourceDomain,
        final ErrorInfo.Scope scope,
        final ErrorInfo.Type type)
    {
        Assert.assertNotNull(
            String.format("Expected Scope=%s, Type=%s, got null", scope, type),
            parser.errorInfo());
        Assert.assertEquals("Error-Scope:", scope, parser.errorInfo().scope());
        Assert.assertEquals("Error-Type:", type, parser.errorInfo().type());
        Assert.assertEquals("SmtpId:", yandexSmtpId, parser.yandexSmtpId());
        Assert.assertEquals("Source domain:",
            sourceDomain, parser.sourceDomain());
    }

    @Test
    public void testFromExt() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets);
        assertState(parser, null, null);
        parser.process(RECEIVED_YNDX_TO_YNDX_2);
        assertState(parser, null, "yandex.net");
        parser.process(RECEIVED_YNDX_TO_YNDX);
        assertState(parser, "bZ8gVjiq44-8j08Wnw7", "yandex.net");
        Assert.assertEquals(
            Collections.singleton("service@services.mail.yandex.net"),
            parser.recipients());
        parser.process(RECEIVED_EXT_TO_YNDX);
        assertState(parser, "bZ8gVjiq44-8j08Wnw7", "google.com");
        parser.process(RECEIVED_YNDX_TO_YNDX_2); // fabricated by ext
        assertState(parser, "bZ8gVjiq44-8j08Wnw7", "google.com");
        // Parsing was completed before YNDX_TO_YNDX_2
        Assert.assertNull(parser.errorInfo());
    }

    @Test
    public void testWrongOrder() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets);
        parser.process(RECEIVED_YNDX_TO_YNDX);
        assertState(parser, "bZ8gVjiq44-8j08Wnw7", "yandex.net");
        parser.process(RECEIVED_YNDX_TO_YNDX_2);
        assertState(parser,
            "bZ8gVjiq44-8j08Wnw7",
            "yandex.net",
            ErrorInfo.Scope.BY,
            ErrorInfo.Type.FIELD_MISSING);
    }

    @Test
    public void testFake() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets);
        parser.process(RECEIVED_YNDX_TO_YNDX);
        parser.process(RECEIVED_FAKE_YNDX_TO_YNDX);
        assertState(parser,
            "bZ8gVjiq44-8j08Wnw7",
            "134.121.1.0",
            ErrorInfo.Scope.FROM,
            ErrorInfo.Type.FRAUD);
    }

    @Test
    public void testNoFrom() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets);
        parser.process(RECEIVED_YNDX_TO_YNDX_2);
        parser.process(RECEIVED_INTERNAL_NO_FROM);
        assertState(parser,
            null,
            "yandex.net");
    }

    @Test
    public void testSuspicious() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets);
        parser.process(RECEIVED_SUSPICIOUS_TO_YNDX);
        assertState(parser,
            "bZ8gVjiq44-8j08Wnw7",
            "example.com");
    }

    @Test
    public void testNonStandardId() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets);
        parser.process(SODEV_1975_FROM_YANDEX_TO_YANDEX);
        assertState(parser, null, "yandex.net");
        Assert.assertEquals(
            Collections.singleton("rudenkoag@ya.ru"),
            parser.recipients());
        parser.process(SODEV_1975_FROM_EXTERNAL_TO_YANDEX);
        assertState(parser, "2tDHvNZpVr-YF14BaWt", "rsgsv.net");
        Assert.assertEquals("mxfront", parser.mailFront());
    }

    @Test
    public void testMSExchange() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets);
        parser.process(MICROSOFT_RECEIVED_1);
        assertState(parser, null, "yandex.net");
        parser.process(MICROSOFT_RECEIVED_2);
        assertState(parser, null, "outlook.com");
    }

    @Test
    public void testCrm() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets);
        parser.process(
            "from mxcorp1j.mail.yandex.net (localhost [127.0.0.1])"
            + " by mxcorp1j.mail.yandex.net with LMTP id vCS7WssHfh-lDtCI5ih;"
            + " Tue, 19 May 2020 13:08:48 +0300");
        parser.process(
            "from mxcorp1j.mail.yandex.net (localhost.localdomain [127.0.0.1])"
            + " by mxcorp1j.mail.yandex.net (Yandex)"
            + " with ESMTP id 735751AA00F7;"
            + " Tue, 19 May 2020 13:08:48 +0300 (MSK)");
        parser.process(
            "from forward300o.mail.yandex.net (forward300o.mail.yandex.net"
            + " [2a02:6b8:0:1a2d::604])"
            + " by mxcorp1j.mail.yandex.net (mxcorp/Yandex)"
            + " with ESMTPS id ohdTpLcjAk-8l4WApIm;"
            + " Tue, 19 May 2020 13:08:47 +0300"
            + " (using TLSv1.2 with cipher "
            + " ECDHE-RSA-AES128-GCM-SHA256 (128/128 bits))"
            + " (Client certificate not present)");
        parser.process(
            "by forward300o.mail.yandex.net (Yandex)"
            + " id AF8C86A0E04; Tue, 19 May 2020 13:08:47 +0300 (MSK)");
        Assert.assertEquals("mxcorp", parser.mailFront());
    }

    @Test
    public void testFbl() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets, true);
        parser.process(
            "from forward105o.mail.yandex.net ([37.140.190.183]:47242)"
            + " by mx49.mail.ru with esmtp"
            + " (envelope-from <avonsokolova@yandex.ru>)"
            + " id 1ei1HT-0004Bi-Lz; Sat, 03 Feb 2018 20:07:12 +0300");
        parser.process(
            "from mxback15g.mail.yandex.net (mxback15g.mail.yandex.net"
            + " [IPv6:2a02:6b8:0:1472:2741:0:8b7:94])"
            + " by forward105o.mail.yandex.net (Yandex)"
            + " with ESMTP id 4AAB04443E59;"
            + " Sat, 3 Feb 2018 20:07:09 +0300 (MSK)");
        parser.process(
            "from localhost (localhost [::1])"
            + " by mxback15g.mail.yandex.net (nwsmtp/Yandex)"
            + " with ESMTP id o0ElzD9TEd-77janX7d;"
            + " Sat, 03 Feb 2018 20:07:08 +0300");
        parser.process(
            "by web57g.yandex.ru with HTTP; Sat, 03 Feb 2018 20:07:07 +0300");
        Assert.assertEquals("nwsmtp", parser.mailFront());
    }

    @Test
    public void testMailFront() {
        ReceivedChainParser parser = new ReceivedChainParser(yandexNets, true);
        parser.process(
            "from sas2-5ecda9fc820e.qloud-c.yandex.net "
            + "(sas2-5ecda9fc820e.qloud-c.yandex.net "
            + "[2a02:6b8:c08:3c28:0:640:5ecd:a9fc]) "
            + "by sas1-f4aeb445c063.qloud-c.yandex.net with LMTP id "
            + "D369aAGDH3-<20210805115234.613454021@zmail00.mfms.ru> "
            + "for <ogm-2505@yandex.ru>; Thu, 05 Aug 2021 11:52:35 +0300");
        parser.process(
            "from zmail00.mfms.ru (zmail00.mfms.ru [91.213.158.74]) "
            + "by sas2-5ecda9fc820e.qloud-c.yandex.net (mxfront/Yandex) with "
            + "ESMTPS id jvE9j2ZzqE-qYsqXE3d; "
            + "Thu, 05 Aug 2021 11:52:34 +0300 "
            + "(using TLSv1.2 with cipher ECDHE-RSA-AES128-GCM-SHA256 "
            + "(128/128 bits)) "
            + "(Client certificate not present)");
        parser.process(
            "from s0546 (smssrv04.mfms [10.160.140.11]) "
            + "by vmail00.mfms.ru (Postfix) with ESMTP id "
            + "vmail004GgMnG3Qz9zFpvh "
            + "for <ogm-2505@yandex.ru>; "
            + "Thu,  5 Aug 2021 11:52:34 +0300 (MSK)");
        Assert.assertEquals("mxfront", parser.mailFront());
    }
}

