package ru.yandex.travel.orders.workflows.invoice.trust.jobs;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FiscalReceiptRefreshDelayServiceTest {
    private final FiscalReceiptProperties properties = FiscalReceiptProperties.builder()
            .baseRefreshDelay(Duration.ofSeconds(3))
            .refreshDelayExponentialFactor(new BigDecimal("1.1"))
            .maxRefreshDelay(Duration.ofSeconds(60))
            .build();
    private final FiscalReceiptRefreshDelayService service = new FiscalReceiptRefreshDelayService(properties);

    @Test
    public void testCalculateNextDelayGrowsEventually() {
        // i-th delay lies in the interval: [0.5 * base * (1.1 ^ i), 1.5 * base * (1.1 ^ i))
        // the next guaranteed longer delay after the 1st is x-th:
        //      (0.5 * base * (1.1 ^ x)) > (1.5 * base * (1.1 ^ 1))
        //      x = ceil(log(1.1 * (1.5 / 0.5)) / log(1.1)) = 13
        Duration firstDelay = service.calculateNextDelay(1);
        Duration lastDelay = service.calculateNextDelay(13);
        assertThat(firstDelay).isLessThan(lastDelay);
    }

    @Test
    public void testCalculateNextDelayGrowthIsLimited() {
        // we limit the delay when (0.5 * 3 sec * (1.1 ^ x)) > 60 sec   =>   1.1 ^ x > (60 / 3 / 0.5)   =>   x = ceil(log(40) / log(1.1)) = 39;
        // all next delays are the same
        for (int i = 0; i < 10; i++) {
            Duration limitedDelay = service.calculateNextDelay(39 + i);
            assertThat(limitedDelay).isEqualTo(properties.getMaxRefreshDelay());
        }
    }
}
