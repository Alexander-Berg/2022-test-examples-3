package ru.yandex.market.loyalty.back.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.dao.coupon.StoreCouponDao;
import ru.yandex.market.loyalty.core.model.promo.PromocodePromoBuilder;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@TestFor(StoreCouponController.class)
public class StoreCouponControllerPromocodeTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final String PROMOCODE = "some_promocode";
    private final static String NOT_ACTIVE_PROMOCODE = "NOT_ACTIVE_PROMOCODE";
    private final static String EXCEEDED_BUDGET_PROMOCODE = "EXCEEDED_BUDGET_PROMOCODE";
    public static final Date PASSED_END_DATE = new GregorianCalendar(2020, Calendar.FEBRUARY, 1).getTime();

    @Autowired
    private MarketLoyaltyClient client;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private StoreCouponDao storeCouponDao;

    private PromocodePromoBuilder promocodePromoBuilder;

    @Before
    public void configure() {
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode().setCode(PROMOCODE));
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(EXCEEDED_BUDGET_PROMOCODE)
                .setBudget(BigDecimal.ZERO));
        promocodePromoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode().setCode(NOT_ACTIVE_PROMOCODE);
        promoManager.createPromocodePromo(promocodePromoBuilder);
    }

    @Test
    public void shouldSavePromocode() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storeCoupon(identity, PROMOCODE);
        assertThat(storeCouponDao.getCoupon(identity), notNullValue());
    }

    @Test
    public void shouldGetPromocode() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storeCoupon(identity, PROMOCODE);
        assertThat(storeCouponDao.getCoupon(identity), notNullValue());

        assertThat(client.getStoredCoupon(identity), is(PROMOCODE));
    }

    @Test
    public void shouldNotGetPromocode() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("246");
        client.storeCoupon(identity, NOT_ACTIVE_PROMOCODE);

        promoManager.updatePromocodePromo(promocodePromoBuilder.setEndDate(PASSED_END_DATE));

        assertThat(storeCouponDao.getCoupon(identity), notNullValue());

        assertThat(client.getStoredCoupon(identity), nullValue());
    }

    @Test
    public void shouldNotGetPromocodeWithExceededBudget() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("357");
        client.storeCoupon(identity, EXCEEDED_BUDGET_PROMOCODE);

        assertThat(storeCouponDao.getCoupon(identity), notNullValue());

        assertThat(client.getStoredCoupon(identity), nullValue());
    }
}
