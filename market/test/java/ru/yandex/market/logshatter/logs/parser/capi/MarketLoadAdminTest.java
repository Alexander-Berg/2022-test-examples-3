package ru.yandex.market.logshatter.logs.parser.capi;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.capi.MarketLoadAdminLogParser.DATE_PATTERN;

public class MarketLoadAdminTest {

    LogParserChecker checker = new LogParserChecker(new MarketLoadAdminLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @Test
    void parse() throws Exception {
        checker.setFile("market-load-admin.log");
        checker.setHost("a3pfy7r35vtsg2lv.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@prod@market-load-admin    ");
        String line = "[2022-07-16 11:16:05,857] INFO  [NativeTvmClient] Cache was updated with 3 service ticket(s):" +
            " 2022-07-16T08:16:05.629394Z";
        Date date = dateFormat.parse("2022-07-16 11:16:05,857");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-07-16T11:16:05.857"), // time
            "marketapi", // project
            "market-load-admin", // service
            "Cache was updated with 3 service ticket(s): 2022-07-16T08:16:05.629394Z", // message
            "prod", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market-load-admin.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"threadName\":\"NativeTvmClient\"}" // rest
        );
    }

    @Test
    void parseJobThreadLog() throws Exception {
        checker.setFile("market-load-admin.log");
        checker.setHost("a3pfy7r35vtsg2lv.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@prod@market-load-admin    ");
        String line = "[2022-07-20 10:17:09,493] INFO  [ClusteredJobScheduler_Worker-9] Fetched orders from LOM [0]: ";
        Date date = dateFormat.parse("2022-07-20 10:17:09,493");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-07-20T10:17:09.493"), // time
            "marketapi", // project
            "market-load-admin", // service
            "Fetched orders from LOM [0]: ", // message
            "prod", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market-load-admin.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"threadName\":\"ClusteredJobScheduler_Worker-9\"}" // rest
        );
    }
}
