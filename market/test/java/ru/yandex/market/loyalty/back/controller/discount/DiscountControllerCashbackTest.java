package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryFeature;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.back.util.CashbackMatchers;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.promo.TechnicalBudgetMode;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoStatusWithBudgetCacheService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.cashback.BucketId;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.discount.constants.DeliveryPartnerType;
import ru.yandex.market.loyalty.core.service.discount.constants.SupplierFlagRestrictionType;
import ru.yandex.market.loyalty.core.service.personalpromo.PersonalPromoService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;
import ru.yandex.market.sdk.userinfo.service.UidConstants;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.INCOMPLETE_REQUEST;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NOT_YA_PLUS_SUBSCRIBER;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NO_SUITABLE_PROMO;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.PAYED_BY_PLUS;
import static ru.yandex.market.loyalty.api.model.PromoType.CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.WELCOME_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_EXTRA_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_EXTRA_PHARMA_CASHBACK;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedEmitCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedSpendCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.cashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.failCashbackPromo;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.notYandexPlusSubscriberCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.restrictedCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.restrictedEmitCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.restrictedSpendCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.successCashbackPromo;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_EMIT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_ERROR;
import static ru.yandex.market.loyalty.core.model.promo.CashbackLevelType.MULTI_ORDER;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_PLATFORM;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.DELIVERY_PARTNER_TYPE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.FAKE_PENDING;
import static ru.yandex.market.loyalty.core.rule.RuleType.CLIENT_PLATFORM_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.DELIVERY_PARTNER_TYPE_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.cashback.CashbackService.DYSON_VENDOR_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PHARMA_ROOT_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withFeatures;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_MSKU;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_MSKU;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_MULTI_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.EXCLUDED_SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.SUPPLIER_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.atSupplierWarehouse;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.cashback;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.discount;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.dropship;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.loyaltyProgramPartner;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.payByYaPlus;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.vendor;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.defaultPercent;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_CODE;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerCashbackTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PersonalPromoService personalPromoService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    private MetaTransactionDao metaTransactionDao;

    @Before
    public void init() {
        configurationService.enable(ConfigurationService.MIN_PAY_SUM_ENABLED);
    }

    @Test
    public void shouldCalcNoPromoCashback() {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        CashbackMatchers.noPromoEmitCashback(),
                                                                        CashbackMatchers.allowedSpendCashback(BigDecimal.valueOf(99))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is(NO_SUITABLE_PROMO.getCode())),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
    }


    @Test
    public void shouldCalcNotSuitableCategoryCashback() {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 vendor(DYSON_VENDOR_ID))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.ZERO
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        CashbackMatchers.noPromoEmitCashback(),
                                                                        CashbackMatchers.notSuitableCategoryTypeSpendCashback()
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is(NO_SUITABLE_PROMO.getCode())),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
    }

    @Test
    public void shouldCalcNotYandexPlusCashback() {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        notYandexPlusSubscriberCashback(),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                notYandexPlusSubscriberCashback(),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                notYandexPlusSubscriberCashback()
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is(NOT_YA_PLUS_SUBSCRIBER.getCode())),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
    }

    @Test
    public void shouldAllowCashbackOnSpend() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withCashbackOptionType(CashbackType.EMIT)
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.TEN)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void testCashbackOnSpendWithSBP() {
        final var expectedEmitAmount = BigDecimal.valueOf(25);
        final var expectedSpendAmount = BigDecimal.valueOf(470);
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                        quantity(1), cashback(cashbackPromo.getPromoKey(), 1))
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(ANOTHER_ITEM_KEY), price(200),
                                        quantity(2), cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.SBP)
                                .build())
                        .withCashbackOptionType(CashbackType.SPEND)
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                expectedEmitAmount,
                                expectedSpendAmount
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        expectedEmitAmount,
                                                        expectedSpendAmount
                                                ),
                                                hasProperty(
                                                        "items",
                                                        containsInAnyOrder(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(5),
                                                                        BigDecimal.valueOf(90)
                                                                ),
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(20),
                                                                        BigDecimal.valueOf(380)
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
    public void testCashbackOnSpendWithNullPaymentType() {
        final var expectedEmitAmount = BigDecimal.valueOf(25);
        final var expectedSpendAmount = BigDecimal.ZERO;
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                        quantity(1), cashback(cashbackPromo.getPromoKey(), 1))
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(ANOTHER_ITEM_KEY), price(200),
                                        quantity(2), cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(null)
                                .build())
                        .withCashbackOptionType(CashbackType.SPEND)
                        .build()
        );

        assertThat(
                discountResponse,
                allowedCashback(
                        expectedEmitAmount,
                        expectedSpendAmount
                )
        );
    }

    @Test
    public void shouldReturnAnaplanIdInCashbackPromo() {
        String anaplanId = "#1077";
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .setAnaplanId(anaplanId)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                        cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withCashbackOptionType(CashbackType.EMIT)
                        .build()
        );

        assertThat(
                discountResponse,
                hasProperty(
                        "orders",
                        everyItem(
                                hasProperty(
                                        "items",
                                        everyItem(
                                                hasProperty(
                                                        "cashback",
                                                        hasProperty(
                                                                "emit",
                                                                hasProperty(
                                                                        "promos",
                                                                        everyItem(
                                                                                hasProperty("anaplanId",
                                                                                 equalTo(anaplanId))
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
    public void shouldSendDiscountEventWithNewFields() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.disable(ConfigurationService.CASHBACK_PROMOS_FROM_REPORT_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withCashbackOptionType(CashbackType.EMIT)
                        .build()
        );

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.TEN)),
                hasProperty("cashbackPartnerAmount", is(nullValue())),
                hasProperty("cashbackMarketAmount", is(nullValue())),
                hasProperty("partnerId", is(nullValue())),
                hasProperty("promoBucketName", equalTo(BucketId.DEFAULT.getId())),
                hasProperty("cashbackOptionType", equalTo(CashbackType.EMIT)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldRestrictSpendCashbackOnSpendWithNoPaymentType() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountRequest build = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                cashback(cashbackPromo.getPromoKey(), 1))
//                        .withPaymentType(PaymentType.BANK_CARD)
                        .build())
                .build();
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                build
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                allowedEmitCashback(BigDecimal.TEN),
                                allowedSpendCashback(BigDecimal.ZERO)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                cashback(
                                                        allowedEmitCashback(BigDecimal.TEN),
                                                        restrictedSpendCashback(INCOMPLETE_REQUEST)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        allowedEmitCashback(BigDecimal.TEN),
                                                                        restrictedSpendCashback(INCOMPLETE_REQUEST)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.TEN)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }


    @Test
    public void shouldAllowCashbackOnSpendWithNoPaymentTypeWithPayedByYaPlus() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountRequest build = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                cashback(cashbackPromo.getPromoKey(), 1), payByYaPlus((100)))
//                        .withPaymentType(PaymentType.BANK_CARD)
                        .build())
                .build();
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                build
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                restrictedEmitCashback(PAYED_BY_PLUS),
                                allowedSpendCashback(BigDecimal.valueOf(99))
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                cashback(
                                                        restrictedEmitCashback(PAYED_BY_PLUS),
                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(PAYED_BY_PLUS),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is(PAYED_BY_PLUS.getCode())),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
    }

    @Test
    public void shouldAllowCashbackForSingleItem() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.TEN)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldAllowCashbackForFixedPromo() {
        Promo cashbackPromo =
         promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(100)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }


    @Test
    public void shouldAllowCashbackForPercentMultiOrderLevelPromo() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10),
         MULTI_ORDER));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(ANOTHER_ITEM_KEY), price(400))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(50),
                                BigDecimal.valueOf(498)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(498)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                                ),
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(399))
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
    public void shouldAllowCashbackForPercentMultiOrderLevelPromoWithDiscount() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10),
         MULTI_ORDER));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse().setCouponValue(BigDecimal.valueOf(100),
         CoreCouponValueType.FIXED));


        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(ANOTHER_ITEM_KEY), price(400))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withCoupon(DEFAULT_COUPON_CODE)
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(40),
                                BigDecimal.valueOf(398)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(398)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(79))
                                                                ),
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(319))
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
    public void shouldNotAllowCashbackIfAllItemsMatchForFixedPromoWithMinOrderTotal() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is(NO_SUITABLE_PROMO.getCode())),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
    }

    @Test
    public void shouldNotAllowCashbackIfClientDeviceNotMatchForFixedPromoWithClientDeviceRestriction() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, UsageClientDeviceType.APPLICATION));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(
                                OperationContextFactory.withUidBuilder(DEFAULT_UID)
                                        .withClientDevice(UsageClientDeviceType.DESKTOP)
                                        .buildOperationContext()
                        )
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is(NO_SUITABLE_PROMO.getCode())),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
    }

    @Test
    public void shouldAllowCashbackIfClientDeviceMatchForFixedPromoWithClientDeviceRestriction() {
        Promo cashbackPromo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, UsageClientDeviceType.APPLICATION));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(
                                OperationContextFactory.withUidBuilder(DEFAULT_UID)
                                        .withClientDevice(UsageClientDeviceType.APPLICATION)
                                        .buildOperationContext()
                        )
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.APPLICATION))
        )));
    }

    @Test
    public void shouldAllowCashbackIfWelcomeCashbackPerkRequired() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000))
                .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, WELCOME_CASHBACK)
                .setEmissionBudget(BigDecimal.valueOf(1000))
        );

        cashbackCacheService.reloadCashbackPromos();
        promoStatusWithBudgetCacheService.reloadCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_FORCE_SWITCH, true);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY,
         cashbackPromo.getPromoKey());
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
         cashbackPromo.getStartDate().toInstant().toString());
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_CASHBACK_AMOUNT, 100);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_THRESHOLD, 3000);
        configurationService.set(ConfigurationService.MARKET_PROMO_ADVERTISING_CAMPAIGN_500_REARR,
        "market_promo_advertising_campaign_500=1");


        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(10000))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(9999)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(9999)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(9999))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        //TODO: тест не отлавливает нужные события. Они по факту отправляются, но не отлавливаются таким способом
//        verify(logBrokerClient).pushEvent(argThat(allOf(
//                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
//                hasProperty("httpMethod", is("calc")),
//                hasProperty("eventType", is(CASHBACK_EMIT)),
//                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100))),
//                hasProperty("uid", is(100L)),
//                hasProperty("email", is(DEFAULT_EMAIL)),
//                hasProperty("promoType", is(CASHBACK)),
//                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
//                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
//        )));
    }

    @Test
    public void shouldNotAllowCashbackIfPriceLessThanThresholdAfterDiscount() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(1000))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(5000))
                .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, WELCOME_CASHBACK)
                .setEmissionBudget(BigDecimal.valueOf(1000))
        );
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("1")
                .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
        );

        cashbackCacheService.reloadCashbackPromos();
        promoStatusWithBudgetCacheService.reloadCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_FORCE_SWITCH, true);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY,
         cashbackPromo.getPromoKey());
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
         cashbackPromo.getStartDate().toInstant().toString());
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_CASHBACK_AMOUNT, 1000);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_THRESHOLD, 5000);
        configurationService.set(ConfigurationService.MARKET_PROMO_ADVERTISING_CAMPAIGN_500_REARR,
        "market_promo_advertising_campaign_500=1");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(1000)
                                , quantity(5))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withCoupon("1")
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(4495)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(4495)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(4495))
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
    public void shouldNotAllowCashbackIfAllItemsMatchForPercentPromoWithMinOrderTotal() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.valueOf(10))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
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
    public void shouldNotAllowCashbackIfSomeItemsNotMatchForFixedPromoWithMinOrderTotal() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000))
                .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, Set.of(DEFAULT_MSKU)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY),
                                 msku(DEFAULT_MSKU), price(100))
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(ANOTHER_ITEM_KEY),
                                 msku(ANOTHER_MSKU), price(3000))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(3098)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(3098)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                                ),
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(2999))
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
    public void shouldNotAllowCashbackIfSomeItemsNotMatchForPercentPromoWithMinOrderTotal() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.valueOf(10))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000))
                .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, Set.of(DEFAULT_MSKU)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY),
                                 msku(DEFAULT_MSKU), price(100))
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(ANOTHER_ITEM_KEY),
                                 msku(ANOTHER_MSKU), price(3000))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(3098)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(3098)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                                ),
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(2999))
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
    public void shouldAllowSpendForFixedPromo() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .setEmissionBudget(BigDecimal.valueOf(1000))
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(1000))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                allowedEmitCashback(
                                        BigDecimal.valueOf(100),
                                        successCashbackPromo(BigDecimal.valueOf(100), cashbackPromo.getPromoKey())
                                ),
                                allowedSpendCashback(BigDecimal.valueOf(999))
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(999)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(999))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        final List<YandexWalletTransaction> transactions = yandexWalletTransactionDao.query(
                YandexWalletTransactionStatus.PENDING, 10);

        assertThat(transactions, hasSize(1));
        assertEquals(transactions.get(0).getUniqueKey(),
                cashbackPromo.getPromoParam(PromoParameterName.CAMPAIGN_NAME).orElse("") +
                        DEFAULT_ORDER_ID + "_" + cashbackPromo.getPromoId().getId());
        assertEquals(transactions.get(0).getStatus(), YandexWalletTransactionStatus.PENDING);
        assertEquals(transactions.get(0).getEmissionBudgetAccId(), cashbackPromo.getBudgetEmissionAccountId());
        assertEquals(transactions.get(0).getEmissionSpendingAccId(), cashbackPromo.getSpendingEmissionAccountId());
        assertEquals(transactions.get(0).getPromoId(), Long.valueOf(cashbackPromo.getPromoId().getId()));
        assertThat(transactions.get(0).getPayload(), is(notNullValue(String.class)));
        assertNotNull(transactions.get(0).getEmissionTransactionId());

        assertThat(
                budgetService.getAccount(cashbackPromo.getBudgetEmissionAccountId()).getBalance(),
                comparesEqualTo(BigDecimal.valueOf(900))
        );
    }

    @Test
    public void shouldAllowSpendIfFixedPromoBudgetExceeded() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .setEmissionBudget(BigDecimal.valueOf(0))
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(1000))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                allowedEmitCashback(
                                        BigDecimal.valueOf(100),
                                        failCashbackPromo(BigDecimal.valueOf(100), cashbackPromo.getPromoKey())
                                ),
                                allowedSpendCashback(BigDecimal.valueOf(999))
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(999)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(999))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        final List<YandexWalletTransaction> transactions = yandexWalletTransactionDao.query(
                YandexWalletTransactionStatus.PENDING, 10);
        assertThat(transactions, hasSize(1));
        assertEquals(transactions.get(0).getUniqueKey(),
                cashbackPromo.getPromoParam(PromoParameterName.CAMPAIGN_NAME).orElse("") +
                        DEFAULT_ORDER_ID + "_" + cashbackPromo.getPromoId().getId());
        assertEquals(transactions.get(0).getStatus(), YandexWalletTransactionStatus.PENDING);
        assertEquals(transactions.get(0).getEmissionBudgetAccId(), cashbackPromo.getBudgetEmissionAccountId());
        assertEquals(transactions.get(0).getEmissionSpendingAccId(), cashbackPromo.getSpendingEmissionAccountId());
        assertEquals(transactions.get(0).getPromoId(), Long.valueOf(cashbackPromo.getPromoId().getId()));
        assertNull(transactions.get(0).getEmissionTransactionId());


        assertThat(
                budgetService.getAccount(cashbackPromo.getBudgetEmissionAccountId()).getBalance(),
                comparesEqualTo(BigDecimal.valueOf(0))
        );
    }

    @Test
    public void shouldSpendCashbackPromoBudgetPositiveBalance() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(10).setEmissionBudget(10000));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(1000))
                                .withOrderId(DEFAULT_ORDER_ID)
                                .build())
                        .build()
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(9900))
                )
        );

        assertThat(yandexWalletTransactionDao.findAllByOrderId(Long.parseLong(DEFAULT_ORDER_ID)),
                contains(
                        allOf(
                                hasProperty("emissionTransactionId", notNullValue()),
                                hasProperty("status", equalTo(FAKE_PENDING)),
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100))),
                                hasProperty("orderId", equalTo(Long.parseLong(DEFAULT_ORDER_ID))),
                                hasProperty("multiOrderId", nullValue())
                        )));
    }

    @Test
    public void shouldSpendCashbackPromoBudgetNegativeBalance() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(10).setEmissionBudget(50));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(1000))
                                .withOrderId(DEFAULT_ORDER_ID)
                                .build())
                        .build()
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(-50))
                )
        );


        assertThat(yandexWalletTransactionDao.findAllByOrderId(Long.parseLong(DEFAULT_ORDER_ID)),
                contains(
                        allOf(
                                hasProperty("emissionTransactionId", notNullValue()),
                                hasProperty("status", equalTo(FAKE_PENDING)),
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100))),
                                hasProperty("orderId", equalTo(Long.parseLong(DEFAULT_ORDER_ID))),
                                hasProperty("multiOrderId", nullValue())
                        )));
    }

    @Test
    public void shouldSpendCashbackPromoBudgetInitiallyNegativeBalance() {
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

        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(1000))
                                .withOrderId(DEFAULT_ORDER_ID)
                                .build())
                        .build()
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(-101))
                )
        );


        assertThat(yandexWalletTransactionDao.findAllByOrderId(Long.parseLong(DEFAULT_ORDER_ID)),
                contains(
                        allOf(
                                hasProperty("emissionTransactionId", notNullValue()),
                                hasProperty("status", equalTo(FAKE_PENDING)),
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100))),
                                hasProperty("orderId", equalTo(Long.parseLong(DEFAULT_ORDER_ID))),
                                hasProperty("multiOrderId", nullValue())
                        )));
    }

    @Test
    public void shouldSpendCashbackPromoBudgetMultiorder() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(10).setEmissionBudget(10000));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(CheckouterUtils.DEFAULT_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(300),
                                                loyaltyProgramPartner(false)
                                        )
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(CheckouterUtils.ANOTHER_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(700),
                                                loyaltyProgramPartner(false)
                                        )
                                        .build())
                        .withMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build()
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(100);

        assertThat(
                budgetService.getAccount(promo.getBudgetEmissionAccountId()),
                hasProperty(
                        "balance",
                        comparesEqualTo(BigDecimal.valueOf(9900))
                )
        );


        assertThat(yandexWalletTransactionDao.findAllByOrderId(CheckouterUtils.DEFAULT_ORDER_ID),
                contains(
                        allOf(
                                hasProperty("emissionTransactionId", notNullValue()),
                                hasProperty("status", equalTo(FAKE_PENDING)),
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(30))),
                                hasProperty("orderId", equalTo(CheckouterUtils.DEFAULT_ORDER_ID)),
                                hasProperty("multiOrderId", nullValue())
                        )));

        assertThat(yandexWalletTransactionDao.findAllByOrderId(CheckouterUtils.ANOTHER_ORDER_ID),
                contains(
                        allOf(
                                hasProperty("emissionTransactionId", notNullValue()),
                                hasProperty("status", equalTo(FAKE_PENDING)),
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(70))),
                                hasProperty("orderId", equalTo(CheckouterUtils.ANOTHER_ORDER_ID)),
                                hasProperty("multiOrderId", nullValue())
                        )));
    }

    @Test
    public void shouldSpendCashbackPromoWithNoBudget() {
        promoManager.createCashbackPromo(defaultPercent(10));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(CheckouterUtils.DEFAULT_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(300),
                                                loyaltyProgramPartner(false)
                                        )
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(CheckouterUtils.ANOTHER_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(700),
                                                loyaltyProgramPartner(false)
                                        )
                                        .build())
                        .withMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build()
        );

        assertThat(metaTransactionDao.getTransactionsToRevert(CheckouterUtils.DEFAULT_ORDER_ID),
                empty());
        assertThat(metaTransactionDao.getTransactionsToRevert(CheckouterUtils.ANOTHER_ORDER_ID),
                empty());
        assertThat(metaTransactionDao.getTransactionsToRevert(DEFAULT_MULTI_ORDER_ID),
                empty());
    }

    @Test
    public void shouldAllowCashbackForFixedPromoWithMinOrderTotal() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(3100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(3099)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(3099)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(3099))
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
    public void shouldDrawUnmatchedThresholdForFixedCashbackPromoWithMinOrderTotal() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(2500))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                allowedEmitCashback(
                                        BigDecimal.ZERO,
                                        null,
                                        contains(
                                                allOf(
                                                        hasProperty("minMultiCartTotal",
                                                         comparesEqualTo(BigDecimal.valueOf(3000))),
                                                        hasProperty("remainingMultiCartTotal",
                                                         comparesEqualTo(BigDecimal.valueOf(500))),
                                                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100)))
                                                )
                                        )
                                ),
                                allowedSpendCashback(BigDecimal.valueOf(2499))
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(2499)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(2499))
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
    public void shouldDrawMatchedThresholdForFixedCashbackPromoWithMinOrderTotal() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(2000)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(2500))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                allowedEmitCashback(
                                        BigDecimal.valueOf(100),
                                        null,
                                        contains(
                                                allOf(
                                                        hasProperty("minMultiCartTotal",
                                                         comparesEqualTo(BigDecimal.valueOf(2000))),
                                                        hasProperty("remainingMultiCartTotal",
                                                         comparesEqualTo(BigDecimal.valueOf(0))),
                                                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100)))
                                                )
                                        )
                                ),
                                allowedSpendCashback(BigDecimal.valueOf(2499))
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(2499)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(2499))
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
    public void shouldAllowCashbackForPercentPromoWithMinOrderTotal() {
        Promo cashbackPromo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.valueOf(10))
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(3100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(310),
                                BigDecimal.valueOf(3099)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(310),
                                                        BigDecimal.valueOf(3099)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(310),
                                                                        BigDecimal.valueOf(3099)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        //TODO: тест не отлавливает нужные события. Они по факту отправляются, но не отлавливаются таким способом
//        verify(logBrokerClient).pushEvent(argThat(allOf(
//                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
//                hasProperty("httpMethod", is("calc")),
//                hasProperty("eventType", is(CASHBACK_EMIT)),
//                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(310))),
//                hasProperty("uid", is(100L)),
//                hasProperty("email", is(DEFAULT_EMAIL)),
//                hasProperty("promoType", is(CASHBACK)),
//                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
//                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
//        )));
    }

    @Test
    public void shouldRoundUpEmitCashback() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(4)));

        performCashbackCheck(cashbackPromo);
    }

    private void performCashbackCheck(Promo cashbackPromo) {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(promoKeys(cashbackPromo.getPromoKey()), warehouse(MARKET_WAREHOUSE_ID),
                                 itemKey(DEFAULT_ITEM_KEY), price(101), cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(5),
                                BigDecimal.valueOf(100)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(5),
                                                        BigDecimal.valueOf(100)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(5),
                                                                        BigDecimal.valueOf(100)
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
    public void shouldAllowEmitForDSBS() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(4))
                        .addCashbackRule(DELIVERY_PARTNER_TYPE_FILTER_RULE, DELIVERY_PARTNER_TYPE,
                         DeliveryPartnerType.SHOP)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_DSBS_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(101),
                                        dropship(true),
                                        atSupplierWarehouse(true)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPlatform(MarketPlatform.WHITE)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(5),
                                BigDecimal.valueOf(100)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(5),
                                                        BigDecimal.valueOf(100)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(5),
                                                                        BigDecimal.valueOf(100)
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
    public void shouldAllowEmitForNullMsku() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(4))
                        .addCashbackRule(DELIVERY_PARTNER_TYPE_FILTER_RULE, DELIVERY_PARTNER_TYPE,
                         DeliveryPartnerType.SHOP)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_DSBS_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        msku(null),
                                        price(101),
                                        dropship(true),
                                        atSupplierWarehouse(true)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPlatform(MarketPlatform.WHITE)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(5),
                                BigDecimal.valueOf(100)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(5),
                                                        BigDecimal.valueOf(100)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(5),
                                                                        BigDecimal.valueOf(100)
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
    public void shouldNotAllowEmitForDSBS() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(4))
                        .addCashbackRule(DELIVERY_PARTNER_TYPE_FILTER_RULE, DELIVERY_PARTNER_TYPE,
                         DeliveryPartnerType.YANDEX_MARKET)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_DSBS_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(101),
                                        dropship(true),
                                        atSupplierWarehouse(true)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPlatform(MarketPlatform.WHITE)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(0),
                                BigDecimal.valueOf(100)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(0),
                                                        BigDecimal.valueOf(100)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                hasProperty("cashback", allOf(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(100))
                                                                ))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldAllowCashbackForMultipleItems() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(ANOTHER_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(20),
                                BigDecimal.valueOf(198)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(20),
                                                        BigDecimal.valueOf(198)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
                                                                ),
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", comparesEqualTo(BigDecimal.TEN.add(BigDecimal.TEN))),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }


    @Test
    public void shouldUseExternalDiscountForCashbackCalculation() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        cashback(cashbackPromo.getPromoKey(), 1),
                                        discount("test", 50)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(5),
                                BigDecimal.valueOf(49)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(5),
                                                        BigDecimal.valueOf(49)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(5),
                                                                        BigDecimal.valueOf(49)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(5))),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldUseCoinDiscountForCashbackCalculation() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(50))
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        cashback(cashbackPromo.getPromoKey(), 1)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build()).withCoins(coinKey)
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(5),
                                BigDecimal.valueOf(49)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(5),
                                                        BigDecimal.valueOf(49)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(5),
                                                                        BigDecimal.valueOf(49)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(5))),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldUseCouponDiscountForCashbackCalculation() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("TEST")
                        .setCouponValue(BigDecimal.valueOf(50), CoreCouponValueType.FIXED)
        );


        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        cashback(cashbackPromo.getPromoKey(), 1)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build()).withCoupon("TEST")
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(5),
                                BigDecimal.valueOf(49)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(5),
                                                        BigDecimal.valueOf(49)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(5),
                                                                        BigDecimal.valueOf(49)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(5))),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Ignore("https://st.yandex-team.ru/MARKETDISCOUNT-3717")
    @Test
    public void shouldCalcNotSuitableDeliveryTypeRestrictedCashback() {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 dropship())
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.ZERO
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        CashbackMatchers.notSuitableDeliveryTypeSpendCashback(),
                                                                        CashbackMatchers.notSuitableDeliveryTypeEmitCashback()
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
    public void shouldAllowCashbackForExpUsers() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_REARR, "yandex_cashback_enabled=1");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build(),
                headers("X-Market-Rearrfactors", "yandex_cashback_enabled=1")
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void shouldNotAllowCashbackForWhitePartners() {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(atSupplierWarehouse(true), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPlatform(MarketPlatform.WHITE)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                restrictedCashback(CashbackRestrictionReason.NOT_SUITABLE_PLATFORM),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                restrictedCashback(CashbackRestrictionReason.NOT_SUITABLE_PLATFORM))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldAllowCashbackForAnyUserIfUserInExp() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_REARR,
        "yandex_cashback_any_user_enabled=1");
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_ENABLED, "true");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build(),
                headers("X-Market-Rearrfactors", "yandex_cashback_any_user_enabled=1")
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                allowedEmitCashback(BigDecimal.TEN),
                                restrictedSpendCashback(CashbackRestrictionReason.CASHBACK_DISABLED)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                cashback(
                                                        allowedEmitCashback(BigDecimal.TEN),
                                                        restrictedSpendCashback(CashbackRestrictionReason.CASHBACK_DISABLED)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        allowedEmitCashback(BigDecimal.TEN),
                                                                        restrictedSpendCashback(CashbackRestrictionReason.CASHBACK_DISABLED)
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
    public void shouldAllowCashbackForUnauthUserIfFeatureEnabled() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_FOR_UNAUTH_REARR,
        "YANDEX_CASHBACK_FOR_UNAUTH_REARR");

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)

                                .build())
                        .withOperationContext(OperationContextDto.builderDto().setMuid(1_152_921_504_606_846_976L).setUid(1_152_921_504_606_846_976L).build())
                        .build(),
                headers("X-Market-Rearrfactors", "YANDEX_CASHBACK_FOR_UNAUTH_REARR")
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                allowedEmitCashback(BigDecimal.TEN),
                                restrictedSpendCashback(CashbackRestrictionReason.CASHBACK_DISABLED)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                cashback(
                                                        allowedEmitCashback(BigDecimal.TEN),
                                                        restrictedSpendCashback(CashbackRestrictionReason.CASHBACK_DISABLED)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        allowedEmitCashback(BigDecimal.TEN),
                                                                        restrictedSpendCashback(CashbackRestrictionReason.CASHBACK_DISABLED)
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
    public void shouldNotAllowCashbackForNoExpUsers() {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_REARR, "yandex_cashback_enabled=1");

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        restrictedCashback(CashbackRestrictionReason.CASHBACK_DISABLED),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                restrictedCashback(CashbackRestrictionReason.CASHBACK_DISABLED),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                restrictedCashback(CashbackRestrictionReason.CASHBACK_DISABLED)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldNotAllowCashbackForNonFulfillmentItems() {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(atSupplierWarehouse(true), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        CashbackMatchers.noPromoEmitCashback(),
                                                                        CashbackMatchers.allowedSpendCashback(BigDecimal.valueOf(99))
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
    public void shouldAllowCashbackIfIncorrectPromoKey() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback("incorrectpromokey", 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void shouldRestrictCashbackIfPayedByYaPlus() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                cashback("incorrectpromokey", 1), payByYaPlus(100))
                        .withPaymentType(PaymentType.BANK_CARD)
                        .build())
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                request
        );

        assertThat(
                discountResponse,
                allOf(
                        cashback(
                                restrictedEmitCashback(PAYED_BY_PLUS),
                                allowedSpendCashback(BigDecimal.valueOf(99))
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                cashback(
                                                        restrictedEmitCashback(PAYED_BY_PLUS),
                                                        allowedSpendCashback(BigDecimal.valueOf(99))
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(PAYED_BY_PLUS),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
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
    public void shouldRestrictCashbackForLoyaltyProgramPartnerFlagIsDisabledAndSupplierWarehouseId() {
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.postDelivery())
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        CashbackMatchers.noPromoEmitCashback(),
                                                                        CashbackMatchers.allowedSpendCashback(BigDecimal.valueOf(99))
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
    public void shouldAllowCashbackForLoyaltyProgramPartnerFlagIsEnableAndUseSupplierWarehouseId() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        cashback(cashbackPromo.getPromoKey(), 1),
                                        price(100),
                                        loyaltyProgramPartner(true)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        CashbackMatchers.allowedEmitCashback(BigDecimal.TEN),
                                                                        CashbackMatchers.allowedSpendCashback(BigDecimal.valueOf(99))
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
    public void applyCorrectCashbackByPriority() {
        Integer lowPriority = 10;
        Integer highPriority = 100;
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.withNominalAndPriority(BigDecimal.valueOf(10), highPriority));
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.withNominalAndPriority(BigDecimal.valueOf(100), lowPriority));
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(1)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(10),
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(10),
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(10),
                                                                        BigDecimal.valueOf(99)
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
    public void applyCorrectCashbackByNominal() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10)));
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(50)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void applyExtraCashbackByDeliveryPartnerType() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_CASHBACK)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        atSupplierWarehouse(true),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void shouldApplyExtraCashbackIfSeveralPromoMatch() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_CASHBACK)
                        .setPriority(0)
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5))
                        .setPriority(-1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void shouldSumCashbackFromDifferentBuckets() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_CASHBACK)
                        .setPriority(0)
                        .setPromoBucketName("test")
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5))
                        .setPriority(-1)
                        .setPromoBucketName("default")
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(15),
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(15),
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(15),
                                                                        BigDecimal.valueOf(99)
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
    public void shouldNotApplyExtraCashbackIfNoExtraCashbackPerk() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(15))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_CASHBACK)
                        .setPriority(0)
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .setPriority(-1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, false);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void shouldNotAllowSpendCashbackForExcludedSupplier() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), supplier(EXCLUDED_SUPPLIER_ID),
                                 itemKey(DEFAULT_ITEM_KEY), price(100), cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.ZERO
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(CashbackRestrictionReason.NOT_SUITABLE_CATEGORY),
                                                                        restrictedSpendCashback(CashbackRestrictionReason.NOT_SUITABLE_CATEGORY)
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
    public void shouldApplyExtraPharmaCashbackIfSeveralPromoMatch() {
        Promo extraPharmaCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(20))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_PHARMA_CASHBACK)
                        .addCashbackRule(RuleType.CATEGORY_FILTER_RULE, RuleParameterName.CATEGORY_ID,
                         Set.of(PHARMA_ROOT_CATEGORY_ID))
                        .setPriority(0)
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_CASHBACK)
                        .setPriority(-1)
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5))
                        .setPriority(-2)
        );
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_PHARMA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_PHARMA_CASHBACK_PROMO_KEY,
         extraPharmaCashbackPromo.getPromoKey());
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false),
                                        categoryId(PHARMA_ROOT_CATEGORY_ID)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(20),
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(20),
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.valueOf(20),
                                                                        BigDecimal.valueOf(99)
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
    public void shouldAllowCashbackOnSpendForFakeUser() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.enable(ConfigurationService.USE_FAKE_USERS_RANGE);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                 cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(OperationContextFactory.withUidBuilder(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint()).buildOperationContext())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void shouldAllowCashbackEmitForFakeUser() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleType.FAKE_USER_CUTTING_RULE, RuleParameterName.APPLIED_TO_FAKE_USER, true)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.enable(ConfigurationService.USE_FAKE_USERS_RANGE);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100), cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(OperationContextFactory.withUidBuilder(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint()).buildOperationContext())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void shouldNotAllowCashbackEmitForFakeUser() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleType.FAKE_USER_CUTTING_RULE, RuleParameterName.APPLIED_TO_FAKE_USER, true)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.enable(ConfigurationService.USE_FAKE_USERS_RANGE);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100), cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(OperationContextFactory.withUidBuilder(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint() - 10).buildOperationContext())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        restrictedCashback(
                                NOT_YA_PLUS_SUBSCRIBER
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                restrictedCashback(
                                                        NOT_YA_PLUS_SUBSCRIBER
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                restrictedCashback(NOT_YA_PLUS_SUBSCRIBER)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldApplyExtraCashbackIfSeveralPromoMatchAndUseFakeUser() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_CASHBACK)
                        .setPriority(0)
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5))
                        .setPriority(-1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        configurationService.enable(ConfigurationService.USE_FAKE_USERS_RANGE);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(OperationContextFactory.withUidBuilder(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint()).buildOperationContext())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.TEN,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.TEN,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(
                                                                        BigDecimal.TEN,
                                                                        BigDecimal.valueOf(99)
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
    public void shouldNotApplyCashbackIfSupplierFlagFilterProvide() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5))
                        .addCashbackRule(RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE,
                                RuleParameterName.SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE)
                        .setPriority(1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withDeliveries(courierDelivery(
                                        withFeatures(Set.of(DeliveryFeature.UNKNOWN)),
                                        withPrice(BigDecimal.valueOf(350))))
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.ZERO,
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        restrictedEmitCashback(NO_SUITABLE_PROMO),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
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
    public void shouldApplyCashbackIfSupplierFlagFilterProvide() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.ITEM)
                        .addCashbackRule(RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE,
                                RuleParameterName.SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE)
                        .setPriority(1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withDeliveries(courierDelivery(
                                        withFeatures(Set.of(DeliveryFeature.EXPRESS)),
                                        withPrice(BigDecimal.valueOf(350))))
                                .build())
                        .build()
        );
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                BigDecimal.valueOf(5),
                                BigDecimal.valueOf(99)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        BigDecimal.valueOf(5),
                                                        BigDecimal.valueOf(99)
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        allowedEmitCashback(BigDecimal.valueOf(5)),
                                                                        allowedSpendCashback(BigDecimal.valueOf(99))
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
    public void shouldCorrectlyCreateAccrualOnPaidStageForSingleOrder() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.MASTERCARD)
                        .setPriority(1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MASTERCARD)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                        .build()
        );
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(1));
    }

    @Test
    public void shouldCorrectCalculateCashbackDependingOnBnplFlag() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.MASTERCARD)
                        .setPriority(1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        MultiCartWithBundlesDiscountResponse response =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder
                                .builder(orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(100)
                                        )
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .withDeliveries(courierDelivery(
                                                withPrice(BigDecimal.valueOf(350)),
                                                builder -> builder.setSelected(true)
                                        ))
                                        .build())
                                .withBnplSelected(true)
                                .build()
                );
        assertEquals(response.getCashback().getEmit().getAmount(), BigDecimal.ZERO);
        response =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder
                                .builder(orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(100)
                                        )
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .withDeliveries(courierDelivery(
                                                withPrice(BigDecimal.valueOf(350)),
                                                builder -> builder.setSelected(true)
                                        ))
                                        .build())
                                .withBnplSelected(false)
                                .build()
                );
        assertEquals(response.getCashback().getEmit().getAmount(), BigDecimal.valueOf(5));
    }

    @Test
    public void shouldCorrectlyCalcPromoWithoutAccrualOnOrderCreation() {
        String onDeliveryPromoKey = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
        ).getPromoKey();
        String usualPromoKey = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(7), MULTI_ORDER)
                        .setPromoBucketName("bucket2")
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(100))
        ).getPromoKey();
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        MultiCartWithBundlesDiscountResponse response =
                marketLoyaltyClient.spendDiscount(
                        DiscountRequestWithBundlesBuilder
                                .builder(orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(100)
                                        )
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .withDeliveries(courierDelivery(
                                                withPrice(BigDecimal.valueOf(350)),
                                                builder -> builder.setSelected(true)
                                        ))
                                        .build())
                                .build()
                );
        List<OrderCashbackCalculation> orderCashbackCalculations = orderCashbackCalculationDao.findAll();
        assertThat(orderCashbackCalculations, hasSize(1));
        assertThat(orderCashbackCalculations.get(0), hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(7))));
        assertThat(yandexWalletTransactionDao.findAll(), hasSize(0));
        assertThat(response.getCashback().getEmit().getAmountByPromoKey().keySet(), containsInAnyOrder(
                usualPromoKey, onDeliveryPromoKey
        ));
    }

    @Test
    public void shouldShouldAllowEmitForUnAuthWithFeatureFlag() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                .setCmsDescriptionSemanticId("test-id-1"));
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM)
                .setCmsDescriptionSemanticId("test-id-2"));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_FOR_UNAUTH_ENABLED, true);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderId(OrderRequestWithBundlesBuilder.DEFAULT_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(2800),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withOrderId(ANOTHER_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(3000),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty("cashback", allOf(
                                allowedEmitCashback(BigDecimal.valueOf(638L)),
                                restrictedSpendCashback(CashbackRestrictionReason.CASHBACK_DISABLED),
                                hasProperty("selectedCashbackOption", nullValue()))
                        )
                )
        );
    }

    @Test
    public void shouldCalculateCorrectlySpendAmountOnCalcWithNoPaymentType() {
        var cashbackPromo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        var request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100),
                                cashback(cashbackPromo.getPromoKey(), 1))
//                        .withPaymentType(PaymentType.BANK_CARD)
                        .build())
                .build();
        var discountResponse = marketLoyaltyClient.calculateDiscount(request);
        var spendAmount = BigDecimal.valueOf(99L);

        assertThat(discountResponse, allOf(
                cashback(
                        allowedEmitCashback(BigDecimal.TEN),
                        allowedSpendCashback(spendAmount)
                ),
                hasProperty("orders", contains(allOf(
                        cashback(
                                allowedEmitCashback(BigDecimal.TEN),
                                allowedSpendCashback(spendAmount)
                        ),
                        hasProperty("items", contains(
                                cashback(
                                        allowedEmitCashback(BigDecimal.TEN),
                                        allowedSpendCashback(spendAmount)
                                )
                        ))
                )))
        ));
    }

    @Test
    public void shouldDrawSortedAgitationPriority() {
        Promo promo1 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE, MULTI_ORDER)
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(300))
        );
        Promo promo2 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, MULTI_ORDER)
                .setAgitationPriority(10)
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(300))
                .setPromoBucketName("non-default")
        );
        reloadPromoCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        var request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(1000),
                                cashback(promo1.getPromoKey(), 1))
                        .build())
                .build();
        var discountResponse = marketLoyaltyClient.calculateDiscount(request);

        assertThat(discountResponse.getCashback().getEmit().getThresholds(), hasSize(2));
        assertThat(discountResponse.getCashback().getEmit().getThresholds(), containsInRelativeOrder(
                allOf(
                        hasProperty("promoKey", equalTo(promo2.getPromoKey())),
                        hasProperty("agitationPriority", equalTo(10))
                ),
                allOf(
                        hasProperty("promoKey", equalTo(promo1.getPromoKey())),
                        hasProperty("agitationPriority", equalTo(0))
                )
        ));
        assertThat(discountResponse.getCashback().getEmit().getPromos(), hasSize(2));
        assertThat(discountResponse.getCashback().getEmit().getPromos(), containsInRelativeOrder(
                allOf(
                        hasProperty("promoKey", equalTo(promo2.getPromoKey())),
                        hasProperty("agitationPriority", equalTo(10))
                ),
                allOf(
                        hasProperty("promoKey", equalTo(promo1.getPromoKey())),
                        hasProperty("agitationPriority", equalTo(0))
                )
        ));

    }

    private static HttpHeaders headers(String key, String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(key, value);
        return headers;
    }

}
