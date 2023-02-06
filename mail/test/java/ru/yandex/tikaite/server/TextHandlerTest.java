package ru.yandex.tikaite.server;

import java.io.File;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class TextHandlerTest extends TestBase {
    private static final String LOCALHOST = "http://localhost:";
    private static final String CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final String SINDBAD = "sindbad.pdf";

    @Test
    public void testSindbadPdf() throws Exception {
        try (StaticServer staticServer =
                new StaticServer(Configs.baseConfig());
            Server server =
                new Server(
                    ServerTest.getConfig(
                        staticServer.port(),
                        "\nstorage.uri-suffix = service=tikaite"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            staticServer.add(
                "/get/sindbad?raw&service=tikaite",
                new File(getClass().getResource(SINDBAD).toURI()));
            staticServer.start();
            server.start();
            HttpResponse response = client.execute(
                new HttpGet(LOCALHOST + server.port() + "/text?stid=sindbad&"
                    + "mimetype=application/octet-stream"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpEntity body = response.getEntity();
            Assert.assertEquals(
                CONTENT_TYPE,
                body.getContentType().getValue());
            String text = EntityUtils.toString(body);
            YandexAssert.assertContains("Маршрутная квитанция", text);
        }
    }

    @Test
    public void testSindbadPdfTruncated() throws Exception {
        try (StaticServer staticServer =
                new StaticServer(Configs.baseConfig());
            Server server =
                new Server(
                    ServerTest.getConfig(
                        staticServer.port(),
                        "[extractor]\ntext-length-limit = 25"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            staticServer.add(
                "/get/sindbad-truncated?raw",
                new File(getClass().getResource(SINDBAD).toURI()));
            staticServer.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        LOCALHOST + server.port()
                        + "/text?stid=sindbad-truncated")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpEntity body = response.getEntity();
                Assert.assertEquals(
                    CONTENT_TYPE,
                    body.getContentType().getValue());
                Assert.assertEquals(
                    "Маршрутная квитанция\nэлек",
                    EntityUtils.toString(body));
            }
        }
    }

    @Test
    public void testDefPdf() throws Exception {
        try (StaticServer staticServer =
                new StaticServer(Configs.baseConfig());
            Server server =
                new Server(ServerTest.getConfig(staticServer.port()));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            staticServer.add(
                "/get/base64?gettype=part&part=1.2",
                new File(getClass().getResource("base64.pdf").toURI()));
            staticServer.start();
            server.start();
            HttpResponse response = client.execute(
                new HttpGet(LOCALHOST + server.port() + "/text?stid=base64&"
                    + "hid=1.2&encoding=base64"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpEntity body = response.getEntity();
            Assert.assertEquals(
                CONTENT_TYPE,
                body.getContentType().getValue());
            String text = EntityUtils.toString(body);
            YandexAssert.assertContains("Высота потолка 3.0 м", text);
        }
    }

    @Test
    public void testEncodingAutoDetect() throws Exception {
        try (StaticServer staticServer =
                new StaticServer(Configs.baseConfig());
            Server server =
                new Server(ServerTest.getConfig(staticServer.port()));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            staticServer.add(
                "/get/encoding-auto-detect?gettype=meta",
                new StaticHttpResource(
                    new HeaderHttpItem(
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new FileEntity(
                                new File(
                                    getClass().getResource("utf8attach.txt")
                                        .toURI()))),
                        "X-Mulca-Server-Xml-Header-Size",
                        "842")));
            staticServer.add(
                "/get/encoding-auto-detect?gettype=part&part=1.2",
                "SGVsbG8sIHdvcmxk");
            staticServer.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        server.host() + "/text?stid=encoding-auto-detect"
                        + "&hid=1.2&encoding=auto")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "Hello, world",
                    EntityUtils.toString(response.getEntity()));
            }
            staticServer.add(
                "/get/encoding-auto-detect?gettype=part&part=1.3",
                new File(getClass().getResource("test.ttf").toURI()));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                server.port(),
                "/text?stid=encoding-auto-detect&hid=1.3&encoding=auto");
        }
    }
}

