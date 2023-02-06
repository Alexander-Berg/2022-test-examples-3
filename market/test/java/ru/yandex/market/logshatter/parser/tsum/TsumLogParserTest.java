package ru.yandex.market.logshatter.parser.tsum;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

public class TsumLogParserTest {
    private static final LogParserChecker CHECKER = new LogParserChecker(new TsumLogParser());
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");

    @Test
    public void parse() throws Exception {
        String line = "2022-02-28 20:47:15,538 INFO  [SandboxTaskRunner Thread-61711] " +
            "Sandbox task 1229723737 has status EXECUTING. shouldStop = false.";
        String host = "sas3-1361-bef-sas-market-prod--88b-16641.gencfg-c.yandex.net";
        Date date = SIMPLE_DATE_FORMAT.parse("2022-02-28 20:47:15,538");
        CHECKER.setLogBrokerTopic("rt3.kafka-bs--tsum-health@stable--tsum-tms");
        CHECKER.setHost(host);
        CHECKER.check(
            line,
            date,
            LocalDateTime.parse("2022-02-28T20:47:15.538"),
            "market-devexp",
            "tsum-tms",
            "Sandbox task 1229723737 has status EXECUTING. shouldStop = false.", // message
            "prod",
            "", // cluster
            Level.INFO, // level
            host, // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "SandboxTaskRunner Thread-61711", // component
            UUID.fromString("078dd81a-6faa-3219-8c1d-b51f271e11f1"), // record_id
            "", // validation_err
            "{}" // rest
        );
    }

    @Test
    public void parseWrongDate() throws Exception {
        String line = "20222-02-28 20:47:15,538 INFO  [SandboxTaskRunner Thread-61711] " +
            "Sandbox task 1229723737 has status EXECUTING. shouldStop = false.";
        CHECKER.checkEmpty(line);
    }

    @Test
    public void parseWrongPattern() throws Exception {
        String line = "2022-02-28 20:47:15,538 INFO " +
            "Sandbox task 1229723737 has status EXECUTING. shouldStop = false.";
        CHECKER.checkEmpty(line);

        line = "2022-02-28 20:47:15,538 INFO  NEW_INFO [SandboxTaskRunner Thread-61711] " +
            "Sandbox task 1229723737 has status EXECUTING. shouldStop = false.";
        CHECKER.checkEmpty(line);
    }
}
