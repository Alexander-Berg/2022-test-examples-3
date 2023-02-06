package ru.yandex.market.logshatter.parser.trace;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.health.configs.logshatter.sanitizer.SnooperSanitizerTest;
import ru.yandex.market.logshatter.parser.LogParserChecker;


/**
 * @author Maksim Kupriianov <a href="mailto:maxk@yandex-team.ru"></a>
 * @date 24/03/2020
 */
public class HaproxyTraceLogParserTest {

    private static final String EXPECTED_QUERY_PARAMETERS_WITHOUT_MASK = "/favicon.ico?secret=" +
        SnooperSanitizerTest.STRING_WITH_RAW_SECRET;
    private static final String EXPECTED_QUERY_PARAMETERS_WITH_MASK = "/favicon.ico?secret=" +
        SnooperSanitizerTest.STRING_WITH_MASKED_SECRET;

    LogParserChecker checker;

    @Before
    public void setup() {
        checker = new LogParserChecker(new HaproxyTraceLogParser());
    }

    public void test(String expectedQueryParameters) throws Exception {
        final URL resource = getClass().getClassLoader().getResource("market-slb-haproxy-json.log");
        final String line = FileUtils.readLines(new File(resource.toURI()), StandardCharsets.UTF_8).get(1);
        checker.check(
            line,
            new Date(1585040693647L),
            1585040693647L,
            "f0b118abe18722a704d77f0b96a10500",
            new Integer[]{},
            1585040693650L,
            1585040693704L,
            53L,
            RequestType.PROXY,
            "haproxy",
            "fslb01ht.market.yandex.net",
            "",
            "fdee:fdee:0:3400:0:3c9:0:198",
            "",
            "sas2-1268-948-sas-market-test--1e6-28000.gencfg-c.yandex.net:28000",
            Environment.UNKNOWN,
            "",
            302,
            1,
            "",
            "http",
            "GET",
            expectedQueryParameters,
            "",
            "",
            HaproxyTraceLogParser.KV_KEYS,
            new String[]{"6685", "51", "0", "0", "1", "2", "----", "2a02:6b8:c02:5b7:0:663:5caf:9394",
                "2a02:6b8:c02:5b7:0:663:5caf:9394"},
            new Object[]{},
            new Object[]{},
            new Object[]{},
            "",
            "",
            493L
        );
    }

    @Test
    public void testWithoutMask() throws Exception {
        checker.setParam("sanitizer", "false");
        test(EXPECTED_QUERY_PARAMETERS_WITHOUT_MASK);
    }

    @Test
    public void testWithMask() throws Exception {
        test(EXPECTED_QUERY_PARAMETERS_WITH_MASK);
    }
}
