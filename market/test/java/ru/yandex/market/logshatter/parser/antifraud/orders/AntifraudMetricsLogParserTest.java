package ru.yandex.market.logshatter.parser.antifraud.orders;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

/**
 * @author dzvyagin
 */
public class AntifraudMetricsLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new AntifraudMetricsLogParser());

    @Test
    public void testRecord() throws Exception {
        checker.check(
            "{\"datetime\": \"2019-10-04T00:07:59 +0300\", \"dc\": \"sas\", \"host\": \"sas-1\", " +
                "\"environment\": \"PRESTABLE\", " +
                "\"key\": \"key\", \"subkey\": \"subkey\", \"value\": 2.1, \"requestId\": \"requestId\"}",
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse("2019-10-04T00:07:59 +0300"),
            "sas",
            "sas-1",
            Environment.PRESTABLE,
            "key",
            "subkey",
            2.1d,
            "requestId"
        );
    }

    @Test
    public void testRecordDefaultValue() throws Exception {
        checker.check(
            "{\"datetime\": \"2019-10-04T00:07:59 +0300\", \"dc\": \"sas\", \"host\": \"sas-1\", " +
                "\"environment\": \"PRESTABLE\", " +
                "\"key\": \"key\", \"subkey\": \"subkey\", \"value\": 2.1}",
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse("2019-10-04T00:07:59 +0300"),
            "sas",
            "sas-1",
            Environment.PRESTABLE,
            "key",
            "subkey",
            2.1d,
            ""
        );
    }
}
