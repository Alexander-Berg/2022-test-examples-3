package ru.yandex.market.logshatter.parser.strm;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

class StrmNginxRtmpLogParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new StrmNginxRtmpLogParser());
        checker.setHost("testhost");
    }

    @Test
    void parseAccess() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-rtmp-access.log", 0);
        checker.setFile("/logs/nginx/rtmp-access.log");
        checker.check(
            line,

            new Date(1613052307357L), // date
            1613052307.357, // timestamp_ms
            "access", // type
            "testhost", // host

            "2a02:6b8:c08:d912:0:4620:7afd:0", // remote_addr
            "PUBLISH", // command
            "live", // app
            "4967a750099c7d9b81069ccfa336e384", // name
            "", // args
            16504414L, // bytes_received
            807L, // bytes_sent
            "FMLE/3.0 (compatible; Lavf57.83", // flashver
            "", // pageurl
            120L, // session_time

            "", // level
            0L, // connectionId
            "", // event
            "" // message
        );
    }

    @Test
    void parseErrorConnect() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-rtmp-error.log", 0);
        checker.setFile("/logs/nginx/error.log");
        checker.check(
            line,

            new Date(1613046953099L), // date
            1613046953.099, // timestamp_ms
            "error", // type
            "testhost", // host

            "2a02:6b8:c08:d912:0:4620:7afd:0", // remote_addr
            "", // command
            "live", // app
            "", // name
            "", // args
            0L, // bytes_received
            0L, // bytes_sent
            "FMLE/3.0 (compatible; Lavf57.83", // flashver
            "", // pageurl
            0L, // session_time

            "info", // level
            109377L, // connectionId
            "connect", // event
            "connect: app='live' args='' flashver='FMLE/3.0 (compatible; Lavf57.83' swf_url='' tc_url='rtmp://src-ugc" +
                ".tst.strm.yandex.net:1935/live' page_url='' acodecs=0 vcodecs=0 object_encoding=0, client: " +
                "2a02:6b8:c08:d912:0:4620:7afd:0, server: [::]:1935" // message
        );
    }

    @Test
    void parseErrorConnect2() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-rtmp-error.log", 1);
        checker.setFile("/logs/nginx/error.log");
        checker.check(
            line,

            new Date(1613146058541L), // date
            1613146058.541, // timestamp_ms
            "error", // type
            "testhost", // host

            "2a02:6b8:c0b:4a26:0:434f:c91b:0", // remote_addr
            "", // command
            "live", // app
            "", // name
            "", // args
            0L, // bytes_received
            0L, // bytes_sent
            "LNX 10,0,32,18", // flashver
            "", // pageurl
            0L, // session_time

            "info", // level
            1442L, // connectionId
            "connect", // event
            "connect: app='live' args='' flashver='LNX 10,0,32,18' swf_url='' tc_url='rtmp://strm-ugc-src-test-1.sas" +
                ".yp-c.yandex.net:1935/live' page_url='' acodecs=3191 vcodecs=252 object_encoding=0, client: " +
                "2a02:6b8:c0b:4a26:0:434f:c91b:0, server: [::]:1935" // message
        );
    }
}
