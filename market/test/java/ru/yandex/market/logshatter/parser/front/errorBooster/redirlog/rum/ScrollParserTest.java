package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

public class ScrollParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new ScrollParser());
    }

    @Test
    public void parseFullFieldsScroll() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/@@reqid=1598368568.80249.85278.62227@@path=690.2096" +
            ".768@@slots=204315,0,94;263313,0,21;268013,0,65;221194,0,94;135686,0,21;267993,0,27@@vars=287=generic," +
            "rum_id=ru.zen-platform.desktop_article,-project=zen-platform,-page=desktop_article,-version=content_v113" +
            ".0.743004d0c,-env=production,-additional=%7B%22total-embeds-count%22%3A%222%22%2C%22yandex-direct-count" +
            "%22%3A%222%22%7D,1042=Mozilla%2F5.0%20(Windows%20NT%206.3%3B%20Win64%3B%20x64)%20AppleWebKit%2F537.36%20" +
            "(KHTML%2C%20like%20Gecko)%20Chrome%2F88.0.4324.190%20Safari%2F537.36,d=54.644439627607575," +
            "-cdn=unknown@@cts=1598368622389@@at=1@@uah=941849269@@icookie=7556028031598249998@@x-req-id" +
            "=1598368622634579-13362706540081255610@@robotness=0.0@@user_agent=Mozilla/5.0 (Windows NT 10.0; WOW64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.86 YaBrowser/20.8.0.903 Yowser/2.5 Yptp/1.21 " +
            "Safari/537.36@@url=@@1598368622@@94.159.39.74,94.159.39.74@@7556028031598249998";

        checker.check(
            line,
            new Date(1598368622000L),
            "zen-platform", // PROJECT
            "", // SERVICE
            "desktop_article", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "yandex.ru", //VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(135686, 204315, 221194, 263313, 267993, 268013), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BROWSER_ENGINE_VERSION
            "YandexBrowser", // BROWSER_NAME
            "18.9.0.3363", // BROWSER_VERSION
            "18.9", // BROWSER_VERSION_MAJOR
            "Chromium", // BROWSER_BASE
            "MacOS", // OS_FAMILY
            "Mac OS X Sierra", // OS_NAME
            "10.12.6", // OS_VERSION
            "10.12", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "content_v113.0.743004d0c", // VERSION
            0, // REGION
            "1598368568.80249.85278.62227", // REQUEST_ID
            UnsignedLong.valueOf("17821218886945416816"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("7556028031598249998"), // YANDEXUID
            Arrays.asList("total-embeds-count", "yandex-direct-count"), // KV_KEYS
            Arrays.asList("2", "2"), // KV_VALUES
            false, // IS_INTERNAL
            "unknown", // CDN
            "::ffff:94.159.39.74", // IPv6
            false, // LOGGED_IN
            ScrollType.SCROLL, // TYPE
            54.644439627607575 // SCROLL
        );
    }

    @Test
    public void parseFullFieldsScrollLatency() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/@@reqid=1598368568.80249.85278.62227@@path=690.2096.768" +
            ".2373@@slots=204315,0,94;263313,0,21;268013,0,65;221194,0,94;135686,0,21;267993,0,27@@vars=287=generic," +
            "rum_id=ru.zen-platform.desktop_article,-project=zen-platform,-page=desktop_article,-version=content_v113" +
            ".0.743004d0c,-env=production,-additional=%7B%22total-embeds-count%22%3A%222%22%2C%22yandex-direct-count" +
            "%22%3A%222%22%7D,1042=Mozilla%2F5.0%20(Windows%20NT%206.1%3B%20)%20AppleWebKit%2F537.36%20" +
            "(KHTML%2C%20like%20Gecko)%20Chrome%2F81.0.4044.138%20YaBrowser%2F20.4.3.257%20Yowser%2F2" +
            ".5%20Safari%2F537.36,d=14.86606874688144," +
            "-cdn=unknown@@cts=1598368622389@@at=1@@uah=941849269@@icookie=7556028031598249998@@x-req-id" +
            "=1598368622634579-13362706540081255610@@robotness=0.0@@user_agent=Mozilla/5.0 (Windows NT 10.0; WOW64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.86 YaBrowser/20.8.0.903 Yowser/2.5 Yptp/1.21 " +
            "Safari/537.36@@url=@@1598368622@@94.159.39.74,94.159.39.74@@7556028031598249998";

        checker.check(
            line,
            new Date(1598368622000L),
            "zen-platform", // PROJECT
            "", // SERVICE
            "desktop_article", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "yandex.ru", //VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(135686, 204315, 221194, 263313, 267993, 268013), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BROWSER_ENGINE_VERSION
            "YandexBrowser", // BROWSER_NAME
            "18.9.0.3363", // BROWSER_VERSION
            "18.9", // BROWSER_VERSION_MAJOR
            "Chromium", // BROWSER_BASE
            "MacOS", // OS_FAMILY
            "Mac OS X Sierra", // OS_NAME
            "10.12.6", // OS_VERSION
            "10.12", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "content_v113.0.743004d0c", // VERSION
            0, // REGION
            "1598368568.80249.85278.62227", // REQUEST_ID
            UnsignedLong.valueOf("17821218886945416816"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("7556028031598249998"), // YANDEXUID
            Arrays.asList("total-embeds-count", "yandex-direct-count"), // KV_KEYS
            Arrays.asList("2", "2"), // KV_VALUES
            false, // IS_INTERNAL
            "unknown", // CDN
            "::ffff:94.159.39.74", // IPv6
            false, // LOGGED_IN
            ScrollType.SCROLL_LATENCY, // TYPE
            14.86606874688144 // SCROLL
        );
    }
}
