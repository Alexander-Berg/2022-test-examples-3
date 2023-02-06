package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import java.util.Arrays;
import java.util.Date;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimemarkParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new TimemarkParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ua/@@reqid=1551594539.97231.139792.877100@@ruip=217.118.91" +
            ".74@@is_robot=1@@is_internal=1@@path=690.2096.207@@slots=63208,0,92@@vars=143=28.15.899,287=143,1961=0," +
            "1964=0,1965=1,-loggedin=true,-project=morda,-service=zen,-page=plain,-platform=desktop," +
            "-additional=%7B%22resource%22%3A%7B%22content_id%22%3A%22482bf91b3a2151fa8f11e763486fa0f4%22%2C" +
            "%22content_type%22%3A%22channel%22%7D%2C%22from%22%3A%22morda%22%2C%22from_block%22%3A%22media" +
            "-footer_stream_item%22%2C%22count%22%3A%2010%7D,-experiments=aaa=1;bb=7;ccc=yes;ddd,-env=production," +
            "-version=2.2026,-blocker=,1042=Mozilla%2F5.0%20(Windows%20NT%206.1%3B%20WOW64)%20AppleWebKit%2F537.36%20" +
            "(KHTML%2C%20like%20Gecko)%20Chrome%2F71.0.3578.98%20Safari%2F537.36%20OPR%2F58.0.3135.79,143" +
            ".2129=1551594537209,143.2112=2533,1088=1,3105=1,143.2119=3238,1701=2793,207=10022.5,2924=28.15.1604," +
            "2925=1,2796.2797=s-55-59.s-77-87.soa-10-28.sod-1010-1027.so-210-226.coa-50-65.cod-21-34.cou-54-66" +
            ".mc-123-134.u-100-110,689.2322=6101" +
            ".7@@cts=1551594547233@@at=1@@uah=2969118508@@icookie=1640265471530517429@@x-req-id=1551594547134646" +
            "-9788025603629138857@@url=@@1551594547@@77.111.244.107,77.111.244.107@@1640265471530517429";

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
            Arrays.asList("resource", "from", "from_block", "count"), // KV_KEYS
            Arrays.asList("{\"content_id\":\"482bf91b3a2151fa8f11e763486fa0f4\",\"content_type\":\"channel\"}",
                "morda", "media-footer_stream_item", "10"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:217.118.91.74", // IPv6
            true, // LOGGED_IN
            "first-paint", // NAME
            3921L, // TIME
            1, //SUBPAGE_INDEX
            "ru.morda.zen", // SUBPAGE_NAME
            6101L, // SUBPAGE_START_TIME
            126, // LONG_TASK_ALL
            14, // LONG_TASK_SELF
            10, // LONG_TASK_UNKNOWN
            11, // LONG_TASK_MULTIPLE_CONTEXTS
            12, // LONG_TASK_CROSS_ORIGIN_UNREACHABLE
            13, // LONG_TASK_CROSS_ORIGIN_DESCENDANT
            15, // LONG_TASK_CROSS_ORIGIN_ANCESTOR
            16, // LONG_TASK_SAME_ORIGIN
            17, // LONG_TASK_SAME_ORIGIN_DESCENDANT
            18, // LONG_TASK_SAME_ORIGIN_ANCESTOR
            true, // IS_PREFETCH
            true, // IS_PRERENDER
            UnsignedLong.valueOf(1551594537209L) // NAVIGATION_START
        );
    }

    @Test
    public void parseWithoutSubpageFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ua/@@reqid=1551594539.97231.139792.877100@@ruip=217.118.91" +
            ".74@@is_robot=1@@is_internal=1@@path=690.2096.207@@slots=63208,0,92@@vars=143=28.15.899,287=143,1961=0," +
            "1964=0,1965=1,-project=appmetrica%2Didea%2Dplugin,-service=zen,-page=plain,-platform=desktop," +
            "-additional=%7B%22resource%22%3A%7B%22content_id%22%3A%22482bf91b3a2151fa8f11e763486fa0f4%22%2C" +
            "%22content_type%22%3A%22channel%22%7D%2C%22from%22%3A%22morda%22%2C%22from_block%22%3A%22media" +
            "-footer_stream_item%22%2C%22count%22%3A%2010%7D,-experiments=aaa=1;bb=7;ccc=yes;ddd,-env=production," +
            "-version=2.2026,-blocker=,1042=Mozilla%2F5.0%20(Windows%20NT%206.1%3B%20WOW64)%20AppleWebKit%2F537.36%20" +
            "(KHTML%2C%20like%20Gecko)%20Chrome%2F71.0.3578.98%20Safari%2F537.36%20OPR%2F58.0.3135.79,143" +
            ".2129=1551594537209,143.2112=2533,1088=1,3105=1,143.2119=3238,1701=2793,207=10022.5,2796.2797=s-55-59" +
            ".s-77-87.soa-10-28.sod-1010-1027.so-210-226.coa-50-65.cod-21-34.cou-54-66.mc-123-134.u-100-110,689" +
            ".2322=6101.7@@cts=1551594547233@@at=1@@uah=2969118508@@icookie=1640265471530517429@@x-req-id" +
            "=1551594547134646-9788025603629138857@@url=@@1551594547@@77.111.244.107,77.111.244" +
            ".107@@1640265471530517429";

        checker.check(
            line,
            new Date(1551594547000L),
            "appmetrica-idea-plugin", // PROJECT
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
            Arrays.asList("resource", "from", "from_block", "count"), // KV_KEYS
            Arrays.asList("{\"content_id\":\"482bf91b3a2151fa8f11e763486fa0f4\",\"content_type\":\"channel\"}",
                "morda", "media-footer_stream_item", "10"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:217.118.91.74", // IPv6
            false, // LOGGED_IN
            "first-paint", // NAME
            10023L, // TIME
            0, //SUBPAGE_INDEX
            "", // SUBPAGE_NAME
            6101L, // SUBPAGE_START_TIME
            126, // LONG_TASK_ALL
            14, // LONG_TASK_SELF
            10, // LONG_TASK_UNKNOWN
            11, // LONG_TASK_MULTIPLE_CONTEXTS
            12, // LONG_TASK_CROSS_ORIGIN_UNREACHABLE
            13, // LONG_TASK_CROSS_ORIGIN_DESCENDANT
            15, // LONG_TASK_CROSS_ORIGIN_ANCESTOR
            16, // LONG_TASK_SAME_ORIGIN
            17, // LONG_TASK_SAME_ORIGIN_DESCENDANT
            18, // LONG_TASK_SAME_ORIGIN_ANCESTOR
            true, // IS_PREFETCH
            true, // IS_PRERENDER
            UnsignedLong.valueOf(1551594537209L) // NAVIGATION_START
        );
    }

    @Test
    public void skipEmptyProjectFields() throws Exception {
        String line = "HTTP_REFERER=@@path=690.2096.207" +
            "@@vars=," +
            "@@1550683651@@94.29.81.173,94.29.81.173@@";

        checker.checkEmpty(line);
    }

    @Test
    public void parseTime() {
        assertEquals(Long.valueOf(0), TimemarkContainer.parseTime(ImmutableMap.of("time", "0", "action.start_time",
            "0"), ""));
        assertEquals(Long.valueOf(61535), TimemarkContainer.parseTime(ImmutableMap.of("time", "61535.1", "action" +
            ".start_time", "0"), ""));
        assertEquals(Long.valueOf(61536), TimemarkContainer.parseTime(ImmutableMap.of("time", "61535.5", "action" +
            ".start_time", "0"), ""));
        assertEquals(Long.valueOf(61536), TimemarkContainer.parseTime(ImmutableMap.of("time", "61535.7", "action" +
            ".start_time", "0"), ""));
        assertEquals(Long.valueOf(6610023), TimemarkContainer.parseTime(ImmutableMap.of("time", "6610022.7", "action" +
            ".start_time", "0"), ""));
        assertEquals(Long.valueOf(61536), TimemarkContainer.parseTime(ImmutableMap.of("time", "61535.7", "action" +
            ".start_time", "1000"), ""));
        assertEquals(Long.valueOf(3922), TimemarkContainer.parseTime(ImmutableMap.of("time", "6610022.9", "action" +
            ".start_time", "6606101.01"), "page"));
        assertEquals(Long.valueOf(4194967296L), TimemarkContainer.parseTime(ImmutableMap.of("time", "4201573396.9",
            "action.start_time", "6606101.01"), "page"));
        assertEquals(Long.valueOf(0), TimemarkContainer.parseTime(ImmutableMap.of("time", "10.7",
            "action.start_time", "20.1"), "page"));
    }
}
