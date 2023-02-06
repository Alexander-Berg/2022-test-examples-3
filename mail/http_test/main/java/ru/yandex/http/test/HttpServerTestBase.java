package ru.yandex.http.test;

import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import ru.yandex.http.config.ServerHttpsConfig;
import ru.yandex.http.server.async.BaseAsyncServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.client.ClientBuilder;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public abstract class HttpServerTestBase<T> extends TestBase {
    protected static final int WORKERS = 10;
    protected static final int CONNECTIONS = 50;
    protected static final int TIMEOUT = 3000;
    // HTTPS buffer size is 16 KB, so use something bigger than that
    protected static final int LONG_LINE_SIZE = 20000;
    protected static final int BUFFER_SIZE = 8192;
    protected static final int LOG_DELAY = 300;
    protected static final String SERVER = "Server";
    protected static final String NAME = "HttpServer";
    protected static final String ORIGIN = "HttpServer v0.01";
    protected static final String LOCALHOST = "localhost";
    protected static final String LOG_EXT = ".log";
    protected static final String LOG_FILENAME = "[log]\nfile = ";
    protected static final String LOGROTATE_URI = "/logrotate";
    protected static final String LOGROTATE_RESPONSE = "logs rotated";
    protected static final String TEXT = "Привет, мир!";
    protected static final String URI = "/uri";
    protected static final String PING = "/ping";
    protected static final String DISABLE_PING = "/disablePing";
    protected static final String STATUS = "/status";
    protected static final String PONG = "pong";
    protected static final String GET = "\"GET ";
    protected static final String ACCESSLOG_FILENAME =
        "\n[accesslog]\nfile = ";
    protected static final String STAT = "/stat";
    protected static final String SYSTEMEXIT = "/systemexit";
    protected static final String FORCE_GC = "/force-gc";
    protected static final String HTTP_1_1 =
        " HTTP/1.1\nConnection: keep-alive\r\n\r\n";
    protected static final String HTTP_200_OK = "HTTP/1.1 200 OK";
    protected static final String HTTP_400_BAD_REQUEST =
        "HTTP/1.0 400 Bad Request";
    protected static final String UTF_8 = "UTF-8";
    protected static final String UNAUTHORIZED = "serv:UNAUTHORIZED";
    protected static final String SERV = "serv:";
    protected static final String NO_USER = "-";
    protected static final Header CONNECTION_CLOSE =
        new BasicHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
    protected static final String CONFIG =
        "server.port = 0\n"
        + "server.workers.min = " + WORKERS + '\n'
        + "server.workers.percent = 0\n"
        + "server.connections = " + CONNECTIONS + '\n'
        + "server.backlog = " + CONNECTIONS + '\n'
        + "server.timeout = " + TIMEOUT + '\n';
    protected static final String SHORT_ACCESS_LOG_FORMAT =
        "\nformat = %{user} \"%{request}\" %{status}";
    protected static final String GZIP = "gzip";
    protected static final String LOGGER_OFF =
        "[log]\nlevel.min = off\n[accesslog]\nlevel.min = off\n";
    protected static final String ITERATION = "Iteration #";
    protected final ImmutableBaseServerConfig defaultConfig;
    protected final SSLConnectionSocketFactory sslConnectionSocketFactory;

    protected HttpServerTestBase() {
        super(true, 500L);
        try {
            defaultConfig = config().build();
            if (https()) {
                sslConnectionSocketFactory =
                    ClientBuilder.createSSLConnectionSocketFactory(
                        Configs.clientHttpsConfig(clientBc()));
                try (StaticServer staticServer =
                        new StaticServer(defaultConfig);
                    BaseAsyncServer<ImmutableBaseServerConfig> asyncServer =
                        new BaseAsyncServer<>(
                            new BaseServerConfigBuilder(defaultConfig)
                                .name("AsyncServer")
                                .build());
                    CloseableHttpClient syncClient =
                        ClientBuilder.createClient(
                            Configs.targetConfig(clientBc()),
                            Configs.dnsConfig());
                    SharedConnectingIOReactor reactor =
                        new SharedConnectingIOReactor(
                            defaultConfig,
                            Configs.dnsConfig());
                    AsyncClient asyncClient = new AsyncClient(
                        reactor,
                        Configs.targetConfig(clientBc())))
                {
                    staticServer.logger().info("Pre-tests https warmup");
                    staticServer.start();
                    asyncServer.start();
                    reactor.start();
                    asyncClient.start();
                    try (CloseableHttpResponse response =
                            syncClient.execute(
                                new HttpGet(staticServer.host() + PING)))
                    {
                        HttpAssert.assertStatusCode(
                            HttpStatus.SC_OK,
                            response);
                    }
                    asyncClient.execute(
                        asyncServer.host(),
                        new BasicAsyncRequestProducerGenerator("/ping"),
                        BasicAsyncResponseConsumerFactory.INSTANCE,
                        EmptyFutureCallback.INSTANCE)
                        .get();
                    staticServer.logger().info("HTTPS warmup completed");
                }
            } else {
                sslConnectionSocketFactory = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract HttpServer<ImmutableBaseServerConfig, T> createServer(
        ImmutableBaseServerConfig config)
        throws IOException;

    protected T createDummyHandler() {
        return createDummyHandler(HttpStatus.SC_OK);
    }

    protected abstract T createDummyHandler(final int status);

    protected abstract T createDummyHandler(final String response);

    protected abstract T createSlowpokeHandler(T next, long delay);

    protected abstract T createJsonNormalizingHandler();

    protected abstract boolean https();

    protected abstract boolean serverBc();

    protected abstract boolean clientBc();

    protected HttpServer<ImmutableBaseServerConfig, T> server(
        final ImmutableBaseServerConfig config)
        throws ConfigException, IOException
    {
        return createServer(
            applyHttps(new BaseServerConfigBuilder(config)).build());
    }

    public BaseServerConfigBuilder config()
        throws ConfigException, IOException
    {
        return config("");
    }

    public BaseServerConfigBuilder config(final String suffix)
        throws ConfigException, IOException
    {
        IniConfig config = new IniConfig(new StringReader(CONFIG + suffix));
        BaseServerConfigBuilder builder =
            new BaseServerConfigBuilder(config).name(NAME);
        config.checkUnusedKeys();
        return new BaseServerConfigBuilder(applyHttps(builder).build());
    }

    public BaseServerConfigBuilder applyHttps(
        final BaseServerConfigBuilder builder)
    {
        if (https()) {
            ServerHttpsConfig httpsConfig = builder.httpsConfig();
            builder.httpsConfig(Configs.serverHttpsConfig(serverBc()));
            if (httpsConfig != null) {
                builder.httpsConfig().httpPort(httpsConfig.httpPort());
            }
        }
        return builder;
    }

    public Socket connectTo(
        final HttpServer<ImmutableBaseServerConfig, ?> server)
        throws Exception
    {
        Socket socket = new Socket(LOCALHOST, server.port());
        if (server.sslContext() != null) {
            socket = sslConnectionSocketFactory.createLayeredSocket(
                socket,
                LOCALHOST,
                server.port(),
                null);
        }
        return socket;
    }

    public static void yield() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
    }
}

