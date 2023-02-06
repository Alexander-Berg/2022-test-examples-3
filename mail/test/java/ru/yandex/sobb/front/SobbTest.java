package ru.yandex.sobb.front;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class SobbTest extends TestBase {
    private static final String LOCALHOST = "http://localhost:";

    Server sobbServer;
    StaticServer blackbox;
    StaticServer msearch;

    @Before
    public void setupCluster() throws IOException, ConfigException {
        blackbox = new StaticServer(Configs.baseConfig("blackbox"));
        blackbox.start();
        msearch = new StaticServer(Configs.baseConfig("msearch"));
        msearch.start();

        IniConfig config = new IniConfig(
            new InputStreamReader(
                getClass().getResourceAsStream("test.conf"),
                StandardCharsets.UTF_8));
        config.put("blackbox.host", LOCALHOST + blackbox.port());
        config.put("msearch.host", LOCALHOST + msearch.port());
        sobbServer = new Server(new Config(config));
        sobbServer.start();
    }

    @Test
    public void testUuids() throws IOException, InterruptedException {
        blackbox.add(
            "/blackbox?method=sessionid&host=yandex.ru&get_user_ticket=yes"
            + "&format=json&userip=127.0.0.1&sessionid=scookie",
            "{\"status\":{\"id\":0}, \"uid\": {\"value\": \"4001517835\"},"
            + " \"user_ticket\": \"3:user:ticket\"}");

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(LOCALHOST + sobbServer.port() + "/uuids"))
            .header(YandexHeaders.SESSION_ID, "scookie")
            .GET()
            .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
            .send(req, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(HttpStatus.SC_OK, resp.statusCode());
        Assert.assertEquals(
            "Qh_XyW53xCl4VXSBVRRdafg\n"
            + "QE2DX_V7YHddR4_nrIgrybg\n"
            + "QNnjKnuUBjiE6Ve5lwwQczw\n"
            + "QfRIFfsxDAbbGE0SlDY6uQA\n"
            + "QhTtyHpFK77rEaZF-7NrDug\n"
            + "QdROIznNu3opNOTxAqS8Dww\n"
            + "QgJgA0w7kSKzwoKgZYSlLxg\n"
            + "Q0rNSJGOyS3dzALbhQyziCQ\n"
            + "QEja7qvgCSdwCZ5duPZiesw\n"
            + "QfYDllj_cMV4ZQCBKAwD8NA\n"
            + "Qu9i0uE9c3gyBgIPPtt9RRg\n"
            + "Qob3uIRxz74neshMeqi0BaQ\n"
            + "QyuzSfFb8XI7msSrSQwaf5w\n"
            + "QE_JzR5N16mkRA34N1GeTUw\n"
            + "Q-YjmjSExKPRpL_JIDD38aw\n"
            + "QReCvirK9CooETG1QFe3OTQ\n"
            + "QgsH7fyjruN5VPkDUSxEawQ\n"
            + "QM3nO_QTswlhnqzbN-rGaQw\n"
            + "QmShkL-uvwEh94FcMgunJcA\n"
            + "Q7fd1E52dfTUeZvwyDoSQXw\n"
            + "QiT3ll2HzIS4_FB6JaEcQug\n"
            + "QWMFfmd7wB_GhtGfL-luD6w\n"
            + "QovUJj0FInDI_Eywrbvskew\n"
            + "Q9iv4BK8FDWp7tiuwPAoMag\n"
            + "QgcPo9g5iWPRotyhFO-wNHw",
            resp.body());
    }

    @Test
    public void testUuidsUnauthorised()
        throws IOException, InterruptedException
    {
        blackbox.add(
            "/blackbox?method=sessionid&host=yandex.ru&get_user_ticket=yes"
            + "&format=json&userip=127.0.0.1&sessionid=scookie",
            "{\"status\":{\"id\":2}}");

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(LOCALHOST + sobbServer.port() + "/uuids"))
            .header(YandexHeaders.SESSION_ID, "scookie")
            .GET()
            .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
            .send(req, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED, resp.statusCode());
        Assert.assertEquals("", resp.body());
    }



    @Test
    public void testScore()
        throws IOException, InterruptedException
    {
        blackbox.add(
            "/blackbox?method=sessionid&host=yandex.ru&get_user_ticket=yes"
            + "&format=json&userip=127.0.0.1&sessionid=scookie",
            "{\"status\":{\"id\":0}, \"uid\": {\"value\": \"4001517835\"},"
            + " \"user_ticket\":\"3:user:ticket\"}");
        String report = "{\n"
            + " \"all_messages\": 10,\n"
            + " \"inbox_messages\": 5,\n"
            + " \"report_time\": 1603002001\n}";
        msearch.add(
            "/api/async/so/sobb-get-score",
            new ExpectingHeaderHttpItem(
                new StaticHttpItem(report),
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:ticket"));

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(LOCALHOST + sobbServer.port() + "/score"))
            .header(YandexHeaders.SESSION_ID, "scookie")
            .GET()
            .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
            .send(req, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(HttpStatus.SC_OK, resp.statusCode());
        Assert.assertEquals(report, resp.body());
    }

}
