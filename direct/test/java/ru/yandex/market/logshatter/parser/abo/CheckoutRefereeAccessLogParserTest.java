package ru.yandex.market.logshatter.parser.abo;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.logshatter.abo.CheckoutRefereeAccessLogParser;
import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author antipov93.
 */
public class CheckoutRefereeAccessLogParserTest {

    private LogParserChecker checker;

    @Before
    public void before() {
        checker = new LogParserChecker(new CheckoutRefereeAccessLogParser());
    }

    @Test
    public void testSearchWithOrders() throws Exception {
        checker.check("[04/Feb/2019:13:22:34 +0300]\t1549275754\t82.222.252.165\tGET\t" +
                "/arbitrage/conversations/searchWithOrders\t200\t154\t0\t1\t542885\tpi\tfalse\n",
            1549275754, checker.getHost(), "/arbitrage/conversations/searchWithOrders", "GET", 200, 154,
            "/arbitrage/conversations/searchWithOrders", 0L, true, 542885L, "pi", false);
    }
    @Test
    public void testPing() throws Exception {
        checker.check("[04/Feb/2019:13:22:34 +0300]\t1549275754\t2a02:6b8:b010:5026:5220:4711:ae06:50cc\tGET\t" +
            "/arbitrage/conversations/ping\t200\t5\t0\t1\tnull\tnull\tnull",
            1549275754, checker.getHost(), "/arbitrage/conversations/ping", "GET", 200, 5,
            "/arbitrage/conversations/ping",  0L, true, 0L, "", false);
    }
}
