package ru.yandex.market.logshatter.parser.java;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.EnvironmentMapper;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 02.08.17
 */
public class JavaParallelGcLogParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() throws Exception {
        checker = new LogParserChecker(new JavaParallelGcLogParser());
        checker.setHost("braavos.market.yandex.net");
        checker.setFile("/var/log/yandex/clickphite/clickphite.log.gc");
        checker.setParam(EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX + checker.getOrigin(), "PRODUCTION");
    }

    @Test
    public void parseUsualGC() throws Exception {
        checker.check(
            "2017-08-01T18:05:05.526+0300: 83667.799: [GC (Allocation Failure) [PSYoungGen: 7344344K->558770K" +
                "(7626752K)] 20293518K->13699987K(24403968K), 0.2875775 secs] [Times: user=6.13 sys=0.00, real=0.29 " +
                "secs] ",
            new Date(1501599905526L),
            "braavos.market.yandex.net",
            "clickphite",
            JavaParallelGcLogParser.GcType.GC,
            JavaParallelGcLogParser.Cause.ALLOCATION_FAILURE,
            7344344,
            558770,
            7626752,
            -1,
            -1,
            -1,
            20293518,
            13699987,
            24403968,
            287577,
            6130,
            0,
            290,
            Environment.PRODUCTION
        );
    }

    //После времени может быть число с запятой, а не точкой.
    @Test
    public void parseUsualGCWithComma() throws Exception {
        checker.check(
            "2017-08-01T18:05:05.526+0300: 83667,799: [GC (Allocation Failure) [PSYoungGen: 7344344K->558770K" +
                "(7626752K)] 20293518K->13699987K(24403968K), 0,2875775 secs] [Times: user=6,13 sys=0,00, real=0,29 " +
                "secs] ",
            new Date(1501599905526L),
            "braavos.market.yandex.net",
            "clickphite",
            JavaParallelGcLogParser.GcType.GC,
            JavaParallelGcLogParser.Cause.ALLOCATION_FAILURE,
            7344344,
            558770,
            7626752,
            -1,
            -1,
            -1,
            20293518,
            13699987,
            24403968,
            287577,
            6130,
            0,
            290,
            Environment.PRODUCTION
        );
    }

    @Test
    public void parseFullGC() throws Exception {
        checker.check(
            "2017-08-01T18:08:21.929+0300: 83864.203: [Full GC (Ergonomics) [PSYoungGen: 485143K->0K(7435264K)] " +
                "[ParOldGen: 16556957K->925728K(16777216K)] 17042100K->925728K(24212480K), [Metaspace: 42757K->42757K" +
                "(1087488K)], 0.6542757 secs] [Times: user=10.82 sys=0.04, real=0.66 secs]",
            new Date(1501600101929L),
            "braavos.market.yandex.net",
            "clickphite",
            JavaParallelGcLogParser.GcType.FULL_GC,
            JavaParallelGcLogParser.Cause.ERGONOMICS,
            485143,
            0,
            7435264,
            16556957,
            925728,
            16777216,
            17042100,
            925728,
            24212480,
            654275,
            10820,
            40,
            660,
            Environment.PRODUCTION
        );
    }

    @Test
    public void getModuleName() throws Exception {
        Assertions.assertEquals(
            "clickphite", JavaParallelGcLogParser.getModuleName("clickphite.log.gc")
        );

        Assertions.assertEquals(
            "tsum-tms", JavaParallelGcLogParser.getModuleName("tsum-tms.gc.log.0.current")
        );

        Assertions.assertEquals(
            "tsum-tms", JavaParallelGcLogParser.getModuleName("tsum-tms.gc.log.3.current")
        );
    }
}
