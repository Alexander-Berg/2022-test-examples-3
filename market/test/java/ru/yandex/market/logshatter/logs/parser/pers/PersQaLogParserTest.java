package ru.yandex.market.logshatter.logs.parser.pers;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.pers.PersQaLogParser.DATE_PATTERN;


public class PersQaLogParserTest {
    LogParserChecker checker = new LogParserChecker(new PersQaLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @Test
    void parse() throws Exception {
        checker.setFile("pers-qa-tms.log");
        checker.setHost("testing-market-pers-qa-tms-vla-1.vla.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@testing@pers--pers-qa-tms");
        String line = "2022-06-01 16:31:50,003 INFO [PersLoggerImpl requestThreadPool-38] hello world|>hi again";
        Date date = dateFormat.parse("2022-06-01 16:31:50,003");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-06-01T16:31:50.003"), // time
            "pers", // project
            "pers-qa-tms", // service
            "hello world\nhi again", // message
            "testing", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "VLA", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "pers-qa-tms.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"loggerName\":\"PersLoggerImpl\",\"threadName\":\"requestThreadPool-38\"}" // rest
        );
    }
}
