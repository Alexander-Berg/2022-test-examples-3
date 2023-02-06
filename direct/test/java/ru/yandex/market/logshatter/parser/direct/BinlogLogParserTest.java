package ru.yandex.market.logshatter.parser.direct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class BinlogLogParserTest {

    @Test
    public void testParse() throws Exception {
        String line = "{\"utcTimestamp\":1568896726,\"source\":\"testing:ppc:11\"," +
            "\"serverUuid\":\"4221e2d9-3e84-11e9-83ca-9ee39a59f0a6\",\"transactionId\":10562666,\"queryIndex\":11," +
            "\"db\":\"ppc\",\"table\":\"campaigns\",\"operation\":\"UPDATE\",\"traceInfoReqId\":2649258305510020299," +
            "\"traceInfoService\":\"direct.jobs\",\"traceInfoMethod\":\"activeorders.ActiveOrdersImportJob\"," +
            "\"traceInfoOperatorUid\":0,\"essTag\":\"\",\"rows\":[{\"rowIndex\":0,\"primaryKey\":{\"cid\":40976750}," +
            "\"before\":{\"ClientID\":35465330,\"sum_spent\":8192.456800,\"sum_spent_units\":39572,\"shows\":39572," +
            "\"clicks\":339,\"lastShowTime\":[2019,9,19,15,33,53]},\"after\":{\"ClientID\":35465330," +
            "\"sum_spent\":8221.880600,\"sum_spent_units\":39577,\"shows\":39577,\"clicks\":340," +
            "\"lastShowTime\":[2019,9,19,15,38,46]}}],\"schemaChanges\":[]," +
            "\"gtid\":\"4221e2d9-3e84-11e9-83ca-9ee39a59f0a6:10562666\",\"queryChunkIndex\":0}";
        LogParserChecker checker = new LogParserChecker(new BinlogLogParser());

        checker.check(line,
            new Date(1568896726000L),
            2649258305510020299L,
            "activeorders.ActiveOrdersImportJob",
            "direct.jobs",
            "testing:ppc:11",
            "ppc",
            "campaigns",
            2,  // UPDATE
            "4221e2d9-3e84-11e9-83ca-9ee39a59f0a6",
            10562666L,
            11,
            "40976750",
            "cid",
            new ArrayList<String>(Arrays.asList("clicks", "ClientID", "lastShowTime", "shows", "sum_spent",
                "sum_spent_units")),
            new ArrayList<String>(Arrays.asList("340", "35465330", "[2019,9,19,15,38,46]", "39577", "8221.880600",
                "39577")),
            new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0, 0))
        );
    }
}
