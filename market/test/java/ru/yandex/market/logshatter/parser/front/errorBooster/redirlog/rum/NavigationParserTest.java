package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

public class NavigationParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new NavigationParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://mail.yandex.ru/touch/folder/1?skip-app-promo=1@@reqid=TOUCH-72915703" +
            "-1577719527300@@path=690.2096.2892@@slots=187392,0,97;175496,0,47;195597,0,69;193822,0,56;152837,0,70;" +
            "166663,0,13;156241,0,15;194756,0,66@@vars=143=28.176.584.711,-project=quinn,-platform=touch,-version=10" +
            ".11.6,2116=301,2114=204.8,2886=25560,2124=3588.5,2131=2608.4,2123=2607.4,2770=2607.3,2113=204.8,2112=98" +
            ".4,2136=3611.9,2887=11060,2888=navigation,2111=20.6,2889=navigation,2126=3611.9,2125=3588.6," +
            "2890=http%2F1.1,1385=0,2110=0,2109=0,2117=301.2,2120=391.7,2119=387.5,2115=218.7,2322=0,2323=13140," +
            "76=navigate,2128=0,2127=0,2137=0,2437=2067,2439=Infinity," +
            "2870=4g@@cts=1577719529355@@at=1@@uah=20404118@@icookie=6209958380753528685@@x-req-id=1577719530810119" +
            "-1695466549895113092@@robotness=0.0@@user_agent=Mozilla/5.0 (Linux; Android 8.1.0; Redmi 5 Plus " +
            "Build/OPM1.171019.019; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/67.0.3396.87 Mobile" +
            " Safari/537.36 YandexSearch/7.45 YandexSearchBrowser/7.45@@url=@@1577719530@@83.97.109.60,83.97.109" +
            ".60@@4523641621575617639";

        checker.check(
            line,
            new Date(1577719530000L),
            "quinn", // PROJECT
            "", // SERVICE
            "28.176.584.711", // PAGE
            Platform.TOUCH, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "mail.yandex.ru", //VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(152837, 156241, 166663, 175496, 187392, 193822, 194756, 195597), // TEST_IDS
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
            "10.11.6", // VERSION
            0, // REGION
            "TOUCH-72915703-1577719527300", // REQUEST_ID
            UnsignedLong.valueOf("5464317202171191049"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("4523641621575617639"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES,
            false, // IS_INTERNAL
            "", // CDN
            "::ffff:83.97.109.60", // IPv6
            false, // LOGGED_IN
            204, // CONNECT_START
            301, // CONNECT_END
            98, // DOMAIN_LOOKUP_START
            204, // DOMAIN_LOOKUP_END
            0, // REDIRECT_START
            0, // REDIRECT_END
            387, // RESPONSE_START
            391, // RESPONSE_END
            218, // SECURE_CONNECTION_START
            301, // REQUEST_START
            20, // FETCH_START
            3611, // DURATION
            0, // WORKER_START
            0, // START_TIME
            13140, // TRANSFER_SIZE
            11060, // ENCODED_BODY_SIZE
            25560, // DECODED_BODY_SIZE
            "http/1.1", // NEXT_HOP_PROTOCOL
            0, // UNLOAD_START
            0, // UNLOAD_END
            2607, // DOM_INTERACTIVE_TOTAL
            2607, // DOM_CONTENT_LOADED
            2608, // DOM_CONTENT_LOADED_FINISHED
            ConnectionType.WIFI, // CONNECTION_TYPE
            3588, // DOM_COMPLETE
            3588, // LOAD_EVENT_START
            3611 // LOAD_EVENT_END
        );
    }
}
