package ru.yandex.market.loyalty.core.model.bundle;

import org.junit.Test;

import java.time.LocalDateTime;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.status;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;

public class PromoBundleDescriptionTest {

    @Test
    public void isActiveOnWithoutEndDate() {
        final PromoBundleDescription first = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(1L),
                promoKey("key"),
                shopPromoId("shop_key"),
                strategy(GIFT_WITH_PURCHASE)
        );
        assertNull(first.getEndTime());
        assertTrue(first.isActiveOn(LocalDateTime.now()));
        assertFalse(first.isActiveOn(LocalDateTime.now().minusHours(1L)));

        final PromoBundleDescription second = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(1L),
                promoKey("key"),
                shopPromoId("shop_key"),
                strategy(GIFT_WITH_PURCHASE),
                status(PromoBundleStatus.UNKNOWN)
        );
        assertNull(second.getEndTime());
        assertFalse(second.isActiveOn(LocalDateTime.now()));
        assertFalse(second.isActiveOn(LocalDateTime.now().minusHours(1L)));
    }

    @Test
    public void isActiveOnWithEndDate() {
        final LocalDateTime now = LocalDateTime.now();
        final PromoBundleDescription first = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(1L),
                promoKey("key"),
                shopPromoId("shop_key"),
                strategy(GIFT_WITH_PURCHASE),
                starts(now),
                ends(now.plusYears(1L))
        );
        assertNotNull(first.getEndTime());
        assertTrue(first.isActiveOn(now));
        assertFalse(first.isActiveOn(now.minusHours(1L)));
        assertTrue(first.isActiveOn(now.plusYears(1L)));
        assertFalse(first.isActiveOn(now.plusYears(1L).plusMinutes(1L)));

        final PromoBundleDescription second = bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(1L),
                promoKey("key"),
                shopPromoId("shop_key"),
                strategy(GIFT_WITH_PURCHASE),
                status(PromoBundleStatus.UNKNOWN),
                starts(now),
                ends(now.plusYears(1L))
        );
        assertNotNull(second.getEndTime());
        assertFalse(second.isActiveOn(now));
        assertFalse(second.isActiveOn(now.minusHours(1L)));
        assertFalse(second.isActiveOn(now.plusYears(1L)));
        assertFalse(second.isActiveOn(now.plusYears(1L).minusMinutes(1L)));
    }
}
