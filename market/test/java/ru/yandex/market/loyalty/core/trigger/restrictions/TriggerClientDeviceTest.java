package ru.yandex.market.loyalty.core.trigger.restrictions;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.EmissionClientDeviceType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.time.Duration;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withClientDeviceType;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class TriggerClientDeviceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private CoinService coinService;

    @Test
    public void shouldCreateCoinOnlyForTouch() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createCoinTriggerWithClientDeviceRestriction(
                promo,
                new ClientDeviceRestrictionFactory.ClientDeviceRestrictionDto(Collections.singleton(
                        EmissionClientDeviceType.TOUCH
                ))
        );

        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withUid(DEFAULT_UID),
                        withClientDeviceType(EmissionClientDeviceType.TOUCH))
        );
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(ANOTHER_UID),
                withClientDeviceType(EmissionClientDeviceType.DESKTOP))
        );
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(coinService.search.getInactiveCoinsByUid(DEFAULT_UID, CoreMarketPlatform.BLUE), not(empty()));
        assertThat(coinService.search.getInactiveCoinsByUid(ANOTHER_UID, CoreMarketPlatform.BLUE), is(empty()));
    }

    @Test
    public void shouldCreateCoinForIfNotFilled() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createCoinTriggerWithClientDeviceRestriction(
                promo,
                new ClientDeviceRestrictionFactory.ClientDeviceRestrictionDto(Collections.emptySet())
        );

        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withUid(DEFAULT_UID),
                        withClientDeviceType(EmissionClientDeviceType.TOUCH))
        );
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(coinService.search.getInactiveCoinsByUid(DEFAULT_UID, CoreMarketPlatform.BLUE), not(empty()));
    }
}
