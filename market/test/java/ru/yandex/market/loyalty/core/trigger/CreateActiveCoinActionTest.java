package ru.yandex.market.loyalty.core.trigger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResult;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResultDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinNoAuth;
import ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes;
import ru.yandex.market.loyalty.core.model.trigger.event.data.TriggerEventData;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.EventHandleMode;
import ru.yandex.market.loyalty.core.service.trigger.EventHandleRestrictionType;
import ru.yandex.market.loyalty.core.service.trigger.TriggerDataCache;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventProcessor;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.ProcessResultUtils;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.EMISSION_BUDGET_EXCEEDED;
import static ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResultStatus.FAILED_RESTRICTION;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate.EFFECTIVELY_PROCESSING;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate.PAID;
import static ru.yandex.market.loyalty.core.utils.EventFactory.noAuth;
import static ru.yandex.market.loyalty.core.utils.EventFactory.orderStatusUpdated;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withOrderId;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withPaymentType;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class CreateActiveCoinActionTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long UID = 123;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private ProcessResultUtils processResultUtils;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private TriggerEventDao triggerEventDao;

    //TODO MARKETDISCOUNT-1087 temporal. Пока мейлер не научился поддерживать неактивные монеты
    @Test
    @Ignore
    public void shouldCreateOneCoinForOrderWithPendingAndProcessing() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, orderRestriction(PAID));

        long orderId = 123123L;
        triggerEventQueueService.addEventToQueue(
                orderStatusUpdated(
                        withOrderId(orderId),
                        withPaymentType(PaymentType.PREPAID),
                        withUid(UID)
                )
        );
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        triggerEventQueueService.addEventToQueue(
                orderStatusUpdated(
                        withOrderId(orderId),
                        withPaymentType(PaymentType.PREPAID),
                        withUid(UID)
                )
        );
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        List<Coin> coins = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(UID));
        assertThat(coins, hasSize(1));
    }

    @Test
    public void shouldCreateCoinForOrder() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(withUid(UID)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        List<Coin> coins = coinService.search.getInactiveCoinsByUid(UID, CoreMarketPlatform.BLUE);
        assertThat(coins, not(empty()));
        Coin coin = coins.iterator().next();
        assertEquals(CoreCoinType.FIXED, coin.getType());
        assertEquals(CoreCoinStatus.INACTIVE, coin.getStatus());
        assertThat(coin.getNominal(), comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
    }

    @Test
    public void shouldCreateCoinForOrderOnPending() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, TriggerGroupType.MANDATORY_TRIGGERS, "{\"alreadyActive\":true}",
                orderRestriction(EFFECTIVELY_PROCESSING)
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withPaymentType(PaymentType.PREPAID),
                withUid(UID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        List<Coin> coins = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(UID)
                .platform(CoreMarketPlatform.BLUE));
        assertThat(coins, not(empty()));
        Coin coin = coins.iterator().next();
        assertEquals(CoreCoinType.FIXED, coin.getType());
        assertEquals(CoreCoinStatus.ACTIVE, coin.getStatus());
        assertThat(coin.getNominal(), comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
    }

    @Test
    public void shouldCreateCoinForNoAuth() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo);

        long uidForNoAuth = 1152921504608207923L;
        String uuid = "asdasdas";
        String yandexUid = "asdasdasdasdasd";
        List<TriggerActionResult> results = triggerEventQueueService.insertAndProcessEvent(
                orderStatusUpdated(withUid(uidForNoAuth), b -> b.setUuid(uuid), b -> b.setYandexUid(yandexUid),
                        noAuth()),
                discountUtils.getRulesPayload(), BudgetMode.SYNC);
        List<Coin> coins = processResultUtils.request(results, Coin.class);

        assertThat(coins, not(empty()));
        Coin coin = coins.iterator().next();
        assertEquals(CoreCoinType.FIXED, coin.getType());
        assertThat(coin,
                allOf(
                        hasProperty("status", equalTo(CoreCoinStatus.INACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );
        ;
        assertNull(coin.getUid());
        assertThat(coin.getNominal(), comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));

        CoinNoAuth coinNoAuth =
                coinService.search.getCoinsNoAuth(Collections.singleton(coin.getCoinKey())).get(coin.getCoinKey());
        assertEquals(yandexUid, coinNoAuth.getUserInfo().getYandexUid());
        assertEquals(uuid, coinNoAuth.getUserInfo().getUuid());
    }

    @Test
    public void shouldUseSameActivationTokenForOneEvent() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        Promo secondPromo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(firstPromo);
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(secondPromo);

        List<TriggerActionResult> results = triggerEventQueueService.insertAndProcessEvent(
                orderStatusUpdated(noAuth()), discountUtils.getRulesPayload(), BudgetMode.SYNC);
        List<Coin> coins = processResultUtils.request(results, Coin.class);

        assertEquals(coins.get(0).getActivationToken(), coins.get(1).getActivationToken());
    }

    @Test
    public void shouldProcessOrderStatusUpdatedWithoutErrorWhenExceededBudget() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.ONE));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(withUid(UID)));
        triggerEventQueueService.addEventToQueue(orderStatusUpdated(withUid(DEFAULT_UID)));

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        List<Coin> coins = coinService.search.getInactiveCoinsByUid(UID, CoreMarketPlatform.BLUE);
        coins.addAll(coinService.search.getInactiveCoinsByUid(DEFAULT_UID, CoreMarketPlatform.BLUE));
        assertThat(coins, hasSize(1));

        List<TriggerEvent> results = triggerEventDao.getAll();
        assertThat(results, containsInAnyOrder(
                allOf(
                        hasProperty("eventType", equalTo(TriggerEventTypes.ORDER_STATUS_UPDATED)),
                        hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS))
                ),
                allOf(
                        hasProperty("eventType", equalTo(TriggerEventTypes.ORDER_STATUS_UPDATED)),
                        hasProperty("processedResult", equalTo(TriggerEventProcessedResult.NO_TRIGGERS))
                )
        ));
    }
}
