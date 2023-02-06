package ru.yandex.market.logshatter.parser.mbo;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author amaslak
 */
public class MboJettyLogParserTest {

    LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new MboJettyLogParser());
        checker.setFile("**/mbo-lite-jetty-health.log");
    }

    @Test
    public void testLine() throws Exception {
        String line = "date=2017-08-15T00:02:07+0300 requests_time_max=5885 responses_1xx=0 requests_time_min=25 " +
            "requests_time_avg=1997 responses_2xx=8 responses_3xx=0 responses_4xx=0 responses_5xx=0 " +
            "requests_received=8 requests_active=0 requests_processed=8 requests_active_max=5";
        SimpleDateFormat dateFormat = new SimpleDateFormat(MboJettyLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("2017-08-15T00:02:07+0300"),
            "hostname.test",
            "mbo-lite",
            8, 8, 0, 5, 25, 1997, 5885, 0, 8, 0, 0, 0);
    }

    @Test
    public void testRejectedLine() throws Exception {
        String line = "date=2017-08-15T00:02:07+0300 requests_time_max=5885 responses_1xx=0 requests_time_min=25 " +
            "requests_time_avg=1997 responses_2xx=8 responses_3xx=0 responses_4xx=0 responses_5xx=0 " +
            "requests_received=8 requests_active=0 requests_processed=8";
        checker.checkEmpty(line);
    }

    @Test
    public void testWrongFile() throws Exception {
        checker.setFile("**/mbo-lite.log");
        String line = "date=2017-08-15T00:02:07+0300 requests_time_max=5885 responses_1xx=0 requests_time_min=25 " +
            "requests_time_avg=1997 responses_2xx=8 responses_3xx=0 responses_4xx=0 responses_5xx=0 " +
            "requests_received=8 requests_active=0 requests_processed=8 requests_active_max=5";
        checker.checkEmpty(line);
    }
}
