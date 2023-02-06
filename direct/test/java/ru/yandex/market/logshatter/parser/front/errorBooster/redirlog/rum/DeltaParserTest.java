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

public class DeltaParserTest {
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
        checker = new LogParserChecker(new DeltaParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/@@reqid=11111%2F1551522448.78149.140360.260413@@is_robot=1@@is_internal=1@@path=690.2096.2877@@slots=63207,0,60;125307,0,30;121749,0,53;125042,0,90;108758,0,25@@vars=143=28.15.899,287=43,1961=0,1964=1,1965=0,-project=morda,-service=zen,-page=touch%3Aproduct,-platform=desktop,-env=production,-version=2.2026,-blocker=,1042=Mozilla%2F5.0%20(Windows%20NT%205.1)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F49.0.2623.112%20Safari%2F537.36,143.2129=1551522511013,143.2112=190,143.2119=592,1701=628,207.2154=3221,207.1428=3887,2877=666,2924=28.15.1604,2925=1,689.2322=1572.995@@cts=1551522514906@@at=1@@uah=763090462@@icookie=2887475811551512597@@x-req-id=1551522453064543-6932803949893231700@@url=@@1551522453@@178.46.101.118,178.46.101.118@@2887475811551512597";

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
            "js", // NAME
            666, // DELTA
            1, //SUBPAGE_INDEX
            "ru.morda.zen" ////SUBPAGE_NAME
        );
    }
}
