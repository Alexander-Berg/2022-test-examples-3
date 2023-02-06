package ru.yandex.market.logshatter.parser.pricelabs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;
import ru.yandex.market.logshatter.parser.trace.RequestType;

@RunWith(Parameterized.class)
public class PricelabsMessagesParserParameterizedTest {

    @Parameterized.Parameter
    public String resource;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"pricelabs_tms2_http_ga.json"},
                {"pricelabs_tms2_http_indexer.json"},
                {"pricelabs_tms2_http_mds.json"},
                {"pricelabs_tms2_http_partner_api.json"},
                {"pricelabs_tms2_http_report.json"},
                {"pricelabs_tms2_http_ym.json"}
        });
    }

    @Test
    public void testTsm2HttpOut() throws Exception {
        final String line = "tskv\tdate=2020-02-25T21:30:19.515+03:00\ttype=OUT\t" +
                "source_module=market_pricelabs_tms2\ttarget_host=api.partner.market.fslb.yandex.ru\t" +
                "http_method=GET\trequest_method=/v2/campaigns/21112527/balance\ttime_millis=15\thttp_code=200\t" +
                "kv.activeTime=15\tkv.waitingTime=3\tkv.taskId=7384449\tkv.inputRows=4\tkv.outputRows=5\t" +
                "request_id=1582655419500/4ffcc29949c9d2fd34c914ae6a9f0500\tkv.interim=1\tkv.jobId=3665\tkv.jobType=4";

        final LogParserChecker checker = PricelabsMessagesParserTest.checker(resource);
        checker.check(line,
                new Date(1582655419000L),
                1582655419500L,
                "4ffcc29949c9d2fd34c914ae6a9f0500",
                new Integer[0],
                1582655419500L,
                1582655419515L,
                15,
                RequestType.OUT,
                "",
                "hostname.test",
                "market_pricelabs_tms2",
                "hostname.test",
                "",
                "api.partner.market.fslb.yandex.ru",
                Environment.UNKNOWN,
                "/v2/campaigns/21112527/balance",
                200,
                1,
                "",
                "",
                "GET",
                "",
                "",
                "",
                new Object[0],
                new Object[0],
                new Object[0],
                new Object[0],
                new Integer[0],
                "",
                "",
                -1,
                7384449L,
                3665L,
                4,
                15L,
                3L,
                4,
                5,
                "",
                (byte) 1);
    }

}
