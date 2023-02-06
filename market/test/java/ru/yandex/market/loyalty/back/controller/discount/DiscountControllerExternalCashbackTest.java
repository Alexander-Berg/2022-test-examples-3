package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.PaymentInfo;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.BankTestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.api.model.discount.PaymentFeature.YA_BANK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_BANK_CASHBACK;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedCashback;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PAYMENT_FEATURE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_CASHBACK_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PAYMENT_FEATURES_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_BANK_CASHBACK_ENABLED;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_BANK_CASHBACK_PROMO_KEY;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_CASHBACK_ENABLED;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class DiscountControllerExternalCashbackTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final String PAYMENT_METHOD_ID = "somePaymentMethodId";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private BankTestUtils bankTestUtils;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;

    private Promo externalCashbackPromo;

    @Before
    public void init() {
        externalCashbackPromo = promoManager.createExternalCashbackPromo(
                PromoUtils.ExternalCashback.defaultBank()
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_BANK_CASHBACK)
                        .addCashbackRule(MAX_ORDER_TOTAL_CUTTING_RULE, MAX_ORDER_TOTAL, BigDecimal.valueOf(15000))
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.ONE)
                        .addCashbackRule(PAYMENT_FEATURES_CUTTING_RULE, PAYMENT_FEATURE, YA_BANK)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE)
        );

        configurationService.enable(YANDEX_CASHBACK_ENABLED);
        configurationService.enable(YANDEX_BANK_CASHBACK_ENABLED);
        configurationService.set(YANDEX_BANK_CASHBACK_PROMO_KEY, externalCashbackPromo.getPromoKey());

        cashbackCacheService.reloadCashbackPromos();

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
    }

    @Test
    public void shouldCalcExternalCashback() {
        bankTestUtils.mockCalculatorWithDefaultResponse();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withPaymentInfo(new PaymentInfo(PAYMENT_METHOD_ID, Collections.singleton(YA_BANK)))
                        .build()
        );

        assertThat(discountResponse, allowedCashback(
                BigDecimal.valueOf(10),
                null
        ));
    }

    @Test
    public void shouldCalcExternalCashbackWithMaxCashbackThreshold() {
        bankTestUtils.mockCalculatorWithDefaultResponse();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(10000))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withPaymentInfo(new PaymentInfo(PAYMENT_METHOD_ID, Collections.singleton(YA_BANK)))
                        .build()
        );

        assertThat(discountResponse, allowedCashback(
                BankTestUtils.MAX_AMOUNT_DEFAULT,
                null
        ));
    }

    @Test
    public void shouldNotCalcExternalCashbackWhenNoPerk() {
        bankTestUtils.mockCalculatorWithNotFoundResponse();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withPaymentInfo(new PaymentInfo(PAYMENT_METHOD_ID, Collections.singleton(YA_BANK)))
                        .build()
        );

        assertThat(discountResponse, allowedCashback(
                BigDecimal.ZERO,
                null
        ));
    }



    @Test
    public void shouldNotCalcExternalCashbackWhenNoPaymentInfo() {
        bankTestUtils.mockCalculatorWithDefaultResponse();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(1000))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        //.withPaymentInfo(new PaymentInfo(PAYMENT_METHOD_ID, Collections.singleton(YA_BANK)))
                        .build()
        );

        assertThat(discountResponse, allOf(
                allowedCashback(
                        BigDecimal.ZERO,
                        null
                ),
                hasProperty("cashback", hasProperty("emit", allOf(
                        hasProperty("thresholds", contains(allOf(
                                hasProperty("promoKey", equalTo(externalCashbackPromo.getPromoKey())),
                                hasProperty("amount", comparesEqualTo(BankTestUtils.PERCENT_DEFAULT))
                        ))),
                        hasProperty("promos", hasSize(0))
                )))
        ));
    }

    @Test
    public void shouldNotCalcExternalCashbackWhenOrderExceedsLimit() {
        bankTestUtils.mockCalculatorWithDefaultResponse();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(16000))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withPaymentInfo(new PaymentInfo(PAYMENT_METHOD_ID, Collections.singleton(YA_BANK)))
                        .build()
        );

        assertThat(discountResponse, allowedCashback(
                BigDecimal.ZERO,
                null
        ));
    }

    @Test
    public void shouldSaveCalculationForExternalCashbackOnSpend() {
        bankTestUtils.mockCalculatorWithDefaultResponse();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withPaymentInfo(new PaymentInfo(PAYMENT_METHOD_ID, Collections.singleton(YA_BANK)))
                        .build()
        );

        assertThat(discountResponse, allowedCashback(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(99)
        ));

        List<OrderCashbackCalculation> orderCashbackCalculations = orderCashbackCalculationDao.findAll();
        assertThat(orderCashbackCalculations, hasSize(2));
        assertThat(orderCashbackCalculations.get(0),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(10))));
        assertThat(yandexWalletTransactionDao.findAll(), hasSize(0));
    }

    @Test
    public void shouldSaveCalculationForExternalCashbackOnSpendWithoutPaymentInfo() {
        bankTestUtils.mockCalculatorWithDefaultResponse();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        //.withPaymentInfo(new PaymentInfo(PAYMENT_METHOD_ID, Collections.singleton(YA_BANK)))
                        .build()
        );

        assertThat(discountResponse, allowedCashback(
                BigDecimal.ZERO,
                null
        ));

        List<OrderCashbackCalculation> orderCashbackCalculations = orderCashbackCalculationDao.findAll();
        assertThat(orderCashbackCalculations, hasSize(2));
        assertThat(orderCashbackCalculations.get(0),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.ZERO)));
        assertThat(yandexWalletTransactionDao.findAll(), hasSize(0));
    }
}
