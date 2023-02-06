package ru.yandex.http.test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.http.config.DnsConfigBuilder;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.ImmutableDnsConfig;
import ru.yandex.http.config.ImmutableHttpTargetConfig;
import ru.yandex.http.config.RetriesConfigBuilder;
import ru.yandex.http.util.BadResponseException;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public abstract class HttpClientTest<
    C extends GenericAutoCloseable<IOException>>
    extends TestBase
{
    protected static final int TIMEOUT = 3000;
    protected static final int RETRIES = 3;
    protected static final String PING = "/ping";
    protected static final String URI = "/uri";

    private final ImmutableBaseServerConfig serverConfig;

    protected HttpClientTest() {
        super(true, 1000L);
        try {
            if (https()) {
                serverConfig = new BaseServerConfigBuilder(Configs.baseConfig())
                    .httpsConfig(Configs.serverHttpsConfig(serverBc()))
                    .build();
            } else {
                serverConfig = Configs.baseConfig();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract C createClient(
        ImmutableHttpTargetConfig backendConfig,
        ImmutableDnsConfig dnsConfig)
        throws Exception;

    protected void sendRequest(final C client, final String uri)
        throws Exception
    {
        sendRequest(client, new HttpGet(uri));
    }

    protected void sendRequest(final C client, final HttpUriRequest request)
        throws Exception
    {
        sendRequest(client, HttpStatus.SC_OK, request);
    }

    protected abstract void sendRequest(
        C client,
        int expectedStatus,
        HttpUriRequest request)
        throws Exception;

    protected abstract boolean https();

    protected abstract boolean serverBc();

    protected abstract boolean clientBc();

    public ImmutableBaseServerConfig config() throws Exception {
        return serverConfig;
    }

    public ImmutableBaseServerConfig config(final String name)
        throws Exception
    {
        return serverConfig;
    }

    @Test
    public void testProxyHostHeader() throws Exception {
        try (StaticServer proxy = new StaticServer(config());
            C client =
                createClient(
                    new HttpTargetConfigBuilder(
                        Configs.targetConfig(clientBc()))
                        .proxy(proxy.host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String httpUri = "http://localhost:8081/some/uri?here";
            proxy.add(
                httpUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    HTTP.TARGET_HOST,
                    proxy.host().toHostString()));
            String httpsUri = "https://localhost:8081/some/uri?here";
            proxy.add(
                httpsUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_ACCEPTED),
                    HTTP.TARGET_HOST,
                    proxy.host().toHostString()));
            proxy.start();
            sendRequest(client, httpUri);
            sendRequest(client, HttpStatus.SC_ACCEPTED, new HttpGet(httpsUri));
        }
    }

    @Test
    public void testConnectionReuse() throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client = createClient(
                Configs.targetConfig(clientBc()),
                Configs.dnsConfig()))
        {
            Set<Object> connections = new HashSet<>();
            server.register(
                new Pattern<>(URI, false),
                new ContextConsumingHttpRequestHandler(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    context ->
                        connections.add(
                            context.getAttribute(
                                HttpCoreContext.HTTP_CONNECTION))));
            server.start();
            final int requests = 10;
            for (int i = 0; i < requests; ++i) {
                sendRequest(client, server.host() + URI);
            }
            Assert.assertEquals(1, connections.size());
        }
    }

    @Test
    public void testProxyConnectionReuse() throws Exception {
        try (StaticServer proxy = new StaticServer(config());
            C client =
                createClient(
                    new HttpTargetConfigBuilder(
                        Configs.targetConfig(clientBc()))
                        .proxy(proxy.host())
                        .build(),
                    Configs.dnsConfig()))
        {
            Set<Object> connections = new HashSet<>();
            String httpUri = "http://localhost:8082/some/uri?here";
            proxy.add(
                httpUri,
                new StaticHttpResource(
                    new ContextConsumingHttpRequestHandler(
                        new StaticHttpItem(HttpStatus.SC_OK),
                        context ->
                            connections.add(
                                context.getAttribute(
                                    HttpCoreContext.HTTP_CONNECTION)))));
            String httpsUri = "https://localhost:8082/some/uri?here";
            proxy.add(
                httpsUri,
                new StaticHttpResource(
                    new ContextConsumingHttpRequestHandler(
                        new StaticHttpItem(HttpStatus.SC_ACCEPTED),
                        context ->
                            connections.add(
                                context.getAttribute(
                                    HttpCoreContext.HTTP_CONNECTION)))));
            proxy.start();
            final int requests = 10;
            for (int i = 0; i < requests; ++i) {
                sendRequest(client, httpUri);
                sendRequest(
                    client,
                    HttpStatus.SC_ACCEPTED,
                    new HttpGet(httpsUri));
            }
            // Two target uris created two different http routes
            Assert.assertEquals(2, connections.size());
        }
    }

    @Test
    public void testNoConnectionReuse() throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client =
                createClient(
                    new HttpTargetConfigBuilder(
                        Configs.targetConfig(clientBc()))
                        .keepAlive(false)
                        .build(),
                    Configs.dnsConfig()))
        {
            Set<Object> connections = new HashSet<>();
            server.register(
                new Pattern<>(URI, false),
                new ExpectingHeaderHttpItem(
                    new ContextConsumingHttpRequestHandler(
                        new StaticHttpItem(HttpStatus.SC_OK),
                        context ->
                            connections.add(
                                context.getAttribute(
                                    HttpCoreContext.HTTP_CONNECTION))),
                    HTTP.CONN_DIRECTIVE,
                    HTTP.CONN_CLOSE));
            server.start();
            final int requests = 10;
            for (int i = 0; i < requests; ++i) {
                sendRequest(client, server.host() + URI);
            }
            Assert.assertEquals(requests, connections.size());
        }
    }

    @Test
    public void testAcceptCharset() throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client =
                createClient(
                    new HttpTargetConfigBuilder(
                        Configs.targetConfig(clientBc()))
                        .responseCharset(StandardCharsets.UTF_16BE)
                        .build(),
                    Configs.dnsConfig()))
        {
            server.add(
                URI,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    HttpHeaders.ACCEPT_CHARSET,
                    "UTF-16BE"));
            server.start();
            sendRequest(client, server.host() + URI);
        }
    }

    @Test
    public void testNoUserAgent() throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client = createClient(
                Configs.targetConfig(clientBc()),
                Configs.dnsConfig()))
        {
            server.add(
                URI,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    HttpHeaders.USER_AGENT,
                    null));
            server.start();
            sendRequest(client, server.host() + URI);
        }
    }

    @Test
    public void testNoAcceptCharset() throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client = createClient(
                new HttpTargetConfigBuilder(
                    Configs.targetConfig(clientBc()))
                    .briefHeaders(true)
                    .build(),
                Configs.dnsConfig()))
        {
            server.add(
                URI,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    HttpHeaders.ACCEPT_CHARSET,
                    null));
            server.start();
            sendRequest(client, server.host() + URI);
        }
    }

    private void testRedirects(final boolean redirects) throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client = createClient(
                new HttpTargetConfigBuilder(
                    Configs.targetConfig(clientBc()))
                    .redirects(redirects)
                    .build(),
                Configs.dnsConfig()))
        {
            server.start();
            String redirectUri = "/second-uri";
            String body = "Test body";
            server.add(
                redirectUri,
                new StaticHttpItem(HttpStatus.SC_OK),
                new ExpectingHttpItem(body),
                NotImplementedHttpItem.INSTANCE);
            server.add(
                URI,
                new StaticHttpResource(
                    new HeaderHttpItem(
                        new StaticHttpItem(HttpStatus.SC_TEMPORARY_REDIRECT),
                        HttpHeaders.LOCATION,
                        redirectUri)));

            int expectedStatus;
            int redirectUriAccessCount;
            if (redirects) {
                expectedStatus = HttpStatus.SC_OK;
                redirectUriAccessCount = 1;
            } else {
                expectedStatus = HttpStatus.SC_TEMPORARY_REDIRECT;
                redirectUriAccessCount = 0;
            }

            sendRequest(
                client,
                expectedStatus,
                new HttpGet(server.host() + URI));
            Assert.assertEquals(1, server.accessCount(URI));
            Assert.assertEquals(
                redirectUriAccessCount,
                server.accessCount(redirectUri));

            HttpPost post = new HttpPost(server.host() + URI);
            post.setEntity(new StringEntity(body, ContentType.TEXT_PLAIN));
            sendRequest(client, expectedStatus, post);
            Assert.assertEquals(2, server.accessCount(URI));
            Assert.assertEquals(
                redirectUriAccessCount << 1,
                server.accessCount(redirectUri));
        }
    }

    @Test
    public void testRedirects() throws Exception {
        testRedirects(true);
    }

    @Test
    public void testRedirectsDisabled() throws Exception {
        testRedirects(false);
    }

    @Test
    public void testFakeDns() throws Exception {
        HttpTargetConfigBuilder targetConfig =
            new HttpTargetConfigBuilder(Configs.targetConfig(clientBc()));
        targetConfig.httpsConfig().verifyHostname(false);
        try (StaticServer server = new StaticServer(config());
            C client = createClient(
                targetConfig.build(),
                new DnsConfigBuilder(Configs.dnsConfig())
                    .dnsHostsMapping(
                        Collections.singletonMap("google.com", "localhost"))
                    .build()))
        {
            server.add(URI, HttpStatus.SC_OK);
            server.start();
            sendRequest(
                client,
                server.scheme() + "://google.com:" + server.port() + URI);
        }
    }

    @Test
    public void testFakeDnsUnknownHost() throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client = createClient(
                Configs.targetConfig(clientBc()),
                new DnsConfigBuilder(Configs.dnsConfig())
                    .dnsHostsMapping(Collections.singletonMap("yandex.ru", ""))
                    .build()))
        {
            server.add(URI, HttpStatus.SC_OK);
            server.start();
            try {
                sendRequest(
                    client,
                    server.scheme() + "://yandex.ru:" + server.port() + URI);
                Assert.fail();
            } catch (Exception e) {
                if (e instanceof ExecutionException) {
                    YandexAssert.assertInstanceOf(
                        UnknownHostException.class,
                        e.getCause());
                } else {
                    YandexAssert.assertInstanceOf(
                        UnknownHostException.class,
                        e);
                }
            }
        }
    }

    @Test
    public void testSocketTimeout() throws Exception {
        final int serverTimeout = 10000;
        final int clientTimeout = 2000;
        try (StaticServer server = new StaticServer(
                new BaseServerConfigBuilder(config())
                    .timeout(serverTimeout)
                    .build());
            C client = createClient(
                new HttpTargetConfigBuilder(
                    Configs.targetConfig(clientBc()))
                    .timeout(clientTimeout)
                    .build(),
                Configs.dnsConfig()))
        {
            String slowpokeUri = "/slowpoke";
            server.add(URI, HttpStatus.SC_OK);
            server.add(
                slowpokeUri,
                new SlowpokeHttpResource(
                    StaticHttpItem.OK,
                    clientTimeout + (clientTimeout >> 2)));
            server.start();
            sendRequest(client, server.host() + URI);
            // Right after requests connection kept alive
            Assert.assertEquals(
                server.status(true).get(HttpServer.ACTIVE_CONNECTIONS),
                1);
            Thread.sleep(clientTimeout + (clientTimeout >> 2));
            // Check that connection is still there
            Assert.assertEquals(
                server.status(true).get(HttpServer.ACTIVE_CONNECTIONS),
                1);
            try {
                sendRequest(client, server.host() + slowpokeUri);
                Assert.fail();
            } catch (Exception e) {
                if (e instanceof ExecutionException) {
                    YandexAssert.assertInstanceOf(
                        SocketTimeoutException.class,
                        e.getCause());
                } else {
                    YandexAssert.assertInstanceOf(
                        SocketTimeoutException.class,
                        e);
                }
                // Server still not aware that this connection is dead
                Assert.assertEquals(
                    server.status(true).get(HttpServer.ACTIVE_CONNECTIONS),
                    1);
            }
            Thread.sleep(clientTimeout + (clientTimeout >> 2));
            // Server realized that connection is dead
            Assert.assertEquals(
                0,
                server.status(true).get(HttpServer.ACTIVE_CONNECTIONS));
        }
    }

    protected void testIoRetriesPostCheck(C client) throws Exception {
    }

    @Test
    public void testIoRetries() throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client =
                createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .connections(1)
                        .timeout(TIMEOUT)
                        .poolTimeout(TIMEOUT >> 1)
                        .ioRetries(new RetriesConfigBuilder().count(RETRIES))
                        .build(),
                    Configs.dnsConfig()))
        {
            server.add(
                URI,
                new SlowpokeHttpResource(
                    new StaticHttpResource(HttpStatus.SC_OK),
                    TIMEOUT << 1));
            server.start();
            try {
                sendRequest(client, server.host() + URI);
                Assert.fail();
            } catch (SocketTimeoutException e) {
            }
            Thread.sleep(TIMEOUT << 1);
            Assert.assertEquals(RETRIES + 1, server.accessCount(URI));
            testIoRetriesPostCheck(client);
        }
    }

    @Test
    public void testHttpRetries() throws Exception {
        try (StaticServer server = new StaticServer(config());
            C client =
                createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .connectTimeout(0)
                        .httpRetries(new RetriesConfigBuilder().count(RETRIES))
                        .build(),
                Configs.dnsConfig()))
        {
            server.add(URI, HttpStatus.SC_GATEWAY_TIMEOUT, "Hello world");
            server.start();
            try {
                sendRequest(client, server.host() + URI);
                Assert.fail();
            } catch (HttpException e) {
                Assert.assertEquals(RETRIES + 1, server.accessCount(URI));
            }
            server.add(
                URI,
                new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                new StaticHttpItem(HttpStatus.SC_ACCEPTED));
            sendRequest(
                client,
                HttpStatus.SC_ACCEPTED,
                new HttpGet(server.host() + URI));
            Assert.assertEquals(2, server.accessCount(URI));
            server.add(
                URI,
                new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                new StaticHttpItem(HttpStatus.SC_NOT_FOUND));
            try {
                sendRequest(client, server.host() + URI);
                Assert.fail();
            } catch (BadResponseException e) {
                Assert.assertEquals(
                    HttpStatus.SC_NOT_FOUND,
                    e.statusCode());
            }
            Assert.assertEquals(2, server.accessCount(URI));
        }
    }
}

