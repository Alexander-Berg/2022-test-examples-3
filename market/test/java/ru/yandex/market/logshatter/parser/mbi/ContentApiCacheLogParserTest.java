package ru.yandex.market.logshatter.parser.mbi;

import java.text.DateFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * Created by apershukov on 07.10.16.
 */
public class ContentApiCacheLogParserTest {

    private LogParserChecker checker;
    private DateFormat dateFormat;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new ContentApiCacheLogParser());
        dateFormat = ContentApiHelper.dateFormat();
    }

    @Test
    public void testParseErrorEntry() throws Exception {
        String line = "[2016-10-04 18:46:26,653] WARN  [pool-1-thread-1] - #cache, LOAD_END, NAME: " +
            "persGradeRejectReasons.xml, SOURCE: /home/apershukov/var/cache/persGradeRejectReasons.xml, ERROR: java" +
            ".lang.RuntimeException, ERROR_MSG: \"java.io.FileNotFoundException: " +
            "/home/apershukov/var/cache/persGradeRejectReasons.xml.temp (No such file or directory)\", " +
            "PRIMARY_EXCEPTION: java.io.FileNotFoundException, DURATION: 0, #tm100";
        checker.check(line,
            dateFormat.parse("2016-10-04 18:46:26,653"),
            "hostname.test",
            "persGradeRejectReasons.xml",
            "LOAD",
            true,
            0);
    }

    @Test
    public void testParseCacheEntry() throws Exception {
        String line = "[2016-10-07 08:36:52,294] INFO  [main]  #cache, LOAD_CACHE_END, NAME: georegions.xml, SOURCE: " +
            "/home/apershukov/var/cache/georegions.xml, DURATION: 1232, #tm2k";
        checker.check(line,
            dateFormat.parse("2016-10-07 08:36:52,294"),
            "hostname.test",
            "georegions.xml",
            "LOAD_CACHE",
            false,
            1232);
    }

    @Test
    public void testSkipStartOperationEntry() throws Exception {
        checker.checkEmpty("[2016-10-05 14:57:39,395] DEBUG [pool-1-thread-1] - #cache, DUMP_START, NAME: clients" +
            ".csv, SOURCE: /home/apershukov/var/cache/clients.csv");
    }

    @Test
    public void testSkipIrrelevantEntry() throws Exception {
        checker.checkEmpty("[2016-08-19 08:44:28,740] INFO  [pool-1-thread-1] - #profiler, METHOD: " +
            "ConductorServiceImpl.getGroupHosts, DURATION: 56, #tm100");
    }
}
