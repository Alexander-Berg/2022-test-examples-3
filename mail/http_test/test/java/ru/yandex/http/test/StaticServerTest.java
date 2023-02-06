package ru.yandex.http.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.PatternMap;
import ru.yandex.http.server.sync.ContentWriter;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHttpStatus;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.logger.AccessLoggerConfigDefaults;
import ru.yandex.logger.LoggerConfigBuilder;
import ru.yandex.logger.LoggerFileConfigBuilder;
import ru.yandex.logger.LoggersConfigBuilder;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class StaticServerTest extends TestBase {
    private static final int BUFFER_SIZE = 65536;
    private static final String LOCALHOST = "localhost";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String URI = "/";
    private static final String RFC = "rfc3261.txt";
    private static final String CHECK_FAILED =
        "HTTP/1.1 501 Not Implemented: For '";

    @Test
    public void test() throws Exception {
        String body = ExpectingServerTest.streamToString(
            getClass().getResourceAsStream(RFC));
        StringEntity entity = new StringEntity(body);
        entity.setChunked(true);
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer server = new StaticServer(Configs.baseConfig()))
        {
            HttpPost post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(entity);
            server.add(URI, entity);
            server.start();
            HttpResponse response = client.execute(post);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                body,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testEntities() throws Exception {
        StringEntity entity = new StringEntity(HTTP_LOCALHOST);
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer server = new StaticServer(Configs.baseConfig()))
        {
            HttpPost post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(entity);
            server.add(URI, HttpStatus.SC_SERVICE_UNAVAILABLE, entity);
            server.start();
            HttpResponse response = client.execute(post);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_SERVICE_UNAVAILABLE,
                response);
            Assert.assertEquals(
                HTTP_LOCALHOST,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testRequestCharset() throws Exception {
        BaseServerConfigBuilder config =
            new BaseServerConfigBuilder(Configs.baseConfig());
        config.headersCharset(StandardCharsets.UTF_8);
        try (StaticServer server = new StaticServer(config.build())) {
            server.add("/somepath?text=%D0%B4%D0%BE%D0%BB%D0%B3%D0%B0%D1%8F+"
                + "%D0%B4%D0%BE%D1%80%D0%BE%D0%B3%D0%B0+%D0%B2+"
                + "%D0%BF%D0%BE%D0%B8%D1%81%D0%BA", "Привет, мир!");
            server.start();
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpResponse response = client.execute(
                    new HttpGet(HTTP_LOCALHOST + server.port()
                        + "/somepath?text=долгая+дорога+в+поиск"));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "Привет, мир!",
                    CharsetUtils.toString(response.getEntity()));
            }
            try (Socket socket = new Socket(LOCALHOST, server.port())) {
                socket.getOutputStream().write(("GET /somepath?text=долгая+"
                    + "дорога+в+поиск HTTP/1.1\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                Assert.assertEquals("HTTP/1.1 200 OK", new BufferedReader(
                    new InputStreamReader(socket.getInputStream(),
                        StandardCharsets.UTF_8)).readLine());
            }
        }
    }

    @Test
    public void testNoClosingChunk() throws Exception {
        Path accessLog = Files.createTempFile(null, null);
        BaseServerConfigBuilder config =
            new BaseServerConfigBuilder(Configs.baseConfig());
        config.loggers(
            new LoggersConfigBuilder().accessLoggers(
                new PatternMap<>(
                    new LoggerConfigBuilder(
                        AccessLoggerConfigDefaults.INSTANCE).add(
                        new LoggerFileConfigBuilder(
                            AccessLoggerConfigDefaults.INSTANCE)
                            .file(accessLog.toFile())))));

        try (StaticServer server = new StaticServer(config.build())) {
            String path = "/no-closing-chunk";
            server.add(path, new ContentProducer() {
                @Override
                public void writeTo(final OutputStream out)
                    throws IOException
                {
                    out.write(new byte[BUFFER_SIZE]);
                    out.flush();
                    throw new IOException("You doomed!");
                }
            });
            server.start();
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpResponse response = client.execute(
                    new HttpGet(HTTP_LOCALHOST + server.port() + path));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                try {
                    CharsetUtils.toString(response.getEntity());
                    Assert.fail();
                } catch (IOException e) {
                    YandexAssert.assertContains(
                        "\" " + YandexHttpStatus.SC_REMOTE_CLOSED_REQUEST
                        + ' ',
                        Files.readAllLines(accessLog, StandardCharsets.UTF_8)
                            .get(0));
                }
            }
        } finally {
            Files.delete(accessLog);
        }
    }

    @Test
    public void testUncloseableStream() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig())) {
            String path = "/uncloseable";
            server.add(path, new ContentWriter() {
                @Override
                public void writeTo(final Writer writer) throws IOException {
                    for (int i = 0; i < BUFFER_SIZE; ++i) {
                        writer.write("Exception is coming!");
                    }
                    writer.flush();
                    throw new IOException("Prepare to die!");
                }
            });
            server.start();
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpResponse response = client.execute(
                    new HttpGet(HTTP_LOCALHOST + server.port() + path));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                try {
                    CharsetUtils.toString(response.getEntity());
                    Assert.fail();
                } catch (IOException e) {
                    return;
                }
            }
        }
    }

    @Test
    public void testClientClosedConnection() throws Exception {
        Path accessLog = Files.createTempFile(null, null);
        BaseServerConfigBuilder config =
            new BaseServerConfigBuilder(Configs.baseConfig());
        config.loggers(new LoggersConfigBuilder().accessLoggers(
            new PatternMap<>(
                new LoggerConfigBuilder(
                    AccessLoggerConfigDefaults.INSTANCE).add(
                        new LoggerFileConfigBuilder(
                            AccessLoggerConfigDefaults.INSTANCE)
                            .file(accessLog.toFile())))));
        try {
            try (StaticServer server = new StaticServer(config.build())) {
                final int wait = 10;
                server.add("/close-me", new ContentWriter() {
                    @Override
                    public void writeTo(final Writer writer)
                        throws IOException
                    {
                        for (int i = 0; i < BUFFER_SIZE; ++i) {
                            writer.write("How do you do?");
                            writer.flush();
                            try {
                                Thread.sleep(wait);
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                    }
                });
                server.start();
                try (Socket socket = new Socket(LOCALHOST, server.port())) {
                    socket.getOutputStream().write(
                        "GET /close-me HTTP/1.1\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    Assert.assertEquals(
                        "HTTP/1.1 200 O" + 'K',
                        new BufferedReader(
                            new InputStreamReader(socket.getInputStream(),
                                StandardCharsets.UTF_8)).readLine());
                    for (int i = 0; i <= 2; ++i) {
                        Thread.sleep(wait);
                    }
                }
            }
            YandexAssert.assertContains(
                Character.toString('"') + ' '
                + YandexHttpStatus.SC_CLIENT_CLOSED_REQUEST + ' ',
                Files.readAllLines(accessLog, StandardCharsets.UTF_8).get(0));
        } finally {
            Files.delete(accessLog);
        }
    }

    @Test
    public void testStringChecker() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer server = new StaticServer(Configs.baseConfig()))
        {
            HttpPost post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(new StringEntity("Open Sesame"));
            server.add(URI, new ExpectingHttpItem("Open Sesame!", ""));
            server.start();
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
                YandexAssert.assertContains(
                    CHECK_FAILED + URI
                    + "' string mismatch:\n Open Sesame\n-!",
                    CharsetUtils.toString(response.getEntity()));
            }
            post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(
                new StringEntity(
                    "Prefix and some long wording here and there suffix"));
            server.add(
                URI,
                new ExpectingHttpItem(
                    "Prefix some long wording here and there"
                    + " followed by the same useless and simple suffix", ""));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
                YandexAssert.assertContains(
                    CHECK_FAILED + URI
                    + "' string mismatch:\n Prefix \n+and "
                    + "\n some long wording here and ther"
                    + "\n-e followed by the same useless and simpl"
                    + "\n e suffix",
                    CharsetUtils.toString(response.getEntity()));
            }
            post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(
                new StringEntity(
                    "Start new fable about evil sorcerer and beautiful"
                    + " princess hidden under donkey skin with inevitable"
                    + " end"));
            server.add(
                URI,
                new ExpectingHttpItem(
                    "Start a new tale about evil sorcerer and beautiful"
                    + " princess hidden under donkey skin with"
                    + " inevitable and dreadful end"));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
                YandexAssert.assertContains(
                    CHECK_FAILED + URI
                    + "' string mismatch:\n Start \n-a new ta\n"
                    + "+new fab\n"
                    + " le about evil sorcerer and beautiful princess hidden"
                    + " under donkey skin with inevitable\n"
                    + "- and dreadful\n"
                    + "  end",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testJsonChecker() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer server = new StaticServer(Configs.baseConfig()))
        {
            HttpPost post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(
                new StringEntity(
                    "{\"a\":1,\"b\":1.05e1,"
                    + "\"c\":\"d\",\"e\":true,\"f\":null,\"g\":1e1}"));
            Map<String, Object> expected = new HashMap<>();
            final long ten = 10L;
            final double tenDotFive = 10.5d;
            expected.put("a", 1);
            expected.put("b", tenDotFive);
            expected.put("c", "d");
            expected.put("e", true);
            expected.put("f", null);
            expected.put("g", ten);
            JsonChecker checker = new JsonChecker(expected);
            Assert.assertEquals(expected.hashCode(), checker.hashCode());
            server.add(URI, new ExpectingHttpItem(checker));
            server.start();
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
                YandexAssert.assertContains(
                    CHECK_FAILED + URI
                    + "' string mismatch:\n {\n     \"a\": \n"
                    + "-\"1(java.lang.Integer)\"\n+1\n"
                    + " ,\n     \"b\": 10.5,\n     \"c\": \"d\",\n"
                    + "     \"e\": true,\n     \"f\": null,\n"
                    + "     \"g\": 10\n }",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBadJsonChecker() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer server = new StaticServer(Configs.baseConfig()))
        {
            HttpPost post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(new StringEntity("invalid json"));
            JsonChecker checker = new JsonChecker(new HashMap<>());
            Assert.assertEquals("{}", checker.toString());
            server.add(URI, new ExpectingHttpItem(checker));
            server.start();
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
                YandexAssert.assertContains(
                    CHECK_FAILED + URI + "' failed to parse <invalid json>",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

