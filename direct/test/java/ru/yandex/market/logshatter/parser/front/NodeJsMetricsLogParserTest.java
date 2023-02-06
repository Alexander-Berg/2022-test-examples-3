package ru.yandex.market.logshatter.parser.front;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author Roman Garanko<a href="mailto:mrgrien@yandex-team.ru"></a>
 * @date 22/04/16
 */
public class NodeJsMetricsLogParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new NodeJsMetricsLogParser());
    }

    @Test
    public void parseNodeJsMetrics() throws Exception {
        String line = "2016/02/17 13:12:01 +0300\tpid:24253\tvsize:1453191168\trss:445165568";
        checker.setFile("/var/log/yandex/market-skubi/metrics.log");
        checker.check(
            line,
            new Date(1455703921000L), checker.getHost(), 1453191168L, 445165568, ""
        );
    }

    @Test
    public void parseNodeJsMetricsByExperiment() throws Exception {
        String line = "2016/02/17 13:04:01 +0300\tpid:24253\tvsize:1455288320\trss:448692224";
        checker.setFile("/var/log/yandex/market-skubi-exp/experiment1/metrics.log");
        checker.check(
            line,
            new Date(1455703441000L), checker.getHost(), 1455288320L, 448692224, "experiment1"
        );
    }

    @Test
    public void parseNodeJsMetricsWithLimitValues() throws Exception {
        Long maxUInt = 4294967295L;
        String line = "2016/02/17 13:12:01 +0300\tpid:24253\tvsize:4294967295\trss:2127483647";
        checker.setFile("/var/log/yandex/market-skubi/metrics.log");
        checker.check(
                line,
                new Date(1455703921000L), checker.getHost(), maxUInt, 2127483647, ""
        );
    }

}