package ru.yandex.market.loyalty.back.controller;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
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
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.INVALID_CLIENT_DEVICE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_PLATFORM;
import static ru.yandex.market.loyalty.core.rule.RuleType.CLIENT_PLATFORM_CUTTING_RULE;
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
public class ClientDeviceTypeCuttingRuleTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CouponService couponService;
    @Autowired
    private DiscountUtils discountUtils;

    private Coupon createCouponAndPromo(UsageClientDeviceType... clientDeviceType) {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon
                .defaultSingleUse()
                .addPromoRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, ImmutableSet.copyOf(clientDeviceType))
        );
        return couponService.createOrGetCoupon(CouponCreationRequest.builder(UUID.randomUUID().toString(),
                promo.getId())
                .forceActivation(true)
                .build(), discountUtils.getRulesPayload());
    }

    @Test
    public void shouldFailIfDeviceIsNotMatched() {
        Coupon coupon = createCouponAndPromo(UsageClientDeviceType.APPLICATION, UsageClientDeviceType.DESKTOP);

        MultiCartDiscountResponse discountResponse = calcDiscount(coupon, UsageClientDeviceType.TOUCH);
        assertThat(discountResponse, hasCouponError(INVALID_CLIENT_DEVICE));
    }

    @Test
    public void shouldSuccessIfDeviceIsMatched() {
        Coupon coupon = createCouponAndPromo(UsageClientDeviceType.APPLICATION);
        calcDiscount(coupon, UsageClientDeviceType.APPLICATION);
    }

    @Test
    public void shouldSuccessIfDeviceIsNotFilledInPromo() {
        Coupon coupon = createCouponAndPromo();
        calcDiscount(coupon, UsageClientDeviceType.APPLICATION);
    }

    @Test
    public void shouldFailIfDeviceIsNotSpecifiedInRequest() {
        Coupon coupon = createCouponAndPromo(UsageClientDeviceType.APPLICATION);
        assertThat(calcDiscount(coupon, null), hasCouponError(INVALID_CLIENT_DEVICE));
    }

    @Test
    public void shouldSuccessIfDeviceIsSpecifiedInRequestButNoRestrictionInPromo() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        Coupon coupon = couponService.createOrGetCoupon(
                CouponCreationRequest.builder(UUID.randomUUID().toString(), promo.getId())
                        .forceActivation(true)
                        .build(), discountUtils.getRulesPayload());
        calcDiscount(coupon, UsageClientDeviceType.TOUCH);
    }

    private MultiCartDiscountResponse calcDiscount(Coupon coupon, UsageClientDeviceType clientDeviceType) {
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 1, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        return marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder.build()
                )
                        .withCoupon(coupon.getCode())
                        .withOperationContext(
                                OperationContextFactory
                                        .withUidBuilder(123L)
                                        .withClientDevice(clientDeviceType)
                                        .buildOperationContextDto()
                        )
                        .build()
        );
    }

}
