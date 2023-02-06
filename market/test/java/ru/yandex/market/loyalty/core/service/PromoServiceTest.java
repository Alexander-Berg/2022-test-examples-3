package ru.yandex.market.loyalty.core.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestDao;
import ru.yandex.market.loyalty.core.model.GenericParam;
import ru.yandex.market.loyalty.core.model.promo.CorePromoType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.SingleUseCouponPromoBuilder;
import ru.yandex.market.loyalty.core.rule.ParamsContainer;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.PROMO_SOURCE;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.SHOP_ID;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 01.06.17
 */
public class PromoServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoService promoService;
    @Autowired
    BunchGenerationRequestDao bunchGenerationRequestDao;

    @Test
    public void promoWithEndDateInFuture() {
        final String promoName = "test";
        SingleUseCouponPromoBuilder promo = PromoUtils.Coupon.defaultSingleUse()
                .setStartDate(new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime())
                .setEndDate(new GregorianCalendar(2030, Calendar.FEBRUARY, 1).getTime())
                .setName(promoName);
        promoService.addPromo(promo);

        assertThat(promoService.getAllPromos(true), hasItem(hasProperty("name", is(promoName))));
    }

    @Test
    public void promoWithEndDateInPast() {
        final String promoName = "test";
        SingleUseCouponPromoBuilder promo = PromoUtils.Coupon.defaultSingleUse()
                .setStartDate(new GregorianCalendar(1900, Calendar.FEBRUARY, 1).getTime())
                .setEndDate(new GregorianCalendar(1910, Calendar.FEBRUARY, 1).getTime());
        promoService.addPromo(promo);

        assertThat(promoService.getAllPromos(true), not(hasItem(hasProperty("name", is(promoName)))));
    }

    @Test
    public void promoWithEndDateToday() {
        final String promoName = "test";
        SingleUseCouponPromoBuilder promo = PromoUtils.Coupon.defaultSingleUse()
                .setName(promoName)
                .setStartDate(new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime())
                .setEndDate(Date.from(clock.instant()));
        promoService.addPromo(promo);

        assertThat(promoService.getAllPromos(true), hasItem(hasProperty("name", is(promoName))));

        clock.spendTime(1, ChronoUnit.DAYS);

        assertThat(promoService.getAllPromos(true), not(hasItem(hasProperty("name", is(promoName)))));
    }

    @Test
    public void shouldGetPromoIdByShopPromoId() {
        long promoId = promoService.addPromo(PromoUtils.SmartShopping.defaultFreeDelivery()
                .setActionCodeInternal("somePromoAlias"));

        Assert.assertEquals(promoId, (long) promoService.getPromoIdByShopPromoId("L" + promoId));
    }

    @Test
    public void shouldGetPromoParamsWithNames() {
        long promoId = promoService.addPromo(PromoUtils.SmartShopping.defaultFreeDelivery()
                .setActionCodeInternal("somePromoAlias"));
        promoService.setPromoParam(promoId, PROMO_SOURCE, 67);
        promoService.setPromoParam(promoId, SHOP_ID, 49L);

        ParamsContainer<PromoParameterName<?>> promoParams = promoService.getPromoParams(promoId,
                ImmutableSet.of(PROMO_SOURCE, SHOP_ID));
        Optional<Integer> promoSource =
                ParamsContainer.getSingleParam(promoParams, PROMO_SOURCE).map(GenericParam::value);
        Optional<Long> shopId = ParamsContainer.getSingleParam(promoParams, SHOP_ID).map(GenericParam::value);

        Assert.assertEquals(Integer.valueOf(67), promoSource.get());
        Assert.assertEquals(Long.valueOf(49), shopId.get());
    }

    @Test
    public void shouldGetActivePromosWithExceededBudgetByThreshold() {
        long id = promoService.addPromo(PromoUtils.SmartShopping.defaultFixedPromocode(new BigDecimal(100))
                .setCode("PROMOCODE")
                .setBudget(new BigDecimal(1000)), new BigDecimal(10000), false, false);
        Promo promo = promoService.getPromo(id);

        id = promoService.addPromo(PromoUtils.SmartShopping.defaultFixedPromocode(new BigDecimal(100))
                .setCode("PROMOCODE_1")
                .setBudget(new BigDecimal(100000)), new BigDecimal(10000), false, false);

        Promo promoWithActiveBudget = promoService.getPromo(id);

        assertThat(promoWithActiveBudget, notNullValue());
        configurationService.set("market.loyalty.config.disable.by.budget.threshold.enabled", Boolean.TRUE);
        List<Promo> promoResult = promoService.getActivePromocodeCoinPromoWithExceededBudget();

        promoResult.get(0).equals(promo);

        assertThat(promoResult, hasItem(promo));
        assertThat(promoResult, not(hasItem(promoWithActiveBudget)));
        assertThat(promoResult, hasSize(1));
    }

    @Test
    public void shouldNotGetActivePromosWithExceededBudgetByThresholdWithNullThreshold() {
        promoService.addPromo(PromoUtils.SmartShopping.defaultFixedPromocode(new BigDecimal(100))
                .setCode("PROMOCODE"));

        List<Promo> promoResult = promoService.getActivePromocodeCoinPromoWithExceededBudget();
        assertThat(promoResult, empty());
    }

    @Test
    public void shouldExportPromosWithStartsWithinMode() {
        long id1 = promoService.addPromo(PromoUtils.SmartShopping.defaultFixed(new BigDecimal(100))
                .setStartDate(Date.from(clock.instant()))
                .setEndDate(Date.from(clock.instant().plus(30, ChronoUnit.DAYS))));
        long id2 = promoService.addPromo(PromoUtils.SmartShopping.defaultFixed(new BigDecimal(50))
                .setStartDate(Date.from(clock.instant().plus(15, ChronoUnit.DAYS)))
                .setEndDate(Date.from(clock.instant().plus(45, ChronoUnit.DAYS))));

        List<Promo> result1 = promoService.getPromosByDates(
                Date.from(clock.instant().minus(1, ChronoUnit.DAYS)),
                Date.from(clock.instant().plus(14, ChronoUnit.DAYS)),
                PromoSearchMode.STARTS_WITHIN,
                EnumSet.of(CorePromoType.SMART_SHOPPING)
        );
        List<Promo> result2 = promoService.getPromosByDates(
                Date.from(clock.instant().plus(14, ChronoUnit.DAYS)),
                Date.from(clock.instant().plus(45, ChronoUnit.DAYS)),
                PromoSearchMode.STARTS_WITHIN,
                EnumSet.of(CorePromoType.SMART_SHOPPING)
        );

        assertThat(result1, contains(hasProperty("id", equalTo(id1))));
        assertThat(result2, contains(hasProperty("id", equalTo(id2))));
    }

    @Test
    public void shouldExportPromosWithEndsWithinMode() {
        long id1 = promoService.addPromo(PromoUtils.SmartShopping.defaultFixed(new BigDecimal(100))
                .setStartDate(Date.from(clock.instant()))
                .setEndDate(Date.from(clock.instant().plus(30, ChronoUnit.DAYS))));
        long id2 = promoService.addPromo(PromoUtils.SmartShopping.defaultFixed(new BigDecimal(50))
                .setStartDate(Date.from(clock.instant().plus(15, ChronoUnit.DAYS)))
                .setEndDate(Date.from(clock.instant().plus(45, ChronoUnit.DAYS))));

        List<Promo> result1 = promoService.getPromosByDates(
                Date.from(clock.instant().minus(1, ChronoUnit.DAYS)),
                Date.from(clock.instant().plus(14, ChronoUnit.DAYS)),
                PromoSearchMode.ENDS_WITHIN,
                EnumSet.of(CorePromoType.SMART_SHOPPING)
        );
        List<Promo> result2 = promoService.getPromosByDates(
                Date.from(clock.instant().plus(14, ChronoUnit.DAYS)),
                Date.from(clock.instant().plus(45, ChronoUnit.DAYS)),
                PromoSearchMode.ENDS_WITHIN,
                EnumSet.of(CorePromoType.SMART_SHOPPING)
        );

        assertThat(result1, empty());
        assertThat(result2, containsInAnyOrder(
                hasProperty("id", equalTo(id1)), hasProperty("id", equalTo(id2))));
    }

    @Test
    public void shouldExportPromosWithStrongMode() {
        long id1 = promoService.addPromo(PromoUtils.SmartShopping.defaultFixed(new BigDecimal(100))
                .setStartDate(Date.from(clock.instant()))
                .setEndDate(Date.from(clock.instant().plus(30, ChronoUnit.DAYS))));
        long id2 = promoService.addPromo(PromoUtils.SmartShopping.defaultFixed(new BigDecimal(50))
                .setStartDate(Date.from(clock.instant().plus(15, ChronoUnit.DAYS)))
                .setEndDate(Date.from(clock.instant().plus(45, ChronoUnit.DAYS))));

        List<Promo> result1 = promoService.getPromosByDates(
                Date.from(clock.instant()),
                Date.from(clock.instant().plus(31, ChronoUnit.DAYS)),
                PromoSearchMode.STRONG,
                EnumSet.of(CorePromoType.SMART_SHOPPING)
        );
        List<Promo> result2 = promoService.getPromosByDates(
                Date.from(clock.instant().plus(15, ChronoUnit.DAYS)),
                Date.from(clock.instant().plus(46, ChronoUnit.DAYS)),
                PromoSearchMode.STRONG,
                EnumSet.of(CorePromoType.SMART_SHOPPING)
        );

        assertThat(result1, contains(hasProperty("id", equalTo(id1))));
        assertThat(result2, contains(hasProperty("id", equalTo(id2))));
    }

    @Test
    public void shouldExportPromosWithSoftMode() {
        long id1 = promoService.addPromo(PromoUtils.SmartShopping.defaultFixed(new BigDecimal(100))
                .setStartDate(Date.from(clock.instant()))
                .setEndDate(Date.from(clock.instant().plus(30, ChronoUnit.DAYS))));
        long id2 = promoService.addPromo(PromoUtils.SmartShopping.defaultFixed(new BigDecimal(50))
                .setStartDate(Date.from(clock.instant().plus(15, ChronoUnit.DAYS)))
                .setEndDate(Date.from(clock.instant().plus(45, ChronoUnit.DAYS))));

        List<Promo> result1 = promoService.getPromosByDates(
                Date.from(clock.instant().minus(1, ChronoUnit.DAYS)),
                Date.from(clock.instant().plus(16, ChronoUnit.DAYS)),
                PromoSearchMode.SOFT,
                EnumSet.of(CorePromoType.SMART_SHOPPING)
        );

        assertThat(result1, containsInAnyOrder(
                hasProperty("id", equalTo(id1)), hasProperty("id", equalTo(id2))));
    }
}
