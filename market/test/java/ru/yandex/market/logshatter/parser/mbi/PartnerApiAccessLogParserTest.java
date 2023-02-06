package ru.yandex.market.logshatter.parser.mbi;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

public class PartnerApiAccessLogParserTest {

    @Test
    public void testParse() throws Exception {
        String line = "[Wed Dec 24 21:48:04 2014]\tGET\tHTTP/1.1\t/v2/campaigns/21034376/feeds.json\t95.108.201" +
            ".137\t\"-\" \t200\t/campaigns/*/feeds\t21\t13186958\t21034376\t81\t77";
        String line2 = "[Wed Dec 24 21:48:04 2014]\tGET\tHTTP/1.1\t/v2/campaigns/21034376/feeds.json\t95.108.201" +
            ".137\t\"-\" \t200\t/campaigns/*/feeds\t21\t13186958\t21034376\t81\t77\t1419442424" +
            "\tbd16ddb868894713bd7b707f75ddb217";
        String line3 = "[Fri Oct 06 06:29:49 2017]\tGET\tHTTP/1" +
            ".0\t/ping\t2a02:6b8:b010:5026:fca0:249f:711f:b20d\t\"-\"\t200\t-\t-\t-\t-\t4\t1\t1507260589\t-\t-";
        String line4 = "[Tue Nov 28 12:42:20 2017]\tGET\tHTTP/1.0\t/campaigns" +
            ".json\t2a02:6b8:c0e:11d:0:1407:bde6:c421\t\"-\"\t200\t/campaigns\t1\t318947777\t-\t692\t413\t1511862140" +
            "\t-\t1\t1511862140339/e94af87b35d40c45194442a5467132a2/1";
        String line5 = "[Wed Jul 10 10:45:11 2019]\tGET\tHTTP/1.0\t//api.partner.market.yandex" +
            ".ru/v2/campaigns/21023434.json\t2a02:6b8:b010:5092:225:90ff:fe95:b382\t\"-\"\t200\t/campaigns/*\t2" +
            "\t104158583\t21023434\t103\t82\t1562744711\t-\t1\t1562744711142/c0a625b54a9c228cd177216d9f79459f\tSHOP";

        LogParserChecker checker = new LogParserChecker(new PartnerApiAccessLogParser());

        checker.setOrigin("market-health-dev");
        checker.setParam("logbroker://market-health-dev", "DEVELOPMENT");

        checker.check(
            line,
            new Date(1419446884000L), checker.getHost(), "/campaigns/*/feeds", 200, 77, 21, 13186958L, 21034376L,
            "/v2/campaigns/21034376/feeds.json", "", 0, "GET", 0, 0L, "-", new Integer[0], "", Environment.DEVELOPMENT
        );

        checker.check(
            line2,
            new Date(1419442424000L), checker.getHost(), "/campaigns/*/feeds", 200, 77, 21, 13186958L, 21034376L,
            "/v2/campaigns/21034376/feeds.json", "bd16ddb868894713bd7b707f75ddb217", 0, "GET", 0, 0L, "-",
            new Integer[0], "",
            Environment.DEVELOPMENT
        );

        checker.check(
            line3,
            new Date(1507260589000L), checker.getHost(), "-", 200, 1, 0, 0L, 0L, "/ping", "-", 0, "GET", 1, 0L, "-",
            new Integer[0], "", Environment.DEVELOPMENT
        );

        checker.check(
            line4,
            new Date(1511862140000L), checker.getHost(), "/campaigns", 200, 413, 1, 318947777L, 0L,
            "/campaigns.json", "-", 1, "GET", 0, 1511862140339L, "e94af87b35d40c45194442a5467132a2",
            new Integer[]{1}, "", Environment.DEVELOPMENT
        );

        checker.check(
            line5,
            new Date(1562744711000L), checker.getHost(), "/campaigns/*", 200, 82, 2, 104158583L, 21023434L,
            "//api.partner.market.yandex.ru/v2/campaigns/21023434.json", "-", 1, "GET", 0, 1562744711142L,
            "c0a625b54a9c228cd177216d9f79459f", new Integer[0], "SHOP", Environment.DEVELOPMENT
        );

        checker.check(
            "[Sun Dec 14 07:20:56 2014]\tGET\tHTTP/1.1\t/v2/models/11126988.json?regionId=2\t62.152.53.210\t\"-\" " +
                "\t200\t/models/*\t12\t253101834\t-\t124\t19"
        );

        checker.check(
            "[Sun Dec 14 20:17:37 2014]\tGET\tHTTP/1.0\t/v1/models" +
                ".xml?regionId=54&query=??????????+Defender+??+Assistant+SM-670&oauth_client_id" +
                "=7cc4283edfcf4b5ba455aefbefeaee24&oauth_login=uaclick&oauth_token=7086668c979f44cab70b7f9b5196f148" +
                "\t37.187.132.76\t\"Apishops\"\t420\t/models\t12\t134120136\t-\t162\t103"
        );

        //return "(date Date, timestamp UInt32, host String, api_resource String, http_code UInt16, resptime_ms
        // Int32, " +
        // "group_id UInt32, uid UInt64, campaign_id UInt64, url String) ENGINE = MergeTree(date, (timestamp), 8192)";
    }
}
