package ru.yandex.market.logshatter.parser.marketout;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;


public class RtyStatsLogParserTest {
    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new RtyStatsLogParser());

        checker.check(
            "{ \"timestamp\":1538480987, \"hub_ts\":1538480977, \"download_ts\":1538480907, \"feed_id\":\"15\", \"msku_id\":\"10\", \"offer_id\":\"offer1\", \"price\":100, \"old_price\":100.42, \"currency\":643, \"type\":\"price\", \"env\":\"production\", \"report_sub_role\":\"main\", \"report_status\":0 }",
            new Date(1538480987000L), checker.getHost(),
            1538480987, 1538480977, 1538480907, "15", "offer1", "10", 100.0, 100.42, 643, "price", "production", "main", 0, "white"
        );

        checker.check(
            "{ \"timestamp\":1538480987, \"hub_ts\":1538480977, \"download_ts\":1538480907, \"feed_id\":\"15\", \"offer_id\":\"offer1\", \"type\":\"price\", \"env\":\"production\", \"report_sub_role\":\"blue-api\", \"report_status\":1 }",
            new Date(1538480987000L), checker.getHost(),
            1538480987, 1538480977, 1538480907, "15", "offer1", "", Double.NaN, Double.NaN, 0, "price", "production", "blue-api", 1, "blue"
        );

        checker.check(
            "{ \"timestamp\":1538480987, \"hub_ts\":1538480977, \"download_ts\":1538480907, \"feed_id\":\"15\", \"offer_id\":\"offer1\", \"type\":\"price\", \"env\":\"production\", \"report_sub_role\":\"red-market\", \"report_status\":1 }",
            new Date(1538480987000L), checker.getHost(),
            1538480987, 1538480977, 1538480907, "15", "offer1", "", Double.NaN, Double.NaN, 0, "price", "production", "red-market", 1, "red"
        );

        checker.checkEmpty(
            "{ \"timestamp\":1538480987, \"hub_ts\":1538480977, \"download_ts\":1538480907, \"feed_id\":\"15\", \"offer_id\":\"offer1\", \"type\":\"price\", \"env\":\"production\", \"report_sub_role\":\"shadow\", \"report_status\":1 }"
        );
        checker.checkEmpty(
            "{ \"timestamp\":1538480987, \"hub_ts\":1538480977, \"download_ts\":1538480907, \"feed_id\":\"15\", \"offer_id\":\"offer1\", \"type\":\"price\", \"env\":\"production\", \"report_sub_role\":\"bk\", \"report_status\":1 }"
        );
    }
}
