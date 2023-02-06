package ru.yandex.market.loyalty.core.trigger.restrictions;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.RegionSettings;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyRegionSettingsCoreMockedDbTest;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withDeliveryRegion;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;

public class ExcludedRegionRestrictionTest extends MarketLoyaltyRegionSettingsCoreMockedDbTest {
    @Autowired
    private RegionSettingsService regionSettingsService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoService promoService;

    @Test
    public void shouldNotCreateCoinIfUserInExceptExperiment() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo);

        regionSettingsService.reloadCache();

        int excludedRegion = regionSettingsService.getAllWithDisabledCoinEmission()
                .stream()
                .findAny()
                .map(RegionSettings::getRegionId)
                .orElseThrow(AssertionError::new);
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withDeliveryRegion(excludedRegion)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }
}
