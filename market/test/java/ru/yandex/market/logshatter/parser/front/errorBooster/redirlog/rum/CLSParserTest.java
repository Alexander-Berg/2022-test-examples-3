package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

public class CLSParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new CLSParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/@@reqid=1598368568.80249.85278.62227@@path=690.2096" +
            ".4004@@slots=204315,0,94;263313,0,21;268013,0,65;221194,0,94;135686,0,21;267993,0,27@@vars=143=28.15" +
            ".1049,287=213,1961=0,1964=0,1965=0,2923=1,-project=morda,-page=widget,-platform=desktop,-env=production," +
            "-version=2020-08-20-1,-additional=%7B%7D,1042=Mozilla%2F5.0%20(Windows%20NT%2010.0%3B%20WOW64)" +
            "%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F84.0.4147.86%20YaBrowser%2F20.8.0" +
            ".903%20Yowser%2F2.5%20Yptp%2F1.21%20Safari%2F537.36,s=1.224753," +
            "-cdn=unknown@@cts=1598368622389@@at=1@@uah=941849269@@icookie=7556028031598249998@@x-req-id" +
            "=1598368622634579-13362706540081255610@@robotness=0.0@@user_agent=Mozilla/5.0 (Windows NT 10.0; WOW64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.86 YaBrowser/20.8.0.903 Yowser/2.5 Yptp/1.21 " +
            "Safari/537.36@@url=@@1598368622@@94.159.39.74,94.159.39.74@@7556028031598249998";

        checker.check(
            line,
            new Date(1598368622000L),
            "morda", // PROJECT
            "", // SERVICE
            "widget", // PAGE
            Platform.DESKTOP, // PLATFORM
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
            true, // ADBLOCK
            "2020-08-20-1", // VERSION
            213, // REGION
            "1598368568.80249.85278.62227", // REQUEST_ID
            UnsignedLong.valueOf("17821218886945416816"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("7556028031598249998"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "unknown", // CDN
            "::ffff:94.159.39.74", // IPv6
            false, // LOGGED_IN
            1.224753 // SCORE
        );
    }
}
