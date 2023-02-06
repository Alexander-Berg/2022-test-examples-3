package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.back.util.CashbackMatchers;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.cashback.CashbackCacheService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NO_SUITABLE_PROMO;
import static ru.yandex.market.loyalty.api.model.PromoType.CASHBACK;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.allowedCashback;
import static ru.yandex.market.loyalty.back.util.CashbackMatchers.cashback;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_EMIT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_ERROR;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * Из-за отсутствия удобного использования junit5 и параметризованных тестов, а так же достаточно узкого количества
 * проверок - каркас определенных тестов был украден из
 * {@link ru.yandex.market.loyalty.back.controller.discount.DiscountControllerCashbackTest}
 */
@TestFor(DiscountController.class)
public class DiscountControllerCashbackWithQuantityTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CashbackCacheService cashbackCacheService;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private DiscountService discountService;

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
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY),
                                        price(BigDecimal.valueOf(100.34)), quantity(BigDecimal.valueOf(1.576)))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        BigDecimal expectedEmitCashbackAmount =
                BigDecimal.ZERO;
        BigDecimal expectedSpendCashbackAmount = BigDecimal.valueOf(157);
        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(expectedEmitCashbackAmount,
                                expectedSpendCashbackAmount
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(expectedEmitCashbackAmount,
                                                        expectedSpendCashbackAmount
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                cashback(
                                                                        CashbackMatchers.noPromoEmitCashback(),
                                                                        CashbackMatchers.allowedSpendCashback(expectedSpendCashbackAmount)
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
    public void shouldAllowCashbackOnSpend() {
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));
        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        BigDecimal price = BigDecimal.valueOf(333.66);
        BigDecimal quantity = BigDecimal.valueOf(2.294);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY),
                                        price(price), quantity(quantity),
                                        OrderRequestUtils.cashback(cashbackPromo.getPromoKey(), 1))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withCashbackOptionType(CashbackType.EMIT)
                        .build()
        );

        BigDecimal expectedEmitCashbackAmount =
                BigDecimal.valueOf(77);
        BigDecimal expectedSpendCashbackAmount = BigDecimal.valueOf(764);

        assertThat(
                discountResponse,
                allOf(
                        allowedCashback(
                                expectedEmitCashbackAmount,
                                expectedSpendCashbackAmount
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                allowedCashback(
                                                        expectedEmitCashbackAmount,
                                                        expectedSpendCashbackAmount
                                                ),
                                                hasProperty(
                                                        "items",
                                                        contains(
                                                                allowedCashback(expectedEmitCashbackAmount,
                                                                        expectedSpendCashbackAmount
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
                hasProperty("discount", is(expectedEmitCashbackAmount)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(cashbackPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

}
