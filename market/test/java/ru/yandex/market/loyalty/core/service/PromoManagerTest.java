package ru.yandex.market.loyalty.core.service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.core.dao.promocode.PromocodeEntryDao;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.CashbackPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.PromocodePromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.exception.DuplicateCouponCodeViolationException;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.DIRECT_PRIORITY;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.CASHBACK_PROMO_FAKE_SOURCE_FLAG_NAME;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;
import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE_VALUE;

/**
 * @author dinyat
 * 07/07/2017
 */
public class PromoManagerTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final String COUPON_CODE = "couponCode";
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private PromocodeEntryDao promocodeEntryDao;

    @Test
    public void secondInfinitePromo() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse().setCouponCode(COUPON_CODE));

        assertThrows(DuplicateCouponCodeViolationException.class, () ->
                promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse().setCouponCode(COUPON_CODE))
        );
    }

    @Test
    public void shouldAddBudgetWithConversion() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setCouponValue(BigDecimal.valueOf(300), CoreCouponValueType.FIXED)
                .setBudget(BigDecimal.valueOf(3000))
                .setEmissionBudget(BigDecimal.valueOf(9))
        );

        promoManager.addBudget(BigDecimal.valueOf(1000), BigDecimal.valueOf(3), BigDecimal.valueOf(100), null,
                promo.getPromoId().getId(), "test");

        promo = promoService.getPromo(promo.getPromoId().getId());

        assertThat(promo.getCurrentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(12)));
        assertThat(promo.getCurrentBudget(), comparesEqualTo(BigDecimal.valueOf(4000)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddBudgetWithConversion() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setCouponValue(BigDecimal.valueOf(300), CoreCouponValueType.FIXED)
                .setBudget(BigDecimal.valueOf(3000))
                .setEmissionBudget(BigDecimal.valueOf(9))
        );

        promoManager.addBudget(BigDecimal.valueOf(1000), BigDecimal.valueOf(2), BigDecimal.valueOf(100), null,
                promo.getPromoId().getId(), "test");
    }

    @Test
    public void shouldAddBudgetWithConversionAndAverageBill() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
                .setAverageBill(BigDecimal.valueOf(300))
                .setConversion(BigDecimal.TEN)
                .setBudget(BigDecimal.valueOf(3000))
                .setEmissionBudget(BigDecimal.valueOf(732))
        );

        promoManager.addBudget(BigDecimal.valueOf(1000), BigDecimal.valueOf(896), BigDecimal.valueOf(9),
                BigDecimal.valueOf(200), promo.getPromoId().getId(), "test");

        promo = promoService.getPromo(promo.getPromoId().getId());

        assertThat(promo.getCurrentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(1628)));
        assertThat(promo.getCurrentBudget(), comparesEqualTo(BigDecimal.valueOf(4000)));
    }

    @Test
    public void shouldNotAddBudgetWithConversionAndAverageBill() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
                .setAverageBill(BigDecimal.valueOf(300))
                .setConversion(BigDecimal.TEN)
                .setBudget(BigDecimal.valueOf(3000))
                .setEmissionBudget(BigDecimal.valueOf(732))
        );
        assertThrows(IllegalArgumentException.class, () ->
                promoManager.addBudget(BigDecimal.valueOf(1000), BigDecimal.valueOf(895), BigDecimal.valueOf(9),
                        BigDecimal.valueOf(200), promo.getPromoId().getId(), "test")
        );
    }

    @Test
    public void shouldAddBudgetWithConversionChangeToSmall() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setCouponValue(BigDecimal.valueOf(300), CoreCouponValueType.FIXED)
                .setBudget(BigDecimal.valueOf(3000))
                .setEmissionBudget(BigDecimal.valueOf(9))
                .setConversion(BigDecimal.valueOf(100))
        );

        promoManager.addBudget(BigDecimal.valueOf(1000), BigDecimal.valueOf(117), BigDecimal.TEN, null,
                promo.getPromoId().getId()
                , "test");

        promo = promoService.getPromo(promo.getPromoId().getId());

        assertThat(promo.getCurrentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(126)));
        assertThat(promo.getCurrentBudget(), comparesEqualTo(BigDecimal.valueOf(4000)));
    }

    @Test
    public void shouldAddBudgetAfterSpend() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setCouponValue(BigDecimal.valueOf(2000), CoreCouponValueType.FIXED)
                .setBudget(BigDecimal.valueOf(2100))
                .setEmissionBudget(BigDecimal.ONE)
                .setConversion(BigDecimal.valueOf(100))
        );

        promoService.updateStatus(promo, PromoStatus.ACTIVE);

        Coupon coupon = couponService.createOrGetCoupon(CouponCreationRequest.builder("shouldAddBudgetAfterSpend",
                promo.getPromoId().getId())
                .forceActivation(true)
                .build(), discountUtils.getRulesPayload()
        );

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse = discountService.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder().withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                price(BigDecimal.valueOf(4000))
                        )
                                .build()
                ).withCoupon(coupon.getCode()).build(),
                applicabilityPolicy,
                null
        );
        assertThat(discountResponse.getPromocodeErrors(), empty());

        promoManager.addBudget(BigDecimal.valueOf(2100), BigDecimal.ONE, BigDecimal.valueOf(100), null,
                promo.getPromoId().getId()
                , "test");

        promo = promoService.getPromo(promo.getPromoId().getId());

        assertThat(promo.getCurrentEmissionBudget(), comparesEqualTo(BigDecimal.ONE));
        assertThat(promo.getCurrentBudget(), comparesEqualTo(BigDecimal.valueOf(2200)));
    }

    @Test
    public void shouldNotAddBudgetForDynamicCoinPromoIfConversionSet() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultDynamic()
                .setCoinCreationReason(CoreCoinCreationReason.ORDER_DYNAMIC));
        assertThrows(
                IllegalArgumentException.class,
                () -> promoManager.addBudget(
                        BigDecimal.valueOf(1000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(100),
                        null,
                        smartShoppingPromo.getPromoId().getId(),
                        "test")
        );
    }

    @Test
    public void shouldAddBudgetForDynamicCoinPromo() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultDynamic()
                .setCoinCreationReason(CoreCoinCreationReason.ORDER_DYNAMIC));
        promoManager.addBudget(BigDecimal.valueOf(1000), BigDecimal.ZERO, null, null,
                smartShoppingPromo.getPromoId().getId(), "test");
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldNotCreateIntersectedPromocodes() {
        final String promoCode = "PROMOCODE";

        promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setEmissionBudget(BigDecimal.ONE)
                        .setStartDate(new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime())
                        .setEndDate(new GregorianCalendar(2019, Calendar.FEBRUARY, 1).getTime())
                        .setCode(promoCode));

        promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setEmissionBudget(BigDecimal.ONE)
                        .setStartDate(new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime())
                        .setEndDate(new GregorianCalendar(2020, Calendar.FEBRUARY, 1).getTime())
                        .setCode(promoCode));
    }

    @Test
    public void shouldAllowCreateSamePromocodesInDifferentInterval() {
        final String promoCode = "PROMOCODE";

        promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setEmissionBudget(BigDecimal.ONE)
                        .setStartDate(new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime())
                        .setEndDate(new GregorianCalendar(2019, Calendar.FEBRUARY, 1).getTime())
                        .setCode(promoCode));

        promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setEmissionBudget(BigDecimal.ONE)
                        .setStartDate(new GregorianCalendar(2019, Calendar.FEBRUARY, 2).getTime())
                        .setEndDate(new GregorianCalendar(2020, Calendar.FEBRUARY, 1).getTime())
                        .setCode(promoCode));
    }

    @Test
    public void shouldUpdatePromocode() {
        final String promocode = promocodeService.generateNewPromocode();
        final PromocodePromoBuilder promoBuilder = PromoUtils.SmartShopping
                .defaultFixedPromocode().setCode(promocode);
        final Promo promocodePromo = promoManager.createPromocodePromo(promoBuilder);

        assertThat(promocodePromo.getActionCode(), equalTo(promocode));
        assertThat(promocodeEntryDao.countByCode(promocode), equalTo(1));

        promoManager.updatePromocodePromo(promoBuilder.setBudget(BigDecimal.ONE));

        assertThat(promocodeService.hasPromocode(promocode), equalTo(true));

        promoManager.updatePromocodePromo(promoBuilder
                .setBudget(BigDecimal.ONE)
                .setEndDate(new GregorianCalendar(2020, Calendar.FEBRUARY, 1).getTime())
        );

        assertThat(promocodeService.hasPromocode(promocode), equalTo(false));

        assertThat(promocodeEntryDao.countByCode(promocode), equalTo(1));
    }

    @Test // MARKET-DISCOUNT-7898
    public void shouldSetFakePromoSourceWhenMatchFlag() {
        var testFlag = "test_fake_flag";
        var promo = promoManager.createCashbackPromo(getDefaultCashbackPromoBuilder(testFlag));
        // PROMO_SOURCE будет установлен по умолчанию - LOYALTY_VALUE
        assertEquals((int) promo.getPromoParam(PromoParameterName.PROMO_SOURCE).get(), LOYALTY_VALUE);

        configurationService.set(CASHBACK_PROMO_FAKE_SOURCE_FLAG_NAME, testFlag);
        var promoBuilder = getDefaultCashbackPromoBuilder(testFlag)
                .setId(promo.getPromoId().getId());
        promoManager.updateCashbackPromo(promoBuilder);
        promo = promoService.getPromo(promo.getPromoId().getId());
        int actual = promo.getPromoParamRequired(PromoParameterName.PROMO_SOURCE);
        // PROMO_SOURCE подменен при обновлении акции
        assertEquals(PARTNER_SOURCE_VALUE, actual);
    }

    @Test
    public void shouldSetDirectPromoPriorityParam() {
        // set directPriority on create cashback promo
        Integer priority = -199;
        var promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.TEN)
                .setPriority(priority));
        assertEquals(priority, promo.getPromoParam(DIRECT_PRIORITY).orElse(null));

        // change directPriority on update cashback promo
        priority = -99;
        var promoBuilder = getDefaultCashbackPromoBuilder("")
                .setId(promo.getPromoId().getId())
                .setPriority(priority);
        promoManager.updateCashbackPromo(promoBuilder);
        promo = promoService.getPromo(promo.getPromoId().getId());
        assertEquals(priority, promo.getPromoParam(DIRECT_PRIORITY).orElse(null));
    }


    private CashbackPromoBuilder getDefaultCashbackPromoBuilder(String experimentFlag) {
        return PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                .addCashbackRule(RuleType.EXPERIMENTS_CUTTING_RULE, RuleParameterName.EXPERIMENTS, experimentFlag)
                .setBillingSchema(BillingSchema.SOLID);
    }

}
