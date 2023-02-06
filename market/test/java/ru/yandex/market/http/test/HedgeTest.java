package ru.yandex.market.http.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

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
import ru.yandex.market.http.util.HttpConfig;
import ru.yandex.market.http.util.TestClientHelper;
import ru.yandex.market.http.util.listener.TestRequestProcessorEventListener;
import ru.yandex.market.http.util.listener.TestRetryStrategyEventListener;
import ru.yandex.market.http.util.rules.HttpRule;

/**
 * @author dimkarp93
 */
public class HedgeTest {
    private final FuturesHelper helper = new FuturesHelperImpl(new NioEventLoopGroup());
    private final RequestProcessorFactory factory = new CommonRequestProcessorFactory(helper, NioSocketChannel.class);

    private TestRequestProcessorEventListener requestProcessorEventListener = new TestRequestProcessorEventListener();
    private TestRetryStrategyEventListener retryStrategyEventListener = new TestRetryStrategyEventListener();

    @Rule
    public final HttpRule rule = new HttpRule()
            .addPermanent(requestProcessorEventListener)
            .addPermanent(retryStrategyEventListener);

    @Test(expected = RuntimeException.class)
    public void hedgeFailMore() throws Throwable {
        HttpClient client = new HttpClientBuilder(
                3000,
                1000,
                200,
                128 * 1024 * 1024,
                helper
        )
                .requestProcessorFactory(factory)
                .requestProcessorEventListener(requestProcessorEventListener)
                .retryOnFail(5)
                .retryOnFail(retryStrategyEventListener)
                .build();

        HttpConfig config1 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> resp.setStatus(500)

                );

        HttpConfig config2 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("success");
                            } catch (IOException e) {

                            }
                        }
                );


        TestClientHelper testClientHelper = new TestClientHelper(i -> i <= 6 ? config1 : config2, rule);

        try {
            testClientHelper.start();
            client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/hedge")))
                    .getBody().getFuture().get();
        } catch (Exception e) {
            Assert.assertEquals(6, requestProcessorEventListener.startCounter);
            Assert.assertEquals(6, retryStrategyEventListener.requestEndCounter);
            Assert.assertEquals(6, retryStrategyEventListener.serviceErrorCounter);

            throw e.getCause();
        }

    }

    @Test(expected = TimeoutException.class)
    public void hedgeFailTooLong() throws Throwable {
        HttpClient client = new HttpClientBuilder(
                5000,
                1000,
                1000,
                128 * 1024 * 1024,
                helper
        )
                .requestProcessorFactory(factory)
                .requestProcessorEventListener(requestProcessorEventListener)
                .retryOnFail(2)
                .retryOnFail(retryStrategyEventListener)
                .build();

        HttpConfig config1 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("success");
                            } catch (IOException e) {

                            }
                        }
                )
                .timeout(10000);

        HttpConfig config2 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("success");
                            } catch (IOException e) {

                            }
                        }
                );

        TestClientHelper testClientHelper = new TestClientHelper(i -> i <= 3 ? config1 : config2, rule);
        try {
            testClientHelper.start();
            byte[] result = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/hedge")))
                    .getBody().getFuture().get();
          } catch (Exception e) {
            //Не более 2-х тасок хеджируется одновременно, сейчас особенность куда - нужно ли так вообще - надо подумтаь
            Assert.assertEquals(2, requestProcessorEventListener.startCounter);
            Assert.assertEquals(2, retryStrategyEventListener.requestEndCounter);
            Assert.assertEquals(0, retryStrategyEventListener.serviceErrorCounter);

            throw e.getCause();
        }

    }

    @Test
    public void hedgeFirstTest() throws Exception {
        HttpClient client = new HttpClientBuilder(
                3000,
                1000,
                1500,
                128 * 1024 * 1024,
                helper
        )
                .requestProcessorFactory(factory)
                .requestProcessorEventListener(requestProcessorEventListener)
                .retryOnFail(3)
                .retryOnFail(retryStrategyEventListener)
                .build();

        HttpConfig config = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("success");
                            } catch (IOException e) {

                            }
                        }
                )
                .timeout(100);


        TestClientHelper testClientHelper = new TestClientHelper(i -> config, rule);
        testClientHelper.start();

        byte[] result = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/hedge")))
                .getBody().getFuture().get();
        Assert.assertArrayEquals("success".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void hedgeSecondTest() throws Throwable {
        HttpClient client = new HttpClientBuilder(
                3000,
                1000,
                1000,
                128 * 1024 * 1024,
                helper
        )
                .requestProcessorFactory(factory)
                .requestProcessorEventListener(requestProcessorEventListener)
                .retryOnFail(3)
                .retryOnFail(retryStrategyEventListener)
                .build();

        HttpConfig config1 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("success");
                            } catch (IOException e) {

                            }
                        }
                )
                .timeout(2000);

        HttpConfig config2 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("success");
                            } catch (IOException e) {

                            }
                        }
                );


        TestClientHelper testClientHelper = new TestClientHelper(i -> i <= 1 ? config1 : config2, rule);
        testClientHelper.start();

        byte[] result = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/hedge")))
                .getBody().getFuture().get();
        Assert.assertEquals(2, requestProcessorEventListener.startCounter);
        Assert.assertEquals(2, retryStrategyEventListener.requestEndCounter);
        Assert.assertArrayEquals("success".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void hedgeThirdTest() throws Throwable {
        HttpClient client = new HttpClientBuilder(
                3000,
                1000,
                1000,
                128 * 1024 * 1024,
                helper
        )
                .requestProcessorFactory(factory)
                .requestProcessorEventListener(requestProcessorEventListener)
                .retryOnFail(2)
                .retryOnFail(retryStrategyEventListener)
                .build();


        HttpConfig config1 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("success");
                            } catch (IOException e) {

                            }
                        }
                )
                .timeout(4000);

        HttpConfig config2 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> resp.setStatus(500)

                );

        HttpConfig config3 = new HttpConfig()
                .addProcessor(
                        req -> "/hedge".equals(req.getRequestURI()),
                        (req, resp) -> {
                            resp.setStatus(200);
                            try {
                                resp.getOutputStream().print("success");
                            } catch (IOException e) {

                            }
                        }
                );


        TestClientHelper testClientHelper = new TestClientHelper(
                i -> {
                    if (i <= 1) {
                        return config1;
                    } else if (i == 2) {
                        return config2;
                    } else {
                        return config3;
                    }
                },
                rule
        );
        testClientHelper.start();

        byte[] result = client.doRequest(Http.get().uri(testClientHelper.getBaseUrl("/hedge")))
                .getBody().getFuture().get();
        Assert.assertEquals(3, requestProcessorEventListener.startCounter);
        Assert.assertEquals(3, retryStrategyEventListener.requestEndCounter);
        Assert.assertArrayEquals("success".getBytes(StandardCharsets.UTF_8), result);
    }
}
