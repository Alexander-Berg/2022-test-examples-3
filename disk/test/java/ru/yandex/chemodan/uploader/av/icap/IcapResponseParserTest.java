package ru.yandex.chemodan.uploader.av.icap;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class IcapResponseParserTest {
    public static final String NOT_MODIFIED_SUCCESS_RESPONSE =
            "ICAP/1.0 204 Not modified\r\n" +
            "ISTag: \"4-5-3-28936\"\r\n" +
            "Connection: close\r\n" +
            "Encapsulated: null-body=0\r\n" +
            "\r\n";

    public static final String INFECTED_RESPONSE =
            "ICAP/1.0 200 OK\r\n" +
            "ISTag: \"4-5-3-28936\"\r\n" +
            "Connection: close\r\n" +
            "Encapsulated: res-hdr=0, res-body=87\r\n" +
            "\r\n" +
            "HTTP/1.1 403 Blocked\r\n" +
            "Content-Length: 8\r\n" +
            "Content-Type: text/html\r\n" +
            "Connection: close\r\n" +
            "\r\n" +
            "8\r\n" +
            "infected\r\n" +
            "0\r\n\r\n";

    public static final String NOTSCANNED_RESPONSE =
            "ICAP/1.0 200 OK\r\n" +
            "ISTag: \"4-5-3-28963\"\r\n" +
            "Connection: close\r\n" +
            "Encapsulated: res-hdr=0, res-body=88\r\n" +
            "\r\n" +
            "HTTP/1.1 403 Blocked\r\n" +
            "Content-Length: 10\r\n" +
            "Content-Type: text/html\r\n" +
            "Connection: close\r\n" +
            "\r\n" +
            "a\r\n" +
            "notscanned\r\n" +
            "0\r\n\r\n";

    public static final String YAVS_ANTIVIRUS_INFECTED_RESPONSE =
        "ICAP/1.0 200 OK\r\n" +
        "Server: C-ICAP/1.0\r\n" +
        "Connection: keep-alive\r\n" +
        "ISTag: CI0001YAVS\r\n" +
        "Encapsulated: res-hdr=0, res-body=383\r\n\r\n" +
        "HTTP/1.0 403 Forbidden\r\n" +
        "X-Infection-Found: Type=0; Resolution=0; Threat=EICAR_test_file\r\n" +
        "X-Virus-ID: EICAR_test_file\r\n" +
        "Date: Tue Mar 03 2020 16:08:30 GMT\r\n" +
        "Last-Modified: Tue Mar 03 2020 16:08:30 GMT\r\n" +
        "Content-Length: 8\r\n" +
        "Via: ICAP/1.0 (C-ICAP/1.0 Yandex AV Service)\r\n" +
        "Content-Type: text/html\r\n" +
        "Connection: close\r\n\r\n" +
        "a\r\n" +
        "infected\r\n" +
        "Via: ICAP/1.0 uploader2h.dst.yandex.net (C-ICAP/1.0 Yandex AV Service )\r\n" +
        "\r\n\r\n";

    public static final String YAVS_ANTIVIRUS_HEALTHY_RESPONSE = "ICAP/1.0 200 OK\r\n" +
        "Server: C-ICAP/1.0\r\n" +
        "Connection: keep-alive\r\n" +
        "ISTag: CI0001YAVS\r\n" +
        "Encapsulated: res-hdr=0, res-body=282\r\n\r\n" +
        "HTTP/1.0 200 OK\r\n" +
        "Date: Thu Mar 05 2020 08:32:19 GMT\r\n" +
        "Last-Modified: Thu Mar 05 2020 08:32:19 GMT\r\n" +
        "Content-Length: 5\r\n" +
        "Via: ICAP/1.0 (C-ICAP/1.0 Yandex AV Service)\r\n" +
        "Content-Type: text/html\r\n" +
        "Connection: close\r\n" +
        "Via: ICAP/1.0 uploader2h.dst.yandex.net (C-ICAP/1.0 Yandex AV Service )\r\n\r\n";

    @Test
    public void parseIcapStatusCode() {
        IcapResponseParser parser = new IcapResponseParser();
        Assert.equals(204, parser.parseIcapStatusCode(NOT_MODIFIED_SUCCESS_RESPONSE));
        Assert.equals(200, parser.parseIcapStatusCode(INFECTED_RESPONSE));
        Assert.equals(200, parser.parseIcapStatusCode(NOTSCANNED_RESPONSE));
    }

    @Test
    public void parseHttpStatusCode() {
        IcapResponseParser parser = new IcapResponseParser();
        Assert.none(parser.parseHttpStatusCode(NOT_MODIFIED_SUCCESS_RESPONSE));
        Assert.some(403, parser.parseHttpStatusCode(INFECTED_RESPONSE));
        Assert.some(403, parser.parseHttpStatusCode(NOTSCANNED_RESPONSE));
    }

    @Test
    public void parseBlockReason() {
        IcapResponseParser parser = new IcapResponseParser();
        Assert.none(parser.parseBlockReason(NOT_MODIFIED_SUCCESS_RESPONSE));
        Assert.some("infected", parser.parseBlockReason(INFECTED_RESPONSE));
        Assert.some("notscanned", parser.parseBlockReason(NOTSCANNED_RESPONSE));
    }

    @Test
    public void parseYavsInfectedResponse() {
        IcapResponseParser parser = new IcapResponseParser();
        Assert.equals(200, parser.parseIcapStatusCode(YAVS_ANTIVIRUS_INFECTED_RESPONSE));
        Assert.some(403, parser.parseHttpStatusCode(YAVS_ANTIVIRUS_INFECTED_RESPONSE));
        Assert.some("infected", parser.parseBlockReason(YAVS_ANTIVIRUS_INFECTED_RESPONSE));
    }

    @Test
    public void parseYavsHealthyResponse() {
        IcapResponseParser parser = new IcapResponseParser();
        Assert.equals(200, parser.parseIcapStatusCode(YAVS_ANTIVIRUS_HEALTHY_RESPONSE));
        Assert.some(200, parser.parseHttpStatusCode(YAVS_ANTIVIRUS_HEALTHY_RESPONSE));
    }

}
