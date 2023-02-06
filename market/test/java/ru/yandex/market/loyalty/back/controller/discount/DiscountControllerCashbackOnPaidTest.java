package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.allowedPaymentTypes;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerCashbackOnPaidTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    private PromoManager promoManager;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;

    @Test
    public void shouldCorrectlyCreateCalculationForPaidStageForSingleOrder() {
        createCashbackPromoAndEnableCashback();
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        quantity(15)
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
        assertThat(calculations.size(), equalTo(2));
        OrderCashbackCalculation calculation = calculations.get(0);
        assertThat(calculation, allOf(
                hasProperty("orderId", equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(75)))
        ));
    }

    @Test
    public void shouldFixMarketdiscount7856() {
        createCashbackPromoAndEnableCashback();
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        quantity(15)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MAESTRO)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                        .build()
        );
        assertThat(discountResponse.getCashback().getEmit().getDetails(),
                hasProperty(
                        "groups",
                        is(empty())
                )
        );
    }


    @Test
    public void shouldNotCreateCalculationForPaidStageForSingleOrderEvenIfPromoDeclined() {
        createCashbackPromoAndEnableCashback();
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        quantity(1)
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
        assertThat(calculations.size(), equalTo(0));
    }

    @Test
    public void shouldCorrectlyCreateCalculationForPaidStageForSingleOrderWithNotMasterCardPayed() {
        createCashbackPromoAndEnableCashback();
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        quantity(15)
                                )
                                .withPaymentType(PaymentType.CASH_ON_DELIVERY)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                        .build()
        );
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        OrderCashbackCalculation calculation = calculations.get(0);
        assertThat(calculation, allOf(
                hasProperty("orderId", equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE))
        ));
    }

    @Test
    public void shouldCorrectlyCreateCalculationForPaidStageForMultiOrder() {
        createCashbackPromoAndEnableCashback();
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                        .withOrderId("1")
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(100),
                                                quantity(15)
                                        )
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .withDeliveries(courierDelivery(
                                                withPrice(BigDecimal.valueOf(350)),
                                                builder -> builder.setSelected(true)
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(500),
                                                quantity(5)
                                        )
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .withDeliveries(courierDelivery(
                                                withPrice(BigDecimal.valueOf(350)),
                                                builder -> builder.setSelected(true)
                                        ))
                                        .build()
                        )
                        .build()
        );
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        OrderCashbackCalculation calculation = calculations.get(0);
        assertThat(calculation, allOf(
                hasProperty("orderId", equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(200)))
        ));
    }


    @Test
    public void shouldCalculateCashbackOnlyForItemWithAllowedPaymentType() {
        createCashbackPromoAndEnableCashback();
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId("1")
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(10000),
                                                quantity(1),
                                                allowedPaymentTypes(Set.of(PaymentType.CASH_ON_DELIVERY))
                                        )
                                        .withPaymentType(PaymentType.YANDEX)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .withDeliveries(courierDelivery(
                                                withPrice(BigDecimal.valueOf(350)),
                                                builder -> builder.setSelected(true)
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderId("2")
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(1000),
                                                quantity(1),
                                                allowedPaymentTypes(Set.of(PaymentType.YANDEX))
                                        )
                                        .withPaymentType(PaymentType.YANDEX)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .withDeliveries(courierDelivery(
                                                withPrice(BigDecimal.valueOf(350)),
                                                builder -> builder.setSelected(true)
                                        ))
                                        .build()
                        )
                        .build()
        );
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        OrderCashbackCalculation calculation = calculations.get(0);
        assertThat(calculation, allOf(
                hasProperty("orderId", equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(50)))
        ));
    }

    private void createCashbackPromoAndEnableCashback() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.MASTERCARD)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .setPriority(1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
    }

}
