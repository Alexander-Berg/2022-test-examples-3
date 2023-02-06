package ru.yandex.market.http.test;

import java.net.URI;
import java.util.Collections;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Promise;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.http.FuturesHelperImpl;
import ru.yandex.market.http.Http;
import ru.yandex.market.http.HttpClient;
import ru.yandex.market.http.HttpClientBuilder;
import ru.yandex.market.http.HttpResponse;
import ru.yandex.market.http.RequestProcessorFactory;
import ru.yandex.market.http.concurrent.FuturesHelper;
import ru.yandex.market.http.util.listener.TestRetryStrategyEventListener;
import ru.yandex.market.http.util.requests.MockRequestProcessorFactory;

/**
 * @author dimkarp93
 */
public class RetryMockConnectTimeoutTest {
    private FuturesHelper futuresHelper = new FuturesHelperImpl(new NioEventLoopGroup());

    @Test
    public void connectTimeoutSuccessfulRetry() throws Throwable {
        RequestProcessorFactory factory = new MockRequestProcessorFactory(
                i -> {
                    Promise<HttpResponse> p = futuresHelper.newPromise();
                    HttpResponse response = HttpResponse.of(
                            200,
                            Collections.emptyList(),
                            new byte[0]
                    );

                    if (i <= 2) {
                        p.tryFailure(new NullPointerException());
                    } else {
                        p.trySuccess(response);
                    }

                    return p;
                }
        );

        TestRetryStrategyEventListener retryStrategyListener = new TestRetryStrategyEventListener();

        HttpClient client = new HttpClientBuilder(
                1000,
                200,
                0,
                128 * 1024 * 1024,
                futuresHelper
        )
                .requestProcessorFactory(factory)
                .retryOnFail(2)
                .retryOnFail(retryStrategyListener)
                .build();

        client.doRequest(Http.get().uri(new URI("http://localhost")))
                .getFuture()
                .get();

        Assert.assertEquals(3, retryStrategyListener.requestEndCounter);
        Assert.assertEquals(
                NullPointerException.class,
                retryStrategyListener.requestEndErrors.get(0).getClass()
        );

        Assert.assertEquals(
                NullPointerException.class,
                retryStrategyListener.requestEndErrors.get(1).getClass()
        );
    }

    @Test(expected = NullPointerException.class)
    public void connectTimeoutTooManyErrors() throws Throwable {
        RequestProcessorFactory factory = new MockRequestProcessorFactory(
                i -> {
                    Promise<HttpResponse> p = futuresHelper.newPromise();
                    HttpResponse response = HttpResponse.of(
                            200,
                            Collections.emptyList(),
                            new byte[0]
                    );

                    if (i <= 3) {
                        p.tryFailure(new NullPointerException());
                    } else {
                        p.trySuccess(response);
                    }

                    return p;
                }
        );

        TestRetryStrategyEventListener retryStrategyListener = new TestRetryStrategyEventListener();

        HttpClient client = new HttpClientBuilder(
                1000,
                200,
                0,
                128 * 1024 * 1024,
                futuresHelper
        )
                .requestProcessorFactory(factory)
                .retryOnFail(2)
                .retryOnFail(retryStrategyListener)
                .build();

        try {
            client.doRequest(Http.get().uri(new URI("http://localhost")))
                    .getFuture()
                    .get();
        } catch (Exception e) {
            Assert.assertEquals(3, retryStrategyListener.requestEndCounter);
            Assert.assertEquals(
                    NullPointerException.class,
                    retryStrategyListener.requestEndErrors.get(0).getClass()
            );

            Assert.assertEquals(
                    NullPointerException.class,
                    retryStrategyListener.requestEndErrors.get(1).getClass()
            );

            Assert.assertEquals(
                    NullPointerException.class,
                    retryStrategyListener.requestEndErrors.get(2).getClass()
            );


            throw e.getCause();
        }


    }
}
