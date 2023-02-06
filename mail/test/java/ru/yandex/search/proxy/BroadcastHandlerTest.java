package ru.yandex.search.proxy;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.YandexAssert;

public class BroadcastHandlerTest {
    private static final String LOCALHOST = "http://localhost:";
    private static final String SOCKET_TIMEOUT_EXCEPTION =
        ":\njava.net.SocketTimeoutException";

    private String broadcastRequest(final int port, final String uri) {
        return LOCALHOST + port + "/broadcast" + uri;
    }

    @Test
    public void test() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend1 = new StaticServer(Configs.baseConfig());
            StaticServer backend2 = new StaticServer(Configs.baseConfig());
            StaticServer indexer1 = new StaticServer(Configs.baseConfig());
            StaticServer indexer2 = new StaticServer(Configs.baseConfig());
            SearchProxy<ImmutableSearchProxyConfig> proxy = new SearchProxy<>(
                new SearchProxyConfigBuilder(UnicastHandlerTest.config(
                    new SearchMapConfigBuilder().content(
                        UnicastHandlerTest.searchmap(
                            backend1.port(),
                            indexer1.port())
                        + UnicastHandlerTest.searchmap(
                            backend2.port(),
                            indexer2.port()))))
                    .indexerConfig(Configs.targetConfig())
                    .build()))
        {
            String uri = "/handler/%20here?service=disk&prefix=1";
            proxy.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(broadcastRequest(proxy.port(), uri))))
            {
                String text = CharsetUtils.toString(response.getEntity());
                YandexAssert.assertContains(
                    backend1.host().toString() + SOCKET_TIMEOUT_EXCEPTION,
                    text);
                YandexAssert.assertContains(
                    backend2.host().toString() + SOCKET_TIMEOUT_EXCEPTION,
                    text);
            }
            String body = "Привет, мир!";
            backend1.add(uri, body);
            backend1.start();
            backend2.add(uri, body);
            backend2.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(broadcastRequest(proxy.port(), uri))))
            {
                String text = CharsetUtils.toString(response.getEntity());
                String line1 =
                    backend1.host().toString()
                    + ':' + '\n' + body + '\n' + '\n';
                String line2 =
                    backend2.host().toString()
                    + ':' + '\n' + body + '\n' + '\n';
                if (!text.equals(line1 + line2)) {
                    Assert.assertEquals(
                        line2 + line1,
                        CharsetUtils.toString(response.getEntity()));
                }
            }
            uri = "/?prefix=3&service=disk&index";
            indexer1.add(
                uri,
                new ExpectingHttpItem(
                    new StringChecker(body),
                    HttpStatus.SC_OK,
                    "Hello"));
            indexer1.start();
            indexer2.add(
                uri,
                new ExpectingHttpItem(
                    new StringChecker(body),
                    HttpStatus.SC_OK,
                    "world"));
            indexer2.start();
            HttpPost post = new HttpPost(broadcastRequest(proxy.port(), uri));
            post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                String text = CharsetUtils.toString(response.getEntity());
                YandexAssert.assertContains(
                    indexer1.host() + ":\nHello\n\n",
                    text);
                YandexAssert.assertContains(
                    indexer2.host() + ":\nworld\n\n",
                    text);
            }
        }
    }
}

