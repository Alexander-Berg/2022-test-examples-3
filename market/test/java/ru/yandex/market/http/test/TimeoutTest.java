package ru.yandex.market.http.test;

import java.util.concurrent.TimeoutException;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Assert;
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
import ru.yandex.market.http.util.rules.HttpRule;

/**
 * @author dimkarp93
 */
public class TimeoutTest {
    private final FuturesHelper helper = new FuturesHelperImpl(new NioEventLoopGroup());
    private final RequestProcessorFactory factory = new CommonRequestProcessorFactory(helper, NioSocketChannel.class);

    private final TestRequestProcessorEventListener requestProcessorEventListener = new TestRequestProcessorEventListener();

    private final HttpClientBuilder builder = new HttpClientBuilder(
            300,
            100,
            0,
            128 * 1024 * 1024,
            helper
    );

    private final HttpClient client = builder
            .retryOnFail(2)
            .requestProcessorFactory(factory)
            .requestProcessorEventListener(requestProcessorEventListener)
            .build();

    public HttpRule rule = new HttpRule();

    @Test(expected = TimeoutException.class)
    public void timeout() throws Throwable {
        HttpConfig config = new HttpConfig().timeout(200000);
        TestClientHelper testClientHelper = new TestClientHelper(i -> config, rule);

        try {
            testClientHelper.start();
            client.doRequest(Http.get().uri(testClientHelper.getBaseUrl()))
                    .getFuture().get();

            Assert.assertEquals(1, requestProcessorEventListener.startCounter);
        } catch (Exception e) {
            throw e.getCause();
        }

    }
}
