package ru.yandex.market.logshatter.parser.direct;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import org.junit.Test;

import java.text.SimpleDateFormat;

public class CampaignBalanceLogParserTest {
    private SimpleDateFormat dateFormat;

    @Test
    public void testParse() throws Exception {
        String line = "{\"log_time\":\"2019-03-06 00:00:26\",\"method\":\"BalanceClient.NotifyOrder2\"," +
            "            \"service\":\"direct.intapi\",\"ip\":\"2a02:6b8:c02:747:0:627:e5d0:6380\"," +
            "            \"reqid\":2869966445253696216,\"log_hostname\":\"sas2-0290-sas-ppc-java-intapi-13904.gencfg-c.yandex.net\"," +
            "            \"log_type\":\"campaign_balance\",\"data\":{\"cid\":15631250,\"type\":\"wallet\",\"currency\":\"RUB\"," +
            "            \"ClientID\":7979589,\"tid\":9233692923469,\"sum\":11184379.4369,\"sum_delta\":2192.400000,\"sum_balance\":0}}";

        LogParserChecker checker = new LogParserChecker(new CampaignBalanceLogParser());

        dateFormat = new SimpleDateFormat(CampaignBalanceLogParser.DATE_PATTERN);

        checker.check(line,
            dateFormat.parse("2019-03-06 00:00:26"),
            "direct.intapi",
            "BalanceClient.NotifyOrder2",
            "sas2-0290-sas-ppc-java-intapi-13904.gencfg-c.yandex.net",
            2869966445253696216L,
            "2a02:6b8:c02:747:0:627:e5d0:6380",
            "9233692923469",
            15631250L,
            7979589L,
            "wallet",
            "RUB",
            11184379436900L,
            2192400000L,
            0L
        );
    }
}
