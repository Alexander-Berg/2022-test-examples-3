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
 * @date 23/04/2020
 */
public class BalancerTraceLogParserTest {

    private static final String EXPECTED_QUERY_PARAMETERS_WITHOUT_MASK = "/v2.1.0/models/385696251/offers?req_id" +
        "=k9bpubgjayb27lexhqf064ongamx52r5%2F16&geo_id=101754&clid=2270459" +
        "&count=1&fields=OFFER_PHOTO%2COFFER_SHOP%2COFFER_DELIVERY%2COFFER_OUTLET%2COFFER_OUTLET_COUNT" +
        "%2CSHOP_RATING%2COFFER_DISCOUNT%2COFFER_CATEGORY%2COFFER_GIFT%2COFFER_VENDOR%2COFFER_ACTIVE_FILTERS" +
        "&-1=1~&groupBy=SHOP&report_show-preorder=1&with_model=0&source_base=wildberries.ru&pp=482" +
        "&secret=" + SnooperSanitizerTest.STRING_WITH_RAW_SECRET;

    private static final String EXPECTED_QUERY_PARAMETERS_WITH_MASK = "/v2.1.0/models/385696251/offers?req_id" +
        "=k9bpubgjayb27lexhqf064ongamx52r5%2F16&geo_id=101754&clid=2270459" +
        "&count=1&fields=OFFER_PHOTO%2COFFER_SHOP%2COFFER_DELIVERY%2COFFER_OUTLET%2COFFER_OUTLET_COUNT" +
        "%2CSHOP_RATING%2COFFER_DISCOUNT%2COFFER_CATEGORY%2COFFER_GIFT%2COFFER_VENDOR%2COFFER_ACTIVE_FILTERS" +
        "&-1=1~&groupBy=SHOP&report_show-preorder=1&with_model=0&source_base=wildberries.ru&pp=482" +
        "&secret=" + SnooperSanitizerTest.STRING_WITH_MASKED_SECRET;

    private LogParserChecker checker;

    @Before
    public void setup() {
        checker = new LogParserChecker(new BalancerTraceLogParser());
    }

    public void test(String expectedQueryParameters) throws Exception {
        final URL resource = getClass().getClassLoader().getResource("market-slb-balancer-json.log");
        final String line = FileUtils.readLines(new File(resource.toURI()), StandardCharsets.UTF_8).get(1);
        checker.check(
            line,
            new Date(1587582857400L),
            1587582857400L,
            "322e83b37a0f9438b55144f0e5a30500",
            new Integer[]{},
            1587582857346L,
            1587582857400L,
            54L,
            RequestType.PROXY,
            "balancer",
            "hostname.test",
            "",
            "2a02:6b8:c08:ce97:10c:b3b6:0:3020",
            "",
            "[fdee:fdee::1:216]:80",
            Environment.UNKNOWN,
            "",
            200,
            1,
            "",
            "http",
            "GET",
            expectedQueryParameters,
            "",
            "",
            BalancerTraceLogParser.KV_KEYS,
            new String[]{"2", "", "api.content.market.yandex.ru", "", "0", "54.389", "54.181", "content_api_haproxy",
                "succ 200", "got/9.6.0 (https://github.com/sindresorhus/got)", "GoogleBot", "2a02:6b8::1:216", "443",
                "188.191.126.203", "2a02:6b8:c08:ce97:10c:b3b6:0:3020",
                "771,4866-4867-4865-49199-49195-49200-49196-158-49191-103-49192" +
                "-107-163-159-52393-52392-52394-49327-49325-49315-49311-49245-49249-49239-49235-162-49326-49324-49314" +
                "-49310-49244-49248-49238-49234-49188-106-49187-64-49162-49172-57-56-49161-49171-51-50-157-49313" +
                "-49309-49233-156-49312-49308-49232-61-60-53-47-255,0-11-10-35-22-23-13-43-45-51-41,29-23-30-25-24," +
                "0-1-2", "1027-2052-1025-1283-2053-1281-2054-1537-513,,772-771-770-769,h2-http/1.1,29,1"},
            new Object[]{},
            new Object[]{},
            new Object[]{},
            "",
            "",
            8201L
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
