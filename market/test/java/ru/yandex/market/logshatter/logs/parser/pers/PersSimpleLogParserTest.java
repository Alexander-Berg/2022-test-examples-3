package ru.yandex.market.logshatter.logs.parser.pers;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.pers.PersSimpleLogParser.DATE_PATTERN;


public class PersSimpleLogParserTest {
    LogParserChecker checker = new LogParserChecker(new PersSimpleLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @Test
    void parse() throws Exception {
        checker.setFile("pers-author.log");
        checker.setHost("testing-market-pers-tms-vla-1.vla.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@testing@pers--pers-author");
        String line = "[2022-06-01 16:31:50,003] INFO [requestThreadPool-38] hi [still message]|>hi again";
        Date date = dateFormat.parse("2022-06-01 16:31:50,003");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-06-01T16:31:50.003"), // time
            "pers", // project
            "pers-author", // service
            "hi [still message]\nhi again", // message
            "testing", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "VLA", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "pers-author.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"threadName\":\"requestThreadPool-38\"}" // rest
        );
    }
}
