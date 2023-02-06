package ru.yandex.market.billing.distribution.share;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiDistributionTariffSupplierParamsFallbackTest {

    @Test
    public void testFalseFalse() {
        var fb = new ApiDistributionTariffSupplierParamsFallback(false, false);
        assertFalse(fb.isMobileAppInstall());
        assertFalse(fb.isTargetGeo());
        assertFalse(fb.doFallbackStep());
    }

    @Test
    public void testTrueTrue() {
        var fb = new ApiDistributionTariffSupplierParamsFallback(true, true);
        assertTrue(fb.doFallbackStep());
        assertFalse(fb.isTargetGeo());
        assertTrue(fb.isMobileAppInstall());

        assertTrue(fb.doFallbackStep());
        assertTrue(fb.isTargetGeo());
        assertFalse(fb.isMobileAppInstall());

        assertTrue(fb.doFallbackStep());
        assertFalse(fb.isTargetGeo());
        assertFalse(fb.isMobileAppInstall());

        assertFalse(fb.doFallbackStep());
    }

    @Test
    public void testFalseTrue() {
        var fb = new ApiDistributionTariffSupplierParamsFallback(false, true);
        assertTrue(fb.doFallbackStep());
        assertFalse(fb.isTargetGeo());
        assertFalse(fb.isMobileAppInstall());

        assertFalse(fb.doFallbackStep());
    }

    @Test
    public void testTrueFalse() {
        var fb = new ApiDistributionTariffSupplierParamsFallback(true, false);
        assertTrue(fb.doFallbackStep());
        assertFalse(fb.isTargetGeo());
        assertFalse(fb.isMobileAppInstall());

        assertFalse(fb.doFallbackStep());
    }

}