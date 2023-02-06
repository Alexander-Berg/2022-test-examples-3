package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.events;

import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.logshatter.sanitizer.FakeSanitizer;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.EventType;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

public class EventsParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new EventsParser(new FakeSanitizer()));
    }

    @Test
    public void parseAllFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/search/?lr=39&clid=2242347&msid=1543764562.4732.122087.856243" +
            "&text=%D0%BF%D1%80%D0%BE%D0%BC%D0%B5%D1%82%D0%B5%D0%B9" +
            "@@robotness=1.0@@is_internal=1" +
            "@@cts=1543764674355" +
            "@@ruip=178.76.222.51@@url=//yandex.ru/" +
            "@@u=6042811641515572709" +
            "@@ref=orjY4mGPRjk5boDnW0uvlrrd71vZw9kpibIAK_uZ4bX2d0VUnsewRf_stGH-rdQtwS0S5EvsSnxDv4ypraMsYzBhHDc" +
            "_86aovcpsP_A6GOMb1PMknnHJs0KFJIRgtwG2ehrNoyvUE5-djQpgQPigXBjHh6qlVPExRayUO03E2CEEnoWT0ywmGg,," +
            "@@ver=14644@@at=3@@icookie=6042811641515572708" +
            "@@reqid=11111%2F1543764671160555-947891346418723336261425-man1-4034" +
            "@@slots=105520,0,79;103926,0,34;104582,0,35;63207,0,56;103849,0,55" +
            "@@vars=-env=prestable," +
            "-experiments=recommendations%3Aitem2item%3Bselection_events%3Arecommended%3Brubric_landing%3Adefault%3B" +
            "best_filter%3Aon%3Bconcert_recommended_selection%3Aoff%3Bactual_events_main%3Aon," +
            "-additional=%7B%22resource%22%3A%7B%22content_id%22%3A%22482bf91b3a2151fa8f11e763486fa0f4%22%2C%22" +
            "content_type%22%3A%22channel%22%7D%2C%22from%22%3A%22morda%22%2C%22from_block%22%3A%22" +
            "media-footer_stream_item%22%2C%22count%22%3A%2010%7D," +
            "-project=appmetrica%2Didea%2Dplugin,-platform=desktop,-version=0xrelease%2Fdesktop%2Fv1.157.0," +
            "-page=touch%3Aproduct," +
            "-service=zen%3Asite_desktop%3Ayandex%3Asite_desktop," +
            "-name=click," +
            "-referrer=https://yandex.com/search/text=%D0%BF%D1%80%D0%BE%D0%BC%D0%B5%D1%82%D0%B5%D0%B9," +
            "-yandexuid=8567101671562274770," +
            "-type=string,-value=click-to-projects," +
            "-ua=Mozilla%2F5.0%20(Macintosh%3B%20Intel%2" +
            "0Mac%20OS%20X%2010_12_6)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F68.0.3440.106%20Y" +
            "aBrowser%2F18.9.0.3363%20Yowser%2F2.5%20Safari%2F537.36,-ts=1543330234716" +
            "@@1543764674@@178.76.222.51,178.76.222.51@@6042811641515572709";

        checker.check(
            line,
            new Date(1543764674000L),
            "appmetrica-idea-plugin", // PROJECT
            "click", // NAME
            "zen:site_desktop:yandex:site_desktop", // SERVICE
            "touch:product", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://yandex.com/search/text=прометей", // URL
            UnsignedLong.valueOf("13322248798649617483"), // URL_ID
            "yandex.com", // VHOST
            Environment.PRESTABLE, // ENVIRONMENT
            Arrays.asList(63207, 103849, 103926, 104582, 105520), // TEST_IDS
            Arrays.asList(
                "recommendations:item2item",
                "selection_events:recommended",
                "rubric_landing:default",
                "best_filter:on",
                "concert_recommended_selection:off",
                "actual_events_main:on"
            ), // EXP_FLAGS
            "0xrelease/desktop/v1.157.0", // VERSION
            "11111/1543764671160555-947891346418723336261425-man1-4034", // REQUEST_ID
            UnsignedLong.valueOf("5560926446039861426"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("8567101671562274770"), // YANDEXUID
            Arrays.asList("resource", "from", "from_block", "count"), // KV_KEYS
            Arrays.asList("{\"content_id\":\"482bf91b3a2151fa8f11e763486fa0f4\",\"content_type\":\"channel\"}",
                "morda", "media-footer_stream_item", "10"), // KV_VALUES
            "::ffff:178.76.222.51", // IPv6
            EventType.STRING, // VALUE_TYPE
            "click-to-projects", // VALUE_STRING
            UnsignedLong.valueOf(0), // VALUE_INTEGER
            0d, // VALUE_FLOAT
            UnsignedLong.valueOf("1543330234716") // CLIENT_TIMESTAMP
        );
    }

    @Test
    public void parseInteger() throws Exception {
        String line = "@@vars=-type=integer,-value=2202906307356721367" + "@@1550683651@@2a00:1fa2:46a:9a12:650b:b1cf" +
            ":e907:afd0,94.29.81.173@@";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1550683651000L),
            "unknown-project", // PROJECT
            "unknown-name", // NAME
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // VERSION
            "", // REQUEST_ID
            hashOfEmptyString, // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            "2a00:1fa2:46a:9a12:650b:b1cf:e907:afd0", // IPv6
            EventType.INTEGER, // VALUE_TYPE
            "", // VALUE_STRING
            UnsignedLong.valueOf("2202906307356721367"), // VALUE_INTEGER
            0d, // VALUE_FLOAT
            UnsignedLong.valueOf(0) // CLIENT_TIMESTAMP
        );
    }

    @Test
    public void parseFloat() throws Exception {
        String line = "@@vars=-type=float,-value=999999.9999999" + "@@1550683651@@2a00:1fa2:46a:9a12:650b:b1cf:e907" +
            ":afd0,94.29.81.173@@";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1550683651000L),
            "unknown-project", // PROJECT
            "unknown-name", // NAME
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // VERSION
            "", // REQUEST_ID
            hashOfEmptyString, // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            "2a00:1fa2:46a:9a12:650b:b1cf:e907:afd0", // IPv6
            EventType.FLOAT, // VALUE_TYPE
            "", // VALUE_STRING
            UnsignedLong.valueOf(0), // VALUE_INTEGER
            999999.9999999d, // VALUE_FLOAT
            UnsignedLong.valueOf(0) // CLIENT_TIMESTAMP
        );
    }

    @Test
    public void parseMinimalFields() throws Exception {
        String line = "@@1550683651@@2a00:1fa2:46a:9a12:650b:b1cf:e907:afd0,94.29.81.173@@";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1550683651000L),
            "unknown-project", // PROJECT
            "unknown-name", // NAME
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // VERSION
            "", // REQUEST_ID
            hashOfEmptyString, // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            "2a00:1fa2:46a:9a12:650b:b1cf:e907:afd0", // IPv6
            EventType.STRING, // VALUE_TYPE
            "", // VALUE_STRING
            UnsignedLong.valueOf(0), // VALUE_INTEGER
            0d, // VALUE_FLOAT
            UnsignedLong.valueOf(0) // CLIENT_TIMESTAMP
        );
    }
}
