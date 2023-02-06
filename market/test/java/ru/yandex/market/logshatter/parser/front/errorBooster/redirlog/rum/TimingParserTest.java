package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

public class TimingParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new TimingParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://www.yandex.com/@@reqid=1551527479.88509.141079.272705@@ruip=217.118.91" +
            ".74@@is_robot=1@@is_internal=1@@dtype=stred@@pid=1@@cid=72202@@path=690.1033@@slots=63208,0," +
            "23@@experiments=one=1@@vars=143=28.15.899.1119,287=11494,1961=1,1964=1,1384.1385=111,2110=333,2109=222," +
            "1965=1,-experiments=two=2,-project=morda,-service=zen%2Fv1,-page=plain%2Fv1,-platform=desktop,-cdn=msk," +
            "-env=production,-version=0xrelease%2Fdesktop%2Fv1.157.0,-blocker=,1042=Mozilla%2F5.0%20(Windows%20NT%206" +
            ".3%3B%20Win64%3B%20x64)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F71.0.3578" +
            ".98%20Safari%2F537.36,2129=1551527919798,1036=1,1037=1,1038=1,1039=19843,2128=308,2127=306,1040=1,1040" +
            ".906=19843,1310.2084=874,1310.2085=17023,1383=171,1310.1309=5,2130=640,1041=284,1041.906=454,1310" +
            ".1007=17041,2870=4g,2299=1,2116=1,3140=4096,2114=1,2131=36890,2123=36885,2770=36867,2769=20718,2113=1," +
            "2112=1,2111=1,2117=18165,2120=19844,1088=1,3105=1,3141=256,2119=19844,1484=1,770.76=2,2115=52," +
            "2437=2768@@cts=1551527956695@@at=1@@uah=911342391@@icookie=1659031481551527479@@x-req-id" +
            "=1551527503546163-2207902560205539614@@url=@@1551527503@@212.34.20.107,212.34.20.107@@1659031481551527479";

        checker.check(
            line,
            new Date(1551527503000L),
            "morda", // PROJECT
            "zen/v1", // SERVICE
            "plain/v1", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://www.yandex.com/", // URL
            UnsignedLong.valueOf("1503146173271039555"), // URL_ID
            "www.yandex.com", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(63208), // TEST_IDS
            Arrays.asList("one=1"), // EXP_FLAGS
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
            "0xrelease/desktop/v1.157.0", // VERSION
            11494, // REGION
            "1551527479.88509.141079.272705", // REQUEST_ID
            UnsignedLong.valueOf("11978214116979785479"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("1659031481551527479"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES,
            true, // IS_INTERNAL
            "msk", // CDN
            "::ffff:217.118.91.74", // IPv6
            false, // LOGGED_IN
            UnsignedLong.valueOf(1551527919798L), // NAVIGATION_START
            19844, // RESPONSE_START
            19844, // RESPONSE_END
            1, // DOMAIN_LOOKUP_START
            1, // DOMAIN_LOOKUP_END
            18165, // REQUEST_START
            1, // FETCH_START
            20718, // DOM_LOADING_TOTAL
            36867, // DOM_INTERACTIVE_TOTAL
            36885, // DOM_CONTENT_LOADED
            36890, // DOM_CONTENT_LOADED_FINISHED
            1, // CONNECT_START
            1, // CONNECT_END
            1, // HISTORY_LENGTH
            17041, // DOM_LOADED
            5, // DOM_INIT
            17023, // DOM_INTERACTIVE
            874, // DOM_LOADING
            19843, // HTML_TOTAL
            1, // HTML
            19843, // TTFB
            1, // TCP
            1, // DNS
            1, // WAIT
            Visibility.VISIBLE, // VISIBILITY
            ConnectionType.CELLULAR_4G, // CONNECTION_TYPE
            4096, // DEVICE_MEMORY
            NavigationType.FORWARD, // NAVIGATION_TYPE
            "4g", // EFFECTIVE_TYPE
            256, // HARDWARE_CONCURRENCY
            171, // SSL
            284, // TTFP
            454, // TTFP_TOTAL
            640, // FIRST_PAINT_TIME
            52, // SECURE_CONNECTION_START
            306, // UNLOAD_START
            308, // UNLOAD_END
            111, // REDIRECT_COUNT
            222, // REDIRECT_START
            333, // REDIRECT_END
            true, // IS_PREFETCH
            true // IS_PRERENDER
        );
    }

    @Test
    public void parseTurboAPPFields() throws Exception {
        String line = "HTTP_REFERER=https://www.yandex.com/@@reqid=1551527479.88509.141079.272705@@ruip=217.118.91" +
            ".74@@is_robot=1@@is_internal=1@@dtype=stred@@pid=1@@cid=72202@@path=690.1033@@slots=63208,0," +
            "23@@experiments=one=1@@vars=143=28.15.899.1119,287=11494,1961=1,1964=1,1384.1385=111,2110=333,2109=222," +
            "1965=1,-experiments=two=2,-project=morda,-service=zen%2Fv1,-page=plain%2Fv1,-platform=turboApp,-cdn=msk," +
            "-env=production,-uid=12345678910,-url=https://turboapp.yandex.ru/subpage.html," +
            "-version=0xrelease%2Fdesktop%2Fv1.157.0,-blocker=,1042=Mozilla%2F5.0%20(Windows%20NT%206" +
            ".3%3B%20Win64%3B%20x64)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F71.0.3578" +
            ".98%20Safari%2F537.36,2129=1551527919798,1036=1,1037=1,1038=1,1039=19843,2128=308,2127=306,1040=1,1040" +
            ".906=19843,1310.2084=874,1310.2085=17023,1383=171,1310.1309=5,2130=640,1041=284,1041.906=454,1310" +
            ".1007=17041,2870=4g,2299=1,2116=1,3140=4,2114=1,2131=36890,2123=36885,2770=36867,2769=20718,2113=1," +
            "2112=1,2111=1,2117=18165,2120=19844,1088=1,3105=1,3141=256,2119=19844,1484=1,770.76=2,2115=52," +
            "2437=2768@@cts=1551527956695@@at=1@@uah=911342391@@icookie=1659031481551527479@@x-req-id" +
            "=1551527503546163-2207902560205539614@@url=@@1551527503@@212.34.20.107,212.34.20.107@@1659031481551527479";

        checker.check(
            line,
            new Date(1551527503000L),
            "morda", // PROJECT
            "zen/v1", // SERVICE
            "plain/v1", // PAGE
            Platform.TURBO_APP, // PLATFORM
            "https://turboapp.yandex.ru/subpage.html", // URL
            UnsignedLong.valueOf("4081942679413948515"), // URL_ID
            "turboapp.yandex.ru", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(63208), // TEST_IDS
            Arrays.asList("one=1"), // EXP_FLAGS
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
            "0xrelease/desktop/v1.157.0", // VERSION
            11494, // REGION
            "1551527479.88509.141079.272705", // REQUEST_ID
            UnsignedLong.valueOf("11978214116979785479"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("12345678910"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES,
            true, // IS_INTERNAL
            "msk", // CDN
            "::ffff:217.118.91.74", // IPv6
            false, // LOGGED_IN
            UnsignedLong.valueOf(1551527919798L), // NAVIGATION_START
            19844, // RESPONSE_START
            19844, // RESPONSE_END
            1, // DOMAIN_LOOKUP_START
            1, // DOMAIN_LOOKUP_END
            18165, // REQUEST_START
            1, // FETCH_START
            20718, // DOM_LOADING_TOTAL
            36867, // DOM_INTERACTIVE_TOTAL
            36885, // DOM_CONTENT_LOADED
            36890, // DOM_CONTENT_LOADED_FINISHED
            1, // CONNECT_START
            1, // CONNECT_END
            1, // HISTORY_LENGTH
            17041, // DOM_LOADED
            5, // DOM_INIT
            17023, // DOM_INTERACTIVE
            874, // DOM_LOADING
            19843, // HTML_TOTAL
            1, // HTML
            19843, // TTFB
            1, // TCP
            1, // DNS
            1, // WAIT
            Visibility.VISIBLE, // VISIBILITY
            ConnectionType.CELLULAR_4G, // CONNECTION_TYPE
            4, // DEVICE_MEMORY
            NavigationType.FORWARD, // NAVIGATION_TYPE
            "4g", // EFFECTIVE_TYPE
            256, // HARDWARE_CONCURRENCY
            171, // SSL
            284, // TTFP
            454, // TTFP_TOTAL
            640, // FIRST_PAINT_TIME
            52, // SECURE_CONNECTION_START
            306, // UNLOAD_START
            308, // UNLOAD_END
            111, // REDIRECT_COUNT
            222, // REDIRECT_START
            333, // REDIRECT_END
            true, // IS_PREFETCH
            true // IS_PRERENDER
        );
    }
}
