package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.TechnicalBudgetMode;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.cashback.CashbackCacheService;
import ru.yandex.market.loyalty.core.service.discount.DiscountAntifraudService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;
import ru.yandex.market.loyalty.core.service.perks.StatusFeaturesSet;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.MULTI_ORDER_ID;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.FAKE_CONFIRMED;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.defaultPercent;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(CashbackBudgetProcessor.class)
public class CashbackBudgetProcessorTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CashbackCacheService cashbackCacheService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    @Blackbox
    protected RestTemplate blackboxRestTemplate;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private CashbackBudgetProcessor cashbackBudgetProcessor;
    @Autowired
    private PromoService promoService;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private DiscountAntifraudService discountAntifraudService;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;

    @Test
    public void shouldInactivateCashbackPromoWithNegativeBalance() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(10).setEmissionBudget(50));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        budgetService.performSingleTransaction(BigDecimal.valueOf(51),
                promo.getBudgetEmissionAccountId(),
                promo.getSpendingEmissionAccountId(),
                TechnicalBudgetMode.SYNC_WITH_NEGATIVE_ALLOWED,
                MarketLoyaltyErrorCode.BUDGET_EXCEEDED);
        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(-1))
                )
        );
        assertEquals(promoService.getPromo(promo.getId()).getStatus(), PromoStatus.ACTIVE);


        cashbackBudgetProcessor.inactivateCashbackWithBudgetBelowZero();
        assertEquals(promoService.getPromo(promo.getId()).getStatus(), PromoStatus.INACTIVE);
    }

    @Test
    public void shouldRevertCashbackForCancelAllOrders() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(10).setEmissionBudget(1000));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final MultiCartWithBundlesDiscountRequest request = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(500)), itemKey(DEFAULT_ITEM_KEY))
                        .withOrderId(Long.toString(CheckouterUtils.DEFAULT_ORDER_ID))
                        .build(),
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(700)), itemKey(ANOTHER_ITEM_KEY))
                        .withOrderId(Long.toString(CheckouterUtils.ANOTHER_ORDER_ID))
                        .build())
                .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID).buildOperationContextDto())
                .withMultiOrderId(MULTI_ORDER_ID)
                .withCashbackOptionType(CashbackType.EMIT)
                .build();

        discountService.spendDiscounts(
                request,
                SpendMode.SPEND,
                DiscountUtils.getRulesPayload(
                        SpendMode.SPEND,
                        Collections.emptyMap(),
                        PromoApplicabilityPolicy.ANY,
                        StatusFeaturesSet.enabled(Set.of(YANDEX_PLUS, YANDEX_CASHBACK)),
                        discountAntifraudService.createAntifraudCheckFuture(DEFAULT_UID),
                        discountAntifraudService.createAntifraudMobileOrdersCheckFuture(DEFAULT_UID),
                        UsageClientDeviceType.APPLICATION,
                        null
                ),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(880))
                )
        );

        CheckouterUtils.OrderBuilder orderBuilder1 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(CheckouterUtils.DEFAULT_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(500))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(DEFAULT_ITEM_KEY)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder2 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(CheckouterUtils.ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(700))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(ANOTHER_ITEM_KEY)
                        .build()
                );

        processEvent(orderBuilder1.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        CheckouterUtils.OrderBuilder orderBuilder3 = CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                .setOrderId(CheckouterUtils.DEFAULT_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(500))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(DEFAULT_ITEM_KEY)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder4 = CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                .setOrderId(CheckouterUtils.ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(700))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(ANOTHER_ITEM_KEY)
                        .build()
                );
        processEvent(orderBuilder3.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder4.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(1000))
                )
        );
    }

    @Test
    public void shouldRevertCashbackForCancelOneOrder() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(10).setEmissionBudget(1000));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final MultiCartWithBundlesDiscountRequest request = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(500)), itemKey(DEFAULT_ITEM_KEY))
                        .withOrderId(Long.toString(CheckouterUtils.DEFAULT_ORDER_ID))
                        .build(),
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(700)), itemKey(ANOTHER_ITEM_KEY))
                        .withOrderId(Long.toString(CheckouterUtils.ANOTHER_ORDER_ID))
                        .build())
                .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID).buildOperationContextDto())
                .withMultiOrderId(MULTI_ORDER_ID)
                .withCashbackOptionType(CashbackType.EMIT)
                .build();

        discountService.spendDiscounts(
                request,
                SpendMode.SPEND,
                DiscountUtils.getRulesPayload(
                        SpendMode.SPEND,
                        Collections.emptyMap(),
                        PromoApplicabilityPolicy.ANY,
                        StatusFeaturesSet.enabled(Set.of(YANDEX_PLUS, YANDEX_CASHBACK)),
                        discountAntifraudService.createAntifraudCheckFuture(DEFAULT_UID),
                        discountAntifraudService.createAntifraudMobileOrdersCheckFuture(DEFAULT_UID),
                        UsageClientDeviceType.APPLICATION,
                        null
                ),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(880))
                )
        );

        CheckouterUtils.OrderBuilder orderBuilder1 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(CheckouterUtils.DEFAULT_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(500))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(DEFAULT_ITEM_KEY)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder2 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(CheckouterUtils.ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(700))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(ANOTHER_ITEM_KEY)
                        .build()
                );

        processEvent(orderBuilder1.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        CheckouterUtils.OrderBuilder orderBuilder3 = CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                .setOrderId(CheckouterUtils.DEFAULT_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(500))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(DEFAULT_ITEM_KEY)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder4 = CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                .setOrderId(CheckouterUtils.ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(700))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(ANOTHER_ITEM_KEY)
                        .build()
                );
        processEvent(orderBuilder3.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder4.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(950))
                )
        );
    }

    @Test
    public void shouldConfirmCashbackForDeliveredOrders() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(10).setEmissionBudget(1000));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final MultiCartWithBundlesDiscountRequest request = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(500)), itemKey(DEFAULT_ITEM_KEY))
                        .withOrderId(Long.toString(CheckouterUtils.DEFAULT_ORDER_ID))
                        .build(),
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(700)), itemKey(ANOTHER_ITEM_KEY))
                        .withOrderId(Long.toString(CheckouterUtils.ANOTHER_ORDER_ID))
                        .build())
                .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID).buildOperationContextDto())
                .withMultiOrderId(MULTI_ORDER_ID)
                .withCashbackOptionType(CashbackType.EMIT)
                .build();

        discountService.spendDiscounts(
                request,
                SpendMode.SPEND,
                DiscountUtils.getRulesPayload(
                        SpendMode.SPEND,
                        Collections.emptyMap(),
                        PromoApplicabilityPolicy.ANY,
                        StatusFeaturesSet.enabled(Set.of(YANDEX_PLUS, YANDEX_CASHBACK)),
                        discountAntifraudService.createAntifraudCheckFuture(DEFAULT_UID),
                        discountAntifraudService.createAntifraudMobileOrdersCheckFuture(DEFAULT_UID),
                        UsageClientDeviceType.APPLICATION,
                        null
                ),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(880))
                )
        );

        CheckouterUtils.OrderBuilder orderBuilder1 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(CheckouterUtils.DEFAULT_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(500))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(DEFAULT_ITEM_KEY)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder2 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(CheckouterUtils.ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(700))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(ANOTHER_ITEM_KEY)
                        .build()
                );

        processEvent(orderBuilder1.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        CheckouterUtils.OrderBuilder orderBuilder3 = CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                .setOrderId(CheckouterUtils.DEFAULT_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(500))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(DEFAULT_ITEM_KEY)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder4 = CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                .setOrderId(CheckouterUtils.ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(700))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(ANOTHER_ITEM_KEY)
                        .build()
                );
        processEvent(orderBuilder3.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder4.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        deferredMetaTransactionService.consumeBatchOfTransactions(100);


        assertThat(yandexWalletTransactionDao.findAllByOrderId(CheckouterUtils.DEFAULT_ORDER_ID),
                contains(
                        allOf(
                                hasProperty("emissionTransactionId", notNullValue()),
                                hasProperty("status", equalTo(FAKE_CONFIRMED)),
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(50))),
                                hasProperty("orderId", equalTo(CheckouterUtils.DEFAULT_ORDER_ID)),
                                hasProperty("multiOrderId", nullValue())
                        )));
        assertThat(yandexWalletTransactionDao.findAllByOrderId(CheckouterUtils.ANOTHER_ORDER_ID),
                contains(
                        allOf(
                                hasProperty("emissionTransactionId", notNullValue()),
                                hasProperty("status", equalTo(FAKE_CONFIRMED)),
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(70))),
                                hasProperty("orderId", equalTo(CheckouterUtils.ANOTHER_ORDER_ID)),
                                hasProperty("multiOrderId", nullValue())
                        )));
    }


}
