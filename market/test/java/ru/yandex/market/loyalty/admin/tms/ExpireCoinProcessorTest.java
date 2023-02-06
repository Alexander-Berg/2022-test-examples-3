package ru.yandex.market.loyalty.admin.tms;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.checkouter.CheckouterEventRestProcessor;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.CheckouterMockUtils;
import ru.yandex.market.loyalty.core.trigger.actions.ProcessResultUtils;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.EXPIRED;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.trigger.event.data.OrderEventInfo.createOrderEventInfoFromOrder;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EMISSION_BUDGET;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor({CheckouterEventRestProcessor.class, CoinEndProcessor.class})
public class ExpireCoinProcessorTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private ProcessResultUtils processResultUtils;
    @Autowired
    private CoinEndProcessor coinEndProcessor;
    @Autowired
    private ClockForTests clock;
    @Autowired
    private CheckouterMockUtils checkouterMockUtils;
    @Autowired
    private DiscountUtils discountUtils;

    @Test
    public void shouldExpireCoin() throws InterruptedException {
        String mskuForFirstCoin = "123141412";
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setEmissionBudget(DEFAULT_EMISSION_BUDGET)
                        .addCoinRule(MSKU_FILTER_RULE, MSKU_ID, mskuForFirstCoin)
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID));

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrderStatusUpdatedEvent event = CheckouterUtils.createEvent(
                createOrderEventInfoFromOrder(order).build(),
                null
        );

        List<Coin> coins = processResultUtils.request(triggerEventQueueService.insertAndProcessEvent(event,
                discountUtils.getRulesPayload(), BudgetMode.SYNC
        ), Coin.class);
        Coin coin = coins.get(0);
        assertEquals(INACTIVE, coin.getStatus());

        coinService.lifecycle.activateInactiveCoins(CoreMarketPlatform.BLUE, coins);

        clock.setDate(new Date(coin.getRoundedEndDate().getTime() + 1000));

        coinEndProcessor.process(10_000L);

        coin = coinService.search.getCoin(coin.getCoinKey()).orElseThrow(() -> new AssertionError("Coin not found"));
        assertEquals(EXPIRED, coin.getStatus());

        assertThat(coinService.search.getActiveInactiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)
                .platform(promo.getPlatform())), empty());

        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(), comparesEqualTo(DEFAULT_EMISSION_BUDGET));
    }
}
