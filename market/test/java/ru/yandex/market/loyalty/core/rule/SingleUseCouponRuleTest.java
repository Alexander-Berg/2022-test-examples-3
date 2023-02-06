package ru.yandex.market.loyalty.core.rule;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.sql.Date;
import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;

public class SingleUseCouponRuleTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private RuleFactory ruleFactory;
    @Autowired
    private DiscountUtils discountUtils;

    @Test
    public void testUsagePeriodCommonCase() {
        testUsagePeriod("2018-02-03", "2018-02-12", 5, "2018-02-01", "2018-02-03", "2018-02-06", false);
        testUsagePeriod("2018-02-03", "2018-02-12", 5, "2018-02-05", "2018-02-05", "2018-02-10", false);
        testUsagePeriod("2018-02-03", "2018-02-12", 5, "2018-02-09", "2018-02-09", "2018-02-12", false);
    }

    @Test
    public void testUsagePeriodCouponExpiredBeforePromoStart() {
        testUsagePeriod("2018-02-03", "2018-02-12", 5, "2018-01-01", "2018-02-03", "2018-02-03", false);
    }

    @Test
    public void testUsagePeriodCouponActivatedAfterPromoEnd() {
        testUsagePeriod("2018-02-03", "2018-02-12", 5, "2018-03-01", "2018-02-12", "2018-02-12", false);
    }

    @Test
    public void testUsagePeriodCouponActiveToEndOfPromo() {
        testUsagePeriod("2018-02-03", "2018-06-12", null, "2018-02-09", "2018-02-09", "2018-06-12", true);
    }

    private void testUsagePeriod(String promoStartDate, String promoEndDate, Integer expiryDays,
                                 String couponActivationDate, String expectedPeriodStartDate,
                                 String expectedPeriodEndDate, boolean toEndOfPromo) {
        Coupon coupon = Coupon.builder()
                .setActivationTime(new Timestamp(Date.valueOf(couponActivationDate).getTime()))
                .setStatus(CouponStatus.ACTIVE)
                .build();
        ExpirationPolicy expirationPolicy;
        if (toEndOfPromo) {
            expirationPolicy = ExpirationPolicy.toEndOfPromo();
        } else {
            expirationPolicy = ExpirationPolicy.expireByDays(expiryDays);
        }
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStartDate(Date.valueOf(promoStartDate))
                .setEndDate(Date.valueOf(promoEndDate))
                .setExpiration(expirationPolicy)
        );
        CouponRule r = ruleFactory.getSinglePromoRule(promo.getRulesContainer(), RuleCategory.COUPON_RULE,
                discountUtils.getRulesPayload());
        Pair<java.util.Date, java.util.Date> period = r.usagePeriod(promo, coupon);
        assertEquals(Date.valueOf(expectedPeriodStartDate), period.getLeft());
        assertEquals(Date.valueOf(expectedPeriodEndDate), period.getRight());
    }

}
