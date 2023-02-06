package ru.yandex.market.logshatter.parser.tablo;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TabloHttpdAccessLogParserTest {

    @Test
    public void regexMustMatchLogLine() {
        String logLine = "cubes-tablo.market.yandex.net 2a02:6b8:c02:45a:0:577:e299:ec6c - 2019-10-23T10:25:04.112 " +
            "\"+0300\" 80 \"POST /vizql/w/_303/v/sheet0_1/bootstrapSession/sessions/756D8BA5F07C45FCAB0314330BE3B347" +
            "-6:2 HTTP/1.1\" \"2a02:6b8:0:419:25ff:d12c:e843:1bc0\" 503 406 \"882\" 906045989 " +
            "XbAAUNU@WFgGAUnN1-5ibgAAAHE";
        TabloHttpdAccessLogParser parser = new TabloHttpdAccessLogParser();
        Matcher m = parser.match(logLine);
        assertTrue(m.find());
        assertEquals(m.groupCount(), 5);
        assertEquals(m.group(1), "2019-10-23T10:25:04.112");
        assertEquals(m.group(2), "POST /vizql/w/_303/v/sheet0_1/bootstrapSession/sessions" +
            "/756D8BA5F07C45FCAB0314330BE3B347-6:2 HTTP/1.1");
        assertEquals(m.group(3), "503");
        assertEquals(m.group(4), "406");
        assertEquals(m.group(5), "906045989");
    }

    @Test
    public void mustParseDate() throws ParseException {
        TabloHttpdAccessLogParser parser = new TabloHttpdAccessLogParser();
        Date d = parser.parseDate("2019-10-23T10:25:04.112");
        assertEquals(1571815504112L, d.getTime());
    }

    @Test
    public void mustParseRequest() {
        TabloHttpdAccessLogParser parser = new TabloHttpdAccessLogParser();
        String[] reqParts = parser.parseRequest("POST /vizql/w/_303/v/sheet0_1/bootstrapSession/sessions" +
            "/756D8BA5F07C45FCAB0314330BE3B347-6:2 HTTP/1.1");
        assertTrue(reqParts.length >= 2);
        assertEquals("POST", reqParts[0]);
        assertEquals("/vizql/w/_303/v/sheet0_1/bootstrapSession/sessions/756D8BA5F07C45FCAB0314330BE3B347-6:2",
            reqParts[1]);
    }
}
