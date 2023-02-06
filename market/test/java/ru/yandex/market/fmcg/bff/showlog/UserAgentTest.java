package ru.yandex.market.fmcg.bff.showlog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAgentTest {

    @Test
    void fromString() {
        UserAgent ua = UserAgent.fromString("ru.yandex.supercheck/2.0.2-beta.123 (samsung SM-A605FN; Android 9)");
        assertEquals("Android", ua.getAppPlatform());
        assertEquals("9", ua.getAppPlatformVersion());
        assertEquals("2.0.2-beta.123", ua.getAppVersion());

        ua = UserAgent.fromString("ru.yandex.supercheck/2.0.2.124 (i&nbsp;phone SM-A605FN; IOS 12)");
        assertEquals("IOS", ua.getAppPlatform());
        assertEquals("12", ua.getAppPlatformVersion());
        assertEquals("2.0.2.124", ua.getAppVersion());

        ua = UserAgent.fromString("ru.yandex.supercheck/2.0.2-beta.123");
        assertEquals(null, ua.getAppPlatform());
        assertEquals(null, ua.getAppPlatformVersion());
        assertEquals("2.0.2-beta.123", ua.getAppVersion());

        ua = UserAgent.fromString("");
        assertEquals(null, ua.getAppPlatform());
        assertEquals(null, ua.getAppPlatformVersion());
        assertEquals(null, ua.getAppVersion());

        ua = UserAgent.fromString("sdfasdf");
        assertEquals(null, ua.getAppPlatform());
        assertEquals(null, ua.getAppPlatformVersion());
        assertEquals(null, ua.getAppVersion());

        ua = UserAgent.fromString("Mozilla/5.0 (compatible; YandexMarket/2.0; +http://yandex.com/bots)");
        assertEquals(null, ua.getAppPlatform());
        assertEquals(null, ua.getAppPlatformVersion());
        assertEquals("5.0", ua.getAppVersion());

        ua = UserAgent.fromString("Mozilla/5.0 (compatible; AhrefsBot/5.2; +http://ahrefs.com/robot/)");
        assertEquals(null, ua.getAppPlatform());
        assertEquals(null, ua.getAppPlatformVersion());
        assertEquals("5.0", ua.getAppVersion());

        ua = UserAgent.fromString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
        assertEquals(null, ua.getAppPlatform());
        assertEquals(null, ua.getAppPlatformVersion());
        assertEquals("5.0", ua.getAppVersion());

        // smoke test
        ua = UserAgent.fromString("Mozilla/5.0 (iPhone; CPU iPhone OS 12_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1.1 Mobile/15E148 Safari/604.1");
        assertEquals("5.0", ua.getAppVersion());
    }
}