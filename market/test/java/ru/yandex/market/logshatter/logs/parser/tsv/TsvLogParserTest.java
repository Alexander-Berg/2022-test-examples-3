package ru.yandex.market.logshatter.logs.parser.tsv;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;
import ru.yandex.market.logshatter.parser.trace.HealthEnvironment;

public class TsvLogParserTest {
    private static final String PROJECT = "b2b_market";
    private static final String SERVICE = "market_b2b_office";
    private static final String HOST = "d5b4dzf6tednprj4.sas.yp-c.yandex.net";
    private static final String LOGBROKER_TOPIC = "market-b2b-office@production--b2boffice-logs";

    private LogParserChecker checker;

    @BeforeEach
    void configureCommonParams() {
        checker = new LogParserChecker(new TsvLogParser());
        checker.setOrigin(LOGBROKER_TOPIC);
        checker.setHost(HOST);
        checker.setFile("/var/log/yandex/b2boffice/market-b2b-office-back.log");
        checker.setLogBrokerTopic(LOGBROKER_TOPIC);
        checker.setParam("fields", "time,level,component,request_id,message");
        checker.setParam("project", PROJECT);
        checker.setParam("service", SERVICE);
        checker.setParam("logbroker://market-b2b-office@production--b2boffice-logs", "PRODUCTION");
    }

    @Test
    void testAllFields() throws Exception {
        String cluster = "test_cluster";
        String version = "2022.22";
        String traceId = "54fd002aff16fbcce233293ce131fbc6";
        String spanId = "100";
        String validationErr = "Error: Error";
        String rest = "Name=Alex,Test=Success";
        checker.setParam("fields",
                "time,level,component,request_id,trace_id,span_id,version,message,validation_err,rest");
        checker.setParam("cluster", cluster);

        String line = "2022-05-12 17:15:47,192\t" +
                "INFO\t" +
                "[lbkx-checkouter-event-reader-thread-1]OrderEventsConsumer\t" +
                "1658926144576/1ec9112918f789fcf5ce047bc647c1e1\t" +
                "54fd002aff16fbcce233293ce131fbc6\t" +
                "100\t" +
                "2022.22\t" +
                "Start fetching messages by logbroker: order\t" +
                "Error: Error\t" +
                "Name=Alex,Test=Success";

        LocalDateTime dateTime = LocalDateTime.parse("2022-05-12 17:15:47,192", TsvLogParser.DATE_TIME_FORMATTER);
        Date date = Date.from(dateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant());
        checker.check(
                line,
                date,
                LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
                PROJECT, // project
                SERVICE, // service
                "Start fetching messages by logbroker: order", // message
                HealthEnvironment.PRODUCTION.toString(), // env
                cluster, // cluster
                Level.INFO, // level
                HOST, // hostname
                version, // version
                "SAS", // dc
                "1658926144576/1ec9112918f789fcf5ce047bc647c1e1", // request_id
                traceId, // trace_id
                spanId, // span_id
                "[lbkx-checkouter-event-reader-thread-1]OrderEventsConsumer", // component
                UUID.nameUUIDFromBytes(line.getBytes()), // record_id
                validationErr, // validation_err
                rest // rest
        );
    }

    @Test
    void testLimitedFields() throws Exception {
        String line = "2022-05-12 17:15:47,192\tINFO\t[lbkx-checkouter-event-reader-thread-1]OrderEventsConsumer\t" +
                "1658926144576/1ec9112918f789fcf5ce047bc647c1e1\tStart fetching messages by logbroker: order";

        LocalDateTime dateTime = LocalDateTime.parse("2022-05-12 17:15:47,192", TsvLogParser.DATE_TIME_FORMATTER);
        Date date = Date.from(dateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant());
        checker.check(
                line,
                date,
                LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
                PROJECT, // project
                SERVICE, // service
                "Start fetching messages by logbroker: order", // message
                HealthEnvironment.PRODUCTION.toString(), // env
                "", // cluster
                Level.INFO, // level
                HOST, // hostname
                "", // version
                "SAS", // dc
                "1658926144576/1ec9112918f789fcf5ce047bc647c1e1", // request_id
                "", // trace_id
                "", // span_id
                "[lbkx-checkouter-event-reader-thread-1]OrderEventsConsumer", // component
                UUID.nameUUIDFromBytes(line.getBytes()), // record_id
                "", // validation_err
                "" // rest
        );
    }

    @Test
    void testMissingRequestId() throws Exception {
        String line = "2022-05-12 17:15:47,192\tINFO\t[lbkx-checkouter-event-reader-thread-1]OrderEventsConsumer\t" +
                "\tStart fetching messages by logbroker: order";

        LocalDateTime dateTime = LocalDateTime.parse("2022-05-12 17:15:47,192", TsvLogParser.DATE_TIME_FORMATTER);
        Date date = Date.from(dateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant());
        checker.check(
                line,
                date,
                LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
                PROJECT, // project
                SERVICE, // service
                "Start fetching messages by logbroker: order", // message
                HealthEnvironment.PRODUCTION.toString(), // env
                "", // cluster
                Level.INFO, // level
                HOST, // hostname
                "", // version
                "SAS", // dc
                "", // request_id
                "", // trace_id
                "", // span_id
                "[lbkx-checkouter-event-reader-thread-1]OrderEventsConsumer", // component
                UUID.nameUUIDFromBytes(line.getBytes()), // record_id
                "", // validation_err
                "" // rest
        );
    }

    @Test
    void testMoreSeparationFields() throws Exception {
        String line = "2022-05-12 17:15:47,192\tINFO\t[lbkx-checkouter-event-reader-thread-1]OrderEventsConsumer\t" +
                "1658926144576/1ec9112918f789fcf5ce047bc647c1e1\tStart fetching messages by logbroker: order\t" +
                "More\tMore\tMore";

        LocalDateTime dateTime = LocalDateTime.parse("2022-05-12 17:15:47,192", TsvLogParser.DATE_TIME_FORMATTER);
        Date date = Date.from(dateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant());
        checker.check(
                line,
                date,
                LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
                PROJECT, // project
                SERVICE, // service
                "Start fetching messages by logbroker: order\tMore\tMore\tMore", // message
                HealthEnvironment.PRODUCTION.toString(), // env
                "", // cluster
                Level.INFO, // level
                HOST, // hostname
                "", // version
                "SAS", // dc
                "1658926144576/1ec9112918f789fcf5ce047bc647c1e1", // request_id
                "", // trace_id
                "", // span_id
                "[lbkx-checkouter-event-reader-thread-1]OrderEventsConsumer", // component
                UUID.nameUUIDFromBytes(line.getBytes()), // record_id
                "", // validation_err
                "" // rest
        );
    }

    @Test
    void testUnknownFields() throws Exception {
        String line = "2022-05-12 17:15:47,192\tINFO Unknown message format";

        LocalDateTime dateTime = LocalDateTime.parse("2022-05-12 17:15:47,192", TsvLogParser.DATE_TIME_FORMATTER);
        Date date = Date.from(dateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant());
        checker.check(
                line,
                date,
                LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
                PROJECT, // project
                SERVICE, // service
                "2022-05-12 17:15:47,192\tINFO Unknown message format", // message
                HealthEnvironment.PRODUCTION.toString(), // env
                "", // cluster
                Level.UNKNOWN, // level
                HOST, // hostname
                "", // version
                "SAS", // dc
                "", // request_id
                "", // trace_id
                "", // span_id
                "", // component
                UUID.nameUUIDFromBytes(line.getBytes()), // record_id
                "", // validation_err
                "" // rest
        );
    }
}
