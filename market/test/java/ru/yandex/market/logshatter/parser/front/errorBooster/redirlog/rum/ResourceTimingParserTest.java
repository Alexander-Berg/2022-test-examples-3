package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

public class ResourceTimingParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new ResourceTimingParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/@@reqid=1577701396335959-493751078308489617600122-vla1-3571" +
            "@@ver=15279@@slots=188118,0,19;194836,0,67;118267,0,25;187314,0,25;175664,0,25;196521,0,25;193029,0,14;" +
            "200349,0,22;200838,0,79;201034,0,91;201312,0,86;85058,0,49;27935,0,76;201654,0,59;40254,0,37;194463,0," +
            "42;201219,0,81;200010,0,34;194454,0,63;201354,0,92;201077,0,33;201044,0,59;199975,0,78;201208,0,18;" +
            "200063,0,72;200184,0,12;197071,0,72;188861,0,-1;186872,0,-1;174679,0," +
            "-1@@ruip=2a00:1fa0:8613:4eb9:408a:1e2a:5481:d4f9@@u=9361862261424982234@@reg=10928@@dtype=iweb@@reqid" +
            "=1577701396335959-493751078308489617600122-vla1-3571@@path=690.2096.2044@@vars=143=28.2719.584," +
            "287=10928,-project=turbo,-service=publishers,-page=www.rbc.ru,-env=production,-platform=touch," +
            "-version=v0.245.3,-iframe=1,1042=Mozilla%2F5.0%20" +
            "(iPhone%3B%20CPU%20iPhone%20OS%2013_3%20like%20Mac%20OS%20X)%20AppleWebKit%2F605.1.15%20" +
            "(KHTML%2C%20like%20Gecko)%20Version%2F13.0%20YaBrowser%2F19.12.2.263.10%20Mobile%2F15E148%20Safari%2F604" +
            ".1,143.2129=1577701396310,143.2112=0,143.2119=611,1701=2719.3042,13=https%3A%2F%2Fyandex" +
            ".com%2Fturbo%2Favatars%2Fget-turbo%2F2005407%2Frthf2caa3140cd6c635e066142eed4113da" +
            "%2Fmax_g360_c12_r4x3_pd10,2116=199199,2114=199199,2113=199199,2112=199199,2136=399,2888=resource," +
            "2111=199199,2889=link,2886=13793,2887=13794,2890=h2,2110=0,2109=0,2117=199199,2120=1160,2119=870,2115=0," +
            "2322=199199,2323=57057,2137=0@@cts=1577701397476@@at=3@@uah=1571864900@@icookie=9361862261424982234@@x" +
            "-req-id=1577701397534701-4988591940771554746@@robotness=0.0@@user_agent=Mozilla/5.0 (iPhone; CPU iPhone " +
            "OS 13_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0 YaBrowser/19.12.2.263.10 " +
            "Mobile/15E148 Safari/604.1@@url=//yandex.ru/@@1577701397@@2a00:1fa0:8613:4eb9:408a:1e2a:5481:d4f9," +
            "2a00:1fa0:8613:4eb9:408a:1e2a:5481:d4f9@@9361862261424982234";

        checker.check(
            line,
            new Date(1577701397000L),
            "turbo", // PROJECT
            "publishers", // SERVICE
            "www.rbc.ru", // PAGE
            Platform.TOUCH, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "yandex.ru", //VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(27935, 40254, 85058, 118267, 174679, 175664, 186872, 187314, 188118, 188861, 193029,
                194454, 194463, 194836, 196521, 197071, 199975, 200010, 200063, 200184, 200349, 200838, 201034, 201044,
                201077, 201208, 201219, 201312, 201354, 201654), // TEST_IDS
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
            "v0.245.3", // VERSION
            10928, // REGION
            "1577701396335959-493751078308489617600122-vla1-3571", // REQUEST_ID
            UnsignedLong.valueOf("12898457985353648684"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("9361862261424982234"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES,
            false, // IS_INTERNAL
            "", // CDN
            "2a00:1fa0:8613:4eb9:408a:1e2a:5481:d4f9", // IPv6
            false, // LOGGED_IN
            "turbo.resource", // ID
            "https://yandex.com/turbo/avatars/get-turbo/2005407/rthf2caa3140cd6c635e066142eed4113da" +
                "/max_g360_c12_r4x3_pd10", // RESOURCE_URL
            199199L, // CONNECT_START
            199199L, // CONNECT_END
            199199L, // DOMAIN_LOOKUP_START
            199199L, // DOMAIN_LOOKUP_END
            0L, // REDIRECT_START
            0L, // REDIRECT_END
            870L, // RESPONSE_START
            1160L, // RESPONSE_END
            0L, // SECURE_CONNECTION_START
            199199L, // REQUEST_START
            199199L, // FETCH_START
            399L, // DURATION
            0L, // WORKER_START
            199199L, // START_TIME
            57057L, // TRANSFER_SIZE
            13794L, // ENCODED_BODY_SIZE
            13793L, // DECODED_BODY_SIZE
            "link", // INITIATOR_TYPE
            "h2", // NEXT_HOP_PROTOCOL
            UnsignedLong.valueOf(1577701396310L) // NAVIGATION_START
        );
    }
}
