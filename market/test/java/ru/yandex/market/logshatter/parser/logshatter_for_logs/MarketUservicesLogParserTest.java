package ru.yandex.market.logshatter.parser.logshatter_for_logs;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class MarketUservicesLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketUservicesLogParser());

    @Test
    public void parse() throws Exception {
        String line1 = "tskv\ttimestamp=2016-09-14T12:38:39.123456\tlevel=WARNING\t" +
            "module=Handle ( services/market-shops/src/views/v1/event-log/list/get/view.cpp:462 ) \t" +
            "task_id=7F8884E4AA80\tthread_id=0x00007F88889FF700\ttext=blah-blah, before tab\\tafter tab\t" +
            "link=be920e8bdf524d499c9363fd0db49ed1\tparent_link=72d076ee602d4c4a99fcfd6d8d162ffc\t" +
            "trace_id=7abf0cea1b644b41afcfb66852d8e262\tspan_id=627a43878eb70ab0\tparent_id=37ecd80de57aabfd\t" +
            "string_field=a\\nb\\t";

        String line2 = "tskv\ttimestamp=2016-09-14T12:38:39.000003";

        checker.setHost("eawr3xojyfc4sgnq.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.sas--market-backbone@production--market-shops-log");

        checker.check(
            line1,
            1473845919,
            LocalDateTime.parse("2016-09-14T12:38:39.123456"),
            "market-backbone", // project
            "market-shops", // service
            "blah-blah, before tab\\tafter tab", // message
            "prod", // env
            "", // cluster
            Level.WARN, // level
            "eawr3xojyfc4sgnq.sas.yp-c.yandex.net", // hostname
            "", // version
            "SAS", // dc
            "be920e8bdf524d499c9363fd0db49ed1", // request_id
            "7abf0cea1b644b41afcfb66852d8e262", // trace_id
            "627a43878eb70ab0", // span_id
            "Handle ( services/market-shops/src/views/v1/event-log/list/get/view.cpp:462 )", // component
            UUID.fromString("e4dc2a98-371f-3c9b-aed7-e2cc32d173c1"), // record_id
            "", // validation_err
            "{\"parent_link\":\"72d076ee602d4c4a99fcfd6d8d162ffc\",\"thread_id\":\"0x00007F88889FF700\"," +
            "\"parent_id\":\"37ecd80de57aabfd\",\"task_id\":\"7F8884E4AA80\",\"string_field\":\"a\\\\nb\\\\t\"}"
            // rest
        );
    }
}
