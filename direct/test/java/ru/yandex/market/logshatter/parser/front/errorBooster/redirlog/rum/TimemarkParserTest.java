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

public class TimemarkParserTest {
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
        checker = new LogParserChecker(new TimemarkParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ua/@@reqid=1551594539.97231.139792.877100@@is_robot=1@@is_internal=1@@path=690.2096.207@@slots=63208,0,92@@vars=143=28.15.899,287=143,1961=0,1964=0,1965=1,-project=morda,-service=zen,-page=plain,-platform=desktop,-experiments=aaa=1;bb=7;ccc=yes;ddd,-env=production,-version=2.2026,-blocker=,1042=Mozilla%2F5.0%20(Windows%20NT%206.1%3B%20WOW64)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F71.0.3578.98%20Safari%2F537.36%20OPR%2F58.0.3135.79,143.2129=1551594537209,143.2112=2533,143.2119=3238,1701=2793,207=10022.5,2924=28.15.1604,2925=1,2796.2797=s-55-59.s-77-87.soa-10-28.sod-1010-1027.so-210-226.coa-50-65.cod-21-34.cou-54-66.mc-123-134.u-100-110,689.2322=6101.7@@cts=1551594547233@@at=1@@uah=2969118508@@icookie=1640265471530517429@@x-req-id=1551594547134646-9788025603629138857@@url=@@1551594547@@77.111.244.107,77.111.244.107@@1640265471530517429";

        checker.check(
            line,
            new Date(1551594547000L),
            "morda", // PROJECT
            "zen", // SERVICE
            "plain", // PAGE
            Platform.DESKTOP, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "yandex.ua", //VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(63208), // TEST_IDS
            Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), // EXP_FLAGS
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
            143, // REGION
            "1551594539.97231.139792.877100", // REQUEST_ID
            UnsignedLong.valueOf("3150600538512313225"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("1640265471530517429"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            true, // IS_INTERNAL
            "first-paint", // NAME
            3921, // TIME
            1, //SUBPAGE_INDEX
            "ru.morda.zen", // SUBPAGE_NAME
            126, // LONG_TASK_ALL
            14, // LONG_TASK_SELF
            10, // LONG_TASK_UNKNOWN
            11, // LONG_TASK_MULTIPLE_CONTEXTS
            12, // LONG_TASK_CROSS_ORIGIN_UNREACHABLE
            13, // LONG_TASK_CROSS_ORIGIN_DESCENDANT
            15, // LONG_TASK_CROSS_ORIGIN_ANCESTOR
            16, // LONG_TASK_SAME_ORIGIN
            17, // LONG_TASK_SAME_ORIGIN_DESCENDANT
            18 // LONG_TASK_SAME_ORIGIN_ANCESTOR
        );
    }
}
