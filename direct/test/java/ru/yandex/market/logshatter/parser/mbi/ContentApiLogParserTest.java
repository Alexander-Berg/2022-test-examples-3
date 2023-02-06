package ru.yandex.market.logshatter.parser.mbi;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.DateFormat;

/**
 * Created by tesseract on 10.03.15.
 */
public class ContentApiLogParserTest {

    LogParserChecker checker;
    DateFormat dateFormat;
    ContentApiLogParser parser;

    @Test
    public void httpRequestRetry() throws Exception {
        String line = "[2015-03-10 15:53:10,880] INFO  [defaultEventExecutor-3-1] 03e14c02489c5b #http_request_end, BlackBox, 8e97:2, URL: http://blackbox.yandex.net/blackbox?method=userinfo&userip=213.87.122.76&regname=yes&dbfields=account_info.fio.uid&uid=300401650, RESPONSE_CODE: 200, DURATION: 7, #tm100";
        checker.check(line,
            dateFormat.parse("2015-03-10 15:53:10,880"),
            "hostname.test",
            "03e14c02489c5b",
            "externals",
            "BlackBox",
            7,
            false,
            true);
    }

    @Test
    public void httpRequestWithError() throws Exception {
        String line = "[2015-03-10 15:56:28,822] INFO  [defaultEventExecutor-3-1] 06d14c021334a8 #http_request_end, PersStatic, 9ce6:1, URL: http://pers-static.yandex.net:34522/modelOpinions?page_size=30&page_num=1&sort_by=date&sort_desc=1&modelid=4929085, RESPONSE_CODE: -, ERROR: java.util.concurrent.CancellationException, ERROR_MSG: null, DURATION: 111, #tm200";
        checker.check(line,
            dateFormat.parse("2015-03-10 15:56:28,822"),
            "hostname.test",
            "06d14c021334a8",
            "externals",
            "PersStatic",
            111,
            true,
            false);
    }

    @Test
    public void httpRequestWithoutError() throws Exception {
        String line = "[2015-03-10 15:53:10,880] INFO  [defaultEventExecutor-3-1] 03e14c02489c5b #http_request_end, BlackBox, 8e97:1, URL: http://blackbox.yandex.net/blackbox?method=userinfo&userip=213.87.122.76&regname=yes&dbfields=account_info.fio.uid&uid=300401650, RESPONSE_CODE: 200, DURATION: 7, #tm100";
        checker.check(line,
            dateFormat.parse("2015-03-10 15:53:10,880"),
            "hostname.test",
            "03e14c02489c5b",
            "externals",
            "BlackBox",
            7,
            false,
            false);
    }

    @Test
    public void profiler() throws Exception {
        String line = "[2015-03-10 12:01:15,148] INFO  [requestThreadPool-27] vos14c0277dd25 #profiler, METHOD: XmlView.render, DURATION: 4, #tm100";
        checker.check(line,
            dateFormat.parse("2015-03-10 12:01:15,148"),
            "hostname.test",
            "vos14c0277dd25",
            "profiler",
            "XmlView.render",
            4,
            false,
            false);
    }

    @Test
    public void errorLine() throws Exception {
        String line = "java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.util.concurrent.TimeoutException: Request 9d47 timeout: GET http://marketgurudaemon.yandex.ru:29300/gurudaemon/Filters?use_other_region_if_no_offers=1&currency=RUR&filter-currency=RUR&CAT_ID=969705&region=213&ftype=all";
        checker.checkEmpty(line);
    }


    @Before
    public void setUp() {
        parser = new ContentApiLogParser();
        checker = new LogParserChecker(parser);
        dateFormat = ContentApiHelper.dateFormat();
    }
}
