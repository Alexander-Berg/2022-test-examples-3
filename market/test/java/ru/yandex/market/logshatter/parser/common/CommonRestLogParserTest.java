package ru.yandex.market.logshatter.parser.common;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author a-danilov
 */
public class CommonRestLogParserTest {
    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CommonRestLogParser());

        checker.check("[18/Apr/2016:09:35:38 +0300]\t1460961338\t93.158.141" +
                ".32\tPOST\t/arbitrage/conversations/1/send/1234.xml\t200\t185\t0\t1",
            new Date(1460961338000L), checker.getHost(), "/arbitrage/conversations/1/send/1234.xml", "POST", 200, 185,
            "/arbitrage/conversations/*/send/*", 0L, true);

        checker.check("[18/Apr/2016:09:35:38 +0300]\t1460961338\t93.158.141.32\tPOST\t/arbitrage/conversations/1,2," +
                "3/send.json\t200\t185\t0\t1",
            new Date(1460961338000L), checker.getHost(), "/arbitrage/conversations/1,2,3/send.json", "POST", 200, 185,
            "/arbitrage/conversations/*/send", 0L, true);

        checker.check("[19/Apr/2016:13:14:38 +0300]\t1461060878\tfdef:0:0:0:0:0:0:1\tGET\t/arbitrage/conversations" +
                "/ping\t200\t5\t0\t1",
            new Date(1461060878000L), checker.getHost(), "/arbitrage/conversations/ping", "GET", 200, 5,
            "/arbitrage/conversations/ping", 0L, true);

        checker.check("[19/Apr/2016:13:48:18 +0300]\t1461062898\t217.69.133.11\t-\tsessionToUID\t200\t5\t161658075\t0",
            new Date(1461062898000L), checker.getHost(), "sessionToUID", "-", 200, 5,
            "sessionToUID", 161658075L, false);

        checker.check("[18/Apr/2016:09:35:38 +0300]\t1460961338\t93.158.141" +
                ".25\tPATCH\t/cart/UUID/5a7ae178fcde47e8cb071415db925901/list\t200\t10\t0\t1",
            new Date(1460961338000L), checker.getHost(), "/cart/UUID/5a7ae178fcde47e8cb071415db925901/list", "PATCH",
            200, 10,
            "/cart/UUID/*/list", 0L, true);

        checker.check("[03/Oct/2016:06:45:06 +0300]\t1475466306\t2a02:6b8:0:c01:225:90ff:fe94:9eae\tPOST\t/history" +
                "/YANDEXUID/767089341475411105\t200\t0\t0\t1",
            new Date(1475466306000L), checker.getHost(), "/history/YANDEXUID/767089341475411105", "POST", 200, 0,
            "/history/YANDEXUID/*", 0L, true);
    }
}
