package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class DbShardsIdsLogParserTest {

    private SimpleDateFormat dateFormat;

    @Test
    public void testParse() throws Exception {
        String line = "<134>1 2015-09-12T00:00:10+03:00 ci01f.yandex.ru PPC.dbshards_ids.log 1010531 - - 2018-09-19 " +
            "00:00:10 {\"reqid\":5740497871948699309,\"insert_data\":{\"pid\":null,\"ClientID\":\"45348812\"}," +
            "\"ids\":[42],\"cnt\":\"0\",\"key\":\"pid\"}";
        LogParserChecker checker = new LogParserChecker(new DbShardsIdsLogParser());

        dateFormat = new SimpleDateFormat(DbShardsIdsLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("2015-09-12T00:00:10+03:00"),
            "PPC.dbshards_ids.log",
            "pid",
            5740497871948699309L,
            "ci01f.yandex.ru",
            "{\"pid\":null,\"ClientID\":\"45348812\"}",
            new ArrayList<UnsignedLong>(Arrays.asList(UnsignedLong.valueOf(42L)))
        );
    }
}
