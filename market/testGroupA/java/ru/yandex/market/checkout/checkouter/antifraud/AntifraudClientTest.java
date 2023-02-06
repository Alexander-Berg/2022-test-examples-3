package ru.yandex.market.checkout.checkouter.antifraud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.antifraud.orders.client.MstatAntifraudOrdersCheckouterClient;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.checkout.application.AbstractWebTestBase;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThan;

public class AntifraudClientTest extends AbstractWebTestBase {

    @Autowired
    MstatAntifraudOrdersCheckouterClient antifraudClient;
    @Autowired
    WireMockServer mstatAntifraudOrdersMock;
    @Value("${market.antifraud.orders.client.readTimeout}")
    int readTimeout;
    @Value("${market.antifraud.orders.client.maxConnectionsPerRoute}")
    int maxConnectionsPerRoute;
    @Value("${market.antifraud.orders.client.connectionRequestTimeout}")
    int connectionRequestTimeout;

    @AfterEach
    public void cleanUp() {
        mstatAntifraudOrdersMock.resetAll();
    }

    @Test
    public void readTimeoutsShouldWork() throws InterruptedException, ExecutionException {
        mstatAntifraudOrdersMock.stubFor(
                post(urlPathEqualTo("/antifraud/detect"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withFixedDelay(readTimeout + 5000)));

        ExecutorService executor = Executors.newFixedThreadPool(maxConnectionsPerRoute);
        List<Callable<Long>> callables = new ArrayList<>();
        OrderRequestDto request = OrderRequestDto.builder().build();
        try {
            antifraudClient.detectFraudQuickly(request);
        } catch (Exception e) {
        }
        for (int i = 0; i < maxConnectionsPerRoute; i++) {
            callables.add(() -> {
                var stopwatch = Stopwatch.createStarted();
                try {
                    antifraudClient.detectFraudQuickly(request);
                } catch (Exception e) {
                    assertThat(e.getMessage(), containsString("Read timed out"));
                }
                return stopwatch.elapsed(TimeUnit.MILLISECONDS);
            });
        }
        List<Future<Long>> futures = executor.invokeAll(callables);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        List<Integer> elpaseds = new ArrayList<>();
        for (Future<Long> future : futures) {
            elpaseds.add(future.get().intValue());
        }
        assertThat(elpaseds, everyItem(lessThan(readTimeout + 100)));
    }

    @Test
    public void connectionRequestTimeoutShouldWork() throws InterruptedException, ExecutionException {
        mstatAntifraudOrdersMock.stubFor(
                post(urlPathEqualTo("/antifraud/detect"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("{}")
                                .withFixedDelay(readTimeout + 5000)));

        int threadNumber = maxConnectionsPerRoute + 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadNumber);
        List<Callable<Long>> callables = new ArrayList<>();
        OrderRequestDto request = OrderRequestDto.builder().build();
        AtomicInteger connectionRequestTimeoutCount = new AtomicInteger();
        try {
            antifraudClient.detectFraudQuickly(request);
        } catch (Exception ignored) {
        }
        for (int i = 0; i < threadNumber; i++) {
            callables.add(() -> {
                var stopwatch = Stopwatch.createStarted();

                try {
                    antifraudClient.detectFraudQuickly(request);
                } catch (Exception ignored) {
                }
                return stopwatch.elapsed(TimeUnit.MILLISECONDS);
            });
        }
        List<Future<Long>> futures = executor.invokeAll(callables);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        List<Integer> elpaseds = new ArrayList<>();
        for (Future<Long> future : futures) {
            elpaseds.add(future.get().intValue());
        }
        assertThat(elpaseds, everyItem(lessThan(connectionRequestTimeout + readTimeout)));
    }
}
