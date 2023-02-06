package ru.yandex.market.logshatter.parser.lom;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static ru.yandex.market.logshatter.parser.trace.Environment.PRODUCTION;

public class BacklogTskvLogParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new BacklogTskvLogParser());
        checker.setOrigin("market-health-stable");
        checker.setFile("/var/log/yandex/logistics-lom/market_logistics_lom-backlog-tskv.log");
        checker.setParam("logbroker://market-health-stable", PRODUCTION.toString());
    }

    @Test
    public void parseWarn() throws Exception {
        String line = "tskv\tts=2019-09-10T12:02:41.143+03:00\tlevel=WARN\tformat=plain\tcode=\tpayload=KEEEEEEEK";
        checker.check(
            line,
            new Date(1568106161000L), "market_logistics_lom", PRODUCTION, checker.getHost(),
            BacklogTskvLogParser.Level.WARN, "plain", "", "KEEEEEEEK", "", new String[0], new String[0],
            new String[0], new String[0], new String[0], ""
        );
    }

    @Test
    public void parseError() throws Exception {
        String line = "tskv\tts=2019-09-10T12:11:11.058+03:00\tlevel=ERROR\tformat=json-exception\tcode=" +
            "java.lang.IllegalArgumentException\tpayload={\"message\": \"big error\"}\t" +
            "request_id=testId\tentity_types=order\tentity_values=order:1,order:3,order:4\t" +
            "extra_keys=extraKey1,extraKey2\textra_values=extraValue1,extraValue2\tvcs_id=12345";
        checker.check(
            line,
            new Date(1568106671000L), "market_logistics_lom", PRODUCTION, checker.getHost(),
            BacklogTskvLogParser.Level.ERROR, "json-exception", "java.lang.IllegalArgumentException",
            "{\"message\": \"big error\"}", "testId", new String[0], new String[]{"order"},
            new String[]{"order:1", "order:3", "order:4"}, new String[]{"extraKey1", "extraKey2"},
            new String[]{"extraValue1", "extraValue2"}, "12345"
        );
    }
}
