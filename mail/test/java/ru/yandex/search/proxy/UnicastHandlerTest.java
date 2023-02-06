package ru.yandex.search.proxy;

import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.DirectServer;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.test.StaticStringHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;

public class UnicastHandlerTest {
    private static final String PREFIX_TO_REMOVE = "/prefix-to-remove";
    private static final String UNICAST = "/unicast";

    public static String searchmap(final int port) {
        return searchmap(port, 1);
    }

    public static String searchmap(
        final int searchPort,
        final int indexerPort)
    {
        return "disk host:localhost,shards:0-65534,search_port:"
            + searchPort + ",json_indexer_port:" + indexerPort + '\n';
    }

    public static ImmutableSearchProxyConfig config(final int backendPort)
        throws Exception
    {
        return config(new SearchMapConfigBuilder()
            .content(searchmap(backendPort)));
    }

    public static ImmutableSearchProxyConfig config(
        final SearchMapConfigBuilder searchmap)
        throws Exception
    {
        return new SearchProxyConfigBuilder()
            .name("Proxy")
            .port(0)
            .connections(2)
            .searchMapConfig(searchmap)
            .searchConfig(new HttpTargetConfigBuilder().connections(2))
            .indexerConfig(new HttpTargetConfigBuilder().connections(2))
            .upstreamsConfig(
                new UpstreamsConfigBuilder()
                    .asterisk(new UpstreamConfigBuilder().connections(2))
                    .upstream(
                        new Pattern<>(UNICAST + PREFIX_TO_REMOVE, true),
                        new UpstreamConfigBuilder()
                            .connections(2)
                            .removePrefix(PREFIX_TO_REMOVE)))
            .build();
    }

    private String unicastRequest(final int port, final String uri) {
        return "http://localhost:" + port + UNICAST + uri;
    }

    @Test
    public void test() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            SearchProxy<ImmutableSearchProxyConfig> proxy =
                new SearchProxy<>(config(backend.port())))
        {
            String uri = "/handler/%20here?service=disk&prefix=1";
            proxy.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(unicastRequest(proxy.port(), uri))))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_GATEWAY_TIMEOUT,
                    response);
            }
            String body = "Привет, мир!";
            backend.start();
            backend.add(uri, body);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(unicastRequest(proxy.port(), uri))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    body,
                    CharsetUtils.toString(response.getEntity()));
            }
            uri = "/?here=we+are&service=disk&prefix=2";
            backend.add(uri, new StaticStringHttpItem(body));
            HttpGet get = new HttpGet(unicastRequest(proxy.port(), uri));
            get.setHeader(HttpHeaders.ACCEPT_CHARSET, "utf8");
            get.setHeader(HttpHeaders.ACCEPT_CHARSET, "cp1251");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpEntity entity = response.getEntity();
                Assert.assertEquals(
                    Charset.forName("windows-1251"),
                    CharsetUtils.contentType(entity).getCharset());
                Assert.assertEquals(body, CharsetUtils.toString(entity));
            }

            String prefixedUri = PREFIX_TO_REMOVE + uri;
            backend.add(uri, new StaticStringHttpItem(body));
            get = new HttpGet(unicastRequest(proxy.port(), prefixedUri));
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpEntity entity = response.getEntity();
                Assert.assertEquals(body, CharsetUtils.toString(entity));
            }

            final int len = 10000;
            byte[] buf = new byte[len];
            ByteArrayEntity entity = new ByteArrayEntity(buf);
            entity.setChunked(true);
            backend.add(uri, HttpStatus.SC_ACCEPTED, entity);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(unicastRequest(proxy.port(), uri))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_ACCEPTED, response);
                Assert.assertArrayEquals(
                    buf,
                    CharsetUtils.toByteArray(response.getEntity()));
            }
            backend.add(uri, HttpStatus.SC_INTERNAL_SERVER_ERROR, body);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(unicastRequest(proxy.port(), uri))))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    response);
            }
            uri = "/another?service=none&prefix=0";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(unicastRequest(proxy.port(), uri))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }
        }
    }

    @Test
    public void testProtocolError() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            DirectServer backend = new DirectServer(
                s -> new DirectServer.StaticResponseTask(s, "no thing\r\n"));
            SearchProxy<ImmutableSearchProxyConfig> proxy =
                new SearchProxy<>(config(backend.port())))
        {
            String uri = "/will-fail?service=disk&prefix=0";
            proxy.start();
            backend.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(unicastRequest(proxy.port(), uri))))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
            }
            Assert.assertEquals(1, backend.requestsReceived());
        }
    }
}

