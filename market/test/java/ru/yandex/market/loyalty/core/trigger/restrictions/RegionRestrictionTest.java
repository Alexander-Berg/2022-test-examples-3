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
import static ru.yandex.market.loyalty.core.utils.EventFactory.orderStatusUpdated;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withDeliveryRegion;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.negateRegionRestriction;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.regionRestriction;

public class RegionRestrictionTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoService promoService;

    private static final long SPB_AND_LEN_OBL_REGION_ID = 10174L; // Санкт Петербург и Ленинградская область
    private static final long SPB_AND_LEN_OBL_SOME_NESTED_REGION_ID = 137933L; // Ленинградская область / Сегла

    @Test
    public void shouldNotCreateCoinIfUserInExceptRegion() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                regionRestriction(Long.toString(SPB_AND_LEN_OBL_REGION_ID)),
                negateRegionRestriction(Long.toString(SPB_AND_LEN_OBL_SOME_NESTED_REGION_ID))
        );

        triggerEventQueueService.addEventToQueue(
                orderStatusUpdated(withDeliveryRegion(SPB_AND_LEN_OBL_SOME_NESTED_REGION_ID)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotCreateCoinIfRegionNotExist() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                regionRestriction(Long.toString(400))
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(withDeliveryRegion(400L)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldCreateCoinIfUserInRegion() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                regionRestriction(Long.toString(SPB_AND_LEN_OBL_REGION_ID))
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withDeliveryRegion(SPB_AND_LEN_OBL_REGION_ID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldCreateCoinIfUserWithinRegion() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                regionRestriction(Long.toString(SPB_AND_LEN_OBL_REGION_ID))
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withDeliveryRegion(SPB_AND_LEN_OBL_SOME_NESTED_REGION_ID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(promoService.getPromo(
                promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }
}
