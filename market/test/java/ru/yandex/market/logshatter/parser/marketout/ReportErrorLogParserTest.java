package ru.yandex.market.logshatter.parser.marketout;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Oleg Makovski <a href="mailto:omakovski@yandex-team.ru"></a>
 * @date 25/11/16
 */
public class ReportErrorLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new ReportErrorLogParser());
        // Extra message line containing stack trace
        checker.checkEmpty("TBackTrace::Capture()+32 (0x3B04C80)");
        // Typical log line with error code and message
        checker.check(
            "tskv\ttskv_format=market-report-error-log\tpid=4912\tenv=production\tlocation=sas\tsub_role=blue-shadow" +
                "\tcluster=1\thost=2\ttimestamp=2016-11-25T15:21:48+0300\tcategory=REPORT\turl_hash=\tcode=1010" +
                "\tmessage=Nonexistent category with mandatory tag age: " +
                "[529119]\tplace=prime\trgb=GREEN\tclient=pokupki" +
                ".touch\tclient_page_id=blue-market_product\tclient_scenario=fetchSkus\tis_suspicious=1\tsource_role" +
                "=parallel\tcloud_service=int_man",
            new Date(1480076508000L), checker.getHost(), 1010, "REPORT", "", "", "production", 1, "Nonexistent " +
                "category with mandatory tag age: [529119]", "prime", "white", "pokupki.touch",
            "blue-market_product", "fetchSkus", true, "parallel", "int_man"
        );
        // Missing error code
        checker.check(
            "tskv\ttskv_format=market-report-error-log\tpid=4912\tenv=production\tlocation=sas\tsub_role=blue-shadow" +
                "\tcluster=1\thost=2\ttimestamp=2016-11-25T15:21:48+0300\tcategory=REPORT\turl_hash=\tmessage" +
                "=Nonexistent category with mandatory tag age: [529119]\tplace=prime\trgb=BLUE\tsource_role=bk",
            new Date(1480076508000L), checker.getHost(), 0, "REPORT", "", "", "production", 1, "Nonexistent category " +
                "with mandatory tag age: [529119]", "prime", "blue", "", "", "", false, "bk", ""
        );
        // Log line with data_file
        checker.check(
            "tskv\ttskv_format=market-report-error-log\tpid=4912\tenv=production\tlocation=sas\tsub_role=blue-shadow" +
                "\tcluster=1\thost=2\ttimestamp=2016-11-25T15:21:48+0300\tcategory=REPORT\turl_hash" +
                "=58cdca8725466c989f8ddb40774aa7e1\tdata_file=/var/lib/search/report-data/categories" +
                ".xml\tmessage=Nonexistent category with mandatory tag age: " +
                "[529119]\tplace=prime\tsource_role=blue-shadow",
            new Date(1480076508000L), checker.getHost(), 0, "REPORT", "/var/lib/search/report-data/categories.xml",
            "58cdca8725466c989f8ddb40774aa7e1", "production", 1, "Nonexistent category with mandatory tag age: " +
                "[529119]", "prime", "", "", "", "", false, "blue-shadow", ""
        );
        // Missing message
        checker.check(
            "tskv\ttskv_format=market-report-error-log\tpid=4912\tenv=production\tlocation=sas\tsub_role=blue-shadow" +
                "\tcluster=1\thost=2\ttimestamp=2016-11-25T15:21:48+0300\tcategory=REPORT\turl_hash=\tcode=1010" +
                "\tplace=prime",
            new Date(1480076508000L), checker.getHost(), 1010, "REPORT", "", "", "production", 1, "", "prime", "",
            "", "", "", false, "", ""
        );
        // Missing place
        checker.check(
            "tskv\ttskv_format=market-report-error-log\tpid=4912\tenv=production\tlocation=sas\tsub_role=blue-shadow" +
                "\tcluster=1\thost=2\ttimestamp=2016-11-25T15:21:48+0300\tcategory=REPORT\turl_hash=\tcode=1010" +
                "\tmessage=Nonexistent category with mandatory tag age: [529119]",
            new Date(1480076508000L), checker.getHost(), 1010, "REPORT", "", "", "production", 1, "Nonexistent " +
                "category with mandatory tag age: [529119]", "", "", "", "", "", false, "", ""
        );
        // Ignore lines during warming-up
        checker.checkEmpty(
            "tskv\ttskv_format=market-report-error-log\tpid=4912\tenv=production\tlocation=sas\tsub_role=blue-shadow" +
                "\tcluster=1\thost=2\ttimestamp=2016-11-25T15:21:48+0300\tcategory=REPORT\turl_hash=\tcode=1010" +
                "\tmessage=Nonexistent category with mandatory tag age: " +
                "[529119]\tplace=prime\trgb=GREEN\tclient=pokupki" +
                ".touch\tclient_page_id=blue-market_product\tclient_scenario=fetchSkus\tis_suspicious=1" +
                "\tis_warming_up=1"
        );
    }
}
