package ru.yandex.market.logshatter.parser.common;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author kukabara
 */
public class ObsoleteCommonRestLogParserTest {
    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new ObsoleteCommonRestLogParser());

        checker.check("[18/Apr/2016:09:35:38 +0300]\t1460961338\t93.158.141.32\tPOST\t/arbitrage/conversations/1/send/1234.xml\t200\t185\t0\t1",
            new Date(1460961338000L), checker.getHost(), "/arbitrage/conversations/*/send/*", "POST", 200, 185,
            "/arbitrage/conversations/1/send/1234.xml", 0L, true);

        checker.check("[18/Apr/2016:09:35:38 +0300]\t1460961338\t93.158.141.32\tPOST\t/arbitrage/conversations/1,2,3/send.json\t200\t185\t0\t1",
            new Date(1460961338000L), checker.getHost(), "/arbitrage/conversations/*/send", "POST", 200, 185,
            "/arbitrage/conversations/1,2,3/send.json", 0L, true);

        checker.check("[19/Apr/2016:13:14:38 +0300]\t1461060878\tfdef:0:0:0:0:0:0:1\tGET\t/arbitrage/conversations/ping\t200\t5\t0\t1",
            new Date(1461060878000L), checker.getHost(), "/arbitrage/conversations/ping", "GET", 200, 5,
            "/arbitrage/conversations/ping", 0L, true);

        checker.check("[19/Apr/2016:13:48:18 +0300]\t1461062898\t217.69.133.11\t-\tsessionToUID\t200\t5\t161658075\t1",
            new Date(1461062898000L), checker.getHost(), "sessionToUID", "-", 200, 5,
            "sessionToUID", 161658075L, true);

        checker.check("[18/Apr/2016:09:35:38 +0300]\t1460961338\t93.158.141.25\tPATCH\t/cart/UUID/5a7ae178fcde47e8cb071415db925901/list\t200\t10\t0\t1",
            new Date(1460961338000L), checker.getHost(), "/cart/UUID/*/list", "PATCH", 200, 10,
            "/cart/UUID/5a7ae178fcde47e8cb071415db925901/list", 0L, true);
    }
}
