package ru.yandex.market.loyalty.core.service.cashback;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.discount.PaymentFeature;
import ru.yandex.market.loyalty.api.model.discount.PaymentInfo;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.cashback.RuleId;
import ru.yandex.market.loyalty.core.model.ids.CashbackPropsId;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingStates;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.order.OrderStage;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest;
import ru.yandex.market.loyalty.core.service.discount.MultiOrderCashback;
import ru.yandex.market.loyalty.core.service.discount.PromoCalculationList;
import ru.yandex.market.loyalty.core.service.discount.PromoMultiStageState;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;
import ru.yandex.market.loyalty.core.service.perks.StatusFeaturesSet;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.RulePayloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.api.model.PaymentSystem.MAESTRO;
import static ru.yandex.market.loyalty.api.model.PaymentSystem.MASTERCARD;
import static ru.yandex.market.loyalty.api.model.PaymentSystem.UNKNOWN;
import static ru.yandex.market.loyalty.api.model.PaymentType.BANK_CARD;
import static ru.yandex.market.loyalty.api.model.PaymentType.YANDEX;
import static ru.yandex.market.loyalty.core.model.multistage.ResolvingState.CANCELLED;
import static ru.yandex.market.loyalty.core.model.multistage.ResolvingState.FINAL;
import static ru.yandex.market.loyalty.core.model.multistage.ResolvingState.INTERMEDIATE;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_CREATION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_PAID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PAYMENT_FEATURE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PAYMENT_FEATURES_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class CashbackServiceMultiStageTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CashbackService cashbackService;

    @Test
    public void shouldMakeUpMultiStageResolutionOnlyForMultiStageRules() {
        final Item item = someItem(null, null, 1000, 1);
        final Set<DiscountCalculationRequest.Cart> carts = Set.of(someCart(List.of(item)));

        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE)
                .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, item.getSku())
        );
        reloadPromoCache();

        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                carts,
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(ORDER_CREATION)
                        .build())
        );


        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();

        // проверяем что акция сработала
        assertEquals(cashback.getCashback().getEmit().getTotalAmount(), BigDecimal.valueOf(10));
        // проверяем что ее нет в состояниях мультистейджинга так как у нее нет мультистейдж правил
        assertNull(promoMultiStageStates.get(promo.getPromoKey()));
    }

    @Test
    public void shouldMakeUpMultiStageResolutionOnlyForMatchedPromo() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE)
                // правило (1) для этого правила isInitialCheckSuccessRequired = false
                .addCashbackRule(ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE, CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, MASTERCARD)
                // правило (2) для этого правила isInitialCheckSuccessRequired = true
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000))
        );
        reloadPromoCache();

        // для этого заказа
        // (1) не пройдет
        // (2) пройдет
        final Item item = someItem(BANK_CARD, MASTERCARD, 1000, 1);
        final Set<DiscountCalculationRequest.Cart> carts = Set.of(someCart(List.of(item)));
        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                carts,
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(ORDER_CREATION)
                        .build())
        );


        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();

        // проверяем что акция не сработала так как (1) не прошел
        assertEquals(cashback.getCashback().getEmit().getTotalAmount(), BigDecimal.valueOf(0));
        // проверяем (2) не попало в состояния мультистейджинга так как промка не сматчилась
        assertTrue(promoMultiStageStates.isEmpty());
    }

    @Test
    public void shouldResolvePromoToIntermediateIfRuleFailOnOrderCreationStage() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE)
                .addCashbackRule(ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE, CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, MASTERCARD)
        );
        reloadPromoCache();

        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(someCart(List.of(someItem(null, null, 1000, 1)))),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(ORDER_CREATION)
                        .build()));


        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();

        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getAmount(), BigDecimal.valueOf(0));
        assertTrue(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().getResolvingState(),
                INTERMEDIATE);
        // проверяем что правило упало но промка все равно сматчилась так как isInitialSuccessRequired = false
        assertFalse(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().getResolvingState(), INTERMEDIATE);
    }

    @Test
    public void shouldUseOrderCreationStageByDefault() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE)
                .addCashbackRule(ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE, CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, MASTERCARD)
        );
        reloadPromoCache();

        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(
                        someCart(List.of(someItem(BANK_CARD, MASTERCARD, 1000, 1)))
                ),
                Collections.emptyMap(),
                // тут не задаем мультистейджинг контекст, по дефолту контекст будет с orderStage = ORDER_CREATION
                getRulesPayload(SpendMode.DRY_RUN, null)
        );


        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();

        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getAmount(), BigDecimal.valueOf(10));
        assertTrue(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().getResolvingState(),
                INTERMEDIATE);
        assertTrue(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().getResolvingState(), INTERMEDIATE);
    }

    @Test
    public void shouldUseOrderCreationStageByDefault1() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE)
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000))
        );
        reloadPromoCache();

        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(
                        someCart(List.of(someItem(BANK_CARD, MASTERCARD, 1000, 1)))
                ),
                Collections.emptyMap(),
                // тут не задаем мультистейджинг контекст, по дефолту контекст будет с orderStage = ORDER_CREATION
                getRulesPayload(SpendMode.DRY_RUN, null)
        );


        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();

        assertEquals(promoMultiStageStates.size(), 0);
    }

    @Test
    public void shouldMakeUpMultiStageResolutionForCancelledPromo() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE)
                .addCashbackRule(ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE, CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, MASTERCARD)
        );
        reloadPromoCache();

        Item item = someItem(BANK_CARD, MAESTRO, 1000, 1);
        DiscountCalculationRequest.Cart cart = someCart(List.of(item));
        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(
                        cart
                ),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(OrderStage.ORDER_PAID)
                        .multiStagePromo(Map.of(promo.getPromoId(), CashbackPropsId.of(promo.getCashbackPropsId())))
                        .lastMultiStageRuleResolvingState(
                                Map.of(
                                        getPaymentSystemRuleId(promo),
                                        new ResolvingStates(INTERMEDIATE, Map.of(ORDER_CREATION, true))
                                )
                        )
                        .lastMultiStageRuleAllowedCarts(Map.of(getPaymentSystemRuleId(promo),
                                AllowedCarts.allItemsAllowed(SpendMode.SPEND, Set.of(item))))
                        .build())
        );


        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();

        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getAmount(), BigDecimal.valueOf(0));
        assertFalse(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().getResolvingState(),
                CANCELLED);
        assertEquals(1, promoMultiStageStates.get(promo.getPromoKey()).getRuleStates().size());
        assertFalse(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().getResolvingState(), CANCELLED);
    }

    @Test
    public void shouldNarrowCashbackPromoIndexAndUseOnlyPromoFromCashbackComputeContext() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE));
        reloadPromoCache();

        // на стадии ORDER_PAID должны матчится только те промки которые переданы в контексте
        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(
                        someCart(List.of(someItem(BANK_CARD, MASTERCARD, 1000, 1)))
                ),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(OrderStage.ORDER_PAID)
                        // тут передаем пустой список промок
                        .multiStagePromo(Map.of())
                        .build())
        );

        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();


        // проверяем что акция не сработала хотя подходила по условиям
        assertEquals(cashback.getCashback().getEmit().getTotalAmount(), BigDecimal.valueOf(0));

        // проверяем что резолвинг пустой
        assertNull(promoMultiStageStates.get(promo.getPromoKey()));
    }

    @Test
    public void shouldResolvePromoToIntermediateOnOrderPaidStage() {
        final Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE)
                .addCashbackRule(ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE, CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, MASTERCARD)
        );
        reloadPromoCache();

        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(someCart(List.of(someItem(BANK_CARD, MASTERCARD, 1000, 1)))),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(OrderStage.ORDER_PAID)
                        .multiStagePromo(Map.of(promo.getPromoId(),
                                getCashbackPropsId(promo)))
                        .lastMultiStageRuleAllowedCarts(Map.of(
                                getPaymentSystemRuleId(promo),
                                AllowedCarts.empty())
                        )
                        .lastMultiStageRuleResolvingState(Map.of(
                                getPaymentSystemRuleId(promo),
                                new ResolvingStates(INTERMEDIATE, Map.of(ORDER_CREATION, false)))
                        )
                        .build())
        );

        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();

        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getAmount(), BigDecimal.valueOf(10));
        assertTrue(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().getResolvingState(),
                INTERMEDIATE);
        assertTrue(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().getResolvingState(), INTERMEDIATE);
    }

    @Test
    public void shouldResolvePromoToFinalOnOrderDeliveredStage() {
        final Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE)
                .addCashbackRule(ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE, CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, MASTERCARD)
        );
        reloadPromoCache();

        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(someCart(List.of(someItem(BANK_CARD, MASTERCARD, 1000, 1)))),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(OrderStage.ORDER_DELIVERED)
                        .multiStagePromo(Map.of(promo.getPromoId(),
                                getCashbackPropsId(promo)))
                        .lastMultiStageRuleAllowedCarts(Map.of(
                                getPaymentSystemRuleId(promo),
                                AllowedCarts.empty())
                        )
                        .lastMultiStageRuleResolvingState(Map.of(
                                getPaymentSystemRuleId(promo),
                                new ResolvingStates(INTERMEDIATE, Map.of(ORDER_CREATION, false, ORDER_PAID, true)))
                        )
                        .build())
        );

        Map<String, PromoMultiStageState> promoMultiStageStates =
                cashback.getCashback().getEmit().getPromoMultiStageStates();

        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getAmount(), BigDecimal.valueOf(10));
        assertTrue(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getPromoResolvingResult().getResolvingState(),
                FINAL);
        assertTrue(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().isSuccess());
        assertEquals(promoMultiStageStates.get(promo.getPromoKey()).getRuleResolvingResult(getPaymentSystemRuleId(promo)).get().getResolvingState(), FINAL);
    }

    @Test
    public void testMaxOrderTotalCuttingRule() {
        final Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE, CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(MAX_ORDER_TOTAL_CUTTING_RULE, MAX_ORDER_TOTAL, BigDecimal.valueOf(15_000))
        );
        reloadPromoCache();
        // сумма заказа меньше порога
        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(someCart(List.of(someItem(BANK_CARD, MASTERCARD, 1000, 3)))),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(ORDER_CREATION)
                        .build())
        );
        assertEquals(BigDecimal.valueOf(30), cashback.getCashback().getEmit().getTotalAmount());
        assertEquals(CashbackPermision.ALLOWED, cashback.getCashback().getEmit().getType());
        // сумма заказа больше порога
        cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(someCart(List.of(someItem(BANK_CARD, MASTERCARD, 1000, 16)))),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN, CashbackComputeContext.builder()
                        .orderStage(ORDER_CREATION)
                        .build())
        );
        assertEquals(BigDecimal.ZERO, cashback.getCashback().getEmit().getTotalAmount());
    }

    @Test
    public void testPaymentFeaturesCuttingRule() {
        final Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE, CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(PAYMENT_FEATURES_CUTTING_RULE, PAYMENT_FEATURE, Set.of(PaymentFeature.YA_BANK))
        );
        reloadPromoCache();
        // в запросе есть нужный PaymentFeature
        MultiOrderCashback cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(someCart(List.of(someItem(
                        YANDEX,
                        UNKNOWN,
                        1000,
                        3,
                        "100S",
                        new PaymentInfo(null, Set.of(PaymentFeature.YA_BANK))
                )))),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN,
                        CashbackComputeContext.builder()
                                .orderStage(ORDER_CREATION)
                                .build())
        );
        assertEquals(BigDecimal.valueOf(30), cashback.getCashback().getEmit().getTotalAmount());
        assertEquals(CashbackPermision.ALLOWED, cashback.getCashback().getEmit().getType());

        // в запросе нет paymentInfo
        cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(someCart(List.of(someItem(YANDEX, UNKNOWN, 1000, 16)))),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN,
                        CashbackComputeContext.builder()
                                .orderStage(ORDER_PAID)
                                .build())
        );
        assertEquals(BigDecimal.ZERO, cashback.getCashback().getEmit().getTotalAmount());

        // в запросе нет нужного paymentFeature
        cashback = cashbackService.calculateCashback(
                DEFAULT_UID,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                Set.of(someCart(List.of(someItem(
                        YANDEX,
                        UNKNOWN,
                        1000,
                        16,
                        "100S",
                        new PaymentInfo(null, Set.of(PaymentFeature.UNKNOWN))
                )))),
                Collections.emptyMap(),
                getRulesPayload(SpendMode.DRY_RUN,
                        CashbackComputeContext.builder()
                                .orderStage(ORDER_PAID)
                                .build())
        );
        assertEquals(BigDecimal.ZERO, cashback.getCashback().getEmit().getTotalAmount());
    }


    private CashbackPropsId getCashbackPropsId(Promo promo) {
        return CashbackPropsId.of(promo.getCashbackPropsId());
    }

    private RuleId getPaymentSystemRuleId(Promo promo) {
        return RuleId.of(getCashbackPropsId(promo),
                ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE.getBeanName());
    }

    @NotNull
    private Item someItem(PaymentType paymentType, PaymentSystem paymentSystem, int price, int quantity) {
        return someItem(paymentType, paymentSystem, price, quantity, "100S", null);
    }

    @NotNull
    private Item someItem(
            PaymentType paymentType,
            PaymentSystem paymentSystem,
            int price,
            int quantity,
            String ssku,
            PaymentInfo paymentInfo
    ) {
        return Item.Builder.create()
                .withKey(ItemKey.withBundle(1L, "1", "cart1", 1L, null))
                .withPrice(BigDecimal.valueOf(price))
                .withQuantity(BigDecimal.valueOf(quantity))
                .withDownloadable(false)
                .withHyperCategoryId(100)
                .withSku("100")
                .withSsku(ssku)
                .withVendorId(100L)
                .withWeight(BigDecimal.ONE)
                .withVolume(10L)
                .withSupplierId(100L)
                .withWarehouseId(100)
                .atSupplierWarehouse(false)
                .loyaltyProgramPartner(false)
                .withPlatform(CoreMarketPlatform.BLUE)
                .primaryInBundle(false)
                .withIsExpressDelivery(false)
                .withPayByYaPlus(0)
                .withMarketBrandedPickup(false)
                .withPaymentType(paymentType)
                .withPaymentSystem(paymentSystem)
                .withPaymentInfo(paymentInfo)
                .build();
    }

    @NotNull
    private DiscountCalculationRequest.Cart someCart(List<Item> items) {
        return new DiscountCalculationRequest.Cart(
                "cart1",
                null,
                null,
                false,
                CoreMarketPlatform.BLUE,
                BigDecimal.valueOf(1),
                10L,
                items,
                Collections.emptyList(),
                SpendMode.SPEND,
                PromoCalculationList.empty()
        );
    }

    private RulePayloads<?> getRulesPayload(SpendMode spendMode, CashbackComputeContext context) {
        return DiscountUtils.getRulesPayload(
                spendMode,
                Collections.emptyMap(),
                null,
                StatusFeaturesSet.enabled(Collections.singleton(PerkType.YANDEX_CASHBACK)),
                null,
                null,
                UsageClientDeviceType.DESKTOP,
                context,
                false
        );
    }
}
