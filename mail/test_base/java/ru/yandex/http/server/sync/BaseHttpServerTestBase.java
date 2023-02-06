package ru.yandex.http.server.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.compress.GzipOutputStream;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.HttpServerTest;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.HeaderUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.client.ClientBuilder;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.http.util.request.RequestPatternParser;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.DecodableByteArrayOutputStream;

public abstract class BaseHttpServerTestBase
    extends HttpServerTest<HttpRequestHandler>
{
    @Override
    protected BaseHttpServer<ImmutableBaseServerConfig> createServer(
        final ImmutableBaseServerConfig config)
        throws IOException
    {
        return new BaseHttpServer<>(config);
    }

    @Override
    protected HttpRequestHandler createDummyHandler(final int status) {
        return new StaticHttpItem(status);
    }

    @Override
    protected HttpRequestHandler createDummyHandler(final String response) {
        return new StaticHttpItem(response);
    }

    @Override
    protected HttpRequestHandler createSlowpokeHandler(
        final HttpRequestHandler next,
        final long delay)
    {
        return new SlowpokeHttpItem(next, delay);
    }

    @Override
    protected HttpRequestHandler createJsonNormalizingHandler() {
        return JsonNormalizingHandler.INSTANCE;
    }

    @Test
    public void testCompressedPost() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LONG_LINE_SIZE; ++i) {
            sb.append('b');
        }
        String body = sb.toString();
        try (CloseableHttpClient client = Configs.createDefaultClient();
            HttpServer<ImmutableBaseServerConfig, HttpRequestHandler> server =
                server(config().build()))
        {
            server.register(
                new Pattern<>(URI, false),
                new ExpectingHttpItem(body));
            server.start();
            HttpPost post = new HttpPost(server.host() + URI);
            post.setEntity(new GzipCompressingEntity(new StringEntity(body)));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
        }
    }

    @Test
    public void testAlreadyCompressedResponse() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LONG_LINE_SIZE; ++i) {
            sb.append('b');
        }
        String body = sb.toString();
        DecodableByteArrayOutputStream out =
            new DecodableByteArrayOutputStream();
        try (Writer writer =
                new OutputStreamWriter(
                    new GzipOutputStream(out),
                    StandardCharsets.UTF_8))
        {
            writer.write(body);
        }
        ByteArrayEntity bodyEntity =
            out.processWith(ByteArrayEntityFactory.INSTANCE);
        bodyEntity.setContentEncoding(
            HeaderUtils.createHeader(HttpHeaders.CONTENT_ENCODING, GZIP));

        try (CloseableHttpClient client = Configs.createDefaultClient();
            StaticServer server =
                new StaticServer(config().gzip(true).build()))
        {
            server.add(URI, bodyEntity);
            server.start();
            HttpGet get = new HttpGet(server.host() + URI);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    body,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void testFailFast(final int sleep, final boolean expected)
        throws Exception
    {
        try (HttpServer<ImmutableBaseServerConfig, HttpRequestHandler> server =
                server(config().connectionCloseCheckInterval(2000L).build());
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .timeout(sleep >> 1)
                        .build(),
                    Configs.dnsConfig()))
        {
            final AtomicBoolean called = new AtomicBoolean();
            String uri = "/slowpoke";
            server.register(
                new Pattern<>(uri, false),
                new HttpRequestHandler() {
                    @Override
                    public void handle(
                        final HttpRequest request,
                        final HttpResponse response,
                        final HttpContext context)
                        throws HttpException, IOException
                    {
                        LoggingHttpServerConnection conn =
                            (LoggingHttpServerConnection)
                                context.getAttribute(
                                    HttpCoreContext.HTTP_CONNECTION);
                        logger.info(
                            "Fail fast check timeout: "
                            + conn.failFastCheckTimeout());
                        EntityTemplate entity = new EntityTemplate(
                            new ContentProducer() {
                                @Override
                                public void writeTo(final OutputStream out) {
                                    called.set(true);
                                }
                            });
                        entity.setChunked(true);
                        response.setEntity(entity);
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                        }
                    }
                });
            server.start();
            try {
                client.execute(new HttpGet(server.host() + PING));
            } catch (IOException e) {
                // Just a warmup
            }
            final long oneSecond = 1000L;
            Thread.sleep(oneSecond);
            logger.info("Warmup completed, sending actual request");
            try {
                client.execute(new HttpGet(server.host() + uri));
                Assert.fail();
            } catch (IOException e) {
                // Just as planned
            }
            Thread.sleep(sleep + (sleep >> 1));
            Assert.assertEquals(expected, called.get());
        }
    }

    @Test
    public void testFailFastSucceeded() throws Exception {
        final int bigSleep = 4000;
        testFailFast(bigSleep, false);
    }

    @Test
    public void testFailFastFasterThanCheck() throws Exception {
        // fast requests handlers won't be checked for fail fast
        final int smallSleep = 500;
        testFailFast(smallSleep, true);
    }

    @Test
    public void testRequestHandlerMapper() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, HttpRequestHandler> server =
                server(config().build());
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(server.host())
                        .build(),
                    Configs.dnsConfig()))
        {
            final String[] full = new String[1];
            final String[] port = new String[1];
            String fullUri = "http://yandex.ru/path";
            String portUri = "http://yandex.ru:888/path";
            server.register(
                new Pattern<>(fullUri, false),
                new HttpRequestHandler() {
                    @Override
                    public void handle(
                        final HttpRequest request,
                        final HttpResponse response,
                        final HttpContext context)
                        throws HttpException, IOException
                    {
                        synchronized (full) {
                            full[0] = request.getRequestLine().getUri();
                        }
                    }
                });
            server.register(
                new Pattern<>(":888/path", false),
                new HttpRequestHandler() {
                    @Override
                    public void handle(
                        final HttpRequest request,
                        final HttpResponse response,
                        final HttpContext context)
                        throws HttpException, IOException
                    {
                        synchronized (port) {
                            port[0] = request.getRequestLine().getUri();
                        }
                    }
                });
            server.start();
            client.execute(new HttpGet(fullUri));
            synchronized (full) {
                Assert.assertEquals(fullUri, full[0]);
            }
            client.execute(new HttpGet(portUri));
            synchronized (port) {
                Assert.assertEquals(portUri, port[0]);
            }
        }
    }

    @Test
    public void testRequestHandlerMapperPathless() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, HttpRequestHandler> server =
                server(config().build()))
        {
            final String[] full = new String[1];
            final String[] port = new String[1];
            String fullUri = "http://yandex.ru";
            String portUri = "http://yandex.ru:888";
            server.register(
                new Pattern<>(fullUri, false),
                new HttpRequestHandler() {
                    @Override
                    public void handle(
                        final HttpRequest request,
                        final HttpResponse response,
                        final HttpContext context)
                        throws HttpException, IOException
                    {
                        synchronized (full) {
                            full[0] = request.getRequestLine().getUri();
                        }
                    }
                });
            server.register(
                new Pattern<>(":888", false),
                new HttpRequestHandler() {
                    @Override
                    public void handle(
                        final HttpRequest request,
                        final HttpResponse response,
                        final HttpContext context)
                        throws HttpException, IOException
                    {
                        synchronized (port) {
                            port[0] = request.getRequestLine().getUri();
                        }
                    }
                });
            server.start();
            String http200 = "HTTP/1.1 200 OK";
            try (Socket socket = connectTo(server)) {
                byte[] data =
                    "GET http://yandex.ru HTTP/1.1\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8)))
                {
                    socket.getOutputStream().write(data);
                    socket.getOutputStream().flush();
                    Assert.assertEquals(http200, reader.readLine());
                }
            }
            synchronized (full) {
                Assert.assertEquals(fullUri, full[0]);
            }
            try (Socket socket = connectTo(server)) {
                byte[] data =
                    "GET http://yandex.ru:888 HTTP/1.1\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8)))
                {
                    socket.getOutputStream().write(data);
                    socket.getOutputStream().flush();
                    Assert.assertEquals(http200, reader.readLine());
                }
            }
            synchronized (port) {
                Assert.assertEquals(portUri, port[0]);
            }
        }
    }

    @Test
    public void testRequestPredicate() throws Exception {
        String uri = "/uri";
        String simple = "simple";
        String cgi = "cgi";
        String methodGet = "method get";
        String methodPost = "method post";
        String someText = "some text here";
        String anotherText = "another text here";
        try (HttpServer<ImmutableBaseServerConfig, HttpRequestHandler> server =
                server(config().build());
            CloseableHttpClient client = ClientBuilder.createClient(
                Configs.targetConfig(),
                Configs.dnsConfig()))
        {
            StaticHttpItem handler =
                new StaticHttpItem(HttpStatus.SC_OK, simple);
            server.register(
                new Pattern<>(uri, false),
                handler,
                RequestHandlerMapper.GET);
            HttpRequestHandler oldHandler = server.register(
                RequestPatternParser.INSTANCE.apply(uri),
                handler,
                RequestHandlerMapper.GET);
            Assert.assertSame(handler, oldHandler);

            server.register(
                RequestPatternParser.INSTANCE.apply("/uri2*{arg_param:value}"),
                new StaticHttpItem(HttpStatus.SC_OK, cgi));
            server.register(
                RequestPatternParser.INSTANCE.apply(
                    "/uri2*{http_authorization:\"le me in\" AND method:GET}"),
                new StaticHttpItem(HttpStatus.SC_OK, methodGet));
            server.register(
                RequestPatternParser.INSTANCE.apply(
                    "/uri2*{http_authorization:le\\ me\\ in AND method:POST}"),
                new StaticHttpItem(HttpStatus.SC_OK, methodPost));
            server.register(
                RequestPatternParser.INSTANCE.apply(
                    "/uri3*{http_my-header:hello OR arg_my-arg:hello}"),
                new StaticHttpItem(HttpStatus.SC_OK, someText));
            server.register(
                RequestPatternParser.INSTANCE.apply("/uri3*"),
                new StaticHttpItem(HttpStatus.SC_OK, anotherText));
            server.start();

            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(server.host() + "/uri2/huri")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(server.host() + "/uri2/huri?param=value")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    cgi,
                    CharsetUtils.toString(response.getEntity()));
            }

            Header header =
                new BasicHeader(HttpHeaders.AUTHORIZATION, "le me in");
            HttpGet get = new HttpGet(server.host() + "/uri23");
            get.addHeader(header);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    methodGet,
                    CharsetUtils.toString(response.getEntity()));
            }
            HttpPost post = new HttpPost(server.host() + "/uri234");
            post.addHeader(header);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    methodPost,
                    CharsetUtils.toString(response.getEntity()));
            }
            HttpPut put = new HttpPut(server.host() + "/uri2345");
            put.addHeader(header);
            try (CloseableHttpResponse response = client.execute(put)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
            }
            String headerName = "My-Header";
            String uri3 = server.host() + "/uri321";
            try (CloseableHttpResponse response =
                client.execute(new HttpGet(uri3)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    anotherText,
                    CharsetUtils.toString(response.getEntity()));
            }
            get = new HttpGet(uri3);
            get.addHeader(headerName, "Hello");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    anotherText,
                    CharsetUtils.toString(response.getEntity()));
            }
            get = new HttpGet(uri3);
            get.addHeader(headerName, "hello");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    someText,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(uri3 + "?my-arg=hello")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    someText,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(uri3 + "?my-arg=ehlo")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    anotherText,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCorpPredicate() throws Exception {
        String corp = "corp";
        String notCorp = "not corp";
        try (HttpServer<ImmutableBaseServerConfig, HttpRequestHandler> server =
                server(config().build());
            CloseableHttpClient client = ClientBuilder.createClient(
                Configs.targetConfig(),
                Configs.dnsConfig()))
        {
            server.register(
                RequestPatternParser.INSTANCE.apply("/corp{corp_uid:true}"),
                new StaticHttpItem(HttpStatus.SC_OK, corp));
            server.register(
                RequestPatternParser.INSTANCE.apply("/corp{corp_uid:false}"),
                new StaticHttpItem(HttpStatus.SC_OK, notCorp));
            server.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            server.host() + "/corp?uid=1120000000004695")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    corp,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            server.host() + "/corp?uid=5598601")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    notCorp,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(server.host() + "/corp")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
            }
        }
    }

    @Test
    public void testExperimentPredicate() throws Exception {
        String first = "first";
        String second = "second";
        try (HttpServer<ImmutableBaseServerConfig, HttpRequestHandler> server =
                server(config().build());
            CloseableHttpClient client = ClientBuilder.createClient(
                Configs.targetConfig(),
                Configs.dnsConfig()))
        {
            server.register(
                RequestPatternParser.INSTANCE.apply("/{experiment:91805}"),
                new StaticHttpItem(HttpStatus.SC_OK, first));
            server.register(
                RequestPatternParser.INSTANCE.apply("/{experiment:92847}"),
                new StaticHttpItem(HttpStatus.SC_OK, second));
            server.start();

            HttpGet get = new HttpGet(server.host().toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_NOT_IMPLEMENTED,
                    response);
            }
            get = new HttpGet(server.host().toString());
            get.setHeader(
                YandexHeaders.X_ENABLED_BOXES,
                "91806,0,7;92847,0,8;92806,0,15;92758,0,83;93511,0,65");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    second,
                    CharsetUtils.toString(response.getEntity()));
            }
            get = new HttpGet(server.host().toString());
            get.setHeader(
                YandexHeaders.X_ENABLED_BOXES,
                "91805,0,7;92847,0,8;92806,0,15;92758,0,83;93511,0,65");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    first,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

