package ru.yandex.http.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicLineParser;
import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.charset.Decoder;
import ru.yandex.collection.Pattern;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.YandexHttpStatus;
import ru.yandex.http.util.client.ClientBuilder;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.http.util.request.RequestPatternParser;
import ru.yandex.http.util.server.AbstractHttpServer;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.HttpServerFactory;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.ByteArrayInputStreamFactory;
import ru.yandex.io.DecodableByteArrayOutputStream;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.logger.AsyncStreamHandler;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.stater.PrefixingStater;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.filesystem.CloseableDeleter;
import ru.yandex.util.string.StringUtils;

public abstract class HttpServerTest<T> extends HttpServerTestBase<T> {
    @Test
    public void testPing() throws Exception {
        final int timeout = 8;
        final int millis = 1000;
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server = server(config()
                .timeout(timeout * millis)
                .build()))
        {
            server.start();
            HttpGet get = new HttpGet(server.host() + PING);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    NAME + ' ' + BaseServerConfigBuilder.BUILT_DATE,
                    response.getFirstHeader(SERVER).getValue());
                Assert.assertEquals(
                    "timeout=7",
                    response.getFirstHeader(HTTP.CONN_KEEP_ALIVE).getValue());
                Assert.assertEquals(
                    PONG,
                    CharsetUtils.toString(response.getEntity()));
                String stats = HttpAssert.stats(client, server);
                HttpAssert.assertStat(
                    "reqstats-codes-2xx-request-length_ammm",
                    Long.toString(0),
                    stats);
                HttpAssert.assertStat(
                    "reqstats-codes-2xx-response-length_ammm",
                    Long.toString(PONG.length()),
                    stats);
            }
            // /force-gc is just like a ping, but slower
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(server.host() + FORCE_GC)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            HttpOptions options = new HttpOptions(server.host() + PING);
            try (CloseableHttpResponse response = client.execute(options)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    Set.of(
                        RequestHandlerMapper.HEAD,
                        RequestHandlerMapper.GET,
                        RequestHandlerMapper.POST,
                        RequestHandlerMapper.OPTIONS,
                        RequestHandlerMapper.PUT),
                    options.getAllowedMethods(response));
            }

            Assert.assertNotNull(
                server.unregister(
                    new Pattern<>(PING, false),
                    RequestHandlerMapper.GET));
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_METHOD_NOT_ALLOWED,
                    response);
                Assert.assertEquals(
                    "HEAD, POST, PUT, OPTIONS",
                    response.getFirstHeader(HttpHeaders.ALLOW).getValue());
            }

            server.unregister(new Pattern<>(PING, false));
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
            }
        }
    }

    @Test
    public void testDisablePing() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();

            HttpGet get = new HttpGet(server.host() + STATUS);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HTTP.CONN_KEEP_ALIVE,
                    "timeout=2",
                    response);
            }

            get = new HttpGet(server.host() + DISABLE_PING);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            get = new HttpGet(server.host() + PING);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_SERVICE_UNAVAILABLE,
                    response);
            }

            get = new HttpGet(server.host() + STATUS);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HTTP.CONN_KEEP_ALIVE,
                    null,
                    response);
                HttpAssert.assertHeader(
                    HTTP.CONN_DIRECTIVE,
                    HTTP.CONN_CLOSE,
                    response);
            }

            get = new HttpGet(server.host() + "/enable-ping");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            get = new HttpGet(server.host() + PING);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HTTP.CONN_DIRECTIVE,
                    null,
                    response);
            }
        }
    }

    @Test
    public void testEnablePing() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server = server(
                config().pingEnabledOnStartup(false).build()))
        {
            server.start();

            HttpGet get = new HttpGet(server.host() + PING);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_SERVICE_UNAVAILABLE,
                    response);
            }

            get = new HttpGet(server.host() + STATUS);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HTTP.CONN_DIRECTIVE,
                    HTTP.CONN_CLOSE,
                    response);
            }

            get = new HttpGet(server.host() + "/enable-ping");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HTTP.CONN_DIRECTIVE,
                    null,
                    response);
            }

            get = new HttpGet(server.host() + PING);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            get = new HttpGet(server.host() + DISABLE_PING);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HTTP.CONN_DIRECTIVE,
                    HTTP.CONN_CLOSE,
                    response);
            }

            get = new HttpGet(server.host() + PING);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_SERVICE_UNAVAILABLE,
                    response);
            }
        }
    }

    @Test
    public void testPingHead() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();
            try (Socket socket = connectTo(server)) {
                try (BufferedReader reader =
                        new BufferedReader(
                            new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8)))
                {
                    for (int i = 0; i <= 1; ++i) {
                        String request = "HEAD " + PING + "#?" + HTTP_1_1;
                        if (i != 0) {
                            request = request.replace("keep-alive", "close");
                        }
                        socket.getOutputStream().write(
                            request.getBytes(StandardCharsets.UTF_8));
                        socket.getOutputStream().flush();
                    }
                    Assert.assertEquals(HTTP_200_OK, reader.readLine());
                    String line = reader.readLine();
                    Assert.assertNotNull(line);
                    Header contentLength = null;
                    while (!line.isEmpty()) {
                        Header header = BasicLineParser.parseHeader(
                            line,
                            BasicLineParser.INSTANCE);
                        if (header.getName().equals(
                            HttpHeaders.CONTENT_LENGTH))
                        {
                            if (contentLength == null) {
                                contentLength = header;
                            } else {
                                Assert.fail("Found " + header
                                    + ", while content length is already "
                                    + contentLength);
                            }
                        }
                        line = reader.readLine();
                        Assert.assertNotNull(line);
                    }
                    Assert.assertEquals("4", contentLength.getValue());
                    Assert.assertEquals(HTTP_200_OK, reader.readLine());
                    line = reader.readLine();
                    Assert.assertNotNull(line);
                    while (!line.isEmpty()) {
                        line = reader.readLine();
                        Assert.assertNotNull(line);
                    }
                    try {
                        Assert.assertNull(reader.readLine());
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testGlobalOptions() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();
            try (Socket socket = connectTo(server)) {
                byte[] data = ("OPTIONS *" + HTTP_1_1)
                    .getBytes(StandardCharsets.UTF_8);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(),
                            StandardCharsets.UTF_8)))
                {
                    socket.getOutputStream().write(data);
                    socket.getOutputStream().flush();
                    Assert.assertEquals(HTTP_200_OK, reader.readLine());
                    String line = reader.readLine();
                    Assert.assertNotNull(line);
                    while (!line.isEmpty()) {
                        line = reader.readLine();
                        Assert.assertNotNull(line);
                    }
                    String methods = "\tGET, HEAD, POST, PUT";
                    String dump = "\t\tUses IBM JVM API to produce various "
                        + "process dumps";
                    line = reader.readLine();
                    Assert.assertEquals(
                        "/add-debug-flag",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tTurns on debug flag",
                        line);

                    line = reader.readLine();
                    Assert.assertEquals(
                        "/config-update",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tUpdates server dynamic config",
                        line);

                    line = reader.readLine();
                    Assert.assertEquals(
                        "/custom-alerts-config",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tPrints custom server alerts config",
                        line);

                    line = reader.readLine();
                    Assert.assertEquals(
                        "/custom-golovan-panel",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tPrints custom server metrics golovan panel",
                        line);

                    line = reader.readLine();
                    Assert.assertEquals(
                        "/debug-flags",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tShows current debug flags",
                        line);
                    line = reader.readLine();

                    Assert.assertEquals(
                        "/disable-ping",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tDisables /ping requests handling",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        DISABLE_PING,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tDisables /ping requests handling",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "/enable-ping",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tEnables /ping requests handling",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        FORCE_GC,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tForce GC",
                        line);

                    if (server.hasIBMDumper()) {
                        line = reader.readLine();
                        Assert.assertEquals(
                            "/heapdump",
                            line);
                        line = reader.readLine();
                        Assert.assertEquals(
                            methods,
                            line);
                        line = reader.readLine();
                        Assert.assertEquals(
                            dump,
                            line);
                        line = reader.readLine();
                        Assert.assertEquals(
                            "/javadump",
                            line);
                        line = reader.readLine();
                        Assert.assertEquals(
                            methods,
                            line);
                        line = reader.readLine();
                        Assert.assertEquals(
                            dump,
                            line);
                    }

                    line = reader.readLine();
                    Assert.assertEquals(
                        LOGROTATE_URI,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tPerforms logs rotation",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        PING,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tSimple ping-pong handler",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "/remove-debug-flag",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tTurns off debug flag",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        STAT,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tPrints server status in golovan format.",
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        STATUS,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tPrints server status: amount of active workers, "
                        + "connections etc.",
                        line);

                    if (server.hasIBMDumper()) {
                        line = reader.readLine();
                        Assert.assertEquals(
                            "/systemdump",
                            line);
                        line = reader.readLine();
                        Assert.assertEquals(
                            methods,
                            line);
                        line = reader.readLine();
                        Assert.assertEquals(
                            dump,
                            line);
                    }

                    line = reader.readLine();
                    Assert.assertEquals(
                        SYSTEMEXIT,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        methods,
                        line);
                    line = reader.readLine();
                    Assert.assertEquals(
                        "\t\tShutdown server",
                        line);
                }
            }
        }
    }
    // CSON: MethodLength

    @Test
    public void testPingWithLongQuery() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LONG_LINE_SIZE; ++i) {
            sb.append('?');
        }
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();
            HttpResponse response =
                client.execute(new HttpGet(server.host() + PING + sb));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                NAME + ' ' + BaseServerConfigBuilder.BUILT_DATE,
                response.getFirstHeader(SERVER).getValue());
            Assert.assertEquals(
                PONG,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testPingWithLongHeader() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LONG_LINE_SIZE; ++i) {
            sb.append('a');
        }
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();
            HttpGet get = new HttpGet(server.host() + PING);
            get.addHeader("Test-Header", sb.toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    NAME + ' ' + BaseServerConfigBuilder.BUILT_DATE,
                    response.getFirstHeader(SERVER).getValue());
                Assert.assertEquals(
                    PONG,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void testLogrotate(final int bufferLength) throws Exception {
        File log = File.createTempFile(
            testName.getMethodName() + bufferLength,
            LOG_EXT);
        Assert.assertTrue(log.delete());
        ImmutableBaseServerConfig config =
            config(
                LOG_FILENAME + log.getAbsolutePath()
                + "\nbuffer = " + bufferLength)
                .origin(ORIGIN)
                .build();
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server = server(config))
        {
            server.start();
            HttpResponse response =
                client.execute(new HttpGet(server.host() + LOGROTATE_URI));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                ORIGIN,
                response.getFirstHeader(SERVER).getValue());
            Assert.assertEquals(
                LOGROTATE_RESPONSE,
                CharsetUtils.toString(response.getEntity()));
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(log),
                    StandardCharsets.UTF_8)))
            {
                StringBuilder sb = new StringBuilder();
                boolean found = false;
                while (!found) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line);
                    sb.append('\n');
                    if (line.indexOf("Starting HttpServer on") != -1) {
                        found = true;
                    }
                }
                if (!found) {
                    Assert.fail("Not found 'Starting HttpServer on' in " + sb);
                }
            }
            Files.delete(log.toPath());
            response =
                client.execute(new HttpGet(server.host() + LOGROTATE_URI));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                LOGROTATE_RESPONSE,
                CharsetUtils.toString(response.getEntity()));
            Thread.sleep(AsyncStreamHandler.QUEUE_SLEEP << 2);
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(log),
                    StandardCharsets.UTF_8)))
            {
                String line = reader.readLine();
                Assert.assertNotNull(line);
                YandexAssert.assertContains("Logs rotated", line);
            }
        }
    }

    @Test
    public void testLogrotate() throws Exception {
        for (int i = 0; i <= 2; ++i) {
            System.out.println("Testing logrotate with buffer size: " + i);
            testLogrotate(i);
        }
    }

    @Test
    public void testNumberingLogrotate() throws Exception {
        File log = File.createTempFile(testName.getMethodName(), LOG_EXT);
        log.deleteOnExit();
        File log0 = new File(log.getAbsolutePath() + ".0");
        log0.deleteOnExit();
        File log1 = new File(log.getAbsolutePath() + ".1");
        log1.deleteOnExit();
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server = server(
                config(
                    ACCESSLOG_FILENAME + log.getAbsolutePath()
                    + "\nrotate = number"
                    + "\nformat = \"%{request}\" %{status} %{response_length}"
                    + " %{http_accept_charset} %{sent_http_server}\n")
                    .build()))
        {
            server.start();
            HttpGet get = new HttpGet(server.host() + LOGROTATE_URI);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertTrue(log0.exists());
                Assert.assertEquals(
                    LOGROTATE_RESPONSE,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertTrue(log1.exists());
                Assert.assertEquals(
                    LOGROTATE_RESPONSE,
                    CharsetUtils.toString(response.getEntity()));
                Assert.assertEquals(
                    "\"GET /logrotate HTTP/1.1\" 200 "
                    + LOGROTATE_RESPONSE.length() + ' ' + UTF_8 + ' '
                    + NAME + ' ' + BaseServerConfigBuilder.BUILT_DATE,
                    Files.readAllLines(log0.toPath(), StandardCharsets.UTF_8)
                        .get(0));
                Assert.assertTrue(log0.delete());
            }
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertTrue(log0.exists());
                Assert.assertEquals(
                    LOGROTATE_RESPONSE,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testBadLogrotate() throws Exception {
        Path dir = Files.createTempDirectory(testName.getMethodName() + "Dir");
        try {
            File log = File.createTempFile(
                testName.getMethodName(),
                LOG_EXT,
                dir.toFile());
            log.deleteOnExit();
            try (CloseableHttpClient client =
                    Configs.createDefaultClient(clientBc());
                HttpServer<ImmutableBaseServerConfig, T> server = server(
                    config(LOG_FILENAME + log.getAbsolutePath()).build()))
            {
                server.start();
                Assert.assertTrue(log.delete());
                Files.delete(dir);
                HttpResponse response =
                    client.execute(new HttpGet(server.host() + LOGROTATE_URI));
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_SERVICE_UNAVAILABLE,
                    response);
                CharsetUtils.consume(response.getEntity());
            }
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void testKeepAlive() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();
            int connections = WORKERS << 1;
            DefaultBHttpClientConnection[] conns =
                new DefaultBHttpClientConnection[connections];
            for (int i = 0; i < connections; ++i) {
                conns[i] = new DefaultBHttpClientConnection(BUFFER_SIZE);
                conns[i].bind(connectTo(server));
                conns[i].setSocketTimeout(TIMEOUT);
            }
            HttpRequest request =
                new BasicHttpRequest(
                    HttpGet.METHOD_NAME,
                    PING,
                    HttpVersion.HTTP_1_1);
            try {
                for (int j = 0; j <= 1; ++j) {
                    for (DefaultBHttpClientConnection conn: conns) {
                        conn.sendRequestHeader(request);
                        conn.flush();
                        HttpResponse response = conn.receiveResponseHeader();
                        HttpAssert.assertStatusCode(
                            HttpStatus.SC_OK,
                            response);
                        conn.receiveResponseEntity(response);
                        Assert.assertEquals(
                            PONG,
                            CharsetUtils.toString(response.getEntity()));
                    }
                }
            } finally {
                for (DefaultBHttpClientConnection conn: conns) {
                    conn.shutdown();
                }
            }
        }
    }

    @Test
    public void testConnections() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(config("\nserver.timeout = 10000").build()))
        {
            server.start();
            final int timeout = 10000;
            DefaultBHttpClientConnection[] conns =
                new DefaultBHttpClientConnection[CONNECTIONS];
            for (int i = 0; i < CONNECTIONS; ++i) {
                conns[i] = new DefaultBHttpClientConnection(BUFFER_SIZE);
                conns[i].bind(connectTo(server));
                conns[i].setSocketTimeout(timeout);
                yield();
            }
            HttpRequest request = new BasicHttpRequest(
                HttpGet.METHOD_NAME,
                PING + "?testConnections",
                HttpVersion.HTTP_1_1);
            request.addHeader(CONNECTION_CLOSE);
            for (int i = 0; i < CONNECTIONS; ++i) {
                DefaultBHttpClientConnection conn = conns[i];
                conn.sendRequestHeader(request);
                conn.flush();
                HttpResponse response = conn.receiveResponseHeader();
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                conn.receiveResponseEntity(response);
                Assert.assertEquals(
                    PONG,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testNoCommandLineArgs() throws ConfigException, IOException {
        PrintStream stderr = System.err;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setErr(new PrintStream(baos, true, UTF_8));
            AbstractHttpServer.main(
                new HttpServerFactory<ImmutableBaseServerConfig, T>() {
                    @Override
                    public String name() {
                        return "some.server.class.Name";
                    }

                    @Override
                    public HttpServer<ImmutableBaseServerConfig, T> create(
                        final IniConfig config)
                        throws ConfigException, IOException
                    {
                        return server(null);
                    }
                });
            Assert.assertEquals(
                "Usage: some.server.class.Name <config file>\n",
                baos.toString(UTF_8));
        } finally {
            System.setErr(stderr);
        }
    }

    @Test
    public void testCommandLine() throws ConfigException, IOException {
        PrintStream stdout = System.out;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, UTF_8));
            File config =
                File.createTempFile(testName.getMethodName(), ".conf");
            config.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(config)) {
                fos.write(CONFIG.getBytes(StandardCharsets.UTF_8));
            }
            try (HttpServer<?, ?> server = AbstractHttpServer.main(
                    new HttpServerFactory<ImmutableBaseServerConfig, T>() {
                        @Override
                        public String name() {
                            return "ru.yandex.http.server.sync.StaticServer";
                        }

                        @Override
                        public HttpServer<ImmutableBaseServerConfig, T> create(
                            final IniConfig config)
                            throws ConfigException, IOException
                        {
                            return server(
                                new BaseServerConfigBuilder(config)
                                    .name("StaticServer")
                                    .build());
                        }
                    },
                    config.getAbsolutePath()))
            {
                Assert.assertEquals("###started###\n", baos.toString(UTF_8));
                Assert.assertTrue(server.port() > 0);
            }
        } finally {
            System.setOut(stdout);
        }
    }

    @Test
    public void testPortBusy() throws ConfigException, IOException {
        try (StaticServer invader = new StaticServer(defaultConfig)) {
            try (HttpServer<ImmutableBaseServerConfig, T> server =
                    server(Configs.baseConfig(invader.port())))
            {
                Assert.fail(
                    "Server initialization should fail, because server on "
                    + invader.address()
                    + " already occupied port required by server on "
                    + server.address());
            } catch (IOException e) {
                // It's OK
            }
        }
    }

    @Test
    public void testLazyPortBusy() throws ConfigException, IOException {
        try (StaticServer invader = new StaticServer(defaultConfig);
            HttpServer<ImmutableBaseServerConfig, T> server =
                    server(
                        new BaseServerConfigBuilder(
                            Configs.baseConfig(invader.port()))
                            .lazyBind(true)
                            .build()))
        {
            try {
                server.start();
                Assert.fail();
            } catch (IOException e) {
                // It's OK
            }
        }
    }

    @Test
    public void testConnCloseHttp10() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();
            HttpGet get = new HttpGet(server.host() + PING);
            get.setProtocolVersion(HttpVersion.HTTP_1_0);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "Connection: Close",
                    Objects.toString(response.getFirstHeader("Connection")));
            }
        }
    }

    @Test
    public void testBriefHeaders() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(config().briefHeaders(true).build()))
        {
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(server.host() + PING)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertNull(response.getFirstHeader(SERVER));
            }
        }
    }

    @Test
    public void testStaticHeadersAndStats() throws Exception {
        System.setProperty("INUM", "42");
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server1 = server(
                config(
                    "server.stats-prefix = prfx-\n"
                    + "[server.static-headers]\n"
                    + "My-Header = My value\n"
                    + "[server.static-stats]\n"
                    + "my-stat_ammm = 100500\n"
                    + "another-stat_ammm = 100501\n"
                    + "[server.stats-aliases]\n"
                    + "my-stat_ammm = my-stat_axxx\n"
                    + "another-stat_ammm = "
                    + "$(INUM)-stat1_ammm, $(INUM)-stat2_ammm\n")
                    .build());
            HttpServer<ImmutableBaseServerConfig, T> server2 =
                server(defaultConfig);
            HttpServer<ImmutableBaseServerConfig, T> server3 = server(
                config(
                    "server.stats-prefix = prfx2-\n"
                    + "server.keep-unprefixed-stats = false\nserver."
                    + "static-stats.yet-another-stat_ammm = 1234567890\n"
                    + "server.stats-aliases.yet-another-stat_ammm = "
                    + "yet-another-stat_axxx\n")
                    .build()))
        {
            server2.registerStater(new PrefixingStater("proxy-", server1));
            server1.start();
            server2.start();
            server3.start();

            String stopitsot = "100500";
            String stopitsot1 = "100501";
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(server1.host() + STAT)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader("my-header", "My value", response);

                String body = HttpAssert.body(response);
                HttpAssert.assertStat("my-stat_ammm", stopitsot, body);
                HttpAssert.assertStat("my-stat_axxx", stopitsot, body);
                HttpAssert.assertStat("another-stat_ammm", stopitsot1, body);
                HttpAssert.assertStat("42-stat1_ammm", stopitsot1, body);
                HttpAssert.assertStat("42-stat2_ammm", stopitsot1, body);

                HttpAssert.assertStat("prfx-my-stat_ammm", stopitsot, body);
                HttpAssert.assertStat("prfx-my-stat_axxx", stopitsot, body);
                HttpAssert.assertStat(
                    "prfx-another-stat_ammm",
                    stopitsot1,
                    body);
                HttpAssert.assertStat("prfx-42-stat1_ammm", stopitsot1, body);
                HttpAssert.assertStat("prfx-42-stat2_ammm", stopitsot1, body);
            }
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(server2.host() + STAT)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                HttpAssert.assertStat("proxy-my-stat_ammm", stopitsot, body);
                HttpAssert.assertStat("proxy-my-stat_axxx", stopitsot, body);
                HttpAssert.assertStat(
                    "proxy-another-stat_ammm",
                    stopitsot1,
                    body);
                HttpAssert.assertStat("proxy-42-stat1_ammm", stopitsot1, body);
                HttpAssert.assertStat("proxy-42-stat2_ammm", stopitsot1, body);

                HttpAssert.assertStat(
                    "proxy-prfx-another-stat_ammm",
                    stopitsot1,
                    body);
                HttpAssert.assertStat(
                    "proxy-prfx-42-stat1_ammm",
                    stopitsot1,
                    body);
                HttpAssert.assertStat(
                    "proxy-prfx-42-stat2_ammm",
                    stopitsot1,
                    body);
            }
            String timestamp = "1234567890";
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(server3.host() + STAT)))
            {
                String body = HttpAssert.body(response);
                HttpAssert.assertStat("yet-another-stat_ammm", null, body);
                HttpAssert.assertStat(
                    "prfx2-yet-another-stat_ammm",
                    timestamp,
                    body);
                HttpAssert.assertStat("yet-another-stat_axxx", null, body);
                HttpAssert.assertStat(
                    "prfx2-yet-another-stat_axxx",
                    timestamp,
                    body);
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            server3.host()
                            + "/custom-golovan-panel?abc=so&editors=dpotapov"
                            + "&tag=itype=myitype&title=Title")))
            {
                String body = HttpAssert.body(response);
                YandexAssert.assertContains(
                    "unistat-prfx2-reqstats-codes-2xx_ammm",
                    body);
            }
        }
    }

    @Test
    public void testFullExceptionStackTrace() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(server.host() + "/stat?json-type=a")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
                YandexAssert.assertContains(
                    "Caused by",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void testAccessLog(final HttpRequest request) throws Exception {
        PrintStream err = System.err;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setErr(new PrintStream(out, true, UTF_8));
            try (CloseableHttpClient client =
                    Configs.createDefaultClient(clientBc());
                HttpServer<ImmutableBaseServerConfig, T> server =
                    server(defaultConfig))
            {
                server.start();
                try (CloseableHttpResponse response =
                        client.execute(server.host(), request))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    CharsetUtils.consume(response.getEntity());
                }
                Thread.sleep(LOG_DELAY);
                synchronized (out) {
                    YandexAssert.assertContains(
                        '"' + request.getRequestLine().toString() + '"',
                        out.toString(UTF_8));
                }
            }
        } finally {
            System.setErr(err);
        }
    }

    @Test
    public void testHeadAccessLog() throws Exception {
        testAccessLog(new BasicHttpRequest(RequestHandlerMapper.HEAD, PING));
    }

    @Test
    public void testGetAccessLog() throws Exception {
        testAccessLog(new BasicHttpRequest(RequestHandlerMapper.GET, PING));
    }

    @Test
    public void testPostAccessLog() throws Exception {
        BasicHttpEntityEnclosingRequest request =
            new BasicHttpEntityEnclosingRequest(
                RequestHandlerMapper.POST,
                PING);
        request.setEntity(
            new StringEntity('"' + TEXT + '"', ContentType.APPLICATION_JSON));
        testAccessLog(request);
    }

    @Test
    public void testExpectContinue() throws Exception {
        PrintStream err = System.err;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setErr(new PrintStream(out, true, UTF_8));
            try (HttpServer<ImmutableBaseServerConfig, T> server =
                    server(defaultConfig))
            {
                server.start();
                boolean passed = false;
                try (Socket socket = connectTo(server)) {
                    try (BufferedReader reader =
                            new BufferedReader(
                                new InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8)))
                    {
                        socket.getOutputStream().write(
                            (RequestHandlerMapper.POST + ' ' + PING
                                + " HTTP/1.1\r\nConnection: close\r\n"
                                + "Expect: 100-continue\r\n"
                                + "Content-Length: 5\r\n\r\n")
                                .getBytes(StandardCharsets.UTF_8));
                        socket.getOutputStream().flush();
                        Assert.assertEquals(
                            "HTTP/1.1 100 Continue",
                            reader.readLine());
                        YandexAssert.assertEmpty(reader.readLine());
                        socket.getOutputStream().write(
                            "hello".getBytes(StandardCharsets.UTF_8));
                        socket.getOutputStream().flush();
                        Assert.assertEquals(HTTP_200_OK, reader.readLine());
                        while (true) {
                            String line = reader.readLine();
                            Assert.assertNotNull(line);
                            if (line.isEmpty()) {
                                break;
                            }
                        }
                        passed = true;
                    }
                } catch (IOException e) {
                    if (passed) {
                        logger.log(
                            Level.WARNING,
                            "Test is passed, but connection was "
                            + "terminated abnormally",
                            e);
                    } else {
                        throw e;
                    }
                }
                Thread.sleep(LOG_DELAY);
                synchronized (out) {
                    String log = out.toString(UTF_8);
                    YandexAssert.assertContains(
                        "\"POST /ping HTTP/1.1\" 200 ",
                        log);
                    YandexAssert.assertNotContains("\" 100 ", log);
                }
            }
        } finally {
            System.setErr(err);
        }
    }

    @Test
    public void testAccessLog499() throws Exception {
        File log = File.createTempFile(testName.getMethodName(), LOG_EXT);
        log.deleteOnExit();
        try (CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(
                        Configs.targetConfig(clientBc()))
                        .timeout(TIMEOUT >> 1)
                        .build(),
                    Configs.dnsConfig());
            HttpServer<ImmutableBaseServerConfig, T> server = server(
                config(
                    ACCESSLOG_FILENAME + log.getAbsolutePath()
                    + "\nformat = \"%{request}\" %{status} %{response_length} "
                    + "%{http_accept_charset} %{sent_http_server}\n")
                    .build()))
        {
            server.register(
                new Pattern<>(URI, false),
                createSlowpokeHandler(createDummyHandler(), TIMEOUT));
            server.start();
            int systemexitResponseLength;
            // Test bad request response from /systemexit
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(server.host() + SYSTEMEXIT)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
                systemexitResponseLength =
                    CharsetUtils.toString(response.getEntity()).length();
            }
            // Test 499
            try {
                client.execute(new HttpGet(server.host() + URI));
                Assert.fail();
            } catch (Exception e) {
                YandexAssert.assertInstanceOf(
                    SocketTimeoutException.class,
                    e);
            }
            Thread.sleep(TIMEOUT);
            List<String> lines =
                Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
            Assert.assertEquals(
                "\"GET /systemexit HTTP/1.1\" 400 "
                + systemexitResponseLength + ' ' + UTF_8 + ' '
                + NAME + ' ' + BaseServerConfigBuilder.BUILT_DATE,
                lines.get(0));
            String prefix499 = GET + URI + " HTTP/1.1\" 499 - UTF-8 ";
            if (!lines.get(1).equals(prefix499 + '-')) {
                Assert.assertEquals(
                    prefix499 + NAME + ' '
                    + BaseServerConfigBuilder.BUILT_DATE,
                    lines.get(1));
            }
        }
    }

    private static void assertStatusCode(
        final File file,
        final String user,
        final String uri,
        final int statusCode)
        throws IOException, InterruptedException
    {
        assertStatusCode(
            file,
            user,
            RequestHandlerMapper.GET,
            uri,
            statusCode);
    }

    private static void assertStatusCode(
        final File file,
        final String user,
        final String method,
        final String uri,
        final int statusCode)
        throws IOException, InterruptedException
    {
        Thread.sleep(LOG_DELAY);
        List<String> lines =
            Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        int linesCount = lines.size();
        if (linesCount == 0) {
            Assert.fail("No lines found in access log");
        }
        String expected =
            user + ' ' + '"' + method + ' '
            + uri + " HTTP/1.1\" " + statusCode;
        String lastLine = lines.get(linesCount - 1);
        if (!expected.equals(lastLine)) {
            Assert.fail(
                StringUtils.join(
                    lines,
                    "\n\t",
                    "Expected status line <" + expected
                    + "> was not found in access log:\n\t",
                    ""));
        }
    }

    @Test
    public void testSocketClose499() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LONG_LINE_SIZE; ++i) {
            sb.append('b');
        }
        String body = sb.toString();
        String request =
            RequestHandlerMapper.POST + ' ' + URI
            + " HTTP/1.1\r\nContent-Length: " + LONG_LINE_SIZE
            + "\r\n\r\n"
            + body;
        File log = File.createTempFile(testName.getMethodName(), LOG_EXT);
        log.deleteOnExit();
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        ACCESSLOG_FILENAME + log.getAbsolutePath()
                        + SHORT_ACCESS_LOG_FORMAT)
                        .build()))
        {
            server.register(
                new Pattern<>(URI, false),
                createSlowpokeHandler(
                    createDummyHandler(body),
                    TIMEOUT));
            server.start();
            try (Socket socket = new Socket(LOCALHOST, server.port())) {
                socket.setTcpNoDelay(true);
                Socket dataStreamSocket;
                if (server.sslContext() == null) {
                    dataStreamSocket = socket;
                } else {
                    dataStreamSocket =
                        sslConnectionSocketFactory.createLayeredSocket(
                            socket,
                            LOCALHOST,
                            server.port(),
                            null);
                }
                dataStreamSocket.getOutputStream().write(
                    request.getBytes(StandardCharsets.UTF_8));
                dataStreamSocket.getOutputStream().flush();
                socket.getOutputStream().flush();
                Thread.sleep(TIMEOUT >> 2);
            }
            Thread.sleep(TIMEOUT);
            assertStatusCode(
                log,
                NO_USER,
                RequestHandlerMapper.POST,
                URI,
                YandexHttpStatus.SC_CLIENT_CLOSED_REQUEST);
        }
    }

    @Test
    public void testAuth() throws Exception {
        try (StaticServer tvm2 =
                new StaticServer(Configs.baseConfig("TVM2")))
        {
            Configs.setupTvmKeys(tvm2);
            tvm2.start();
            String headerName = "ticket";
            String userTicketHeaderName = "user-ticket";
            File log = File.createTempFile(testName.getMethodName(), LOG_EXT);
            log.deleteOnExit();
            try (CloseableHttpClient client =
                    Configs.createDefaultClient(clientBc());
                HttpServer<ImmutableBaseServerConfig, T> server =
                    server(
                        config(
                            ACCESSLOG_FILENAME + log.getAbsolutePath()
                            + SHORT_ACCESS_LOG_FORMAT
                            + "\n[tvm2]\nhost = " + tvm2.host()
                            + "\nconnections = 2\nclient-id = 185"
                            + "\nsecret = 1234567890123456789012"
                            + "\nblackbox-env = test"
                            + "\n[auth]\nstrict = true"
                            + "\nallowed-srcs = 2000410, 2000411"
                            + "\nuser-ticket-presence = optional"
                            + "\n[auth./status{arg_local:false}]"
                            + "\nstrict = true"
                            + "\nallowed-srcs = 999"
                            + "\n[auth./status]\nstrict = true"
                            + "\nallowed-srcs = 999"
                            + "\nbypass-loopback = true"
                            + "\n[auth./stat]\nstrict = false\n"
                            + "\nallowed-srcs ="
                            + "\n[auth./ping{arg_prod:true AND corp_uid:true}]"
                            + "\nstrict = false\nheader-name = " + headerName
                            + "\nallowed-srcs = 2000410"
                            + "\n[auth./ping{arg_prod:true}]\nstrict = false"
                            + "\nallowed-srcs = 2000411\nheader-name = "
                            + headerName
                            + "\n[auth./ping{arg_prod:test}]\nstrict = false"
                            + "\nallowed-srcs = 2000411\nheader-name = "
                            + headerName
                            + "\nuser-ticket-header-name = "
                            + userTicketHeaderName
                            + "\nuser-ticket-presence = required")
                            .build()))
            {
                server.start();
                try (CloseableHttpResponse response = client.execute(
                        new HttpGet(server.host() + PING)))
                {
                    HttpAssert.assertStatusCode(
                        HttpStatus.SC_UNAUTHORIZED,
                        response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        UNAUTHORIZED,
                        PING,
                        HttpStatus.SC_UNAUTHORIZED);
                }
                String prodClient = "2000410";
                String testClient = "2000411";
                String prodTicket =
                    "3:serv:CBAQ__________9_IhQImox6ELkBGgttYWlsOnNlYXJjaA:Jp-"
                    + "SY6mUsPIqFjr7md9Y6bgORWHahjibzE_kz47VT8zuq76tXgJs6Abyd3"
                    + "l5S7UTIeEynkKrwDnATfHOGj7cpB3Zb4n8wFIt-YBld8Z0M-qnmSnzA"
                    + "EGqkrcFnmpfY6As7BvicmGfBTs5BF6ZlLYxHIGgaU-3-ak9QowKqFxN"
                    + "Ed0";
                String testTicket =
                    "3:serv:CBAQ__________9_IhQIm4x6ELkBGgttYWlsOnNlYXJjaA:G_L"
                    + "Wbcyf4MCiVOCaEonFgRhH54A-taXqT32xfHfY8WsolZ23uHuO203EU0"
                    + "2FF99mYNBuHCxA2Io8jOr9F2KvVctH14y2VNS9FV42Ar8wUeeCBOeRY"
                    + "Jsw8sFOSz132MjtImX31Li5M-bpMepTZbq3xtJS3ORLESz5_2z1TJkW"
                    + "ZmM";
                String otherSrcTicket =
                    "3:serv:CBAQ__________9_IhQInIx6ELkBGgttYWlsOnNlYXJjaA:EXW"
                    + "TDvYKe6XG3pND1kQCWhuB9QiMmLP1yQUjrCxlqqZrt_SAR4ltqp0_cA"
                    + "_iAk8zGoBO8UN5rLkUUi76FBWD0fRRJwDd260rekjtGsFPYriqpdVZT"
                    + "AJEGnmkcJbrcJSW8qNuly6HAs3Hkk1x2FMZ85YsaqPq890M1YAzEcp6"
                    + "Fv4";
                String wrongDstTicket =
                    "3:serv:CBAQ__________9_IhQImox6ELoBGgttYWlsOnNlYXJjaA:HO4"
                    + "ur-NvSXDzHKD_KQYGcRRTcID-FOEfiLvAnr7GJNZAtpRjAqOmpi09oP"
                    + "IcFz7jaC9rcguiTCGdcRBh1uwUwjSd-enius2AdYumyKh1k-bZN-VpQ"
                    + "qVy6mJCqr6dBMH9z6pf_PTyWGacfZk7yAGm4OSNS8yMQjlcZdB9kXnw"
                    + "rl4";
                String userTicket =
                    "3:user:CA0Q__________9_GhQKBQiJ29UCEInb1QIg0oXYzAQoAQ:Gio"
                    + "3aixJPSSzMmGM_MtaDkD3OGLIwPTEH9A8c8g8xaSs7XbdtqwQNCWduW"
                    + "aPvUEg1WYQpT1MYT_rpdx1kp265E4T8daAWbxz-YJX6N1KPbj11yhx7"
                    + "piTiOt3BDHB39wqF5zU8QewV8B9IKU_CwQwHcHERSYUW1UVYgTU-NiY"
                    + "GRE";

                HttpGet get = new HttpGet(server.host() + PING);
                get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, prodTicket);
                get.addHeader(YandexHeaders.X_YA_USER_TICKET, userTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        SERV + prodClient + "/user:5598601",
                        PING,
                        HttpStatus.SC_OK);
                }

                get = new HttpGet(server.host() + PING);
                get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, testTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        SERV + testClient,
                        PING,
                        HttpStatus.SC_OK);
                }

                get = new HttpGet(server.host() + PING);
                get.addHeader(
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    otherSrcTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(
                        HttpStatus.SC_UNAUTHORIZED,
                        response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        UNAUTHORIZED,
                        PING,
                        HttpStatus.SC_UNAUTHORIZED);
                }

                get = new HttpGet(server.host() + PING);
                get.addHeader(
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    wrongDstTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(
                        HttpStatus.SC_UNAUTHORIZED,
                        response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        UNAUTHORIZED,
                        PING,
                        HttpStatus.SC_UNAUTHORIZED);
                }

                get = new HttpGet(server.host() + "/status");
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        "-",
                        "/status",
                        HttpStatus.SC_OK);
                }

                get = new HttpGet(server.host() + "/status?local=false");
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(
                        HttpStatus.SC_UNAUTHORIZED,
                        response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        UNAUTHORIZED,
                        "/status?local=false",
                        HttpStatus.SC_UNAUTHORIZED);
                }

                String jsonUri = "/normalize-json";
                String json = "{ \"type\": 1 }";
                String normalizedJson = "{\"type\":1}";
                server.register(
                    new Pattern<>(jsonUri, false),
                    createJsonNormalizingHandler());

                HttpPost post = new HttpPost(server.host() + jsonUri);
                post.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, testTicket);
                post.setEntity(
                    new StringEntity(json, ContentType.APPLICATION_JSON));
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        SERV + testClient,
                        RequestHandlerMapper.POST,
                        jsonUri,
                        HttpStatus.SC_OK);
                    String body = CharsetUtils.toString(response.getEntity());
                    Assert.assertEquals(normalizedJson, body);
                    String stats = HttpAssert.stats(client, server);
                    HttpAssert.assertStat(
                        "reqstats-codes-2xx-request-length_ammm",
                        Long.toString(json.length()),
                        stats);
                }
                post = new HttpPost(server.host() + jsonUri);
                post.addHeader(
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    wrongDstTicket);
                post.setEntity(
                    new StringEntity(json, ContentType.APPLICATION_JSON));
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(
                        HttpStatus.SC_UNAUTHORIZED,
                        response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        UNAUTHORIZED,
                        RequestHandlerMapper.POST,
                        jsonUri,
                        HttpStatus.SC_UNAUTHORIZED);
                }

                // Should accept only prod ticket, warning for test ticket
                String ping = PING + "?prod=true&uid=1120000000004695";
                get = new HttpGet(server.host() + ping);
                get.addHeader(headerName, prodTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        SERV + prodClient,
                        ping,
                        HttpStatus.SC_OK);
                }

                get = new HttpGet(server.host() + ping);
                get.addHeader(headerName, testTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNotNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        UNAUTHORIZED,
                        ping,
                        HttpStatus.SC_OK);
                }

                // Should accept only test ticket, warning for prod ticket
                ping = PING + "?prod=true";
                get = new HttpGet(server.host() + ping);
                get.addHeader(headerName, prodTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNotNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        UNAUTHORIZED,
                        ping,
                        HttpStatus.SC_OK);
                }

                get = new HttpGet(server.host() + ping);
                get.addHeader(headerName, testTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        SERV + testClient,
                        ping,
                        HttpStatus.SC_OK);
                }

                // Should accept only test ticket, require user ticket
                ping = PING + "?prod=test";
                get = new HttpGet(server.host() + ping);
                get.addHeader(headerName, testTicket);
                get.addHeader(userTicketHeaderName, userTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        SERV + testClient + "/user:5598601",
                        ping,
                        HttpStatus.SC_OK);
                }

                get = new HttpGet(server.host() + ping);
                get.addHeader(headerName, testTicket);
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(
                        HttpStatus.SC_UNAUTHORIZED,
                        response);
                    Assert.assertNull(
                        response.getFirstHeader(
                            YandexHeaders.X_YANDEX_WARNING));
                    assertStatusCode(
                        log,
                        SERV + testClient + "/user:UNAUTHORIZED",
                        ping,
                        HttpStatus.SC_UNAUTHORIZED);
                }
            }
        }
    }

    @Test
    public void testMalformedRequestRecovery() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();
            final int requestsCount = 8;
            String request =
                RequestHandlerMapper.POST + ' ' + PING
                + " \r\nTransfer-Encoding: chunked\r\n"
                + "Connection: keep-alive\r\n\r\n0\r\naaaa\r\n\r\n";
            byte[] requestBody = request.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < requestsCount; ++i) {
                try (Socket socket = connectTo(server)) {
                    socket.getOutputStream().write(requestBody);
                    socket.getOutputStream().flush();
                    try {
                        while (true) {
                            int b = socket.getInputStream().read();
                            if (b == -1) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            try (CloseableHttpClient client =
                    Configs.createDefaultClient(clientBc()))
            {
                for (int i = 0; i < requestsCount; ++i) {
                    try (CloseableHttpResponse response =
                            client.execute(new HttpGet(server.host() + PING)))
                    {
                        HttpAssert.assertStatusCode(
                            HttpStatus.SC_OK,
                            response);
                    }
                }
            }
        }
    }

    @Test
    public void testBusyConnectionTimeout() throws Exception {
        ImmutableBaseServerConfig config = defaultConfig;
        try (HttpServer<ImmutableBaseServerConfig, T> server = server(config);
            CloseableHttpClient client =
                Configs.createDefaultClient(clientBc()))
        {
            server.register(
                new Pattern<>(URI, false),
                createSlowpokeHandler(
                    createDummyHandler(),
                    config.timeout() << 1));
            server.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(server.host() + URI)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
        }
    }

    @Test
    public void testDualProtocol() throws Exception {
        if (!https()) {
            return;
        }
        BaseServerConfigBuilder config = config();
        config.httpsConfig().httpPort(0);
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(config.build()))
        {
            server.start();
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(server.host() + PING)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    NAME + ' ' + BaseServerConfigBuilder.BUILT_DATE,
                    response.getFirstHeader(SERVER).getValue());
                Assert.assertEquals(
                    PONG,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(server.httpHost() + PING)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    NAME + ' ' + BaseServerConfigBuilder.BUILT_DATE,
                    response.getFirstHeader(SERVER).getValue());
                Assert.assertEquals(
                    PONG,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCompressedResponse() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < LONG_LINE_SIZE; ++i) {
            sb.append('b');
        }
        sb.append('"');
        String body = sb.toString();
        try (CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(
                        Configs.targetConfig(clientBc()))
                        .contentCompression(false)
                        .build(),
                    Configs.dnsConfig());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(config().gzip(true).build()))
        {
            server.register(
                new Pattern<>(URI, false),
                createJsonNormalizingHandler());
            server.start();
            HttpPost post = new HttpPost(server.host() + URI);
            post.setEntity(
                new StringEntity(body, ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpEntity entity = response.getEntity();
                Assert.assertEquals(body, CharsetUtils.toString(entity));
            }
            post = new HttpPost(server.host() + URI);
            post.setEntity(
                new StringEntity(body, ContentType.APPLICATION_JSON));
            post.addHeader(HttpHeaders.ACCEPT_ENCODING, "very bead encoding");
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpEntity entity = response.getEntity();
                Assert.assertEquals(body, CharsetUtils.toString(entity));
            }
            post = new HttpPost(server.host() + URI);
            post.setEntity(
                new StringEntity(body, ContentType.APPLICATION_JSON));
            post.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate");
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HttpHeaders.CONTENT_ENCODING,
                    GZIP,
                    response);
                DecodableByteArrayOutputStream responseBody =
                    CharsetUtils.toDecodable(response.getEntity());
                YandexAssert.assertLess(LONG_LINE_SIZE, responseBody.length());
                Decoder decoder = new Decoder(StandardCharsets.UTF_8);
                try (GZIPInputStream in =
                    new GZIPInputStream(
                        responseBody.processWith(
                            ByteArrayInputStreamFactory.INSTANCE)))
                {
                    IOStreamUtils.consume(in).processWith(decoder);
                }
                Assert.assertEquals(body, decoder.toString());
            }
            post = new HttpPost(server.host() + URI);
            post.setEntity(
                new StringEntity(body, ContentType.APPLICATION_JSON));
            post.addHeader(HttpHeaders.ACCEPT_ENCODING, GZIP);
            try (CloseableHttpClient client2 =
                    Configs.createDefaultClient(clientBc());
                CloseableHttpResponse response = client2.execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    HttpHeaders.CONTENT_ENCODING,
                    null,
                    response);
                Assert.assertEquals(
                    body,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHugeRequest() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "[server]"
                        + "\nbuffer-size = 5M\nfragment-size-hint = 1048276")
                    .build());
            CloseableHttpClient client =
                Configs.createDefaultClient(clientBc()))
        {
            server.register(
                new Pattern<>(URI, false),
                createJsonNormalizingHandler());
            server.start();
            final int len = 200000;
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            for (int i = 0; i < len; ++i) {
                sb.append(
                    "01234567890123456789012345678901234567890123456789");
            }
            sb.append('"');
            String body = new String(sb);
            final int iterations = 3;
            for (int j = 0; j < iterations; ++j) {
                server.logger().info(ITERATION + j);
                HttpPost post = new HttpPost(server.host() + URI);
                post.setEntity(
                    new StringEntity(body, ContentType.APPLICATION_JSON));
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals(
                        body,
                        CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }

    @Test
    public void testDynamicConfig() throws Exception {
        try (CloseableHttpClient client =
                 Configs.createDefaultClient(clientBc());
             HttpServer<ImmutableBaseServerConfig, T> server =
                server(defaultConfig))
        {
            server.start();

            HttpGet statusGet = new HttpGet(server.host() + STATUS);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, statusGet);

            HttpPost updateByPost = new HttpPost(server.host() + "/config-update");
            updateByPost.setEntity(
                new StringEntity(
                    "[limiter./status]\nconcurrency=0\nerror-status-code = 529\n",
                    StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, updateByPost);

            HttpAssert.assertStatusCode(529, client, statusGet);

            updateByPost.setEntity(
                new StringEntity(
                    "[limiter./status]\nconcurrency=1\nerror-status-code = 529\n",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, updateByPost);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, statusGet);

            updateByPost.setEntity(
                new StringEntity(
                    "[limiter./status]\nconcurrency=0\nerror-status-code = 529\n\n"
                        + "[limiter./stat{arg_prefix:me}]\nconcurrency=0\nerror-status-code = 429\n\n",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, updateByPost);
            HttpAssert.assertStatusCode(529, client, statusGet);
            statusGet = new HttpGet(server.host() + "/stat?prefix=me");
            HttpAssert.assertStatusCode(429, client, statusGet);

            File configFile = File.createTempFile(
                testName.getMethodName(),
                ".conf");
            configFile.deleteOnExit();

            Files.writeString(
                configFile.toPath(),
                "[limiter./ping]\nconcurrency=0\nerror-status-code = 439\n\n",
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);

            HttpGet updateByFile =
                new HttpGet(
                    server.host() + "/config-update?file="
                        + configFile.getAbsolutePath());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, updateByFile);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/status"));
            HttpAssert.assertStatusCode(
                439,
                client,
                new HttpGet(server.host() + "/ping"));
        }
    }

    @Test
    public void testDebugFlags() throws Exception {
        String prefix = "Debug flags become: [";
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(config("server.debug-flags = flag1, flag22\n").build()))
        {
            server.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            server.host() + "/add-debug-flag?flag=f3")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                Assert.assertEquals(
                    prefix,
                    body.substring(0, prefix.length()));
                Assert.assertEquals(
                    Set.of("flag1", "flag22", "f3"),
                    new HashSet<>(
                        Arrays.asList(
                            body.substring(prefix.length(), body.length() - 1)
                                .split(", *"))));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            server.host() + "/remove-debug-flag?flag=flag1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                Assert.assertEquals(
                    prefix,
                    body.substring(0, prefix.length()));
                Assert.assertEquals(
                    Set.of("flag22", "f3"),
                    new HashSet<>(
                        Arrays.asList(
                            body.substring(prefix.length(), body.length() - 1)
                                .split(", *"))));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            server.host() + "/remove-debug-flag?flag=f3")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                Assert.assertEquals(
                    prefix,
                    body.substring(0, prefix.length()));
                Assert.assertEquals(
                    Collections.singleton("flag22"),
                    new HashSet<>(
                        Arrays.asList(
                            body.substring(prefix.length(), body.length() - 1)
                                .split(", *"))));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            server.host() + "/remove-debug-flag?flag=flag22")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                Assert.assertEquals(prefix + ']', body);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(server.host() + "/debug-flags")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                Assert.assertEquals("Debug flags are: []", body);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            server.host() + "/add-debug-flag?flag=flag1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                Assert.assertEquals(
                    prefix,
                    body.substring(0, prefix.length()));
                Assert.assertEquals(
                    Collections.singleton("flag1"),
                    new HashSet<>(
                        Arrays.asList(
                            body.substring(prefix.length(), body.length() - 1)
                                .split(", *"))));
            }
        }
    }

    @Test
    public void testAutostater() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                 server(
                     config(
                         "\n[auto-request-stater]\n" +
                             "status = unconfigured\n")
                         .build()))
        {
            server.register(
                new Pattern<>("/my/super-complex/route", false),
                createDummyHandler());
            server.register(
                new Pattern<>("/my/asterisk", true),
                createDummyHandler());
            server.register(
                RequestPatternParser.INSTANCE.apply(
                    "/my/predicate/prefix*{arg_hochu_pirogok:true AND http_lsr:false}"),
                createDummyHandler());

            server.start();
            HttpAssert.assertStat(
                "my_super_complex_route-codes-5xx_ammm",
                "0",
                server);
            HttpAssert.assertStat(
                "my_asterisk_asterisk-codes-2xx_ammm",
                "0",
                server);
            HttpAssert.assertStat(
                "my_predicate_prefix_asterisk_arg_hochu_pirogok_true_and_http_lsr_false-total_ammm",
                "0",
                server);
        }
    }

    @Test
    public void testArgOnly() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "\n[limiter./ping{argonly_param:pampam}]"
                        + "\n[limiter./ping{arg_param:pampam}]"
                        + "\nconcurrency = 0"
                        + "\n[limiter./ping]"
                        + "\nconcurrency = 0"
                        + "\nerror-status-code = 529")
                        .build()))
        {
            server.start();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_REQUESTS_LIMIT_REACHED,
                client,
                new HttpGet(server.host() + "/ping"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/ping?param=pampam"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_REQUESTS_LIMIT_REACHED,
                client,
                new HttpGet(server.host() + "/ping?param=pamparam"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                client,
                new HttpGet(
                    server.host() + "/ping?param=pampam&param=pamparam"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_REQUESTS_LIMIT_REACHED,
                client,
                new HttpGet(
                    server.host() + "/ping?param=pamparam&param=pampam"));
        }
    }

    @Test
    public void testPathRegex() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "\n[limiter./*{path_regex:/first OR path_regex:/sec}]"
                        + "\nconcurrency = 0"
                        + "\n[limiter./*{path_regex:/second\\\\(s\\\\)?}]"
                        + "\nconcurrency = 0"
                        + "\nerror-status-code = 404"
                        + "\n[limiter./*{path_regex:/\\[sS]econd}]"
                        + "\nconcurrency = 0"
                        + "\nerror-status-code = 529")
                        .build()))
        {
            server.register(
                new Pattern<>("/", true),
                createDummyHandler(""));
            server.start();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                client,
                new HttpGet(server.host() + "/first"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                client,
                new HttpGet(server.host() + "/sec"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_NOT_FOUND,
                client,
                new HttpGet(server.host() + "/second"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_NOT_FOUND,
                client,
                new HttpGet(server.host() + "/seconds"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/seconds2"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_REQUESTS_LIMIT_REACHED,
                client,
                new HttpGet(server.host() + "/Second"));
            // Test that only full match matters
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/secnd"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/prefix/first"));
        }
    }

    @Test
    public void testArgRegex() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "\n[limiter./*{argregex_p:first OR argregex_p:sec}]"
                        + "\nconcurrency = 0"
                        + "\n[limiter./*{argregex_p:second\\\\(s\\\\)?}]"
                        + "\nconcurrency = 0"
                        + "\nerror-status-code = 404"
                        + "\n[limiter./*{argregex_p:\\[sS]econd}]"
                        + "\nconcurrency = 0"
                        + "\nerror-status-code = 529")
                        .build()))
        {
            server.register(
                new Pattern<>("/", true),
                createDummyHandler(""));
            server.start();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                client,
                new HttpGet(server.host() + "/?p=first"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                client,
                new HttpGet(server.host() + "/?p=sec"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_NOT_FOUND,
                client,
                new HttpGet(server.host() + "/?p=second"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_NOT_FOUND,
                client,
                new HttpGet(server.host() + "/?p=seconds"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/?p=seconds2"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_REQUESTS_LIMIT_REACHED,
                client,
                new HttpGet(server.host() + "/?p=Second"));
            // Test that only full match matters
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/?p=secnd"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/?p=prefix_first"));
            // Test that only single arg matters
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/?p=sec&p=sec"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/?p=first&p=sec"));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                client,
                new HttpGet(server.host() + "/?p=sec&p=first"));
        }
    }

    @Test
    public void testFilesStats() throws Exception {
        try (CloseableDeleter deleter =
            new CloseableDeleter(Files.createTempDirectory("root-dir")))
        {
            Path subdir =
                Files.createDirectory(deleter.path().resolve("subdir"));
            Path file1 = deleter.path().resolve("file1.json");
            Path file2 = subdir.resolve("file2.json");
            Path file3 = subdir.resolve("file3.json.java");
            Path file4 = subdir.resolve("file4.json.java");
            Files.write(file1, new byte[5]);
            Files.write(file2, new byte[17]);
            Files.write(file3, new byte[35]);
            Files.write(file4, new byte[1]);
            try (CloseableHttpClient client =
                    Configs.createDefaultClient(clientBc());
                HttpServer<ImmutableBaseServerConfig, T> server =
                    server(
                        config(
                            "[server.files-staters.root]\n"
                            + "root = " + deleter.path()
                            + "\nregex-filter = .*[.]json\n"
                            + "[server.files-staters.sub]\n"
                            + "root = " + deleter.path() + "/subdir\n")
                            .build()))
            {
                server.start();
                String stats = HttpAssert.stats(client, server);
                HttpAssert.assertStat(
                    "root-files-count_ammx",
                    "2",
                    stats);
                HttpAssert.assertStat(
                    "root-files-size_ammx",
                    "22",
                    stats);
                HttpAssert.assertStat(
                    "sub-files-count_ammx",
                    "3",
                    stats);
                HttpAssert.assertStat(
                    "sub-files-size_ammx",
                    "53",
                    stats);
            }
        }
    }

    @Test
    @SuppressWarnings("try")
    public void testHttpChecks() throws Exception {
        try (StaticServer target =
                new StaticServer(Configs.baseConfig("Target")))
        {
            target.add(URI, HttpStatus.SC_OK);
            target.start();
            Thread.sleep(1000L);
            try (CloseableHttpClient client =
                    Configs.createDefaultClient(clientBc());
                HttpServer<ImmutableBaseServerConfig, T> server =
                    server(
                        config(
                            "\n[server.http-check.target]"
                            + "\nuri = " + target.host() + "/uri"
                            + "\nconnections = 10"
                            + "\ntimeout = 20s"
                            + "\nkeep-alive = false"
                            + "\ncheck-interval = 1s")
                            .build()))
            {
                server.start();
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_OK,
                    client,
                    new HttpGet(server.host() + PING));

                target.add(URI, HttpStatus.SC_NOT_FOUND);
                Thread.sleep(2000L);
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_SERVICE_UNAVAILABLE,
                    client,
                    new HttpGet(server.host() + PING));

                target.add(URI, HttpStatus.SC_OK);
                Thread.sleep(2000L);
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_OK,
                    client,
                    new HttpGet(server.host() + PING));

                target.close();
                Thread.sleep(2000L);
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_SERVICE_UNAVAILABLE,
                    client,
                    new HttpGet(server.host() + PING));
            }
        }
    }
}

