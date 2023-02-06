package ru.yandex.market.abo.core.billing.rate;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93.
 * @date 19.06.18.
 */
public class BillingRateServiceTest extends EmptyTest {

    @Autowired
    private BillingRateService billingRateService;

    @Test
    public void testLoadActive() {
        List<BillingRate> active = billingRateService.loadActiveRates(null, null);
        assertFalse(active.isEmpty());
        assertTrue(isInitialized(active.get(0).getBillingResult()));
        assertTrue(isInitialized(active.get(0).getBillingResult().getReport()));
    }
}
