package ru.yandex.market.loyalty.core.model.bundle;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PromoBundleStrategyTest {

    @Test
    public void shouldBeUniqueReportPromoType() {
        final Set<ReportPromoType> reportPromoTypes = new HashSet<>();
        for (PromoBundleStrategy entry : PromoBundleStrategy.values()) {
            reportPromoTypes.add(entry.getReportPromoType());
        }
        assertEquals(PromoBundleStrategy.values().length, reportPromoTypes.size());
    }

    @Test
    public void shouldBeUniquePromoSubType() {
        final Set<PromoSubType> promoSubTypes = new HashSet<>();
        for (PromoBundleStrategy entry : PromoBundleStrategy.values()) {
            if (entry != PromoBundleStrategy.UNKNOWN) {
                promoSubTypes.add(entry.getPromoSubType());
            }
        }
        assertEquals(PromoBundleStrategy.values().length - 1, promoSubTypes.size());
    }

    @Test
    public void shouldBeTheSameDescriptionWithPromoSubType() {
        for (PromoBundleStrategy entry : PromoBundleStrategy.values()) {
            if (entry != PromoBundleStrategy.UNKNOWN) {
                assertNotNull(entry.getPromoSubType());
                assertNotNull(entry.getPromoSubType().getRusDescription());
                assertEquals(entry.getPromoSubType().getRusDescription(), entry.getRusDescription());
            }
        }
    }

    @Test
    public void shouldBeTheSameDescriptionWithPromoType() {
        for (PromoBundleStrategy entry : PromoBundleStrategy.values()) {
            if (entry != PromoBundleStrategy.UNKNOWN) {
                assertNotNull(entry.getPromoType());
                assertEquals(entry.getPromoType().getRusDescription(), entry.getRusDescription());
            }
        }
    }
}
