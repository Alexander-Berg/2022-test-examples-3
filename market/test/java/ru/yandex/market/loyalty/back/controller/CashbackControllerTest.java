package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackOptionsPrecondition;
import ru.yandex.market.loyalty.api.model.CashbackOptionsRequest;
import ru.yandex.market.loyalty.api.model.CashbackOptionsResponse;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackProfileResponse;
import ru.yandex.market.loyalty.api.model.CashbackRequest;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.ItemCashbackRequest;
import ru.yandex.market.loyalty.api.model.ItemCashbackResponse;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OrderCashbackRequest;
import ru.yandex.market.loyalty.api.model.OrderCashbackResponse;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.cashback.CashbackAmountRequests;
import ru.yandex.market.loyalty.api.model.cashback.CashbackAmountResponses;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.api.model.report.InternalSpec;
import ru.yandex.market.loyalty.api.model.report.Specs;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.back.util.ItemPropertiesForTest;
import ru.yandex.market.loyalty.core.mock.AntiFraudMockUtil;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.CashbackPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.PromoStatusWithBudgetCacheService;
import ru.yandex.market.loyalty.core.service.discount.constants.SupplierFlagRestrictionType;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.BankTestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.loyalty.api.model.CashbackOptionsPrecondition.PAYMENT;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.CASHBACK_DISABLED;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.INCOMPLETE_REQUEST;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NOT_SUITABLE_PAYMENT_TYPE;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NOT_SUITABLE_PLATFORM;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NOT_YA_PLUS_SUBSCRIBER;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NO_SUITABLE_PROMO;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.PAYED_BY_PLUS;
import static ru.yandex.market.loyalty.api.model.PaymentType.APPLE_PAY;
import static ru.yandex.market.loyalty.api.model.PaymentType.BANK_CARD;
import static ru.yandex.market.loyalty.api.model.PaymentType.CASH_ON_DELIVERY;
import static ru.yandex.market.loyalty.api.model.PaymentType.GOOGLE_PAY;
import static ru.yandex.market.loyalty.api.model.PaymentType.YANDEX;
import static ru.yandex.market.loyalty.api.model.discount.PaymentFeature.YA_BANK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_BANK_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_EXTRA_PHARMA_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allPaymentTypes;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedEmitCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedSpendCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedWithUiFlags;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.cashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.cashbackPaymentTypes;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.nonCashbackPaymentTypes;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.restrictedEmitCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.restrictedSpendCashback;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MARKET_BRANDED_PICKUP;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_CASHBACK;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_CASHBACK_AGGREGATE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PAYMENT_FEATURE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MARKET_BRANDED_PICKUP_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_CASHBACK_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PAYMENT_FEATURES_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_BANK_CASHBACK_ENABLED;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.FIRST_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.CashbackUtils.validateAmount;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.SUPPLIER_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor({CashbackController.class})
public class CashbackControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private AntiFraudMockUtil antiFraudMockUtil;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    private BankTestUtils bankTestUtils;
    @Autowired
    private PromoService promoService;

    private static final long DEFAULT_REGION_ID = 213;
    private static final long DEFAULT_ORDER_ID = 1L;
    private static final String DEFAULT_CART_ID = "1";
    private static final String UI_FLAG_1 = "1";
    private static final String UI_FLAG_2 = "2";
    private static final String UI_FLAG_3 = "3";
    private static final int LAST_OPTIONS_PROFILE_INDEX = 5;
    public static final Set<Integer> PHARMA_CATEGORY_IDS = Set.of(15754673);
    public static final Specs PHARMA_ALLOWED_SPEC = new Specs(Set.of(new InternalSpec("spec", "baa")));


    @Test
    public void shouldAllowCashbackForSingleItem() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 100, 1)
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        allowedCashback(
                                                null,
                                                BigDecimal.valueOf(99)
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        cashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        null,
                                                                        BigDecimal.valueOf(99)
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                allowedCashback(
                                                                                        null,
                                                                                        BigDecimal.valueOf(99)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                null,
                                                BigDecimal.ZERO
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        nonCashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(null,
                                                                 restrictedSpendCashback(NOT_SUITABLE_PAYMENT_TYPE)),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(null,
                                                                                 restrictedSpendCashback(NOT_SUITABLE_PAYMENT_TYPE))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                BigDecimal.valueOf(10),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        allPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                allowedCashback(
                                                                                        BigDecimal.valueOf(10),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                        ),
                                        hasProperty(
                                                "delivery",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(DeliveryType.PICKUP)
                                                )
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                BigDecimal.valueOf(10),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(BANK_CARD)
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                allowedCashback(
                                                                                        BigDecimal.valueOf(10),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                cashback(allowedEmitCashback(BigDecimal.TEN), null)
                        )
                )
        );
    }

    @Test
    public void shouldRestrictSpendingCashbackForSingleItemWithPostPaidOnly() {
        configurationService.set(ConfigurationService.CASHBACK_OPTIONS_CHECK_ITEMS_PAYMENT_TYPES, true);
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        List.of(new OrderCashbackRequest(
                                        DEFAULT_CART_ID,
                                        DEFAULT_ORDER_ID,
                                        null,
                                        List.of(
                                                createItemRequest(cashbackPromo, 100, 1, Set.of(CASH_ON_DELIVERY))
                                        ),
                                        null
                                ), new OrderCashbackRequest(
                                        DEFAULT_CART_ID + "1",
                                        DEFAULT_ORDER_ID + 1,
                                        null,
                                        List.of(
                                                createItemRequest(cashbackPromo, 200, 2, Set.of(BANK_CARD))
                                        ),
                                        null
                                )
                        ),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

        assertThat(cashbackOptionsResponse.getCashbackOptionsProfiles().get(0).getCashback().getSpend(),
                hasProperty("amount", equalTo(BigDecimal.valueOf(199))));
    }

    @Test
    public void dummyGetCashbackAmountCall() {
        CashbackAmountResponses cashbackAmount =
                marketLoyaltyClient.getCashbackAmount(new CashbackAmountRequests(List.of()));
        assertThat(cashbackAmount.getResponses(), is(empty()));
    }

    @Test
    public void shouldNotAllowCashbackForNotYandexPlusSubscriber() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);


        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 100, 1)
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        cashback(
                                                null,
                                                restrictedSpendCashback(NOT_YA_PLUS_SUBSCRIBER)
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        cashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        null,
                                                                        restrictedSpendCashback(NOT_YA_PLUS_SUBSCRIBER)
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        null,
                                                                                        restrictedSpendCashback(NOT_YA_PLUS_SUBSCRIBER)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                null,
                                                restrictedSpendCashback(NOT_YA_PLUS_SUBSCRIBER)
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        nonCashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        null,
                                                                        restrictedSpendCashback(NOT_YA_PLUS_SUBSCRIBER)
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        null,
                                                                                        restrictedSpendCashback(NOT_YA_PLUS_SUBSCRIBER)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        allPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                        ),
                                        hasProperty(
                                                "delivery",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(DeliveryType.PICKUP)
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(BANK_CARD)
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                cashback(restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER), null)
                        )
                )
        );
    }

    @Test
    public void shouldAllowCashbackForNotYandexPlusSubscriberIfAnyUserExp() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_REARR,
        "yandex_cashback_any_user_enabled=1");
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_ENABLED, "true");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 100, 1)
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                ),
                headers("X-Market-Rearrfactors", "yandex_cashback_any_user_enabled=1")
        );

        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        cashback(
                                                null,
                                                restrictedSpendCashback(CASHBACK_DISABLED)
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        cashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        null,
                                                                        restrictedSpendCashback(CASHBACK_DISABLED)
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        null,
                                                                                        restrictedSpendCashback(CASHBACK_DISABLED)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                null,
                                                restrictedSpendCashback(CASHBACK_DISABLED)
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        nonCashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        null,
                                                                        restrictedSpendCashback(CASHBACK_DISABLED)
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        null,
                                                                                        restrictedSpendCashback(CASHBACK_DISABLED)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                allowedEmitCashback(BigDecimal.TEN),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        allPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        allowedEmitCashback(BigDecimal.TEN),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        allowedEmitCashback(BigDecimal.TEN),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                        ),
                                        hasProperty(
                                                "delivery",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(DeliveryType.PICKUP)
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                allowedEmitCashback(BigDecimal.TEN),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(BANK_CARD)
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        allowedEmitCashback(BigDecimal.TEN),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        allowedEmitCashback(BigDecimal.TEN),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                cashback(allowedEmitCashback(BigDecimal.TEN), null)
                        )
                )
        );
    }


    @Test
    public void shouldNotAllowCashbackForWhitePartners() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                MarketPlatform.WHITE,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 100, 1)
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        cashback(
                                                allowedSpendCashback(BigDecimal.ZERO),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        cashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        null,
                                                                        restrictedSpendCashback(NOT_SUITABLE_PLATFORM)
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        null,
                                                                                        restrictedSpendCashback(NOT_SUITABLE_PLATFORM)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                null,
                                                allowedSpendCashback(BigDecimal.ZERO)
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        nonCashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        null,
                                                                        restrictedSpendCashback(NOT_SUITABLE_PLATFORM)
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        null,
                                                                                        restrictedSpendCashback(NOT_SUITABLE_PLATFORM)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                allowedEmitCashback(BigDecimal.ZERO),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        allPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        restrictedEmitCashback(NOT_SUITABLE_PLATFORM),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        restrictedEmitCashback(NOT_SUITABLE_PLATFORM),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                        ),
                                        hasProperty(
                                                "delivery",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(DeliveryType.PICKUP)
                                                )
                                        )
                                ),
                                allOf(
                                        cashback(
                                                allowedEmitCashback(BigDecimal.ZERO),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(BANK_CARD)
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(
                                                                        restrictedEmitCashback(NOT_SUITABLE_PLATFORM),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        restrictedEmitCashback(NOT_SUITABLE_PLATFORM),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                cashback(allowedEmitCashback(BigDecimal.ZERO), null)
                        )
                )
        );
    }

    @Test
    public void shouldNotAllowCashbackForNoAuthUser() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_PLUS_REQUIRED_FOR_CASHBACK_EMIT, false);

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_MUID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                MarketPlatform.WHITE,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 100, 1)
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );


        assertThat(cashbackOptionsResponse, hasProperty(
                "cashbackOptionsProfiles",
                containsInAnyOrder(
                        cashback(restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER), null),
                        cashback(restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER), null),
                        cashback(restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER), null),
                        cashback(null, restrictedSpendCashback(NOT_YA_PLUS_SUBSCRIBER)),
                        cashback(null, restrictedSpendCashback(NOT_YA_PLUS_SUBSCRIBER)),
                        cashback(restrictedEmitCashback(NOT_YA_PLUS_SUBSCRIBER), null)
                )
        ));
    }

    @Test
    public void shouldAllowCashbackForExpUsers() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_REARR, "yandex_cashback_enabled=1");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 100, 1)
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                ),
                headers("X-Market-Rearrfactors", "yandex_cashback_enabled=1")
        );

        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        allowedCashback(
                                                null,
                                                BigDecimal.valueOf(99)
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        cashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        null,
                                                                        BigDecimal.valueOf(99)
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                allowedCashback(
                                                                                        null,
                                                                                        BigDecimal.valueOf(99)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                null,
                                                BigDecimal.ZERO
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        nonCashbackPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                cashback(null,
                                                                 restrictedSpendCashback(NOT_SUITABLE_PAYMENT_TYPE)),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(null,
                                                                                 restrictedSpendCashback(NOT_SUITABLE_PAYMENT_TYPE))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                BigDecimal.valueOf(10),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        allPaymentTypes()
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                allowedCashback(
                                                                                        BigDecimal.valueOf(10),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                        ),
                                        hasProperty(
                                                "delivery",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(DeliveryType.PICKUP)
                                                )
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                BigDecimal.valueOf(10),
                                                null
                                        ),
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                contains(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(BANK_CARD)
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                allowedCashback(
                                                                                        BigDecimal.valueOf(10),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                cashback(allowedEmitCashback(BigDecimal.TEN), null)

                        )
                )
        );
    }

    @Test
    public void shouldAllowCashbackForLoyaltyProgramPartnerFlagIsEnableAndUseSupplierWarehouseId() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        new ItemCashbackRequest(
                                                "someOfferId",
                                                100L,
                                                BigDecimal.valueOf(100),
                                                BigDecimal.ONE,
                                                false,
                                                BigDecimal.ZERO,
                                                FIRST_CHILD_CATEGORY_ID,
                                                null,
                                                null,
                                                100L,
                                                SUPPLIER_WAREHOUSE_ID,
                                                null,
                                                100L,
                                                "shopId",
                                                "bundleId",
                                                new CashbackRequest(cashbackPromo.getPromoKey(), 1),
                                                null,
                                                null,
                                                true,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null
                                        )
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        allowedCashback(
                                                null,
                                                BigDecimal.valueOf(99)
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                null,
                                                BigDecimal.ZERO
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                BigDecimal.valueOf(10),
                                                null
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                allowedCashback(
                                                                                        BigDecimal.valueOf(10),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                        ),
                                        hasProperty(
                                                "delivery",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(DeliveryType.PICKUP)
                                                )
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                BigDecimal.valueOf(10),
                                                null
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                allowedCashback(
                                                                                        BigDecimal.valueOf(10),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                cashback(allowedEmitCashback(BigDecimal.TEN), null)
                        )
                )
        );
    }

    @Test
    public void shouldReturnPromoCashbackMaximumIfLimitIsExceeded() {
        BigDecimal maximalCashbackForPromo = BigDecimal.valueOf(5);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, maximalCashbackForPromo)
        );

        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(100), BigDecimal.ONE, "testSku1")
        );

        validateAmount(emitProfile.getCashback().getEmit(), maximalCashbackForPromo);
        OrderCashbackResponse orderCashbackResponse = emitProfile.getOrders().iterator().next();
        validateAmount(orderCashbackResponse.getCashback().getEmit(), maximalCashbackForPromo);
        ItemCashbackResponse itemCashbackResponse = orderCashbackResponse.getItems().iterator().next();
        validateAmount(itemCashbackResponse.getCashback().getEmit(), maximalCashbackForPromo);
    }

    @Test
    public void shouldReturnPromoCashbackMaximumIfLimitIsExceededForAlenka() {
        BigDecimal maximalCashbackForPromo = BigDecimal.valueOf(5);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(MAX_CASHBACK_FILTER_RULE)
                                .withSingleParam(MAX_CASHBACK, maximalCashbackForPromo)
                                .withSingleParam(MAX_CASHBACK_AGGREGATE, true)
                        )
        );

        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(100), BigDecimal.ONE, "testSku1")
        );

        validateAmount(emitProfile.getCashback().getEmit(), maximalCashbackForPromo);
        OrderCashbackResponse orderCashbackResponse = emitProfile.getOrders().iterator().next();
        validateAmount(orderCashbackResponse.getCashback().getEmit(), maximalCashbackForPromo);
        ItemCashbackResponse itemCashbackResponse = orderCashbackResponse.getItems().iterator().next();
        validateAmount(itemCashbackResponse.getCashback().getEmit(), maximalCashbackForPromo);
    }


    @Test
    public void shouldReturnPromoCashbackMaximumIfLimitIsExceededForAlenkaWithOtherPromo() {
        BigDecimal maximalCashbackForPromo = BigDecimal.valueOf(5);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(MAX_CASHBACK_FILTER_RULE)
                                .withSingleParam(MAX_CASHBACK, maximalCashbackForPromo)
                                .withSingleParam(MAX_CASHBACK_AGGREGATE, true)
                        )
                        .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, "testSku1")
                        .setPriority(0)
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .addCashbackRule(RuleContainer.builder(MAX_CASHBACK_FILTER_RULE)
                                .withSingleParam(MAX_CASHBACK, BigDecimal.valueOf(3))
                        )
                        .setPriority(-1)
        );

        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(100), BigDecimal.ONE, "testSku1"),
                new ItemPropertiesForTest(BigDecimal.valueOf(100), BigDecimal.ONE, "testSku2")
        );

        validateAmount(emitProfile.getCashback().getEmit(), BigDecimal.valueOf(8));
        OrderCashbackResponse orderCashbackResponse = emitProfile.getOrders().iterator().next();
        validateAmount(orderCashbackResponse.getCashback().getEmit(), BigDecimal.valueOf(8));
        List<ItemCashbackResponse> itemCashbackResponses = orderCashbackResponse.getItems()
                .stream()
                .sorted(Comparator.comparing(ItemCashbackResponse::getOfferId))
                .collect(Collectors.toUnmodifiableList());
        validateAmount(itemCashbackResponses.get(0).getCashback().getEmit(), BigDecimal.valueOf(5));
        validateAmount(itemCashbackResponses.get(1).getCashback().getEmit(), BigDecimal.valueOf(3));
    }

    @Test
    @Ignore
    public void shouldReturnMorePromoCashbackMaximumIfLimitIsExceededAndHavePromoWoMaxCashback() {
        BigDecimal maximalCashbackForCart = BigDecimal.valueOf(3000);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5))
                        .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, "testSku1")
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, maximalCashbackForCart)
                        .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, "testSku2")
                        .setPriority(99)
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(25))
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, maximalCashbackForCart)
                        .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, "testSku3")
                        .setPriority(90)
        );

        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(3600), BigDecimal.ONE, "testSku1"),
                new ItemPropertiesForTest(BigDecimal.valueOf(9999), BigDecimal.ONE, "testSku2"),
                new ItemPropertiesForTest(BigDecimal.valueOf(9999), BigDecimal.ONE, "testSku3"));

        BigDecimal cashbackForMultiOrder = emitProfile.getCashback().getEmit().getAmount();
        validateAmount(emitProfile.getCashback().getEmit(), cashbackForMultiOrder);
        assertThat(cashbackForMultiOrder, greaterThan(maximalCashbackForCart));
    }

    @Test
    public void shouldReturnItemCashbackMaximumIfLimitIsNotExceeded() {
        BigDecimal maximalCashbackForPromo = BigDecimal.valueOf(15);
        BigDecimal expectedCashbackForPromo = BigDecimal.valueOf(10);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, maximalCashbackForPromo)
        );

        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(100), BigDecimal.ONE, "testSku1")
        );

        validateAmount(emitProfile.getCashback().getEmit(), expectedCashbackForPromo);
        OrderCashbackResponse orderCashbackResponse = emitProfile.getOrders().iterator().next();
        validateAmount(orderCashbackResponse.getCashback().getEmit(), expectedCashbackForPromo);
        ItemCashbackResponse itemCashbackResponse = orderCashbackResponse.getItems().iterator().next();
        validateAmount(itemCashbackResponse.getCashback().getEmit(), expectedCashbackForPromo);
    }

    @Test
    public void shouldRecalculateCashbackInCorrectProportion() {
        BigDecimal maximalCashbackForPromo = BigDecimal.valueOf(60);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, maximalCashbackForPromo)
        );

        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(10000), BigDecimal.ONE, "testSku1"),
                new ItemPropertiesForTest(BigDecimal.valueOf(20000), BigDecimal.ONE, "testSku2")
        );

        BigDecimal cashbackForMultiOrder = emitProfile.getCashback().getEmit().getAmount();
        assertThat(cashbackForMultiOrder, comparesEqualTo(maximalCashbackForPromo));
        OrderCashbackResponse orderCashbackResponse = emitProfile.getOrders().iterator().next();
        BigDecimal cashbackForOrder = orderCashbackResponse.getCashback().getEmit().getAmount();
        assertThat(cashbackForOrder, comparesEqualTo(maximalCashbackForPromo));

        List<ItemCashbackResponse> items = orderCashbackResponse.getItems();
        ItemCashbackResponse responseForItemWithIndex1 = findItemResponseByIndex(items, 1);
        ItemCashbackResponse responseForItemWithIndex2 = findItemResponseByIndex(items, 2);
        BigDecimal expectedCashbackForItem1 = BigDecimal.valueOf(20);
        BigDecimal expectedCashbackForItem2 = BigDecimal.valueOf(40);

        validateAmount(getCashbackEmit(responseForItemWithIndex1), expectedCashbackForItem1);
        validateAmount(getCashbackEmit(responseForItemWithIndex2), expectedCashbackForItem2);

    }

    @Test
    public void shouldRecalculateCashbackInCorrectProportionWithNoDefaultQuantityItems() {
        BigDecimal maximalCashbackForPromo = BigDecimal.valueOf(3000);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(25))
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, maximalCashbackForPromo)
        );

        //!!!первый item пойдет в кол-ве 11 шт
        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(889), BigDecimal.valueOf(11), "testSku1"),
                new ItemPropertiesForTest(BigDecimal.valueOf(2760), BigDecimal.ONE, "testSku2")
        );

        BigDecimal cashbackForMultiOrder = emitProfile.getCashback().getEmit().getAmount();
        assertThat(cashbackForMultiOrder, comparesEqualTo(maximalCashbackForPromo));
        OrderCashbackResponse orderCashbackResponse = emitProfile.getOrders().iterator().next();
        BigDecimal cashbackForOrder = orderCashbackResponse.getCashback().getEmit().getAmount();
        assertThat(cashbackForOrder, comparesEqualTo(maximalCashbackForPromo));

        List<ItemCashbackResponse> items = orderCashbackResponse.getItems();
        ItemCashbackResponse responseForItemWithIndex1 = findItemResponseByIndex(items, 1);
        ItemCashbackResponse responseForItemWithIndex2 = findItemResponseByIndex(items, 2);
        BigDecimal expectedCashbackForItem1 = BigDecimal.valueOf(2340);
        BigDecimal expectedCashbackForItem2 = BigDecimal.valueOf(660);

        validateAmount(getCashbackEmit(responseForItemWithIndex1), expectedCashbackForItem1);
        validateAmount(getCashbackEmit(responseForItemWithIndex2), expectedCashbackForItem2);
    }

    private static CashbackOptions getCashbackEmit(ItemCashbackResponse response) {
        return response.getCashback().getEmit();
    }

    @Test
    public void shouldCalculateCorrectCashbackIfAmountNegative() {
        BigDecimal maximalCashbackForPromo = BigDecimal.valueOf(3000);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(25))
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, maximalCashbackForPromo)
        );

        List<CashbackProfileResponse> emitProfile = getCashbackProfileResponseWithNegativeAmount(
                new ItemPropertiesForTest(BigDecimal.valueOf(889), BigDecimal.valueOf(11), "testSku1")
        );
        assertNotNull(emitProfile);
        assertFalse(emitProfile.isEmpty());
        assertEquals(LAST_OPTIONS_PROFILE_INDEX + 1, emitProfile.size());

        assertEquals(BigDecimal.ZERO, emitProfile.get(0).getCashback().getSpend().getAmount());
        assertEquals(CashbackPermision.ALLOWED, emitProfile.get(0).getCashback().getSpend().getType());

        assertEquals(BigDecimal.ZERO, emitProfile.get(1).getCashback().getSpend().getAmount());
        assertEquals(CashbackPermision.ALLOWED, emitProfile.get(1).getCashback().getSpend().getType());

        assertEquals(BigDecimal.ONE, emitProfile.get(2).getCashback().getEmit().getAmount());
        assertEquals(CashbackPermision.ALLOWED, emitProfile.get(2).getCashback().getEmit().getType());

        assertEquals(BigDecimal.ONE, emitProfile.get(3).getCashback().getEmit().getAmount());
        assertEquals(CashbackPermision.ALLOWED, emitProfile.get(3).getCashback().getEmit().getType());

        assertEquals(BigDecimal.ONE, emitProfile.get(4).getCashback().getEmit().getAmount());
        assertEquals(CashbackPermision.ALLOWED, emitProfile.get(4).getCashback().getEmit().getType());
    }

    private List<CashbackProfileResponse> getCashbackProfileResponseWithNegativeAmount(ItemPropertiesForTest item) {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        List<ItemCashbackRequest> itemRequests = new ArrayList<>();
        itemRequests.add(createItemRequestDefaultItemWherePriceWithDeltaEqualsDiscount(item, 1));

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                itemRequests,
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

        return cashbackOptionsResponse
                .getCashbackOptionsProfiles();
    }


    @NotNull
    private static ItemCashbackResponse findItemResponseByIndex(List<ItemCashbackResponse> items, int index) {
        return items.stream().filter(i -> i.getOfferId().contains(String.valueOf(index))).findAny().orElseThrow();
    }

    @NotNull
    private CashbackProfileResponse getCashbackProfileResponse(ItemPropertiesForTest... items) {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        List<ItemCashbackRequest> itemRequests = new ArrayList<>();
        for (ItemPropertiesForTest nextItem : items) {
            itemRequests.add(
                    createItemRequestDefaultItem(nextItem, itemRequests.size() + 1)
            );
        }

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                itemRequests,
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

        return cashbackOptionsResponse
                .getCashbackOptionsProfiles()
                .stream()
                .filter(c -> c.getCashbackTypes().contains(CashbackType.EMIT))
                .findAny()
                .orElseThrow();
    }

    @NotNull
    private static ItemCashbackRequest createItemRequest(Promo cashbackPromo, int itemPrice, int index) {
        return createItemRequest(cashbackPromo, itemPrice, index, null);
    }

    @NotNull
    private static ItemCashbackRequest createItemRequest(Promo cashbackPromo, int itemPrice, int index,
                                                         Set<PaymentType> itemAllowedPaymentTypes) {
        return ItemCashbackRequest.Builder.builder()
                .setOfferId("testOfferId" + index)
                .setFeedId(100L)
                .setPrice(BigDecimal.valueOf(itemPrice))
                .setQuantity(BigDecimal.ONE)
                .setIsDownloadable(false)
                .setDiscount(BigDecimal.ZERO)
                .setHyperCategoryId(FIRST_CHILD_CATEGORY_ID)
                .setVendorId(100L)
                .setWarehouseId(MARKET_WAREHOUSE_ID)
                .setAtSupplierWarehouse(false)
                .setSupplierId(100L)
                .setShopSku("shopId")
                .setBundleId("bundleId")
                .setCashbackRequest(new CashbackRequest(cashbackPromo.getPromoKey(), 1))
                .setAllowedPaymentTypes(itemAllowedPaymentTypes)
                .build();
    }

    @Test
    public void shouldNotReturnCashbackPromoWithPerkRestriction() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_EXTRA_CASHBACK)
        );

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(100), BigDecimal.ONE, "testSku1")
        );
        List<ItemCashbackResponse> items = emitProfile.getOrders().iterator().next().getItems();
        assertEquals(1, items.size());
        assertEquals(NO_SUITABLE_PROMO, items.iterator().next().getCashback().getEmit().getRestrictionReason());
    }

    @Test
    public void shouldReturnCashbackPromoWithPerkRestrictionAndSuitableUserPerk() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_EXTRA_CASHBACK)
        );

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        CashbackProfileResponse emitProfile = getCashbackProfileResponse(
                new ItemPropertiesForTest(BigDecimal.valueOf(100), BigDecimal.ONE, "testSku1")
        );
        List<ItemCashbackResponse> items = emitProfile.getOrders().iterator().next().getItems();
        assertEquals(1, items.size());
        CashbackOptions emittedCashbackForItem = items.iterator().next().getCashback().getEmit();
        assertNull(emittedCashbackForItem.getRestrictionReason());
        assertEquals(CashbackPermision.ALLOWED, emittedCashbackForItem.getType());
        assertThat(BigDecimal.TEN, comparesEqualTo(emittedCashbackForItem.getAmount()));
    }

    @Test
    public void shouldNotReturnCashbackPromoPerkWithBadAntifraudAnswer() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_EXTRA_CASHBACK)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        //----just for BF orders limits-----
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_ENABLED, true);
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_COUNT, 3);
        //----------------------------------
        antiFraudMockUtil.previousOrders(3, 0); // main difference
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        CashbackOptions emit = discountResponse.getCashback().getEmit();
        assertThat(BigDecimal.ZERO, comparesEqualTo(emit.getAmount()));
    }

    @Test
    public void shouldNotReturnCashbackPromoPerkWithBadAntifraudAnswerWithPlatform() {
        final BigDecimal THRESHOLD = BigDecimal.valueOf(3500);
        final BigDecimal CASHBACK = BigDecimal.valueOf(500);
        final Instant promoStart = clock.instant()
                .minus(7, ChronoUnit.DAYS);
        Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .setEmissionBudget(BigDecimal.valueOf(10000))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.WELCOME_CASHBACK)
        );

        cashbackCacheService.reloadCashbackPromos();
        promoStatusWithBudgetCacheService.reloadCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        //----just for first app order promo limits-----
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_ENABLED, true);
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_COUNT, 3);
        configurationService.set(ConfigurationService.WELCOME_APP_500_ANTIFRAUD_GLUE_LIMIT, 1);
        //----------------------------------
        // ---- for welcome cashback perk -------
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY,
         cashbackPromo.getPromoKey());
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
         promoStart.toString());
        configurationService.set(ConfigurationService.MARKET_PROMO_ADVERTISING_CAMPAIGN_500_REARR,
        "market_promo_advertising_campaign_500=1");
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_CASHBACK_AMOUNT, CASHBACK);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_THRESHOLD, THRESHOLD);
        // ---------------------------------------
        antiFraudMockUtil.previousOrders(2, 1); // main difference
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Market-Rearrfactors", "market_promo_advertising_campaign_500=1");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build(),
                httpHeaders
        );
        CashbackOptions emit = discountResponse.getCashback().getEmit();
        assertThat(emit.getAmount(), comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    public void shouldReturnCashbackPromoPerkWithGoodAntifraudAnswer() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_EXTRA_CASHBACK)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        //----just for BF orders limits-----
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_ENABLED, true);
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_COUNT, 3);
        //----------------------------------
        antiFraudMockUtil.previousOrders(2, 0); // main difference
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        CashbackOptions emit = discountResponse.getCashback().getEmit();
        assertThat(BigDecimal.TEN, comparesEqualTo(emit.getAmount()));
    }

    @Test
    public void shouldReturnCashbackPromoPerkWithLimitDisabled() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_EXTRA_CASHBACK)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        //----just for BF orders limits-----
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_ENABLED, false);
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_COUNT, 3);
        //----------------------------------
        antiFraudMockUtil.previousOrders(10, 0); // main difference
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        CashbackOptions emit = discountResponse.getCashback().getEmit();
        assertThat(BigDecimal.TEN, comparesEqualTo(emit.getAmount()));
    }

    @Test
    public void shouldAllowCashbackForExpUsersAndRetrievePromoParams() {
        List<String> uiPromoFlags = Arrays.asList(UI_FLAG_1, UI_FLAG_2, UI_FLAG_3);
        Map<PromoParameterName, Object> paramsMap = Map.of(PromoParameterName.UI_PROMO_FLAGS, String.join(",",
         uiPromoFlags));
        final Promo cashbackPromo = promoManager.createCashbackPromoWithParams(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN), paramsMap);

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_REARR, "yandex_cashback_enabled=1");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 100, 1)
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                ),
                headers("X-Market-Rearrfactors", "yandex_cashback_enabled=1")
        );
        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        allowedCashback(
                                                null,
                                                BigDecimal.valueOf(99)
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                null,
                                                BigDecimal.ZERO
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                BigDecimal.valueOf(10),
                                                null
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        allowedWithUiFlags(UI_FLAG_1,
                                                                                         UI_FLAG_2, UI_FLAG_3),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                        ),
                                        hasProperty(
                                                "delivery",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(DeliveryType.PICKUP)
                                                )
                                        )
                                ),
                                allOf(
                                        allowedCashback(
                                                BigDecimal.valueOf(10),
                                                null
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        null
                                                                ),
                                                                hasProperty(
                                                                        "items",
                                                                        contains(
                                                                                cashback(
                                                                                        allowedWithUiFlags(UI_FLAG_1,
                                                                                         UI_FLAG_2, UI_FLAG_3),
                                                                                        null
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                cashback(allowedEmitCashback(BigDecimal.TEN), null)
                        )
                )
        );

    }

    @NotNull
    private static ItemCashbackRequest createItemRequestDefaultItem(ItemPropertiesForTest item, int index) {
        return new ItemCashbackRequest(
                "testOfferId" + index,
                100L,
                item.getPrice(),
                item.getQuantity(),
                false,
                BigDecimal.ZERO,
                FIRST_CHILD_CATEGORY_ID,
                null,
                item.getSku(),
                100L,
                MARKET_WAREHOUSE_ID,
                false,
                100L,
                "shopId",
                "bundleId",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @NotNull
    private static ItemCashbackRequest createItemRequestDefaultItemWherePriceWithDeltaEqualsDiscount(ItemPropertiesForTest item, int index) {
        return new ItemCashbackRequest(
                "testOfferId" + index,
                100L,
                item.getPrice(),
                item.getQuantity(),
                false,
                item.getPrice().subtract(BigDecimal.valueOf(0.1)),
                FIRST_CHILD_CATEGORY_ID,
                null,
                item.getSku(),
                100L,
                MARKET_WAREHOUSE_ID,
                false,
                100L,
                "shopId",
                "bundleId",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    public void shouldRestrictedCashbackIfItemQuantityZero() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_REARR,
        "yandex_cashback_any_user_enabled=1");
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_ENABLED, "true");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(new ItemCashbackRequest(
                                                "testOfferId",
                                                100L,
                                                BigDecimal.valueOf(100),
                                                BigDecimal.ZERO,
                                                false,
                                                BigDecimal.valueOf(150),
                                                FIRST_CHILD_CATEGORY_ID,
                                                null,
                                                null,
                                                100L,
                                                MARKET_WAREHOUSE_ID,
                                                false,
                                                100L,
                                                "shopId",
                                                "bundleId",
                                                new CashbackRequest(cashbackPromo.getPromoKey(), 1),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null
                                        )
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                ),
                headers("X-Market-Rearrfactors", "yandex_cashback_any_user_enabled=1")
        );
        assertThat(cashbackOptionsResponse, hasProperty(
                "cashbackOptionsProfiles",
                containsInAnyOrder(
                        cashback(null, restrictedSpendCashback(CASHBACK_DISABLED)),
                        cashback(null, restrictedSpendCashback(CASHBACK_DISABLED)),
                        allOf(
                                allowedCashback(BigDecimal.valueOf(0), null),
                                hasProperty(
                                        "orders",
                                        contains(
                                                allOf(
                                                        allowedCashback(
                                                                BigDecimal.valueOf(0),
                                                                null
                                                        ),
                                                        hasProperty(
                                                                "items",
                                                                contains(
                                                                        cashback(
                                                                                restrictedEmitCashback(INCOMPLETE_REQUEST),
                                                                                null
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        allOf(
                                hasProperty(
                                        "cashbackTypes",
                                        contains(CashbackType.EMIT)
                                ),
                                hasProperty(
                                        "cashbackOptionsPreconditions",
                                        containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                ),
                                hasProperty(
                                        "delivery",
                                        hasProperty(
                                                "types",
                                                containsInAnyOrder(DeliveryType.PICKUP)
                                        )
                                )
                        ),
                        allOf(
                                allowedCashback(BigDecimal.valueOf(0), null),
                                hasProperty(
                                        "orders",
                                        contains(
                                                allOf(
                                                        allowedCashback(
                                                                BigDecimal.valueOf(0),
                                                                null
                                                        ),
                                                        hasProperty(
                                                                "items",
                                                                contains(
                                                                        cashback(
                                                                                restrictedEmitCashback(INCOMPLETE_REQUEST),
                                                                                null
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        cashback(allowedEmitCashback(BigDecimal.ZERO), null)
                )
                )
        );
    }

    @Test
    @Ignore
    public void shouldAllowCashbackIfHaveExcludedCategoryBotAllowedSpecs() {
        Promo pharmaCashback = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_PHARMA_CASHBACK)
                        .addPromoRule(CATEGORY_FILTER_RULE, CATEGORY_ID, PHARMA_CATEGORY_IDS)
                        .setPriority(0)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_PHARMA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_PHARMA_CASHBACK_PROMO_KEY,
         pharmaCashback.getPromoKey());


        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(new ItemCashbackRequest(
                                                "testOfferId",
                                                100L,
                                                BigDecimal.valueOf(100),
                                                BigDecimal.ONE,
                                                false,
                                                BigDecimal.valueOf(0),
                                                PHARMA_CATEGORY_IDS.iterator().next(),
                                                null,
                                                null,
                                                100L,
                                                MARKET_WAREHOUSE_ID,
                                                false,
                                                100L,
                                                "shopId",
                                                "bundleId",
                                                new CashbackRequest(pharmaCashback.getPromoKey(), 1),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                PHARMA_ALLOWED_SPEC,
                                                null,
                                                null,
                                                null
                                        )
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );
        assertThat(cashbackOptionsResponse, hasProperty(
                "cashbackOptionsProfiles",
                containsInAnyOrder(
                        cashback(null, allowedSpendCashback(BigDecimal.ZERO)),
                        cashback(null, allowedSpendCashback(BigDecimal.ZERO)),
                        allOf(
                                allowedCashback(BigDecimal.valueOf(10), null),
                                hasProperty(
                                        "orders",
                                        contains(
                                                allOf(
                                                        allowedCashback(
                                                                BigDecimal.valueOf(10),
                                                                null
                                                        ),
                                                        hasProperty(
                                                                "items",
                                                                contains(
                                                                        cashback(
                                                                                allowedEmitCashback(BigDecimal.valueOf(10)),
                                                                                null
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                )
        );
    }


    @Test
    public void shouldRestrictedCashbackIfPayedByPlus() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_REARR,
        "yandex_cashback_any_user_enabled=1");
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_ENABLED, "true");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);
        ItemCashbackRequest itemCashbackRequest = new ItemCashbackRequest(
                "testOfferId",
                100L,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                false,
                BigDecimal.valueOf(150),
                FIRST_CHILD_CATEGORY_ID,
                null,
                null,
                100L,
                MARKET_WAREHOUSE_ID,
                false,
                100L,
                "shopId",
                "bundleId",
                new CashbackRequest(cashbackPromo.getPromoKey(), 1),
                null,
                null,
                null,
                null,
                null,
                null,
                100,
                null,
                null
        );
        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(itemCashbackRequest),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                ),
                headers("X-Market-Rearrfactors", "yandex_cashback_any_user_enabled=1")
        );
        assertThat(cashbackOptionsResponse, hasProperty(
                "cashbackOptionsProfiles",
                containsInAnyOrder(
                        cashback(null, restrictedSpendCashback(CASHBACK_DISABLED)),
                        cashback(null, restrictedSpendCashback(CASHBACK_DISABLED)),
                        allOf(
                                cashback(
                                        restrictedEmitCashback(PAYED_BY_PLUS),
                                        null
                                ),
                                hasProperty(
                                        "orders",
                                        contains(
                                                allOf(
                                                        cashback(
                                                                restrictedEmitCashback(PAYED_BY_PLUS),
                                                                null
                                                        ),
                                                        hasProperty(
                                                                "items",
                                                                contains(
                                                                        cashback(
                                                                                restrictedEmitCashback(PAYED_BY_PLUS),
                                                                                null
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        allOf(
                                hasProperty(
                                        "cashbackTypes",
                                        contains(CashbackType.EMIT)
                                ),
                                hasProperty(
                                        "cashbackOptionsPreconditions",
                                        containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                ),
                                hasProperty(
                                        "delivery",
                                        hasProperty(
                                                "types",
                                                containsInAnyOrder(DeliveryType.PICKUP)
                                        )
                                )
                        ),
                        allOf(
                                cashback(
                                        restrictedEmitCashback(PAYED_BY_PLUS),
                                        null
                                ),
                                hasProperty(
                                        "orders",
                                        contains(
                                                allOf(
                                                        cashback(
                                                                restrictedEmitCashback(PAYED_BY_PLUS),
                                                                null
                                                        ),
                                                        hasProperty(
                                                                "items",
                                                                contains(
                                                                        cashback(
                                                                                restrictedEmitCashback(PAYED_BY_PLUS),
                                                                                null
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        cashback(restrictedEmitCashback(PAYED_BY_PLUS), null)
                )
                )
        );
    }

    @Test
    public void shouldRestrictByExpressDelivery() {
        CashbackPromoBuilder cashbackPromoBuilder = PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                .addCashbackRule(RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE,
                        RuleParameterName.SUPPLIER_FLAG_RESTRICTION_TYPE,
                        SupplierFlagRestrictionType.EVERYTHING_EXCEPT_EXPRESS);

        final Promo cashbackPromo = promoManager.createCashbackPromo(cashbackPromoBuilder);

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_REARR, "yandex_cashback_any_user_enabled=1");
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_ENABLED, "true");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        ItemCashbackRequest itemCashbackRequest = new ItemCashbackRequest(
                "testOfferId",
                100L,
                BigDecimal.valueOf(1000),
                BigDecimal.ONE,
                false,
                BigDecimal.valueOf(150),
                FIRST_CHILD_CATEGORY_ID,
                null,
                null,
                100L,
                MARKET_WAREHOUSE_ID,
                false,
                100L,
                "shopId",
                "bundleId",
                new CashbackRequest(cashbackPromo.getPromoKey(), 1),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Boolean.TRUE,
                null
        );
        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(itemCashbackRequest),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                ),
                headers("X-Market-Rearrfactors", "yandex_cashback_any_user_enabled=1")
        );
        assertThat(cashbackOptionsResponse, hasProperty(
                "cashbackOptionsProfiles",
                containsInAnyOrder(
                        cashback(null, allowedSpendCashback(BigDecimal.valueOf(849L))),
                        cashback(null, allowedSpendCashback(BigDecimal.ZERO)),
                        allOf(
                                cashback(
                                        allowedEmitCashback(BigDecimal.ZERO),
                                        null
                                ),
                                hasProperty(
                                        "orders",
                                        contains(
                                                allOf(
                                                        cashback(
                                                                allowedEmitCashback(BigDecimal.ZERO),
                                                                null
                                                        ),
                                                        hasProperty(
                                                                "items",
                                                                contains(
                                                                        cashback(
                                                                                restrictedEmitCashback(NO_SUITABLE_PROMO),
                                                                                null
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        allOf(
                                hasProperty(
                                        "cashbackTypes",
                                        contains(CashbackType.EMIT)
                                ),
                                hasProperty(
                                        "cashbackOptionsPreconditions",
                                        containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                ),
                                hasProperty(
                                        "delivery",
                                        hasProperty(
                                                "types",
                                                containsInAnyOrder(DeliveryType.PICKUP)
                                        )
                                )
                        ),
                        allOf(
                                cashback(
                                        allowedEmitCashback(BigDecimal.ZERO),
                                        null
                                ),
                                hasProperty(
                                        "orders",
                                        contains(
                                                allOf(
                                                        cashback(
                                                                allowedEmitCashback(BigDecimal.ZERO),
                                                                null
                                                        ),
                                                        hasProperty(
                                                                "items",
                                                                contains(
                                                                        cashback(
                                                                                restrictedEmitCashback(NO_SUITABLE_PROMO),
                                                                                null
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        cashback(allowedEmitCashback(BigDecimal.ZERO), null)
                )
                )
        );
    }


    @Test
    public void shouldCorrectlyReturnNewMockedCashbackProfile() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.CASHBACK_OPTIONS_FOURTH_PROFILE_ENABLE, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 100, 1)
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );
        assertThat(cashbackOptionsResponse.getCashbackOptionsProfiles().size(), equalTo(LAST_OPTIONS_PROFILE_INDEX + 2));
        final CashbackProfileResponse actual = cashbackOptionsResponse.getCashbackOptionsProfiles().get(LAST_OPTIONS_PROFILE_INDEX + 1);
        assertEquals("special-pickup-promo", actual
                .getOrders().get(0).getCashback().getEmit().getPromos().get(0).getUiPromoFlags().get(0));
        assertEquals(new BigDecimal(5), actual
                .getOrders().get(0).getCashback().getEmit().getPromos().get(0).getMarketTariff());
        assertEquals(new BigDecimal(5), actual
                .getOrders().get(0).getCashback().getEmit().getPromos().get(0).getPartnerTariff());
        assertEquals(DEFAULT_CART_ID, actual.getOrders().get(0).getCartId());
        assertEquals(List.of(DeliveryType.PICKUP), actual.getDelivery().getTypes());
        assertEquals(Set.of(CashbackType.EMIT), actual.getCashbackTypes());
        assertEquals(List.of(CashbackOptionsPrecondition.DELIVERY), actual.getCashbackOptionsPreconditions());
    }

    @Test
    public void shouldReturnProfileForMarketBrandedPickup() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                        .addCashbackRule(MARKET_BRANDED_PICKUP_FILTER_RULE, MARKET_BRANDED_PICKUP, true)
                        .setUiPromoFlags(List.of("special-pickup-promo"))

        );
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 3600, 1)
                                ),
                                true
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(CashbackOptionsPrecondition.DELIVERY)
                                        ),
                                        hasProperty(
                                                "delivery",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(DeliveryType.PICKUP)
                                                )
                                        ),
                                        hasProperty(
                                                "cashback",
                                                allOf(
                                                        hasProperty(
                                                                "emit",
                                                                allOf(
                                                                        hasProperty(
                                                                                "amount",
                                                                                equalTo(BigDecimal.valueOf(396))
                                                                        )
                                                                )
                                                        )
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                hasProperty(
                                                                        "cashback",
                                                                        allOf(
                                                                                hasProperty(
                                                                                        "emit",
                                                                                        allOf(
                                                                                                hasProperty(
                                                                                                        "amount",
                                                                                                        equalTo(BigDecimal.valueOf(396))
                                                                                                ),
                                                                                                hasProperty(
                                                                                                        "promos",
                                                                                                        contains(
                                                                                                                allOf(
                                                                                                                        hasProperty(
                                                                                                                                "amount",
                                                                                                                                equalTo(BigDecimal.valueOf(360))
                                                                                                                        ),
                                                                                                                        hasProperty(
                                                                                                                                "uiPromoFlags",
                                                                                                                                containsInAnyOrder("special-pickup-promo")
                                                                                                                        )
                                                                                                                )
                                                                                                        )
                                                                                                ),
                                                                                                hasProperty(
                                                                                                        "type",
                                                                                                        equalTo(CashbackPermision.ALLOWED)
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldReturnNoEmitCashbackForPaymentSystemIfBnplSelected() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE, CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, PaymentSystem.MASTERCARD)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        final CashbackOptionsResponse cashbackOptionsResponse = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                DEFAULT_REGION_ID,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                DEFAULT_CART_ID,
                                DEFAULT_ORDER_ID,
                                null,
                                Collections.singletonList(
                                        createItemRequest(cashbackPromo, 3600, 1)
                                ),
                                true
                        )),
                        UsageClientDeviceType.DESKTOP,
                        true
                )
        );

        assertThat(
                cashbackOptionsResponse,
                hasProperty(
                        "cashbackOptionsProfiles",
                        containsInAnyOrder(
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.SPEND)
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        ),
                                        hasProperty(
                                                "cashbackOptionsPreconditions",
                                                containsInAnyOrder(PAYMENT)
                                        ),
                                        hasProperty(
                                                "payment",
                                                hasProperty(
                                                        "types",
                                                        containsInAnyOrder(BANK_CARD)
                                                )
                                        ),
                                        hasProperty(
                                                "cashback",
                                                allOf(
                                                        hasProperty(
                                                                "emit",
                                                                allOf(
                                                                        hasProperty(
                                                                                "amount",
                                                                                equalTo(BigDecimal.ZERO)
                                                                        )
                                                                )
                                                        )
                                                )
                                        ),
                                        hasProperty(
                                                "orders",
                                                contains(
                                                        allOf(
                                                                hasProperty(
                                                                        "cashback",
                                                                        allOf(
                                                                                hasProperty(
                                                                                        "emit",
                                                                                        allOf(
                                                                                                hasProperty(
                                                                                                        "amount",
                                                                                                        equalTo(BigDecimal.ZERO)
                                                                                                ),
                                                                                                hasProperty(
                                                                                                        "type",
                                                                                                        equalTo(CashbackPermision.ALLOWED)
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                allOf(
                                        hasProperty(
                                                "cashbackTypes",
                                                contains(CashbackType.EMIT)
                                        )
                                ),
                                cashback(allowedEmitCashback(BigDecimal.ZERO), null)
                        )
                )
        );
    }

    @Test
    public void shouldAllowEmitExternalWhenPromoActive() {
        // suspect concurrent promo (MASTERCARD)
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(5)
                        .setPriority(100)
                        .setPromoBucketName("payment-system")
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.ONE)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(2000))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_PLUS)
                        .addCashbackRule(ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE, CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, PaymentSystem.MASTERCARD)
        );
        final var promo = promoManager.createExternalCashbackPromo(
                PromoUtils.ExternalCashback.defaultBank()
                        .setPriority(-100)
                        .setPromoBucketName("bank")
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_BANK_CASHBACK)
                        .addCashbackRule(MAX_ORDER_TOTAL_CUTTING_RULE, MAX_ORDER_TOTAL, BigDecimal.valueOf(15000))
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.ONE)
                        .addCashbackRule(PAYMENT_FEATURES_CUTTING_RULE, PAYMENT_FEATURE, YA_BANK)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE)
        );
        reloadPromoCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.enable(YANDEX_BANK_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_BANK_CASHBACK_PROMO_KEY, promo.getPromoKey());
        configurationService.set(ConfigurationService.YANDEX_BANK_CASHBACK_WITH_PLUS_ONLY, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        bankTestUtils.mockCalculatorWithDefaultResponse();

        final var request = new CashbackOptionsRequest(
                List.of(new OrderCashbackRequest(
                        DEFAULT_CART_ID, DEFAULT_ORDER_ID, MarketPlatform.BLUE,
                        List.of(createItemRequest(promo, 300, 1, Set.of(GOOGLE_PAY, YANDEX, APPLE_PAY))),
                        false)),
                UsageClientDeviceType.APPLICATION,
                false
        );
        CashbackOptionsResponse response = marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID, DEFAULT_REGION_ID, request
        );
        assertThat(response.getCashbackOptionsProfiles().get(LAST_OPTIONS_PROFILE_INDEX).getCashback().getEmit().getPromos(),
                contains(
                        allOf(
                                hasProperty("amount", equalTo(BigDecimal.valueOf(30))),
                                hasProperty("promoKey", equalTo(promo.getPromoKey()))
                        )
                )
        );

        promoService.updateStatus(promo, PromoStatus.INACTIVE);
        reloadPromoCache();
        response = marketLoyaltyClient.calculateCashbackProfiles(DEFAULT_UID, DEFAULT_REGION_ID, request);
        assertThat(response.getCashbackOptionsProfiles().get(LAST_OPTIONS_PROFILE_INDEX).getCashback().getEmit().getPromos(),
                empty()
        );
    }


    private static HttpHeaders headers(String key, String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(key, value);
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
