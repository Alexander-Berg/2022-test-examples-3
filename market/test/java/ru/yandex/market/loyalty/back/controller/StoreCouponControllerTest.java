package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.dao.coupon.CouponDao;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_EXISTS;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;

@TestFor(StoreCouponController.class)
public class StoreCouponControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private MarketLoyaltyClient client;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CouponService couponService;
    @Autowired
    private ClockForTests clock;
    @Autowired
    private CouponDao couponDao;
    @Autowired
    private DiscountUtils discountUtils;

    @Test
    public void shouldSaveAndRestoreCoupon() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEndDate(addDays(Date.from(clock.instant()), 5))
        );
        Coupon coupon =
                couponService.createOrGetCoupon(CouponCreationRequest.builder("test", promo.getId()).forceActivation(true).build(),
                discountUtils.getRulesPayload());

        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storeCoupon(identity, coupon.getCode());
        assertEquals(coupon.getCode(), client.getStoredCoupon(identity));
    }

    @Test
    public void shouldSaveAndRestoreCouponCodeWithoutCouponWithFalseCheckFlag() {
        String someCouponCode = "SOME_COUPON_CODE";
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storeCoupon(identity, someCouponCode, false);
        assertEquals(someCouponCode, client.getStoredCoupon(identity));
    }

    @Test
    public void shouldNotSaveAndRestoreCouponCodeWithoutCoupon() {
        String someCouponCode = "SOME_COUPON_CODE";
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        try {
            client.storeCoupon(identity, someCouponCode);
            fail("Exception with code " + COUPON_NOT_EXISTS.name() + " should be thrown");
        } catch (MarketLoyaltyException e) {
            assertEquals(COUPON_NOT_EXISTS, e.getMarketLoyaltyErrorCode());
        }

        assertNull(client.getStoredCoupon(identity));
    }

    @Test
    public void shouldReturnNullIfNoCoupon() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        assertNull(client.getStoredCoupon(identity));
    }

    @Test
    public void shouldNotAllowStaleCoupon() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEndDate(addDays(Date.from(clock.instant()), 5))
        );
        Coupon coupon =
                couponService.createOrGetCoupon(CouponCreationRequest.builder("test", promo.getId()).forceActivation(true).build(),
                discountUtils.getRulesPayload());

        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storeCoupon(identity, coupon.getCode());
        assertEquals(coupon.getCode(), client.getStoredCoupon(identity));

        clock.spendTime(7, ChronoUnit.DAYS);

        assertNull(client.getStoredCoupon(identity));
    }

    @Test
    public void shouldNotAllowUsedCoupon() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEndDate(addDays(Date.from(clock.instant()), 5))
        );
        Coupon coupon =
                couponService.createOrGetCoupon(CouponCreationRequest.builder("test", promo.getId()).forceActivation(true).build(),
                discountUtils.getRulesPayload());
        coupon = couponDao.getCouponByCode(coupon.getCode()).get();

        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storeCoupon(identity, coupon.getCode());

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder.builder(
                orderRequestBuilder().withOrderItem().build()
        ).withCoupon(coupon.getCode()).build());

        assertNull(client.getStoredCoupon(identity));
    }


    @Test
    public void shouldDeleteCoupon() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEndDate(addDays(Date.from(clock.instant()), 5))
        );
        Coupon coupon =
                couponService.createOrGetCoupon(CouponCreationRequest.builder("test", promo.getId()).forceActivation(true).build(),
                discountUtils.getRulesPayload());
        coupon = couponDao.getCouponByCode(coupon.getCode()).get();

        client.storeCoupon(identity, coupon.getCode());
        assertEquals(coupon.getCode(), client.getStoredCoupon(identity));

        client.deleteStoredCoupon(identity);
        assertNull(client.getStoredCoupon(identity));
    }
}
