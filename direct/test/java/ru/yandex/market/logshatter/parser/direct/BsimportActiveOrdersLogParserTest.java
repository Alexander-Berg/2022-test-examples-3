package ru.yandex.market.logshatter.parser.direct;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class BsimportActiveOrdersLogParserTest {

    @Test
    public void testParse1() throws Exception {
        String line = "{\"service\":\"direct.script\",\"uid\":0,\"log_time\":\"2019-03-26 20:21:35\"," +
            "\"method\":\"bsActiveOrders\",\"log_hostname\":\"man1-9606-man-ppc-direct-scripts-2-16737.gencfg-c.yandex.net\"," +
            "\"log_type\":\"active_orders\",\"data\":[{\"OrderID\":\"25269218\",\"CostCur\":\"313.377800\"," +
            "\"UpdateTime\":\"2019-03-26 20:13:00\",\"Clicks\":\"8\",\"SpentUnits\":\"153\",\"OfferShows\":\"0\"," +
            "\"Shows\":\"1738\",\"Cost\":\"10.271829\",\"Stop\":\"0\"},{\"CostCur\":\"41.060300\",\"OrderID\":\"25269219\"," +
            "\"Clicks\":\"8\",\"SpentUnits\":\"42\",\"UpdateTime\":\"2019-03-26 20:17:00\",\"OfferShows\":\"0\",\"Cost\":\"1.345869\"," +
            "\"Stop\":\"0\",\"Shows\":\"916\"}],\"reqid\":1900394331584881259}";


        BsimportActiveOrdersLogParser parser = new BsimportActiveOrdersLogParser();
        LogParserChecker checker = new LogParserChecker(parser);
        checker.setHost("ppcdiscord.yandex.ru");

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        Date date = dateTimeFormat.parse("2019-03-26 20:21:35");

        Object[] data1 = new Object[]{
            25269218L,
            0,
            1738L,
            8L,
            153L,
            10271829L,
            313377800L,
            dateTimeFormat.parse("2019-03-26 20:13:00")
        };

        Object[] data2 = new Object[]{
            25269219L,
            0,
            916L,
            8L,
            42L,
            1345869L,
            41060300L,
            dateTimeFormat.parse("2019-03-26 20:17:00")
        };

        checker.check(line,
            Arrays.asList(date, date),
            Arrays.asList(data1, data2)
        );
    }
}
