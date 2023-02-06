package ru.yandex.market.logshatter.parser.direct;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import com.google.common.primitives.UnsignedLong;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class DbShardsIdsLogParserTest {

    private SimpleDateFormat dateFormat;

    @Test
    public void testParse() throws Exception {
        String line = "<134>1 2015-09-12T00:00:10+03:00 ci01f.yandex.ru PPC.dbshards_ids.log 1010531 - - 2018-09-19 00:00:10 " +
            "{\"reqid\":5740497871948699309,\"insert_data\":{\"pid\":null,\"ClientID\":\"45348812\"},\"ids\":[42],\"cnt\":\"0\"," +
            "\"key\":\"pid\"}";
        LogParserChecker checker = new LogParserChecker(new DbShardsIdsLogParser());

        dateFormat = new SimpleDateFormat(DbShardsIdsLogParser.DATE_PATTERN_PERL);
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

    @Test
    public void testParse2() throws Exception {
        String line = "{\"log_time\":\"2019-01-21:17:56:46\",\"host\":\"sas2-0064-sas-ppc-direct-java-api5-21909.gencfg-c.yandex.net\"," +
            "\"reqid\":4068150252878553853,\"key\":\"vcard_id\",\"ids\":[46547274],\"insert_data\":{\"ClientID\":[53945664]}}";
        LogParserChecker checker = new LogParserChecker(new DbShardsIdsLogParser());

        dateFormat = new SimpleDateFormat(DbShardsIdsLogParser.DATE_PATTERN_JAVA);
        checker.check(line,
            dateFormat.parse("2019-01-21:17:56:46"),
            "PPC.dbshards_ids.log",
            "vcard_id",
            4068150252878553853L,
            "sas2-0064-sas-ppc-direct-java-api5-21909.gencfg-c.yandex.net",
            "{\"ClientID\":[53945664]}",
            new ArrayList<UnsignedLong>(Arrays.asList(UnsignedLong.valueOf(46547274L)))
        );
    }
}
