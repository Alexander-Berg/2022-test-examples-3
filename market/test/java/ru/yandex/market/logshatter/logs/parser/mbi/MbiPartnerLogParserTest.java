package ru.yandex.market.logshatter.logs.parser.mbi;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.mbi.MbiPartnerLogParser.DATE_PATTERN;


public class MbiPartnerLogParserTest {
    LogParserChecker checker = new LogParserChecker(new MbiPartnerLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @Test
    void whenAllParametersSpecifiedShouldParseCorrectly() throws Exception {
        checker.setFile("mbi-partner.log");
        checker.setHost("dm2zw6iv3tizgldw.vla.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@production--mbi-partner");
        String line = "[2022-05-12 17:15:47,192] INFO  [ForkJoinPool.commonPool-worker-5] " +
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1 Get stocks for offers";
        Date date = dateFormat.parse("2022-05-12 17:15:47,192");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
            "mbi", // project
            "mbi-partner", // service
            "Get stocks for offers", // message
            "prod", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "VLA", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1", // request_id
            "", // trace_id
            "", // span_id
            "mbi-partner.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"threadName\":\"ForkJoinPool.commonPool-worker-5\"}" // rest
        );
    }

    @Test
    void whenMultilineShouldParseCorrectly() throws Exception {
        checker.setFile("mbi-partner.log");
        checker.setHost("dm2zw6iv3tizgldw.vla.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@production--mbi-partner");
        String line = "[2022-05-12 17:15:47,192] INFO  [ForkJoinPool.commonPool-worker-5] " +
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1 Get stocks for offers\nline2\nline3";
        Date date = dateFormat.parse("2022-05-12 17:15:47,192");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
            "mbi", // project
            "mbi-partner", // service
            "Get stocks for offers\nline2\nline3", // message
            "prod", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "VLA", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1", // request_id
            "", // trace_id
            "", // span_id
            "mbi-partner.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"threadName\":\"ForkJoinPool.commonPool-worker-5\"}" // rest
        );
    }

    @Test
    void parseStacktrace() throws Exception {
        checker.setFile("mbi-partner.log");
        checker.setHost("dm2zw6iv3tizgldw.vla.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@production--mbi-partner");

        String expectedMessage = "Unrecoverable error occurred\n"
            + "org.springframework.web.context.request.async.AsyncRequestTimeoutException: null\n"
            + "        at org.springframework.web.context.request.async.TimeoutDeferredResultProcessingInterceptor"
            + ".handleTimeout(TimeoutDeferredResultProcessingInterceptor.java:42) "
            + "~[spring-web-5.1.6.RELEASE.jar:5.1.6.RELEASE]\n"
            + "        at org.springframework.web.context.request.async.DeferredResultInterceptorChain"
            + ".triggerAfterTimeout(DeferredResultInterceptorChain.java:79) "
            + "~[spring-web-5.1.6.RELEASE.jar:5.1.6.RELEASE]\n"
            + "        at org.springframework.web.context.request.async.WebAsyncManager"
            + ".lambda$startDeferredResultProcessing$5(WebAsyncManager.java:424) "
            + "~[spring-web-5.1.6.RELEASE.jar:5.1.6.RELEASE]\n"
            + "        ...\n"
            + "        at java.lang.Thread.run(Thread.java:829) [?:?]";

        String line = "[2022-07-29 10:48:39,025] ERROR [319476:79668854%d:campaigns/{campaignId}/handle/@GET] "
            + "1659080828428/785d6d1d1b559350e6a06edbece40500/30 " + expectedMessage;
        Date date = dateFormat.parse("2022-07-29 10:48:39,025");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-07-29T10:48:39.025"), // time
            "mbi", // project
            "mbi-partner", // service
            expectedMessage, // message
            "prod", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "VLA", // dc
            "1659080828428/785d6d1d1b559350e6a06edbece40500/30", // request_id
            "", // trace_id
            "", // span_id
            "mbi-partner.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"threadName\":\"319476:79668854%d:campaigns/{campaignId}/handle/@GET\"}" // rest
        );
    }
}
