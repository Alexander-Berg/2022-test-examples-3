package ru.yandex.market.logshatter.parser.logshatter_for_logs;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class LogshatterLbSessionsLogParserTest {
    private static final LogParserChecker CHECKER = new LogParserChecker(new LogshatterLbSessionsLogParser());
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");

    @Test
    public void parse() throws Exception {
        URL resource = getClass().getClassLoader().getResource("logshatter-lb-sessions.log");
        String line = FileUtils.readLines(new File(resource.toURI())).get(0).toString();
        resource = getClass().getClassLoader().getResource("logshatter-lb-sessions-rest.json");
        String rest = FileUtils.readLines(new File(resource.toURI())).get(0).toString();
        String host = "sas3-1361-bef-sas-market-prod--88b-16641.gencfg-c.yandex.net";
        Date date = SIMPLE_DATE_FORMAT.parse("2022-03-28T15:46:25.734+03:00");
        CHECKER.setLogBrokerTopic("rt3.myt--market-devexp@production--logshatter-for-logs");
        CHECKER.setHost(host);
        CHECKER.check(
            line,
            date,
            OffsetDateTime.parse("2022-03-28T15:46:25.734+03:00").toLocalDateTime(), // time
            "market-devexp", // project
            "logshatter-for-logs", // service
            "", // message
            "prod", // env
            "", // cluster
            Level.UNKNOWN, // level
            host, // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            rest // rest
        );
    }
}
