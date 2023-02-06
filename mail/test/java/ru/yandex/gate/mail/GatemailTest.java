package ru.yandex.gate.mail;

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
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.TestBase;

public class GatemailTest extends TestBase {
    private static final String LOCALHOST = "http://localhost:";

    Server gatemail;
    StaticServer smtpGate;
    StaticServer blackbox;

    @Before
    public void setupGatemail() throws IOException, ConfigException {
        smtpGate = new StaticServer(Configs.baseConfig("smtpGate"));
        smtpGate.start();
        blackbox = new StaticServer(Configs.baseConfig("blackbox"));
        blackbox.start();

        IniConfig config = new IniConfig(
            new InputStreamReader(
                getClass().getResourceAsStream("test.conf"),
                StandardCharsets.UTF_8));
        config.put("smtpgate.host", LOCALHOST + smtpGate.port());
        config.put("blackbox.host", LOCALHOST + blackbox.port());
        gatemail = new Server(new Config(config));
        gatemail.start();
    }

    @Test
    public void testSimple() throws IOException, InterruptedException, BadRequestException {
        final String smtpHandle = "/mail/store?fid=1&uid=610&service=iex";
        smtpGate.add(smtpHandle, HttpStatus.SC_OK);
        final String blackboxHandle =
            "/blackbox?method=userinfo&userip=127.0.0.1"
            + "&login=dst@yandex.ru&format=json";
        blackbox.add(
            blackboxHandle,
            "{\"users\":[{\"uid\":{\"value\":\"610\"}}]}");

        QueryConstructor qc =
            new QueryConstructor(LOCALHOST + gatemail.port() + "/send-mail?");
        qc.append("from", "src@yandex.ru");
        qc.append("to", "dst@yandex.ru");
        qc.append("subject", "Минимальный запрос");
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(qc.toString()))
            .POST(HttpRequest.BodyPublishers.ofString("MESSAGE BODY"))
            .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
            .send(req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(resp.body(), HttpStatus.SC_OK, resp.statusCode());
        // TODO: check request multipart body
        Assert.assertEquals(
            "smtp gate requests", 1, smtpGate.accessCount(smtpHandle));

        // Check blackbox reuse
        resp = HttpClient.newHttpClient()
            .send(req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(resp.body(), HttpStatus.SC_OK, resp.statusCode());
        Assert.assertEquals(
            "smtp gate requests", 2, smtpGate.accessCount(smtpHandle));
        Assert.assertEquals(
            "blackbox requests", 1, blackbox.accessCount(blackboxHandle));

    }

    @Test
    public void testSpamReport()
        throws IOException, InterruptedException, BadRequestException
    {
        final String handle =
            "/mail/store?fid=1&uid=991949281&service=iex";
        smtpGate.add(handle, HttpStatus.SC_OK);
        blackbox.add(
            "/blackbox?method=userinfo&userip=127.0.0.1"
                + "&login=so-compains@yandex.ru&format=json",
            "{\"users\":[{\"uid\":{\"value\":\"991949281\"}}]}");

        QueryConstructor qc =
            new QueryConstructor(LOCALHOST + gatemail.port() + "/send-mail?");
        qc.append("to", "so-compains@yandex.ru");
        qc.append("msg-to", "hirthwork@yandex.ru,+588355978@uid.ya");
        qc.append("from", "potapov.d@gmail.com");
        qc.append("subject", "2020.04.20+19:59:03:+%5BST%5D%5BSpam%5Dunseen+email");
        qc.append("header", "Date:+Mon,+20+Apr+2020+19:59:03+%2B0300");
        qc.append("header", "knn-dist:+10.0");
        qc.append("header", "X-Yandex-Timestamp:+1587401943");
        qc.append("header", "Message-Id:+uid:588355978/stid:1.632123143.7594801846142779115218810981");
        qc.append("header", "x-yandex-complainer-uid:+588355978");
        qc.append("header", "x-yandex-stid:+1.632123143.7594801846142779115218810981");
        qc.append("header", "x-yandex-mid:+159314836818238392");
        qc.append("header", "x-forward-type:+test_type");
        qc.append("header", "x-folder-name:+Spam");
        qc.append("types", "54,40");
        qc.append("labels", "important");
        qc.append("content-type", "multipart/mixed;+boundary%3Dmy_lucky_boundary;+charset%3DUTF-8");
        qc.append("raw", "true");
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(qc.toString()))
            .POST(HttpRequest.BodyPublishers.ofInputStream(
                () -> getClass().getResourceAsStream("spam-report-body")))
            .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(resp.body(), HttpStatus.SC_OK, resp.statusCode());
        Assert.assertEquals(
            "smtp gate requests", 1, smtpGate.accessCount(handle));
    }
}
