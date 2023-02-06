package ru.yandex.market.http.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.http.CommonRequestProcessorFactory;
import ru.yandex.market.http.FuturesHelperImpl;
import ru.yandex.market.http.Http;
import ru.yandex.market.http.HttpClient;
import ru.yandex.market.http.HttpClientBuilder;
import ru.yandex.market.http.HttpResponse;
import ru.yandex.market.http.RequestProcessorFactory;
import ru.yandex.market.http.concurrent.FuturesHelper;
import ru.yandex.market.http.util.HttpConfig;
import ru.yandex.market.http.util.TestClientHelper;
import ru.yandex.market.http.util.parse.Pair;
import ru.yandex.market.http.util.parse.PairDeserializer;
import ru.yandex.market.http.util.parse.SimpleParser;
import ru.yandex.market.http.util.rules.HttpRule;

/**
 * @author dimkarp93
 */
public class SuccessfulTest {
    private final FuturesHelper helper = new FuturesHelperImpl(new NioEventLoopGroup());
    private final RequestProcessorFactory factory = new CommonRequestProcessorFactory(helper, NioSocketChannel.class);
    private final HttpClientBuilder builder = new HttpClientBuilder(
            1000,
            1,
            0,
            128 * 1024 * 1024,
            helper
    );

    private final HttpClient client = builder
            .requestProcessorFactory(factory)
            .jsonDeserializer(new PairDeserializer())
            .build();

    private final HttpConfig httpConfig = new HttpConfig()
            .addProcessor(
                    req -> "/parser".equals(req.getRequestURI()),
                    (req, resp) -> {
                        try {
                            resp.getOutputStream().print("0;OK");
                        } catch (IOException e) {

                        }
                    }
            )
            .addProcessor(
                    req -> "/response".equals(req.getRequestURI()),
                    (req, resp) -> {
                        try {
                            resp.getOutputStream().print("data");
                            resp.setStatus(404);
                            resp.setHeader("X-Answer", "something");
                        } catch (IOException e) {

                        }
                    }
            )
            .addProcessor(
                    req -> "/body".equals(req.getRequestURI()),
                    (req, resp) -> {
                        try {
                            resp.getOutputStream().print("result");
                        } catch (IOException e) {

                        }
                    }
            )
            .addProcessor(
                    req -> "/deserialize".equals(req.getRequestURI()),
                    (req, resp) -> {
                        try {
                            resp.getOutputStream().print("abc:xyz");
                        } catch (IOException e) {

                        }
                    }
            )
            .addProcessor(
                    req -> "/post_echo".equals(req.getRequestURI()) && "POST".equals(req.getMethod()),
                    (req, resp) -> {
                        try {
                            int bytesRead = req.getInputStream().available();
                            byte[] bytes = new byte[bytesRead];
                            req.getInputStream().read(bytes);
                            resp.getOutputStream().write(bytes);
                        } catch (IOException e) {

                        }
                    }
            )
            .defaultProcessor(
                    (req, resp) -> {
                        try {
                            resp.getOutputStream().print("Not found");
                        } catch (IOException e) {

                        }
                    }
            );

    @Rule
    public final HttpRule rule = new HttpRule();

    @Test
    public void successfulParse() throws Exception {
        TestClientHelper testClientHelper = new TestClientHelper(i -> httpConfig, rule);
        testClientHelper.start();

        Future<String[]> future = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/parser")))
                .parse(new SimpleParser()).getFuture();

        String[] result = future.get();

        Assert.assertEquals(2, result.length);
        Assert.assertEquals("0", result[0]);
        Assert.assertEquals("OK", result[1]);
    }

    @Test
    public void successfulResponse() throws Exception {
        TestClientHelper testClientHelper = new TestClientHelper(i -> httpConfig, rule);
        testClientHelper.start();

        Future<HttpResponse> future = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/response")))
                .getFuture();

        HttpResponse result = future.get();

        Assert.assertEquals(404, result.getHttpStatusCode());
        Assert.assertEquals("something", result.getHeader("X-Answer").get(0));
        Assert.assertArrayEquals("data".getBytes(StandardCharsets.UTF_8), result.getBody());
    }

    @Test
    public void successfulBody() throws Exception {
        TestClientHelper testClientHelper = new TestClientHelper(i -> httpConfig, rule);
        testClientHelper.start();

        Future<byte[]> future = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/body")))
                .getBody().getFuture();

        byte[] result = future.get();

        Assert.assertArrayEquals("result".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void successfulDeserialize() throws Exception {
        TestClientHelper testClientHelper = new TestClientHelper(i -> httpConfig, rule);
        testClientHelper.start();

        Future<Pair> future = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/deserialize")))
                .deserialize(Pair.class).getFuture();

        Pair<String, String> result = future.get();

        Assert.assertEquals("abc", result.first);
        Assert.assertEquals("xyz", result.second);
    }

    @Test
    public void successfullGetNotFound() throws Exception {
        TestClientHelper testClientHelper = new TestClientHelper(i -> httpConfig, rule);
        testClientHelper.start();

        Future<byte[]> future = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/post_echo")))
                .getBody().getFuture();

        byte[] result = future.get();

        Assert.assertArrayEquals("Not found".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void successfullPostWthBody() throws Exception {
        TestClientHelper testClientHelper = new TestClientHelper(i -> httpConfig, rule);
        testClientHelper.start();

        Future<byte[]> future = client.doRequest(Http.post()
                .uri(testClientHelper.getBaseUrl("/post_echo"))
                .requestBody("abcdef".getBytes(StandardCharsets.UTF_8))
        )
                .getBody().getFuture();

        byte[] result = future.get();

        Assert.assertArrayEquals("abcdef".getBytes(StandardCharsets.UTF_8), result);
    }

}
