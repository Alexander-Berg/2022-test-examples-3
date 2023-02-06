package ru.yandex.market.logshatter.parser.yamarec;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;


public class YamarecRTLoggerLogParserTest {
    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new YamarecRTLoggerLogParser());
        checker.check(
            "2015-10-01 21:58:10,171 +0300\tMONRUN\t13689\tUploadManager\tPartition stat: {\"target\": \"hume\", \"read_events\": 2, \"last_timestamp\": 1443633240, \"upload_events\": 4, \"delay\": 6, \"log_type\": \"mrec-log\", \"partition\": \"rt3.iva--cs-http--mrec-log:0\"}",
            new Date(1443725890171L),
            checker.getHost(),
            "mrec-log",
            4L,
            6,
            2L,
            "hume"
        );
        checker.checkEmpty("2015-10-01 21:58:10,171 +0300\tINFO\t13689\tUploadManager\tSome message");
    }
}
