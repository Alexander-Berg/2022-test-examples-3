package ru.yandex.market.logshatter.parser.mbi;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.DateFormat;

/**
 * Created by tesseract on 10.03.15.
 */
public class ContentApiGcLogParserTest {

    LogParserChecker checker;
    DateFormat dateFormat;

    @Test
    public void gcLine() throws Exception {
        String line = "[2015-03-10 14:30:58,644] INFO  [Service Thread]  #gc, NAME: parnew, ACTION: endOfMinorGC, CAUSE: allocationFailure, ID: 2614, DURATION: 11, PAR-SURVIVOR-SPACE: 6217K->8600K, CODE-CACHE: 53M->53M, COMPRESSED-CLASS-SPACE: 6521K->6521K, METASPACE: 56M->56M, PAR-EDEN-SPACE: 273M->0, CMS-OLD-GEN: 538M->537M";
        checker.check(line,
            dateFormat.parse("2015-03-10 14:30:58,644"),
            "hostname.test",
            "endOfMinorGC",
            11,
            537 << 10);
    }

    @Test
    public void gcLineG1() throws Exception {
        String line = "[2016-01-27 15:23:19,566] INFO  [Service Thread]  #gc, NAME: g1YoungGeneration, ACTION: endOfMinorGC, CAUSE: g1EvacuationPause, ID: 82, DURATION: 35, OLD-GEN: 1277M->1278M, CODE-CACHE: 58M->58M, SURVIVOR-SPACE: 100M->102M, COMPRESSED-CLASS-SPACE: 6969K->6969K, METASPACE: 59M->59M, EDEN-SPACE: 2100M->0";
        checker.check(line,
            dateFormat.parse("2016-01-27 15:23:19,566"),
            "hostname.test",
            "endOfMinorGC",
            35,
            1278 << 10);
    }


    @Before
    public void setUp() {
        checker = new LogParserChecker(new ContentApiGcLogParser());
        dateFormat = ContentApiHelper.dateFormat();
    }
}
