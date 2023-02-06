package ru.yandex.market.checkout.checkouter.service;

import java.time.LocalDate;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.service.postomat.PostamatClient;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostamatClientTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer postamatMock;
    @Autowired
    private PostamatClient postamatClient;
    @Value("${market.checkouter.tpl-postamat.api.readTimeout}")
    private int readTimeout;
    @Value("${market.checkouter.tpl-postamat.api.maxConnectionsPerRoute}")
    private int maxConnectionsPerRoute;
    @Value("${market.checkouter.tpl-postamat.api.connectionRequestTimeout}")
    private int connectionRequestTimeout;

    @AfterEach
    public void resetMocks() {
        postamatMock.resetAll();
    }

    @Test
    @Order(0)
    public void requestShouldHaveTvmHeader() {
        postamatMock.stubFor(
                get(urlPathEqualTo("/boxbot/api/pincode/logistics/orders/123/reschedule-expiration"))
                        .willReturn(okJson("[\"2021-08-09\", \"2021-08-10\", \"2021-08-11\", \"2021-08-12\"]")));
        postamatMock.addMockServiceRequestListener((request, response) -> {
            var serviceTicketHeader = request.getHeaders().getHeader(CheckoutHttpParameters.SERVICE_TICKET_HEADER);
            assertThat(serviceTicketHeader.isPresent(), equalTo(true));
        });

        var response = postamatClient.getExpirationDates(123L);
        assertThat(response, hasSize(4));
        assertThat(response, containsInAnyOrder(LocalDate.parse("2021-08-09"),
                LocalDate.parse("2021-08-10"),
                LocalDate.parse("2021-08-11"),
                LocalDate.parse("2021-08-12")));
    }

    @Test
    @Order(1)
    public void readTimeoutsShouldWork() throws InterruptedException, ExecutionException {
        postamatMock.stubFor(
                get(urlPathEqualTo("/boxbot/api/pincode/logistics/orders/123/reschedule-expiration"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withFixedDelay(readTimeout + 5000)));

        ExecutorService executor = Executors.newFixedThreadPool(maxConnectionsPerRoute);
        List<Callable<Long>> callables = new ArrayList<>();
        try {
            postamatClient.getExpirationDates(123L);
        } catch (Exception e) {
        }
        for (int i = 0; i < maxConnectionsPerRoute; i++) {
            callables.add(() -> {
                var stopwatch = Stopwatch.createStarted();
                try {
                    postamatClient.getExpirationDates(123L);
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
    @Order(1)
    public void connectionRequestTimeoutShouldWork() throws InterruptedException, ExecutionException {
        postamatMock.stubFor(
                get(urlPathEqualTo("/boxbot/api/pincode/logistics/orders/123/reschedule-expiration"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withFixedDelay(readTimeout + 5000)));

        int threadNumber = maxConnectionsPerRoute + 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadNumber);
        List<Callable<Long>> callables = new ArrayList<>();
        AtomicInteger connectionRequestTimeoutCount = new AtomicInteger();
        try {
            postamatClient.getExpirationDates(123L);
        } catch (Exception e) {
        }
        for (int i = 0; i < threadNumber; i++) {
            callables.add(() -> {
                var stopwatch = Stopwatch.createStarted();

                try {
                    postamatClient.getExpirationDates(123L);
                } catch (Exception e) {
                    var elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                    System.out.println(elapsed + ": " + e.getMessage());
                    if (e.getMessage().contains("Timeout waiting for connection from pool")) {
                        connectionRequestTimeoutCount.incrementAndGet();
                    }
                    return elapsed;
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
        assertThat(elpaseds, everyItem(lessThan(connectionRequestTimeout + readTimeout + 100)));
        assertThat(connectionRequestTimeoutCount.get(), greaterThan(0));
    }
}
