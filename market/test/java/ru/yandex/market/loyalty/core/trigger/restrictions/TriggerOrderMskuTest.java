package ru.yandex.market.loyalty.core.trigger.restrictions;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withItem;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.mskuRestriction;

public class TriggerOrderMskuTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    private Promo promo;

    @Before
    public void init() {
        promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
    }

    @Test
    public void shouldCreateCoinWhenMskuInList() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, mskuRestriction("1"));

        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withItem(defaultOrderItem().setMsku(1L))));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotCreateCoinWhenMskuNotInList() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, mskuRestriction("1"));
        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withItem(defaultOrderItem().setMsku(2L))));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotCreateCoinWhenMskuInListButNotSufficientTotal() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                mskuRestriction(BigDecimal.valueOf(1_000), null, "1")
        );
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withItem(defaultOrderItem()
                .setMsku(2L)
                .setPrice(BigDecimal.valueOf(500))
                .setCount(BigDecimal.ONE)
        )));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotCreateCoinWhenMskuInListButNotSufficientMaxTotal() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, mskuRestriction(BigDecimal.valueOf(1_000),
                BigDecimal.valueOf(10_000L), "1"));
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withItem(defaultOrderItem()
                .setMsku(2L)
                .setPrice(BigDecimal.valueOf(5000))
                .setCount(BigDecimal.ONE)
        )));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }
}
