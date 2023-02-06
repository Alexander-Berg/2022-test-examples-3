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

public class ExternalResourcesParserTest {
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
        checker = new LogParserChecker(new ExternalResourcesParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/@@reqid=1552806103.32814.140979.770317@@is_robot=1@@is_internal=1@@path=690.2096.2748@@slots=118156,0,97;129002,0,40;63208,0,39;125045,0,14;125302,0,1;127721,0,75;130175,0,58;117970,0,1@@vars=143=28.15.899,287=63,1961=0,1964=1,1965=0,-project=morda,-service=zen,-page=plain,-platform=desktop,-env=production,-version=2.2044,-blocker=,1042=Mozilla%2F5.0%20(Windows%20NT%205.1)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F57.0.2987.137%20YaBrowser%2F17.4.1.919%20Yowser%2F2.5%20Safari%2F537.36,2748=yastatic.net!24!;mail.yandex.ru!1!;avatars.mds.yandex.net!10!18599;mc.yandex.ru!2!;cloud-api.yandex.ru!1!;yabs.yandex.ru!1!;www.tns-counter.ru!1!;awaps.yandex.net!2!;@@cts=1552806108521@@at=1@@uah=2535267517@@icookie=2756006631512377711@@x-req-id=1552806105059099-13807932647028289740@@url=@@1552806105@@134.19.129.142,134.19.129.142@@2756006631512377711";

        checker.check(
            line,
            new Date(1552806105000L),
            "morda", // PROJECT
            "zen", // SERVICE
            "plain", // PAGE
            Platform.DESKTOP, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "yandex.ru", //VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(63208, 117970, 118156, 125045, 125302, 127721, 129002, 130175), // TEST_IDS
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
            "2.2044", // VERSION
            63, // REGION
            "1552806103.32814.140979.770317", // REQUEST_ID
            UnsignedLong.valueOf("4068596562764096646"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("2756006631512377711"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES,
            true, // IS_INTERNAL
            Arrays.asList("yastatic.net", "mail.yandex.ru", "avatars.mds.yandex.net", "mc.yandex.ru", "cloud-api.yandex.ru", "yabs.yandex.ru", "www.tns-counter.ru", "awaps.yandex.net"), // COLUMN_RESOURCES_HOST
            Arrays.asList(24, 1, 10, 2, 1, 1, 1, 2), // COLUMN_RESOURCES_COUNT
            Arrays.asList(0, 0, 18599, 0, 0, 0, 0, 0) // COLUMN_RESOURCES_SIZE
        );

        String line2 = "HTTP_REFERER=https://yandex.com.tr/@@reqid=1559062132.69407.141036.2358@@path=690.2096.2748@@slots=@@cts=1559062132853@@at=1@@uah=137061989@@icookie=6783198371558778450@@x-req-id=1559062133652365-14330438527887915439@@url=@@1559062133@@88.226.15.133,88.226.15.133@@6783198371558778450";

        checker.checkEmpty(line2);
    }
}
