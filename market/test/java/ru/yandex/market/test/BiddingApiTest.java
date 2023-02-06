package ru.yandex.market.test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import com.google.common.util.concurrent.RateLimiter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.bidding.BiddingApi;
import ru.yandex.market.core.auction.bidding.exception.ServiceUnavailableExceptionConverter;

public class BiddingApiTest extends FunctionalTest {
    private static final Logger log = LoggerFactory.getLogger(BiddingApiTest.class);
    private static final int SHOP_ID = 1000;

    @Autowired
    private MockWebServer biddingMockServer;

    @Autowired
    private BiddingApi biddingApi;

    @Test
    @DisplayName("Проверяем, что после N ошибочных запросов, Circuit Breaker прекратит слать запросы " +
            "в биддинг на N секунд. А вместо 503 вернется 429")
    void testDontSendRequestsToBiddingIfTooMany503Or404() {
        final RateLimiter rateLimiter = RateLimiter.create(1);

        repeat(10, (idx) -> addExpectedResponseCode(503));
        repeat(30, (idx) -> addExpectedResponseCode(200));

        AtomicInteger httpErrors = new AtomicInteger(0);
        AtomicInteger circuitBreakerErrors = new AtomicInteger(0);
        AtomicInteger oks = new AtomicInteger(0);
        repeat(30, (idx) -> {
            rateLimiter.acquire();

            try {
                makeBiddingRequest();
                oks.incrementAndGet();
            } catch (WebApplicationException serverError) {
                log.error("Expected error code {}, message {}",
                        serverError.getResponse().getStatus(), serverError.getMessage());
                Assertions.assertEquals(ServiceUnavailableExceptionConverter.REPLACEMENT_CODE.getStatusCode(),
                        serverError.getResponse().getStatus());

                httpErrors.incrementAndGet();
            } catch (ProcessingException re) {
                log.error("Circuit Break is no alternative addresses present. So a fault is thrown. Message {}",
                        re.getMessage());
                circuitBreakerErrors.incrementAndGet();
            }
        });

        Assertions.assertEquals(10, httpErrors.get());
        Assertions.assertTrue(circuitBreakerErrors.get() > 1);
        Assertions.assertTrue(oks.get() > 3);
    }

    private void repeat(int times, Consumer<Integer> action) {
        IntStream.range(0, times).forEach(action::accept);
    }

    private void makeBiddingRequest() {
        biddingApi.renameGroup(SHOP_ID, SHOP_ID, "group1");
    }

    private void addExpectedResponseCode(int responseCode) {
        biddingMockServer.enqueue(new MockResponse().setBody("{'ok': true}").setResponseCode(responseCode));
    }
}
