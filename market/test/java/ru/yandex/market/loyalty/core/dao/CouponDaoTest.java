package ru.yandex.market.loyalty.core.dao;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CouponParamName;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.core.dao.coupon.CouponDao;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 22.05.17
 */
public class CouponDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long UID = 1L;

    @Autowired
    private CouponDao couponDao;
    @Autowired
    private IdentityDao identityDao;
    @Autowired
    private PromoManager promoManager;

    private Long identityId = -1L;
    private Promo promo;

    @Before
    public void init() {
        Optional.ofNullable(identityDao.createIfNecessaryUserIdentity(new Uid(UID))).ifPresent(id -> identityId = id);
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
    }

    @Test
    public void insetOrGetCouponWithParams() {
        String email = "dsfsd@sdfds.ru";
        String code = "code1";
        Coupon coupon = coupon(identityId, "key1", code, promo.getId());
        coupon.setParams(Collections.singletonMap(CouponParamName.USER_EMAIL, email));
        coupon = couponDao.tryInsertAndGetCoupon(coupon);
        assertEquals(email, coupon.getParams().get(CouponParamName.USER_EMAIL));
    }

    @Test
    public void getCouponWithParams() {
        String email = "dsfsd2@sdfds.ru";
        String code = "code1";
        Coupon coupon = coupon(identityId, "key1", code, promo.getId());
        coupon.setParams(Collections.singletonMap(CouponParamName.USER_EMAIL, email));
        couponDao.tryInsertAndGetCoupon(coupon);
        Coupon actual = couponDao.getCouponByCode(code).orElseThrow(() -> new AssertionError("coupon not found"));
        assertEquals(email, actual.getParams().get(CouponParamName.USER_EMAIL));
    }

    @Test
    public void getCouponByCode() {
        String code1 = "code1";
        String code2 = "code2";
        Coupon coupon1 = couponDao.tryInsertAndGetCoupon(coupon(identityId, "key1", code1, promo.getId()));
        Coupon coupon2 = couponDao.tryInsertAndGetCoupon(coupon(identityId, "key2", code2, promo.getId()));
        List<Coupon> coupons = couponDao.getCouponByCode(ImmutableSet.of(code1, code2));
        coupons.sort(Comparator.comparingLong(Coupon::getId));
        assertThat(coupon1, samePropertyValuesAs(coupons.get(0)));
        assertThat(coupon2, samePropertyValuesAs(coupons.get(1)));
    }

    @Test
    public void caseNotSensitiveSearchOfCoupon() {
        String uppercase = "UPPERCASE";
        couponDao.tryInsertAndGetCoupon(coupon(identityId, "key1", uppercase, promo.getId()));
        assertTrue(couponDao.getCouponByCode(uppercase.toLowerCase()).isPresent());

        String lowercase = "lowercase";
        couponDao.tryInsertAndGetCoupon(coupon(identityId, "key2", lowercase, promo.getId()));
        assertTrue(couponDao.getCouponByCode(uppercase.toUpperCase()).isPresent());

        assertEquals(2, couponDao.getCouponByCode(ImmutableSet.of(uppercase, lowercase)).size());
    }

    @Test
    public void getCouponByCodeManyCodes() {
        int cntCoupons = 1532;
        Set<String> codes = IntStream.range(0, cntCoupons)
                .mapToObj(i -> {
                    String code = "CODE" + i;
                    couponDao.tryInsertAndGetCoupon(coupon(identityId, "key" + i, code, promo.getId()));
                    return code;
                }).collect(Collectors.toSet());
        List<Coupon> coupons = couponDao.getCouponByCode(codes);
        assertEquals(cntCoupons, coupons.size());
        assertTrue(codes.containsAll(coupons.stream()
                .map(Coupon::getCode)
                .collect(Collectors.toList())
        ));
    }

    @Test
    public void markCouponActive() {
        String code1 = "code1";
        String code2 = "code2";
        Coupon coupon1 = couponDao.tryInsertAndGetCoupon(coupon(identityId, "key1", code1, promo.getId()));
        Coupon coupon2 = couponDao.tryInsertAndGetCoupon(coupon(identityId, "key2", code2, promo.getId()));
        couponDao.markCouponActive(Arrays.asList(coupon1, coupon2));
        coupon1.setStatus(CouponStatus.ACTIVE);
        coupon2.setStatus(CouponStatus.ACTIVE);
        List<Coupon> coupons = couponDao.getCouponByCode(ImmutableSet.of(code1, code2));
        coupons.sort(Comparator.comparingLong(Coupon::getId));
        assertThat(coupon1, samePropertyValuesAs(coupons.get(0), "modificationTime", "activationTime", "version"));
        assertThat(coupon2, samePropertyValuesAs(coupons.get(1), "modificationTime", "activationTime", "version"));
        assertEquals(coupon1.getVersion() + 1L, (long) coupons.get(0).getVersion());
        assertEquals(coupon1.getVersion() + 1L, (long) coupons.get(1).getVersion());
        assertNotNull(coupons.get(0).getActivationTime());
        assertNotNull(coupons.get(1).getActivationTime());
    }

    static Coupon coupon(long identityId, String key, String code, long promoId) {
        Coupon result = new Coupon();
        result.setStatus(CouponStatus.INACTIVE);
        result.setCode(code);
        result.setSourceKey(key);
        result.setCreatedFor(identityId);
        result.setPromoId(promoId);
        return result;
    }
}
