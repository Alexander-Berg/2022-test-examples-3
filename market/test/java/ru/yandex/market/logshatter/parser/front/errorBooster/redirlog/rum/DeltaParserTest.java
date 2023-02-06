package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

public class DeltaParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new DeltaParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/@@reqid=11111%2F1551522448.78149.140360.260413@@robotness=1" +
            ".0@@ruip=217.118.91.74@@is_internal=1@@path=690.2096.2877@@slots=63207,0,60;125307,0,30;121749,0,53;" +
            "125042,0,90;108758,0,25@@vars=143=28.15.899,287=43,1961=0,1088=1,3105=1,1964=1,1965=0,-project=morda," +
            "-service=zen,-page=touch%3Aproduct,-platform=desktop,-env=production,-version=2.2026,-blocker=," +
            "1042=Mozilla%2F5.0%20(Windows%20NT%205.1)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)" +
            "%20Chrome%2F49.0.2623.112%20Safari%2F537.36,143.2129=1551522511013,143.2112=190,143.2119=592,1701=628," +
            "207.2154=3951.5,207.1428=3887,2877=666,2924=28.15.1604,2925=1,689.2322=1572" +
            ".995@@cts=1551522514906@@at=1@@uah=763090462@@icookie=2887475811551512597@@x-req-id=1551522453064543" +
            "-6932803949893231700@@url=@@1551522453@@178.46.101.118,178.46.101.118@@2887475811551512597";

        checker.check(
            line,
            new Date(1551522453000L),
            "morda", // PROJECT
            "zen", // SERVICE
            "touch:product", // PAGE
            Platform.DESKTOP, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "yandex.ru", //VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(63207, 108758, 121749, 125042, 125307), // TEST_IDS
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
            true, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "2.2026", // VERSION
            43, // REGION
            "11111/1551522448.78149.140360.260413", // REQUEST_ID
            UnsignedLong.valueOf("2040610696738021161"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("2887475811551512597"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:217.118.91.74", // IPv6
            false, // LOGGED_IN
            "js", // NAME
            666L, // DELTA
            2314L, // DELTA_END
            1, //SUBPAGE_INDEX
            "ru.morda.zen", ////SUBPAGE_NAME
            true, // IS_PREFETCH
            true, // IS_PRERENDER
            UnsignedLong.valueOf(1551522511013L), // NAVIGATION_START
            3951L // START_TIME
        );
    }
}
