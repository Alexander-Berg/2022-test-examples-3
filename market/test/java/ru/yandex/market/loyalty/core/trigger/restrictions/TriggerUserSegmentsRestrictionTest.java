package ru.yandex.market.loyalty.core.trigger.restrictions;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.loyalty.core.trigger.restrictions.SetRelation.ALL_FROM_SET_SHOULD_BE_INCLUDED;
import static ru.yandex.market.loyalty.core.utils.EventFactory.orderStatusUpdated;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUserSegments;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.userSegmentsRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class TriggerUserSegmentsRestrictionTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoService promoService;

    @Test
    public void shouldEmmitCoinIfUserSegmentRestrictionMatches() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                userSegmentsRestriction(
                        ALL_FROM_SET_SHOULD_BE_INCLUDED,
                        "segment1"
                )
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withUserSegments("segment1")
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotEmmitCoinIfSegmentRestrictionNotMatch() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                userSegmentsRestriction(
                        ALL_FROM_SET_SHOULD_BE_INCLUDED,
                        "segment1"
                )
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withUserSegments("segment2")
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }
}
