package ru.yandex.market.loyalty.core.service.threshold;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.GenericParam;
import ru.yandex.market.loyalty.core.model.ids.PromoId;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.threshold.PromoThresholdStatus;
import ru.yandex.market.loyalty.core.model.threshold.PromoThresholdType;
import ru.yandex.market.loyalty.core.model.threshold.ThresholdAggregateMode;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.discount.ThresholdId;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.loyalty.core.model.threshold.PromoThresholdParamName.AGGREGATE_MODE;
import static ru.yandex.market.loyalty.core.model.threshold.PromoThresholdParamName.USER_THRESHOLD_VALUE;

public class PromoThresholdServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoThresholdService promoThresholdService;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void shouldSaveThreshold() {
        final PromoThreshold threshold = createDefaultMaxTotalCashbackPromoThreshold("default");
        promoThresholdService.addThreshold(threshold);
        cashbackCacheService.loadThresholds();
        final PromoThreshold saved = promoThresholdService.getThreshold(ThresholdId.of("default"));
        assertMaxTotalCashbackThresholdsEqual(threshold, saved);

    }

    @Test(expected = PromoThresholdNotFoundException.class)
    public void shouldThrowThresholdNotFoundException() {
        promoThresholdService.getThreshold(ThresholdId.of("default"));
    }

    @Test
    public void checkThresholdCache() {
        final PromoThreshold threshold1 = createDefaultMaxTotalCashbackPromoThreshold("default1");
        final PromoThreshold threshold2 = createDefaultMaxTotalCashbackPromoThreshold("default2");
        final Promo promo1 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE));
        final Promo promo2 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE));
        promoThresholdService.addThreshold(threshold1);
        promoThresholdService.addThreshold(threshold2);
        promoThresholdService.updatePromoThresholds(PromoId.of(promo1.getId()), Set.of(ThresholdId.of("default1"),
                ThresholdId.of("default2")));
        promoThresholdService.updatePromoThresholds(PromoId.of(promo2.getId()), Set.of(ThresholdId.of("default2")));
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.loadThresholds();
        final Set<ThresholdId> promoThresholds1 = promoThresholdService.getPromoThresholds(PromoId.of(promo1.getId())
                , true);
        assertThat(promoThresholds1, hasSize(2));
        assertThat(promoThresholds1, containsInAnyOrder(ThresholdId.of("default1"), ThresholdId.of("default2")));
        final Set<ThresholdId> promoThresholds2 = promoThresholdService.getPromoThresholds(PromoId.of(promo2.getId())
                , true);
        assertThat(promoThresholds2, hasSize(1));
        assertThat(promoThresholds2, contains(ThresholdId.of("default2")));
    }

    private void assertMaxTotalCashbackThresholdsEqual(PromoThreshold threshold, PromoThreshold saved) {
        assertEquals(threshold.getName(), saved.getName());
        assertEquals(threshold.getType(), saved.getType());
        assertEquals(threshold.getStatus(), saved.getStatus());
        assertEquals(threshold.getParams().get(USER_THRESHOLD_VALUE), saved.getParams().get(USER_THRESHOLD_VALUE));
        assertEquals(threshold.getParams().get(AGGREGATE_MODE), saved.getParams().get(AGGREGATE_MODE));
    }

    private PromoThreshold createDefaultMaxTotalCashbackPromoThreshold(String name) {
        return PromoThreshold.builder()
                .setName(name)
                .setStatus(PromoThresholdStatus.ACTIVE)
                .setType(PromoThresholdType.MAX_TOTAL_CASHBACK)
                .setParams(Map.of(
                        USER_THRESHOLD_VALUE, GenericParam.of(BigDecimal.valueOf(3000)),
                        AGGREGATE_MODE, GenericParam.of(ThresholdAggregateMode.ALL)
                ))
                .build();
    }
}
