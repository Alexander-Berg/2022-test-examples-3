package ru.yandex.market.logshatter.parser.mstat;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

public class MstatChCacheLogParserTest {
    private static final LogParserChecker CHECKER = new LogParserChecker(new MstatChCacheLogParser());
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Test
    public void parse() throws Exception {
        String line = "2022-03-30 00:54:13 203119      Dummy-3726 | INFO" +
            "  | Query tables: ['//home/market/production/mstat/dictionaries/categories/latest']";
        String host = "sas3-1361-bef-sas-market-prod--88b-16641.gencfg-c.yandex.net";
        Date date = SIMPLE_DATE_FORMAT.parse("2022-03-30 00:54:13");
        CHECKER.setLogBrokerTopic("rt3.kafka-bs--market-devexp@production--mstat-ch-cache");
        CHECKER.setHost(host);
        CHECKER.check(
            line,
            date,
            LocalDateTime.parse("2022-03-30T00:54:13"), // time
            "market-mstat", // project
            "mstat-ch-cache", // service
            "Query tables: ['//home/market/production/mstat/dictionaries/categories/latest']", // message
            "prod", // env
            "", // cluster
            Level.INFO, // level
            host, // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "203119", // component
            UUID.fromString("328cce14-56a0-346a-852e-4c4a43480cb9"), // record_id
            "", // validation_err
            "{}" // rest
        );
    }
}
