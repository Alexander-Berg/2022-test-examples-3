package ru.yandex.market.logshatter.parser.mbo;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author amaslak
 */
public class MboGwtLogParserTest {

    LogParserChecker checker;

    @Test
    public void testLine() throws Exception {
        String line = "2015-02-12 07:15:59,883 INFO   [AutoWiringServlet VisualServiceRemoteImpl" +
            ".getMboData-eadb77-1843] Processing time is 10 ms";
        SimpleDateFormat dateFormat = new SimpleDateFormat(MboGwtLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("2015-02-12 07:15:59,883"),
            "hostname.test",
            "VisualServiceRemoteImpl",
            "getMboData",
            "1843",
            10);
    }

    @Test
    public void testRejectedLine() throws Exception {
        String line = "2015-06-09 11:27:58,749 DEBUG  [TovarTreeService TovarTreeServiceRemoteImpl" +
            ".getWholePublishedTree-1] category names Processing time is 6583 ms";
        checker.checkEmpty(line);
    }


    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new MboGwtLogParser());
    }
}
