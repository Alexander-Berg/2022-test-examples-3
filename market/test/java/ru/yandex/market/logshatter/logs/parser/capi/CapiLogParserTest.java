package ru.yandex.market.logshatter.logs.parser.capi;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.capi.CapiLogParser.DATE_PATTERN;


public class CapiLogParserTest {
    LogParserChecker checker = new LogParserChecker(new CapiLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @Test
    void parse() throws Exception {
        checker.setFile("market-api.log");
        checker.setHost("a3pfy7r35vtsg2lv.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@prestable@capi--capi");
        String line = "[2022-06-01 16:31:50,003] INFO [requestThreadPool-38] " +
            "htesting-market-content-api-sas-1t0008106011f40973a #http_request_end, Checkouter, " +
            "URL: http://checkouter.tst.vs.market.yandex.net:39001/orders RESPONSE_CODE: 200, DURATION: 191 ms, #tm200";
        Date date = dateFormat.parse("2022-06-01 16:31:50,003");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-06-01T16:31:50.003"), // time
            "marketapi", // project
            "capi", // service
            "#http_request_end, Checkouter, URL: http://checkouter.tst.vs.market.yandex.net:39001/orders " +
                "RESPONSE_CODE: 200, DURATION: 191 ms, #tm200", // message
            "prestable", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "htesting-market-content-api-sas-1t0008106011f40973a", // request_id
            "", // trace_id
            "", // span_id
            "market-api.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"threadName\":\"requestThreadPool-38\"}" // rest
        );
    }
}
