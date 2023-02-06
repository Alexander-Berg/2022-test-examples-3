package ru.yandex.market.logshatter.parser.mbo;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

public class MboCategoryLogParserTest {

    LogParserChecker checker;
    static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    @Test
    public void testLine() throws Exception {
        String line = "2022-06-29 16:47:45,001 DEBUG [RequestLoggerHandler qtp1756287901-256 GET /actuator/prometheus" +
            " 1656510465001/682d4cb595346caef92820bdd1fe8828] Processing Market-Req-ID: " +
            "1656510465001/682d4cb595346caef92820bdd1fe8828 url: GET /actuator/prometheus";
        String host = "market_mbo_category";
        SimpleDateFormat dateFormat = new SimpleDateFormat(MboGwtLogParser.DATE_PATTERN);

        checker.setHost(host);
        checker.setLogBrokerTopic("market-mbo@prod@logs--mbo_category_logs_topic");
        checker.check(line,
            dateFormat.parse("2022-06-29 16:47:45,001"),
            LocalDateTime.parse("2022-06-29 16:47:45,001", DATE_TIME_FORMATTER),
            "mbo",
            "mbo_category",
            line,
            "prod",
            "",
            Level.DEBUG,
            host,
            "",
            "",
            "1656510465001/682d4cb595346caef92820bdd1fe8828",
            "",
            "",
            "RequestLoggerHandler",
            UUID.nameUUIDFromBytes(line.getBytes()),
            "",
            "");
    }

    @Test
    public void testLineWithAdditionalSpaces() throws Exception {
        String line = "2022-06-29 16:47:45,001 INFO  [RequestLoggerHandler qtp1756287901-256 GET /actuator/prometheus" +
            " 1656510465001/682d4cb595346caef92820bdd1fe8828] Processing Market-Req-ID: " +
            "1656510465001/682d4cb595346caef92820bdd1fe8828 url: GET /actuator/prometheus";
        String host = "market_mbo_category";
        SimpleDateFormat dateFormat = new SimpleDateFormat(MboGwtLogParser.DATE_PATTERN);

        checker.setHost(host);
        checker.setLogBrokerTopic("market-mbo@prod@logs--mbo_category_logs_topic");
        checker.check(line,
            dateFormat.parse("2022-06-29 16:47:45,001"),
            LocalDateTime.parse("2022-06-29 16:47:45,001", DATE_TIME_FORMATTER),
            "mbo",
            "mbo_category",
            line,
            "prod",
            "",
            Level.INFO,
            host,
            "",
            "",
            "1656510465001/682d4cb595346caef92820bdd1fe8828",
            "",
            "",
            "RequestLoggerHandler",
            UUID.nameUUIDFromBytes(line.getBytes()),
            "",
            "");
    }

    @Test
    public void testLargeLine() throws Exception {
        String line = "2022-06-29 16:47:45,001 DEBUG [RequestLoggerHandler qtp1756287901-256 GET /actuator/prometheus" +
            " 1656510465001/682d4cb595346caef92820bdd1fe8828] Processing Market-Req-ID: " +
            "1656510465001/682d4cb595346caef92820bdd1fe8828 url: GET /actuator/prometheus\n" +
            "nobody@testing-market-mbo-category-sas-1:/place/db/iss3/instances/testing-market-mbo-category-sas" +
            "-1_testing_market_mbo_category_sas_tVJOWlDSLDU/logs/mboc-app$ tail -n10 mbo-category.log\n" +
            "\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:883) ~[jetty-util-9.4" +
            ".44.v20210927.jar:9.4.44.v20210927]\n" +
            "\tat org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:1034) " +
            "~[jetty-util-9.4.44.v20210927.jar:9.4.44.v20210927]\n" +
            "\tat java.lang.Thread.run(Thread.java:829) [?:?]";

        String host = "market_mbo_category";
        SimpleDateFormat dateFormat = new SimpleDateFormat(MboGwtLogParser.DATE_PATTERN);

        checker.setHost(host);
        checker.setLogBrokerTopic("market-mbo@prod@logs--mbo_category_logs_topic");
        checker.check(line,
            dateFormat.parse("2022-06-29 16:47:45,001"),
            LocalDateTime.parse("2022-06-29 16:47:45,001", DATE_TIME_FORMATTER),
            "mbo",
            "mbo_category",
            line,
            "prod",
            "",
            Level.DEBUG,
            host,
            "",
            "",
            "1656510465001/682d4cb595346caef92820bdd1fe8828",
            "",
            "",
            "RequestLoggerHandler",
            UUID.nameUUIDFromBytes(line.getBytes()),
            "",
            "");
    }

    @Test
    public void nothingWrittenWhenLogIsInvalid() throws Exception {
        String line = "ababsd bb fgsfg [  gsdfg45";
        checker.checkEmpty(line);

        String line2 = "0000-00-00 00:00:00,000 bb fgsfg [  gsdfg45] ";
        checker.checkEmpty(line2);
    }

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new MboCategoryLogParser());
    }
}
