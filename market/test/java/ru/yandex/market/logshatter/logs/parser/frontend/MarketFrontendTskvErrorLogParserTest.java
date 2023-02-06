package ru.yandex.market.logshatter.logs.parser.frontend;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;


class MarketFrontendTskvErrorLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketFrontendTskvErrorLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Test
    void thenAllFieldsSpecifiedShouldParseCorrectly() throws Exception {
        checker.setFile("market_front_desktop-errors.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-health@market-health-stable--other");

        String line = "tskv\ttimestamp=1653513683257\tservice=market_front_desktop\tcode=Code\tlevel=error\t" +
            "message=some error message\tstack_trace=some stack trace\t" +
            "stack_trace_hash=49aae5be6191c82de5e2aafc451b2d894893df2aaefc0293a6fc5ec3f0972fe2\tfile=123.js\t" +
            "line_no=5\ttags=t1,t2,t3\textra_keys=k1,k2\textra_values=v1,v2\trevision=pppp\t" +
            "request_id=1652364947076/6bf0d0191421219f2eb4b531d1de0500";


        Date date = dateFormat.parse("2022-05-26T00:21:23+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-26T00:21:23.257"), // time
            "market-frontend", // project
            "market-frontend", // service
            "some error message ~ some stack trace", //
            // message
            "prod", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500", // request_id
            "", // trace_id
            "", // span_id
            "market_front_desktop-errors.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"service\":\"market_front_desktop\",\"code\":\"Code\"," +
                "\"stack_trace_hash\":\"49aae5be6191c82de5e2aafc451b2d894893df2aaefc0293a6fc5ec3f0972fe2\"," +
                "\"file\":\"123.js\",\"line_no\":5,\"tags\":[\"t1\",\"t2\",\"t3\"],\"extra_keys\":[\"k1\",\"k2\"]," +
                "\"extra_values\":[\"v1\",\"v2\"],\"revision\":\"pppp\"}" // rest
        );
    }

    @Test
    void thenMissingRequestIdShouldParseCorrectly() throws Exception {
        checker.setFile("market_front_desktop-errors.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-health@market-health-stable--other");

        String line = "tskv\ttimestamp=1653513683257\tservice=market_front_desktop\tcode=Code\tlevel=error\t" +
            "message=some error message\tstack_trace=some stack trace\t" +
            "stack_trace_hash=49aae5be6191c82de5e2aafc451b2d894893df2aaefc0293a6fc5ec3f0972fe2\tfile=123.js\t" +
            "line_no=5\ttags=t1,t2,t3\textra_keys=k1,k2\textra_values=v1,v2\trevision=pppp";


        Date date = dateFormat.parse("2022-05-26T00:21:23+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-26T00:21:23.257"), // time
            "market-frontend", // project
            "market-frontend", // service
            "some error message ~ some stack trace", //
            // message
            "prod", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market_front_desktop-errors.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"service\":\"market_front_desktop\",\"code\":\"Code\"," +
                "\"stack_trace_hash\":\"49aae5be6191c82de5e2aafc451b2d894893df2aaefc0293a6fc5ec3f0972fe2\"," +
                "\"file\":\"123.js\",\"line_no\":5,\"tags\":[\"t1\",\"t2\",\"t3\"],\"extra_keys\":[\"k1\",\"k2\"]," +
                "\"extra_values\":[\"v1\",\"v2\"],\"revision\":\"pppp\"}" // rest
        );
    }

    @Test
    void thenMissingAdditionalInfoShouldParseCorrectly() throws Exception {
        checker.setFile("market_front_desktop-errors.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-health@market-health-stable--other");

        String line = "tskv\ttimestamp=1653513683257\tstack_trace=some stack trace\t" +
            "request_id=1652364947076/6bf0d0191421219f2eb4b531d1de0500";


        Date date = dateFormat.parse("2022-05-26T00:21:23+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-26T00:21:23.257"), // time
            "market-frontend", // project
            "market-frontend", // service
            " ~ some stack trace", //
            // message
            "prod", // env
            "", // cluster
            Level.UNKNOWN, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500", // request_id
            "", // trace_id
            "", // span_id
            "market_front_desktop-errors.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"line_no\":0}" // rest
        );
    }
}
