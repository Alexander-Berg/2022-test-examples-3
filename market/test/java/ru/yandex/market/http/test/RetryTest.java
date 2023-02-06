package ru.yandex.market.http.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.http.CommonRequestProcessorFactory;
import ru.yandex.market.http.FuturesHelperImpl;
import ru.yandex.market.http.Http;
import ru.yandex.market.http.HttpClient;
import ru.yandex.market.http.HttpClientBuilder;
import ru.yandex.market.http.RequestProcessorFactory;
import ru.yandex.market.http.concurrent.FuturesHelper;
import ru.yandex.market.http.generator.OutboundRequestIdGenerator;
import ru.yandex.market.http.util.HttpConfig;
import ru.yandex.market.http.util.TestClientHelper;
import ru.yandex.market.http.util.listener.TestRequestProcessorEventListener;
import ru.yandex.market.http.util.listener.TestRetryStrategyEventListener;
import ru.yandex.market.http.util.rules.HttpRule;

/**
 * @author dimkarp93
 */
public class RetryTest {
    private final FuturesHelper helper = new FuturesHelperImpl(new NioEventLoopGroup());
    private final RequestProcessorFactory factory = new CommonRequestProcessorFactory(helper, NioSocketChannel.class);

    public static class SimpleOutboundRequestIdGenerator implements OutboundRequestIdGenerator {
        public final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String generateRequestId() {
            return String.valueOf(counter.incrementAndGet());
        }
    }

    private final SimpleOutboundRequestIdGenerator generator = new SimpleOutboundRequestIdGenerator();

    private TestRetryStrategyEventListener retryStrategyEventListener = new TestRetryStrategyEventListener();
    private TestRequestProcessorEventListener requestProcessorListener = new TestRequestProcessorEventListener();

    @Rule
    public HttpRule rule = new HttpRule()
            .addPermanent(retryStrategyEventListener)
            .addPermanent(requestProcessorListener);

    @Test(expected = RuntimeException.class)
    public void retryFailMore() throws Throwable {
        HttpClient client = new HttpClientBuilder(
                3000,
                1000,
                0,
                128 * 1024 * 1024,
                helper
        )
                .requestProcessorFactory(factory)
                .retryOnFail(2)
                .retryOnFail(retryStrategyEventListener)
                .build();

        HttpConfig config1 = new HttpConfig()
                .addProcessor(
                        req -> "/retry".equals(req.getRequestURI()),
                        (req, resp) -> resp.setStatus(500)
                );

        HttpConfig config2 = new HttpConfig()
                .addProcessor(
                        req -> "/retry".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("0;OK");
                            } catch (IOException e) {

                            }
                        }
                );

        TestClientHelper testClientHelper = new TestClientHelper(i -> i <= 4 ? config1 : config2, rule);

        try {
            testClientHelper.start();

            client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/retry")))
                    .getFuture().get();
        } catch (Exception e) {
            Assert.assertEquals(3, retryStrategyEventListener.serviceErrorCounter);
            Assert.assertEquals(3, retryStrategyEventListener.requestEndCounter);
            Assert.assertEquals(
                    RuntimeException.class,
                    retryStrategyEventListener.requestEndErrors.get(0).getClass()
            );

            Assert.assertEquals(
                    RuntimeException.class,
                    retryStrategyEventListener.requestEndErrors.get(1).getClass()
            );

            Assert.assertEquals(
                    RuntimeException.class,
                    retryStrategyEventListener.requestEndErrors.get(2).getClass()
            );

            Throwable t = e.getCause();
            Assert.assertTrue(t.getMessage().contains("Wrong response"));
            throw t;
        }

    }

    @Test(expected = TimeoutException.class)
    public void retryFailTooLong() throws Throwable {
        HttpConfig config1 = new HttpConfig()
                .addProcessor(
                        req -> "/retry".equals(req.getRequestURI()),
                        (req, resp) -> resp.setStatus(500)
                )
                .timeout(2000);

        HttpConfig config2 = new HttpConfig()
                .addProcessor(
                        req -> "/retry".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("0;OK");
                            } catch (IOException e) {

                            }
                        }
                );

        HttpClient client = new HttpClientBuilder(
                3000,
                1000,
                0,
                128 * 1024 * 1024,
                helper
        )
                .requestProcessorFactory(factory)
                .retryOnFail(3)
                .retryOnFail(retryStrategyEventListener)
                .requestProcessorEventListener(requestProcessorListener)
                .build();

        TestClientHelper testClientHelper = new TestClientHelper(i -> i <= 2 ? config1 : config2, rule);

        try {
            testClientHelper.start();

            client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/retry")))
                    .getFuture().get();
        } catch (Exception e) {
            Assert.assertEquals(1, retryStrategyEventListener.serviceErrorCounter);
            Assert.assertEquals(
                    RuntimeException.class,
                    retryStrategyEventListener.requestEndErrors.get(0).getClass());
            Assert.assertEquals(
                    CancellationException.class,
                    retryStrategyEventListener.requestEndErrors.get(1).getClass());
            Assert.assertEquals(2, requestProcessorListener.startCounter);
            throw e.getCause();
        }
    }

    @Test
    public void retrySuccessfulTest() throws Throwable {
        HttpClient client = new HttpClientBuilder(
                3000,
                1000,
                0,
                128 * 1024 * 1024,
                helper
        )
                .requestProcessorFactory(factory)
                .requestProcessorEventListener(requestProcessorListener)
                .retryOnFail(3)
                .retryOnFail(retryStrategyEventListener)
                .requestIdGenerator(generator)
                .build();

        HttpConfig config1 = new HttpConfig()
                .addProcessor(
                        req -> "/retry".equals(req.getRequestURI()),
                        (req, resp) -> resp.setStatus(500)
                );

        HttpConfig config2 = new HttpConfig()
                .addProcessor(
                        req -> "/retry".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("0;OK");
                            } catch (IOException e) {

                            }
                        }
                );


        TestClientHelper testClientHelper = new TestClientHelper(i -> i <= 2 ? config1 : config2, rule);
        testClientHelper.start();

        byte[] result = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/retry")))
                .getBody().getFuture().get();

        Assert.assertEquals(2, retryStrategyEventListener.serviceErrorCounter);
        Assert.assertEquals(3, retryStrategyEventListener.requestEndCounter);
        Assert.assertEquals(
                RuntimeException.class,
                retryStrategyEventListener.requestEndErrors.get(0).getClass()
        );
        Assert.assertEquals(
                RuntimeException.class,
                retryStrategyEventListener.requestEndErrors.get(1).getClass()
        );

        Assert.assertEquals(3, requestProcessorListener.startCounter);

        Assert.assertEquals("1", retryStrategyEventListener.marketRequestIds.get(0));
        Assert.assertEquals("2", retryStrategyEventListener.marketRequestIds.get(1));
        Assert.assertEquals("3", retryStrategyEventListener.marketRequestIds.get(2));

        Assert.assertArrayEquals("0;OK".getBytes(StandardCharsets.UTF_8), result);

    }
}
