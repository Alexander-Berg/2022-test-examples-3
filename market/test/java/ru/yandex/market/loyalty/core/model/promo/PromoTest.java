package ru.yandex.market.loyalty.core.model.promo;

import org.junit.Test;

import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PromoTest {
    private ClockForTests clock = new ClockForTests();

    @Test
    public void testGenerateAlgorithm() {
        String promoKey = Promo.encodePromoId(42L);
        assertEquals("odDG6D8CcyfYRhBj9KxYpg", promoKey);
    }

    @Test
    public void testIsActiveTrueOnlyInActiveStatus() {
        for (PromoStatus promoStatus : PromoStatus.values()) {
            Promo promo = PromoUtils.Coupon.defaultSingleUse()
                    .setStartDate(new Date(0L))
                    .setEndDate(new Date(Long.MAX_VALUE))
                    .setStatus(promoStatus)
                    .basePromo();
            assertEquals(promoStatus == PromoStatus.ACTIVE, promo.isActiveNow(clock));
        }
    }

    @Test
    public void testIsActiveTrueIfDateIsActual() {
        Promo promo = PromoUtils.Coupon.defaultSingleUse()
                .setStatus(PromoStatus.ACTIVE)
                .setStartDate(Date.from(clock.instant().plus(1, ChronoUnit.MINUTES)))
                .setEndDate(Date.from(clock.instant().plus(3, ChronoUnit.MINUTES)))
                .basePromo();
        assertFalse(promo.isActiveNow(clock));
        clock.spendTime(2, ChronoUnit.MINUTES);
        assertTrue(promo.isActiveNow(clock));
        clock.spendTime(2, ChronoUnit.MINUTES);
        assertFalse(promo.isActiveNow(clock));
    }
}
