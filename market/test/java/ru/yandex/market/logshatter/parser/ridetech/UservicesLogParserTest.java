package ru.yandex.market.logshatter.parser.ridetech;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

public class UservicesLogParserTest {

    LogParserChecker checker = new LogParserChecker(new UservicesLogParser());

    @Test
    public void parse() throws Exception {
        String line1 = "tskv\ttimestamp=2016-09-14T12:38:39.123456\tlevel=WARNING\t" +
            "module=Handle ( services/hejmdal/src/views/v1/event-log/list/get/view.cpp:462 ) \ttask_id=7F8884E4AA80\t" +
            "thread_id=0x00007F88889FF700\ttext=blah-blah, before tab\\tafter tab\t" +
            "link=be920e8bdf524d499c9363fd0db49ed1\tparent_link=72d076ee602d4c4a99fcfd6d8d162ffc\t" +
            "trace_id=7abf0cea1b644b41afcfb66852d8e262\tspan_id=627a43878eb70ab0\tparent_id=37ecd80de57aabfd\t" +
            "string_field=a\\nb\\t";

        String line2 = "tskv\ttimestamp=2016-09-14T12:38:39.000003";
        String line3 = "tskv\ttimestamp=2016-09-14T12:38:39.000003\tmodule=other_module\tlevel=INFO";

        checker.setHost("hostname");
        checker.setParam("hostname",
            "{'namespace': 'taxi', 'project': 'taxi-infra', 'service': 'hejmdal', 'env': 'stable'}");
        checker.setParam("module-prefix-to-ignore", "other");
        checker.setLogBrokerTopic("rt3.sas--taxi--taxi-hejmdal-yandex-taxi-hejmdal-log");

        checker.check(
            line1,
            1473845919,
            LocalDateTime.parse("2016-09-14T12:38:39.123456"),
            "taxi_taxi-infra", // project
            "hejmdal", // service
            "blah-blah, before tab\\tafter tab", // message
            "stable", // env
            "", // cluster
            Level.WARN, // level
            "hostname", // hostname
            "", // version
            "dc", // dc
            "be920e8bdf524d499c9363fd0db49ed1", // request_id
            "7abf0cea1b644b41afcfb66852d8e262", // trace_id
            "627a43878eb70ab0", // span_id
            "Handle ( services/hejmdal/src/views/v1/event-log/list/get/view.cpp:462 )", // component
            UUID.fromString("5a4e97fe-20ef-34c9-9726-e5d7508226ee"), // record_id
            "", // validation_err
            "{\"parent_link\":\"72d076ee602d4c4a99fcfd6d8d162ffc\",\"thread_id\":\"0x00007F88889FF700\"," +
                "\"parent_id\":\"37ecd80de57aabfd\",\"task_id\":\"7F8884E4AA80\",\"string_field\":\"a\\\\nb\\\\t\"}"
            // rest
        );

        checker.setParam("hostname", "");

        checker.check(
            line2,
            1473845919,
            LocalDateTime.parse("2016-09-14T12:38:39.000003"),
            "taxi", // project
            "hejmdal", // service
            "", // message
            "prod", // env
            "", // cluster
            Level.UNKNOWN, // level
            "hostname", // hostname
            "", // version
            "dc", // dc
            "0", // request_id
            "", // trace_id
            "", // span_id
            "", // component
            UUID.fromString("f1d97d29-d256-3b2b-904a-64bfedd85b09"), // record_id
            "", // validation_err
            "{}" // rest
        );

        checker.checkEmpty(line3);
    }
}
