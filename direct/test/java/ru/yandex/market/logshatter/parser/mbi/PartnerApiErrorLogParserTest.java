package ru.yandex.market.logshatter.parser.mbi;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

public class PartnerApiErrorLogParserTest {

    @Test
    public void testParse() throws Exception {
        String line = "[2015-03-20 18:46:55 +0300]\tREPORT\tReportException\tIO";
        String line2 = "[2015-03-23 10:46:39 +0300]\tUNKNOWN\tNoSuchRequestHandlingMethodException\tNo matching handler method found\t1511869305697/a966a1d5b0a6a7aabc5ea0f419b2cdce";

        LogParserChecker checker = new LogParserChecker(new PartnerApiErrorLogParser());

        checker.check(
                line,
                new Date(1426866415000L), checker.getHost(), "REPORT", "ReportException", "IO", "0/-"
        );

        checker.check(
                line2,
                new Date(1427096799000L), checker.getHost(), "UNKNOWN", "NoSuchRequestHandlingMethodException", "No matching handler method found",
                "1511869305697/a966a1d5b0a6a7aabc5ea0f419b2cdce"
        );

        checker.check(
                ""
        );
    }
}