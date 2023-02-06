package ru.yandex.market.loyalty.back.controller.discount;

import org.joda.money.Money;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesResponse;
import ru.yandex.market.loyalty.back.controller.CouponControllerClient;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.service.discount.ItemPromoCalculation;
import ru.yandex.market.loyalty.core.service.discount.constants.DefaultCurrencyUnit;
import ru.yandex.market.loyalty.core.utils.Accumulator;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.generateWith;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.same;
import static ru.yandex.market.loyalty.core.utils.DiscountResponseUtil.hasNoErrors;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContext;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_QUANTITY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.OrderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.keyOf;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderItemBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.sumPriceWithDiscount;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.totalDiscount;
import static ru.yandex.market.loyalty.core.utils.SequenceCustomizer.compose;

/**
 * @author ukchuvrus
 */
public abstract class DiscountControllerGenerationSupportedTest extends MarketLoyaltyBackMockedDbTestBase {
    static final Money DISCOUNT_AMOUNT = Money.of(DefaultCurrencyUnit.RUB, 300L);
    private static final Money MINIMAL_PENNIES_CHUNK = Money.ofMinor(DefaultCurrencyUnit.RUB, 1);
    public static final Money MINIMAL_RUBLES_CHUNK = Money.ofMajor(DefaultCurrencyUnit.RUB, 1);

    @Autowired
    private CouponControllerClient couponControllerClient;

    private OrderWithDeliveriesResponse getParametrizedOkDiscount(CouponDto coupon,
                                                                  OperationContextDto operationContext,
                                                                  int itemsCount, int itemPrice, Money minimalChunk) {
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(itemPrice))
        )), itemsCount, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();
        MultiCartDiscountResponse result =
                marketLoyaltyClient.spendDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).withOperationContext(operationContext).build());

        assertThat(result, hasNoErrors());
        OrderWithDeliveriesResponse retOrder = result.getOrders().get(0);
        check(retOrder, itemsCount, itemPrice, minimalChunk);
        return retOrder;
    }

    protected OrderWithDeliveriesResponse calcOk(CouponDto coupon, int itemsCount, int itemPrice) {
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(itemPrice))
        )), itemsCount, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build()
        );
        assertThat(discountResponse, hasNoErrors());
        return discountResponse.getOrders().get(0);
    }

    protected void calcOkRubles(CouponDto coupon, int itemsCount, int itemPrice) {
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(itemPrice))
        )), itemsCount, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();
        MultiCartDiscountResponse result =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build());

        assertThat(result, hasNoErrors());
        check(result.getOrders().get(0), itemsCount, itemPrice, MINIMAL_RUBLES_CHUNK);
    }

    protected OrderWithDeliveriesResponse spendOk(CouponDto coupon, int itemsCount, int itemPrice) {
        return getParametrizedOkDiscount(coupon, uidOperationContext(), itemsCount, itemPrice, MINIMAL_RUBLES_CHUNK);
    }

    protected static String createKey() {
        return UUID.randomUUID().toString();
    }

    protected CouponDto createActivatedCoupon(Long promoId) {
        CouponDto coupon = couponControllerClient.getOrCreateCoupon(createKey(), null, null, promoId);
        return couponControllerClient.activateCoupon(coupon.getCode());
    }

    protected static void checkOverdraft(OrderWithDeliveriesResponse orderResponse, double overdraft) {
        assertThat(Money.of(DefaultCurrencyUnit.RUB, ItemPromoCalculation.calculateTotalDiscount(orderResponse)).getAmount(),
                lessThanOrEqualTo(DISCOUNT_AMOUNT.multipliedBy(overdraft + 1, RoundingMode.FLOOR).getAmount()));
    }

    private static void check(OrderWithDeliveriesResponse response, int itemsCount, int itemPrice, Money minimalChunk) {
        Money MAX_RANGE = Money.of(DefaultCurrencyUnit.RUB, itemPrice)
                .minus(DISCOUNT_AMOUNT.dividedBy(itemsCount, RoundingMode.FLOOR));

        final Money ROUNDED_MAX_RANGE = minimalChunk == MINIMAL_PENNIES_CHUNK ?
                MAX_RANGE : Money.of(DefaultCurrencyUnit.RUB, MAX_RANGE.getAmount().setScale(0, BigDecimal.ROUND_UP));

        final Money MIN_RANGE = MAX_RANGE.minus(minimalChunk);

        assertThat(ItemPromoCalculation.calculateTotalDiscount(response), comparesEqualTo(DISCOUNT_AMOUNT.getAmount()));
        Accumulator<BigDecimal> accDiscount = new Accumulator<>(BigDecimal.ZERO, BigDecimal::add);
        response.getItems().forEach(item -> {
            accDiscount.accept(totalDiscount(item));
            BigDecimal amountAfterDiscount = sumPriceWithDiscount(item);
            assertThat(amountAfterDiscount, greaterThanOrEqualTo(MIN_RANGE.getAmount()));
            assertThat(amountAfterDiscount, lessThanOrEqualTo(ROUNDED_MAX_RANGE.getAmount()));
        });

        assertThat(accDiscount.getValue(), comparesEqualTo(ItemPromoCalculation.calculateTotalDiscount(response)));
    }
}
