package ru.yandex.market.logshatter.parser.direct;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.SimpleDateFormat;

public class BalanceLogParserTest {

    private SimpleDateFormat dateFormat;

    @Test
    public void testParse() throws Exception {
        String line = "<134>1 2015-08-25T13:54:04+03:00 ppcdev1.yandex.ru beta_icenine_8502.balance_calls.log 564083 -" +
            " - [pid=564083,reqid=6649929344820130528,method=Balance.FindClient,data_type=response] [{\"ClientID\":\"5299008\"}]";

        BalanceLogParser parser = new BalanceLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        checker.setHost("ppcdiscord.yandex.ru");
        checker.setFile("/tmp/test");

        dateFormat = new SimpleDateFormat(BalanceLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("2015-08-25T13:54:04"),
            "Balance.FindClient",
            "[{\"ClientID\":\"5299008\"}]",
            6649929344820130528L,
            "ppcdiscord.yandex.ru",
            564083L,
            "response",
            "/tmp/test"
        );
    }

    @Test
    public void testParse2() throws Exception {
        String line = "2019-03-25T19:54:57.300 [pid=502465,reqid=1937501875040996501,method=Balance2.TearOffPromocode,data_type=request,try=1]" +
            " [0,{\"ServiceID\":7,\"ServiceOrderID\":41872827,\"PromocodeID\":6255940}]";

        BalanceLogParser parser = new BalanceLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        checker.setHost("ppcdiscord.yandex.ru");
        checker.setFile("/tmp/test");

        dateFormat = new SimpleDateFormat(BalanceLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("2019-03-25T19:54:57"),
            "Balance2.TearOffPromocode",
            "[0,{\"ServiceID\":7,\"ServiceOrderID\":41872827,\"PromocodeID\":6255940}]",
            1937501875040996501L,
            "ppcdiscord.yandex.ru",
            502465L,
            "request",
            "/tmp/test"
        );
    }

    @Test
    public void testParse3() throws Exception {
        String line = "        at org.apache.xmlrpc.client.XmlRpcStreamTransport.readResponse(XmlRpcStreamTransport.java:197) ~[xmlrpc-client-3.1.3.jar:3.1.3]";

        BalanceLogParser parser = new BalanceLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        checker.setHost("ppcdiscord.yandex.ru");
        checker.setFile("/tmp/test");

        dateFormat = new SimpleDateFormat(BalanceLogParser.DATE_PATTERN);
        checker.check(line);
    }
}
