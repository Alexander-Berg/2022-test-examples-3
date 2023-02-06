package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.coin.OrderStatusUpdatedRequest;
import ru.yandex.market.loyalty.api.model.coin.OrdersUpdatedCoinsForFront;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.config.properties.AntifraudExecutorProperties;
import ru.yandex.market.loyalty.core.dao.DataVersionDao;
import ru.yandex.market.loyalty.core.dao.antifraud.FraudCoinDisposeQueueDao;
import ru.yandex.market.loyalty.core.dao.antifraud.FraudCoinDisposeStatus;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.mock.AntiFraudMockUtil;
import ru.yandex.market.loyalty.core.model.DataVersion;
import ru.yandex.market.loyalty.core.model.antifraud.RecordIdWithCount;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.BadUeUsersService;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.antifraud.AntiFraudService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.test.CheckouterMockUtils;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.B2B_USERS;
import static ru.yandex.market.loyalty.core.rule.RuleType.FOR_B2B_USERS_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(CoinsController.class)
public class CoinsControllerAntifraudTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private RestTemplate antifraudRestTemplate;
    @Autowired
    private FraudCoinDisposeQueueDao fraudCoinDisposeQueueDao;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private BadUeUsersService badUeUsersService;
    @Autowired
    private DataVersionDao dataVersionDao;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoService promoService;
    @Autowired
    private CheckouterMockUtils checkouterMockUtils;
    @Autowired
    private AntiFraudService antifraudService;
    @Autowired
    private AntifraudExecutorProperties executorProps;
    @Autowired
    private AntiFraudMockUtil antiFraudMockUtil;

    @Test
    public void shouldBlockFraudCoinIfAntifraudEnabled() {
        when(antifraudRestTemplate.exchange(any(), eq(LoyaltyVerdictDto.class)))
                .thenReturn(ResponseEntity.ok(new LoyaltyVerdictDto(LoyaltyVerdictType.BLACKLIST, emptyList(),
                        emptyList(), false)));

        configurationService.set("market.loyalty.config.antifraud.enabled", "true");

        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(300)));

        final CoinKey coinKey = coinService.create.createCoin(promo,
                defaultAuth().setStatus(CoreCoinStatus.ACTIVE).build());

        assertThat(
                marketLoyaltyClient.calculateDiscount(
                        builder(orderRequestBuilder()
                                .withOrderItem(
                                        price(500)
                                ).build())
                                .withCoins(coinKey).build()),
                allOf(
                        hasProperty(
                                "coinErrors",
                                hasSize(1)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                hasProperty(
                                                                        "promos",
                                                                        is(empty())
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        antifraudService.awaitAllExecutors(executorProps);

        final Map<Long, List<RecordIdWithCount>> ids =
                fraudCoinDisposeQueueDao
                        .getReadyInQueueUidsWithRecordIds(100, FraudCoinDisposeStatus.IN_QUEUE);

        assertThat(ids.keySet(), hasSize(1));
    }


    @Test
    public void shouldEmitCoinForBadUeUserIfPromoAllowThis() {
        badUeUsersService.addUid(1, DEFAULT_UID);
        dataVersionDao.saveDataVersion(DataVersion.BAD_UE_USERS, 1);
        badUeUsersService.reloadBlacklist();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTrigger(
                promo,
                triggersFactory.createCoinAction("{\"badUeUserAllowed\": true}"),
                TriggerGroupType.MANDATORY_TRIGGERS
        );

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        final OrdersUpdatedCoinsForFront ordersUpdatedCoinsForFront = marketLoyaltyClient.sendOrderStatusUpdatedEvent(
                new OrderStatusUpdatedRequest(
                        orderId,
                        DEFAULT_UID,
                        null,
                        false
                )
        );

        assertThat(
                ordersUpdatedCoinsForFront,
                hasProperty("newCoins", hasSize(1))
        );

        assertThat(
                promoService.getPromo(promo.getPromoId().getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );


        final List<TriggerEvent> events = triggerEventDao.getAll();

        assertEquals(1, events.size());
        assertThat(
                events.get(0),
                allOf(
                        hasProperty("processedResult", equalTo(TriggerEventProcessedResult.FORBIDDEN))
                )
        );
    }

    @Test
    public void shouldApplyB2BCoinOfFirstOrderIfItIsFirstOrderByAntifraud() {
        // Given
        Boolean isB2bUser = true;
        Long businessBalanceId = 111L;
        Boolean isFirstOrder = true;
        configurationService.set(ConfigurationService.ENABLE_B2B_CUTTING_RULE, true);  // enable b2b logic

        // set up antifraud response
        LoyaltyVerdictDto loyaltyVerdict = new LoyaltyVerdictDto(
                LoyaltyVerdictType.OTHER,
                Collections.emptyList(),
                Collections.emptyList(),
                isFirstOrder
        );
        antiFraudMockUtil.loyaltyDetect(loyaltyVerdict,
                hasProperty("body",
                        hasProperty("userParams",
                                hasProperty("businessBalanceId", equalTo(businessBalanceId)))
                        ));

        // create promo for B2B users and allowed for first order only
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(FOR_B2B_USERS_CUTTING_RULE, B2B_USERS, true)  // B2B promo
                        .addCoinRule(RuleContainer.builder(RuleType.FIRST_ORDER_CUTTING_RULE))  // first order
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(1000)
                )
                .build();

        // request to calculate discount
        OperationContextDto operationContext = OperationContextFactory.withUidBuilder(DEFAULT_UID)
                .buildOperationContextDto();
        operationContext.setIsB2B(isB2bUser);
        operationContext.setBusinessBalanceId(businessBalanceId);

        // When
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withOperationContext(operationContext)
                        .withCoins(coinKey).build()
        );

        // Then
        assertFalse(discountResponse.getCoins().isEmpty());
        assertTrue(discountResponse.getCoinErrors().isEmpty());
        assertTrue(discountResponse.getUnusedCoins().isEmpty());
    }

    @Test
    public void shouldNotApplyB2BCoinOfFirstOrderIfItIsNotFirstOrderByAntifraud() {
        // Given
        Boolean isB2bUser = true;
        Long businessBalanceId = 111L;
        Boolean isFirstOrder = false;
        configurationService.set(ConfigurationService.ENABLE_B2B_CUTTING_RULE, true);  // enable b2b logic

        // set up antifraud response
        LoyaltyVerdictDto loyaltyVerdict = new LoyaltyVerdictDto(
                LoyaltyVerdictType.OTHER,
                Collections.emptyList(),
                Collections.emptyList(),
                isFirstOrder
        );
        antiFraudMockUtil.loyaltyDetect(loyaltyVerdict,
                hasProperty("body",
                        hasProperty("userParams",
                                hasProperty("businessBalanceId", equalTo(businessBalanceId)))
                ));

        // create promo for B2B users and allowed for first order only
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(FOR_B2B_USERS_CUTTING_RULE, B2B_USERS, true)  // B2B promo
                        .addCoinRule(RuleContainer.builder(RuleType.FIRST_ORDER_CUTTING_RULE))  // first order
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(1000)
                )
                .build();

        // request to calculate discount
        OperationContextDto operationContext = OperationContextFactory.withUidBuilder(DEFAULT_UID)
                .buildOperationContextDto();
        operationContext.setIsB2B(isB2bUser);
        operationContext.setBusinessBalanceId(businessBalanceId);

        // When
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withOperationContext(operationContext)
                        .withCoins(coinKey).build()
        );

        // Then
        assertFalse(discountResponse.getCoinErrors().isEmpty());
        assertEquals(
                MarketLoyaltyErrorCode.ALLOWED_FOR_FIRST_ORDER_ONLY.createError().getCode(),
                discountResponse.getCoinErrors().get(0).getError().getCode());
    }
}
