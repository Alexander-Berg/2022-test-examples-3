package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;
import ru.yandex.market.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.logshatter.useragent.UserAgentDetector;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class TimingParserTest {
    private LogParserChecker checker;

    private static final Map<String, String> fakeYabroTraits = ImmutableMap.<String, String>builder()
        .put(UserAgentDetector.BROWSER_BASE, "Chromium")
        .put(UserAgentDetector.BROWSER_BASE_VERSION, "68.0.3440.106")
        .put(UserAgentDetector.BROWSER_ENGINE, "WebKit")
        .put(UserAgentDetector.BROWSER_ENGINE_VERSION, "537.36")
        .put(UserAgentDetector.BROWSER_NAME, "YandexBrowser")
        .put(UserAgentDetector.BROWSER_VERSION, "18.9.0.3363")
        .put("CSP1Support", "true")
        .put("CSP2Support", "true")
        .put(UserAgentDetector.OS_FAMILY, "MacOS")
        .put(UserAgentDetector.OS_NAME, "Mac OS X Sierra")
        .put(UserAgentDetector.OS_VERSION, "10.12.6")
        .put("SVGSupport", "true")
        .put("WebPSupport", "true")
        .put("YaGUI", "2.5")
        .put("historySupport", "true")
        .put("isBrowser", "true")
        .put(UserAgentDetector.IS_MOBILE, "false")
        .put("isTouch", "false")
        .put("localStorageSupport", "true")
        .put("postMessageSupport", "true")
        .build();

    @Before
    public void setUp() {
        FakeUserAgentDetector detector = new FakeUserAgentDetector();
        detector.setDetectionResult(fakeYabroTraits);
        checker = new LogParserChecker(new TimingParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://www.yandex.com/@@reqid=1551527479.88509.141079.272705@@is_robot=1@@is_internal=1@@dtype=stred@@pid=1@@cid=72202@@path=690.1033@@slots=63208,0,23@@vars=143=28.15.899.1119,287=11494,1961=1,1964=1,1384.1385=111,2110=333,2109=222,1965=1,-project=morda,-service=zen,-page=plain.com,-platform=desktop,-env=production,-version=2.2026,-blocker=,1042=Mozilla%2F5.0%20(Windows%20NT%206.3%3B%20Win64%3B%20x64)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F71.0.3578.98%20Safari%2F537.36,2129=1551527919798,1036=1,1037=1,1038=1,1039=19843,2128=308,2127=306,1040=1,1040.906=19843,1310.2084=874,1310.2085=17023,1383=171,1310.1309=5,2130=640,1041=284,1041.906=454,1310.1007=17041,2870=4g,2299=1,2116=1,3140=4,2114=1,2131=36890,2123=36885,2770=36867,2769=20718,2113=1,2112=1,2111=1,2117=18165,2120=19844,3141=8,2119=19844,1484=1,770.76=2,2115=52,2437=2768@@cts=1551527956695@@at=1@@uah=911342391@@icookie=1659031481551527479@@x-req-id=1551527503546163-2207902560205539614@@url=@@1551527503@@212.34.20.107,212.34.20.107@@1659031481551527479";

        checker.check(
            line,
            new Date(1551527503000L),
            "morda", // PROJECT
            "zen", // SERVICE
            "plain.com", // PAGE
            Platform.DESKTOP, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "www.yandex.com", //VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(63208), // TEST_IDS
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
            11494, // REGION
            "1551527479.88509.141079.272705", // REQUEST_ID
            UnsignedLong.valueOf("11978214116979785479"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("1659031481551527479"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES,
            true, // IS_INTERNAL
            1551527919, // NAVIGATION_START
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
            8, // HARDWARE_CONCURRENCY
            171, // SSL
            284, // TTFP
            454, // TTFP_TOTAL
            640, // FIRST_PAINT_TIME
            52, // SECURE_CONNECTION_START
            306, // UNLOAD_START
            308, // UNLOAD_END
            111, // REDIRECT_COUNT
            222, // REDIRECT_START
            333 // REDIRECT_END
        );
    }
}
