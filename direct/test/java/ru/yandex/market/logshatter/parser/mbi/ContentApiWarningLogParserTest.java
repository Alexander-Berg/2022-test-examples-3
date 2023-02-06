package ru.yandex.market.logshatter.parser.mbi;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.DateFormat;

/**
 * Created by apershukov on 12.12.16.
 */
public class ContentApiWarningLogParserTest {

    private LogParserChecker checker;
    private DateFormat dateFormat;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new ContentApiWarningLogParser());
        dateFormat = ContentApiHelper.dateFormat();
    }

    @Test
    public void testParseWithRequestId() throws Exception {
        String line = "[2016-12-12 16:23:00,667] WARN  [requestThreadPool-79] ida1212f333481f Type: UNSUPPORTED_REDIRECT, Message: REPORT_REDIRECT_TYPE: Test type";
        checker.check(line,
            dateFormat.parse("2016-12-12 16:23:00,667"),
            "hostname.test",
            "ida1212f333481f",
            "UNSUPPORTED_REDIRECT",
            "REPORT_REDIRECT_TYPE: Test type");
    }

    @Test
    public void testParseWithoutRequestId() throws Exception {
        String line = "[2016-12-13 09:16:10,363] WARN  [main] - Type: UNSUPPORTED_REDIRECT, Message: REPORT_REDIRECT_TYPE: Test type";
        checker.check(line,
            dateFormat.parse("2016-12-13 09:16:10,363"),
            "hostname.test",
            "-",
            "UNSUPPORTED_REDIRECT",
            "REPORT_REDIRECT_TYPE: Test type");
    }

    @Test
    public void testSkipUnrelatedLine() throws Exception {
        String line = "[2016-12-13 09:16:33,068] INFO  [pool-1-thread-1] - #cache, LOAD_END, NAME: nidHierarchy.csv, SOURCE: /home/apershukov/var/cache/nidHierarchy.csv, DURATION: 0, #tm100";
        checker.checkEmpty(line);
    }
}