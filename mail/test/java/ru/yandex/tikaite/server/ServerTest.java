package ru.yandex.tikaite.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.DirectServer;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.DefaultHttpServerFactory;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.tikaite.config.ImmutableTikaiteConfig;
import ru.yandex.tikaite.config.TikaiteConfigBuilder;

public class ServerTest {
    private static final String PING = "/ping";

    private static final String LOCALHOST = "http://localhost:";

    public static Reader getConfigReader(final String suffix) {
        return getConfigReader(1, suffix);
    }

    public static Reader getConfigReader(final int port) {
        return getConfigReader(port, "");
    }

    public static Reader getConfigReader(
        final int port,
        final String suffix)
    {
        return new StringReader(
            "log.level.min = all\nserver.port = 0\nserver.workers.min = 2\n"
            + "server.connections = 50\n"
            + "server.timeout = 16000\nstorage.timeout = 15000\n"
            + "storage.connections = 100\nstorage.host = localhost\n"
            + "storage.port = " + port
            + "\nextractor.received-chain-parser.yandex-nets.file = "
            + Paths.getSandboxResourcesRoot() + "/yandex-nets.txt\n"
            + suffix);
    }

    public static ImmutableTikaiteConfig getConfig()
        throws ConfigException, IOException
    {
        return getConfig(1);
    }

    public static ImmutableTikaiteConfig getConfig(final int port)
        throws ConfigException, IOException
    {
        return getConfig(port, "");
    }

    public static ImmutableTikaiteConfig getConfig(final String suffix)
        throws ConfigException, IOException
    {
        return getConfig(1, suffix);
    }

    public static ImmutableTikaiteConfig getConfig(
        final int port,
        final String suffix)
        throws ConfigException, IOException
    {
        IniConfig properties = new IniConfig(getConfigReader(port, suffix));
        return new TikaiteConfigBuilder(
            new TikaiteConfigBuilder(properties).build())
            .build();
    }

    @Test
    public void testCommandLine()
        throws ConfigException, IOException
    {
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos));
            ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
            System.setErr(new PrintStream(baosErr));
            File config = File.createTempFile("testCommandLine", ".conf");
            config.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(config);
                BufferedReader reader = new BufferedReader(getConfigReader(1)))
            {
                String line;
                while ((line = reader.readLine()) != null) {
                    fos.write((line + '\n').getBytes(StandardCharsets.UTF_8));
                }
            }
            try (HttpServer<?, ?> server =
                    Server.main(
                        new DefaultHttpServerFactory<>(Server.class),
                        config.getAbsolutePath()))
            {
                Assert.assertEquals("###started###\n", baos.toString());
                try (BufferedReader reader =
                        new BufferedReader(
                            new StringReader(baosErr.toString())))
                {
                    boolean success = false;
                    for (
                        String line = reader.readLine();
                        line != null;
                        line = reader.readLine())
                    {
                        String substring = "Starting Tikaite on ";
                        if (line.contains(substring)) {
                            int idx = line.indexOf(substring);
                            int brace = line.indexOf('(', idx);
                            int port =
                                Integer.parseInt(
                                    line.substring(
                                        line.lastIndexOf(':', brace) + 1,
                                        brace));
                            Assert.assertEquals(server.port(), port);
                            HttpAssert.assertStatusCode(
                                HttpStatus.SC_OK,
                                port,
                                PING);
                            success = true;
                            break;
                        }
                    }
                    Assert.assertTrue(success);
                }
            }
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
        }
    }

    @Test
    public void testRequestWithoutName() throws Exception {
        try (Server server = new Server(getConfig(1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            server.start();
            HttpResponse response = client.execute(new HttpGet(LOCALHOST
                + server.port() + "/get/something?param=100500"));
            Assert.assertEquals(
                HttpStatus.SC_BAD_REQUEST,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "Tikaite " + BaseServerConfigBuilder.BUILT_DATE,
                response.getFirstHeader("Server").getValue());
            EntityUtils.consume(response.getEntity());
            response = client.execute(
                new HttpGet(
                    LOCALHOST
                    + server.port()
                    + "/get/something?param=100500&name=abcd"));
            Assert.assertEquals(
                HttpStatus.SC_NOT_IMPLEMENTED,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
        }
    }

    @Test
    public void testProtocolException() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            DirectServer backend = new DirectServer(
                s -> new DirectServer.StaticResponseTask(
                    s,
                    "HTTP/1.1 200 OK\r\nContent-Length 0\r\n\r\n"));
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.start();
            server.start();
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            LOCALHOST + server.port()
                            + "/get/stid?name=disk")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
            }
            Assert.assertEquals(1, backend.requestsReceived());
        }
    }
}

