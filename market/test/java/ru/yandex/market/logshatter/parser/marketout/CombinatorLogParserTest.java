package ru.yandex.market.logshatter.parser.marketout;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class CombinatorLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CombinatorLogParser());

        checker.check(
            "{"
                + " \"ts\": \"2020-05-27T17:15:15.000+0300\""
                + ",\"grpc.code\": \"OK\""
                + ",\"grpc.method\": \"GetSkuDeliveryStats\""
                + ",\"grpc.time_ms\": 123.456"
                + "}",
            new Date(1590588915000L), "OK", "GetSkuDeliveryStats", 123
        );
    }
}
