package ru.yandex.market.loyalty.back.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.OrderItemResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.OperationContext;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.PHONE_NUM_CAN_BE_USED_ONCE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.PHONE_NUM_REQUIRED_TO_SPEND_PROMOCODE;
import static ru.yandex.market.loyalty.core.rule.RuleType.ONCE_PER_PHONE_NUMBER_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.generateWith;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.same;
import static ru.yandex.market.loyalty.core.utils.DiscountResponseUtil.hasCouponError;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_QUANTITY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.OrderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.keyOf;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderItemBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.SequenceCustomizer.compose;

/**
 * Created by maratik.
 */
public class OncePerPhoneNumberFilterRuleTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final int SOME_PRICE = 1000;
    private static final int ITEMS_COUNT = 1;
    private static final String SOME_PHONE_NUMBER = "+7(111)2223333";
    private static final String SAME_PHONE_NUMBER_IN_ANOTHER_FORMAT = "+7(111)222-33-33";
    private static final String EMPTY_PHONE_NUMBER = "";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CouponService couponService;
    @Autowired
    private DiscountUtils discountUtils;

    private Promo promo;

    @Before
    public void init() {
        promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .addPromoRule(ONCE_PER_PHONE_NUMBER_FILTER_RULE)
        );
    }

    @Test
    public void shouldAllowUseOfPhoneOnlyOnce() {
        Coupon coupon = createSomeCoupon("test");
        Coupon anotherCoupon = createSomeCoupon("test1");

        spendCoupon(coupon, SOME_PHONE_NUMBER);

        MultiCartDiscountResponse discountResponse = spendCoupon(anotherCoupon, SOME_PHONE_NUMBER);
        assertThat(discountResponse, hasCouponError(PHONE_NUM_CAN_BE_USED_ONCE));
    }

    @Test
    public void shouldForbidUseSamePhoneInAnotherFormat() {
        Coupon coupon = createSomeCoupon("test");
        Coupon anotherCoupon = createSomeCoupon("test1");

        spendCoupon(coupon, SOME_PHONE_NUMBER);

        MultiCartDiscountResponse discountResponse = spendCoupon(anotherCoupon, SAME_PHONE_NUMBER_IN_ANOTHER_FORMAT);
        assertThat(discountResponse, hasCouponError(PHONE_NUM_CAN_BE_USED_ONCE));
    }

    @Test
    public void shouldAllowUseOfPhoneAfterRevert() {
        Coupon coupon = createSomeCoupon("test");
        MultiCartDiscountResponse spendCoupon = spendCoupon(coupon, SOME_PHONE_NUMBER);

        revertCoupon(spendCoupon);
        spendCoupon(coupon, SOME_PHONE_NUMBER);
    }

    @Test
    public void shouldForbidSpendWithoutPhone() {
        Coupon coupon = createSomeCoupon("test");
        MultiCartDiscountResponse discountResponse = spendCoupon(coupon, EMPTY_PHONE_NUMBER);
        assertThat(discountResponse, hasCouponError(PHONE_NUM_REQUIRED_TO_SPEND_PROMOCODE));
    }

    @Test
    public void shouldForbidCalcWithUsedPhone() {

        Coupon coupon = createSomeCoupon("test");
        Coupon anotherCoupon = createSomeCoupon("test1");

        spendCoupon(coupon, SOME_PHONE_NUMBER);
        MultiCartDiscountResponse discountResponse = calcCoupon(anotherCoupon, SOME_PHONE_NUMBER);
        assertThat(discountResponse, hasCouponError(PHONE_NUM_CAN_BE_USED_ONCE));
    }

    @Test
    public void shouldAllowCalcWithNoPhone() {

        Coupon coupon = createSomeCoupon("test");
        Coupon anotherCoupon = createSomeCoupon("test1");

        spendCoupon(coupon, SOME_PHONE_NUMBER);
        calcCoupon(anotherCoupon, null);
        try {
            spendCoupon(anotherCoupon, SOME_PHONE_NUMBER);
        } catch (MarketLoyaltyException ex) {
            assertEquals(MarketLoyaltyErrorCode.PHONE_NUM_CAN_BE_USED_ONCE, ex.getMarketLoyaltyErrorCode());
        }
    }

    @Test
    public void shouldSuccessesCalcWithNotUsedPhone() {
        Coupon coupon = createSomeCoupon("test");
        calcCoupon(coupon, SOME_PHONE_NUMBER);
    }


    private Coupon createSomeCoupon(String couponKey) {
        return couponService.createOrGetCoupon(CouponCreationRequest.builder(couponKey, promo.getId())
                .forceActivation(true)
                .build(), discountUtils.getRulesPayload());
    }

    private MultiCartDiscountResponse spendCoupon(Coupon coupon, String phone) {
        return spendCoupon(coupon, phone, true);
    }

    private MultiCartDiscountResponse calcCoupon(Coupon coupon, String phone) {
        return spendCoupon(coupon, phone, false);
    }

    private MultiCartDiscountResponse spendCoupon(Coupon coupon, String phone, Boolean spend) {
        OperationContext operationContext = OperationContextFactory
                .withUidBuilder(1L)
                .withCouponCode(coupon.getCode())
                .withPhone(phone)
                .buildOperationContext();
        if (spend) {
            OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
            generateWith(same(orderItemBuilder(
                    quantity(DEFAULT_QUANTITY),
                    price(BigDecimal.valueOf(SOME_PRICE))
            )), ITEMS_COUNT, compose(keyOf(), OrderRequestUtils::itemKey))
                    .forEach(orderRequestBuilder::withOrderItem);
            return marketLoyaltyClient.spendDiscount(
                    DiscountRequestBuilder
                            .builder(orderRequestBuilder
                                    .build()
                            )
                            .withCoupon(coupon.getCode())
                            .withPlatform(PromoUtils.DEFAULT_PLATFORM.getApiPlatform())
                            .withOperationContext(operationContext)
                            .build()
            );
        } else {
            OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
            generateWith(same(orderItemBuilder(
                    quantity(DEFAULT_QUANTITY),
                    price(BigDecimal.valueOf(SOME_PRICE))
            )), ITEMS_COUNT, compose(keyOf(), OrderRequestUtils::itemKey))
                    .forEach(orderRequestBuilder::withOrderItem);
            return marketLoyaltyClient.calculateDiscount(
                    DiscountRequestBuilder
                            .builder(orderRequestBuilder
                                    .build()
                            )
                            .withCoupon(coupon.getCode())
                            .withPlatform(PromoUtils.DEFAULT_PLATFORM.getApiPlatform())
                            .withOperationContext(operationContext)
                            .build()
            );
        }
    }

    private void revertCoupon(MultiCartDiscountResponse orderResponse) {
        marketLoyaltyClient.revertDiscount(
                orderResponse.getOrders().stream().flatMap(o -> o.getItems().stream())
                        .map(OrderItemResponse::getPromos)
                        .flatMap(Collection::stream)
                        .map(ItemPromoResponse::getDiscountToken)
                        .collect(Collectors.toSet())
        );
    }
}
