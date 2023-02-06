package ru.yandex.market.delivery.transport_manager.domain.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QuotaLimitsTest {

    @Test
    void negativePallets() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new QuotaLimits(-1, 1));
    }

    @Test
    void negativeItems() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new QuotaLimits(1, -1));
    }

    @Test
    void min0() {
        Assertions.assertEquals(QuotaLimits.ZERO, QuotaLimits.min());
    }

    @Test
    void min1() {
        QuotaLimits limits = new QuotaLimits(1, 2);
        Assertions.assertEquals(limits, QuotaLimits.min(limits));
    }

    @Test
    void min2() {
        QuotaLimits limits1 = new QuotaLimits(1, 2);
        QuotaLimits limits2 = new QuotaLimits(3, 1);
        QuotaLimits minLimits = new QuotaLimits(1, 1);
        Assertions.assertEquals(minLimits, QuotaLimits.min(limits1, limits2));
    }

    @Test
    void min3() {
        QuotaLimits limits1 = new QuotaLimits(1, 2);
        QuotaLimits limits2 = new QuotaLimits(3, 1);
        QuotaLimits limits3 = new QuotaLimits(2, 2);
        QuotaLimits minLimits = new QuotaLimits(1, 1);
        Assertions.assertEquals(minLimits, QuotaLimits.min(limits1, limits2, limits3));
    }

    @Test
    void orMaxPallets() {
        QuotaLimits limits = new QuotaLimits(10, 100);
        QuotaLimits limitsWithPalletLimitation = new QuotaLimits(5, 100);
        Assertions.assertEquals(limitsWithPalletLimitation, limits.widthMaxPallets(5));
    }

    @Test
    void add() {
        QuotaLimits limits = new QuotaLimits(10, 100);
        QuotaLimits expected = new QuotaLimits(15, 110);
        Assertions.assertEquals(expected, limits.add(new QuotaLimits(5, 10)));
    }

    @Test
    void contains() {
        QuotaLimits limits = new QuotaLimits(10, 100);
        Assertions.assertTrue(limits.contains(new QuotaLimits(9, 99)));
        Assertions.assertTrue(limits.contains(limits));
        Assertions.assertFalse(limits.contains(new QuotaLimits(9, 101)));
        Assertions.assertFalse(limits.contains(new QuotaLimits(11, 99)));
    }

    @Test
    void withItems() {
        QuotaLimits limits = new QuotaLimits(10, 100);
        QuotaLimits expected = new QuotaLimits(10, 50);
        Assertions.assertEquals(expected, limits.withItems(50));
    }

    @Test
    void withItemsNegative() {
        QuotaLimits limits = new QuotaLimits(10, 100);
        Assertions.assertThrows(IllegalArgumentException.class, () -> limits.withItems(-1));
    }
}
