package ru.yandex.market.loyalty.core.dao.promo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

import NMarket.Common.Promo.Promo.ESourceType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.PromocodePromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static java.sql.Timestamp.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.ANAPLAN_ID;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.IGNORE_CHECK_UNIQUE_WHEN_GENERATE_SHOP_PROMO_ID;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

public class PromoDaoOldTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private PromoDaoOld promoDao;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private ClockForTests clock;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void shouldInsertPromoWithPromoParamsAndRulesParams() {
        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .addPromoRule(RuleType.MAX_PROMO_NOMINAL_FILTER_RULE, RuleParameterName.MAX_PROMO_NOMINAL,
                        Set.of(BigDecimal.valueOf(3)))
                .addPromoRule(RuleType.FIRST_ORDER_CUTTING_RULE)
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.LOYALTY_VALUE);

        long promoId = promoDao.insert(promoBuilder.basePromo(), false);

        Promo promo = promoDao.getPromo(promoId);
        assertThat(promo, notNullValue());

        // COUPON_VALUE_TYPE, BUDGET_MODE, COUPON_VALUE, PROMO_SOURCE
        assertThat(promo.getPromoParams().keys(), hasSize(4));
        assertThat(promo.getRulesContainer().getAllRules(), hasSize(5));
    }

    @Test
    public void shouldInsertPromoWithShopPromoId() {
        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.LOYALTY_VALUE);

        long promoId = promoDao.insert(promoBuilder.basePromo(), false);

        Promo promo = promoDao.getPromo(promoId);
        assertThat(promo, notNullValue());

        String promoShopPromoId = promo.getShopPromoId();
        assertThat(promoShopPromoId, equalTo("L" + promoId));
    }

    @Test
    public void shouldNotInsertNotLoyaltyPromocodeWithoutShopPromoId() {
        PromocodePromoBuilder coinPromoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("")
                .setPromoStorageId("")
                .setAnaplanId("")
                .setPromoSource(ESourceType.AFFILIATE_VALUE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> promoDao.insert(coinPromoBuilder.basePromo(), false));

        assertThat(exception.getMessage(), startsWith("Cannot get shopPromoId for promo PROMOCODE "));
        assertThat(exception.getMessage(), endsWith(" with type AFFILIATE"));
    }

    @Test
    public void shouldNotInsertNotLoyaltyCouponWithoutShopPromoId() {
        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .setShopPromoId("")
                .setPromoStorageId("")
                .setAnaplanId("")
                .setPromoSource(ESourceType.AFFILIATE_VALUE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> promoDao.insert(promoBuilder.basePromo(), false));

        assertThat(exception.getMessage(), startsWith("Cannot get shopPromoId for promo COUPON "));
        assertThat(exception.getMessage(), endsWith(" with type AFFILIATE"));
    }

    @Test
    public void shouldInsertNotLoyaltyPromocodeWithoutShopPromoIdWhenConfigIsSet() {
        configurationService.set(ConfigurationService.IGNORE_CHECK_PROMO_TYPE_WHEN_GENERATE_SHOP_PROMO_ID, true);

        PromocodePromoBuilder coinPromoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("")
                .setPromoStorageId("")
                .setAnaplanId("")
                .setPromoSource(ESourceType.AFFILIATE_VALUE);

        long promoId = promoDao.insert(coinPromoBuilder.basePromo(), false);

        Promo promo = promoDao.getPromo(promoId);
        assertThat(promo, notNullValue());

        assertThat(promo.getShopPromoId(), notNullValue());
        assertTrue(promo.getPromoParam(ANAPLAN_ID).isEmpty());
    }

    @Test
    public void shouldNotInsertPromoWithExistedShopPromoId() {
        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.LOYALTY_VALUE)
                .setShopPromoId("THE_SAME");

        long promoId = promoDao.insert(promoBuilder.basePromo(), false);

        promoBuilder.setCouponCode("REFERRAL_CODE2");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> promoDao.insert(promoBuilder.basePromo(), false));

        assertThat(exception.getMessage(), equalTo("Уже существует акция " + promoId + " с shopPromoId THE_SAME"));
    }

    @Test
    public void shouldInsertPromoWhenExistsInactivePromoWithTheSameShopPromoId() {
        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.LOYALTY_VALUE)
                .setShopPromoId("THE_SAME")
                .setStatus(PromoStatus.INACTIVE);

        promoDao.insert(promoBuilder.basePromo(), false);

        promoBuilder.setCouponCode("REFERRAL_CODE2")
                .setStatus(PromoStatus.ACTIVE);

        long secondPromoId = promoDao.insert(promoBuilder.basePromo(), false);

        Promo promo = promoDao.getPromo(secondPromoId);
        assertThat(promo, notNullValue());
        assertThat(promo.getShopPromoId(), equalTo("THE_SAME"));
    }

    @Test
    public void shouldInsertPromoWhenExistsOldPromoWithTheSameShopPromoId() {
        clock.setDate(valueOf("2019-01-09 10:00:00"));
        PromoDaoOld promoDao = new PromoDaoOldImpl(clock, jdbcTemplate, configurationService);

        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.LOYALTY_VALUE)
                .setShopPromoId("THE_SAME");

        promoDao.insert(promoBuilder.basePromo(), false);

        clock.setDate(toDate(LocalDateTime.now()));
        promoBuilder.setCouponCode("REFERRAL_CODE2");

        long secondPromoId = promoDao.insert(promoBuilder.basePromo(), false);

        Promo promo = promoDao.getPromo(secondPromoId);
        assertThat(promo, notNullValue());
        assertThat(promo.getShopPromoId(), equalTo("THE_SAME"));
    }

    @Test
    public void shouldInsertPromoWithExistedActiveShopPromoIdWhenToggle() {
        configurationService.set(IGNORE_CHECK_UNIQUE_WHEN_GENERATE_SHOP_PROMO_ID, true);

        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.LOYALTY_VALUE)
                .setShopPromoId("THE_SAME");

        promoDao.insert(promoBuilder.basePromo(), false);

        promoBuilder.setCouponCode("REFERRAL_CODE2");

        long secondPromoId = promoDao.insert(promoBuilder.basePromo(), false);

        Promo promo = promoDao.getPromo(secondPromoId);
        assertThat(promo, notNullValue());
        assertThat(promo.getShopPromoId(), equalTo("THE_SAME"));
    }

    @Test
    public void shouldNotActivatePromoWhenExistAnotherPromoWithTheSameShopPromoId() {
        var promoBuilderFirst = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.AFFILIATE_VALUE)
                .setShopPromoId("THE_SAME")
                .setStatus(PromoStatus.INACTIVE);

        long promoIdFirst = promoDao.insert(promoBuilderFirst.basePromo(), false);

        var promoBuilderSecond = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE2")
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.AFFILIATE_VALUE)
                .setShopPromoId("THE_SAME")
                .setStatus(PromoStatus.INACTIVE);

        long promoIdSecond = promoDao.insert(promoBuilderSecond.basePromo(), false);

        promoDao.updatePromoStatus(promoBuilderFirst.setId(promoIdFirst).basePromo(), PromoStatus.ACTIVE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> promoDao.updatePromoStatus(promoBuilderFirst.setId(promoIdSecond).basePromo(),
                        PromoStatus.ACTIVE));

        assertThat(exception.getMessage(), equalTo("Уже существует акция " + promoIdFirst + " с shopPromoId THE_SAME"));
    }

    @Test
    public void shouldUpdatePromoParams() {
        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("REFERRAL_CODE")
                .addPromoRule(RuleType.MAX_PROMO_NOMINAL_FILTER_RULE, RuleParameterName.MAX_PROMO_NOMINAL,
                        Set.of(BigDecimal.valueOf(3)))
                .addPromoRule(RuleType.FIRST_ORDER_CUTTING_RULE)
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.LOYALTY_VALUE);

        long promoId = promoDao.insert(promoBuilder.basePromo(), false);

        Promo promo = promoDao.getPromo(promoId);
        assertThat(promo, notNullValue());

        assertThat(promo.getPromoParams(PromoParameterName.BUDGET_MODE), equalTo(Set.of(BudgetMode.SYNC)));
        assertThat(promo.getPromoParam(PromoParameterName.BUDGET_MODE).orElseThrow(),
                equalTo(BudgetMode.SYNC));

        assertThat(promo.getRulesContainer().getAllRules(), hasSize(5));

        promoBuilder.setId(promoId)
                .setPromoKey(promo.getPromoKey())
                .setBudgetMode(BudgetMode.ASYNC)
                .setCouponValue(BigDecimal.valueOf(100), CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.DELIVERY_PARTNER_TYPE_FILTER_RULE);

        Promo promoAfterUpdate = promoBuilder.basePromo();
        promoDao.update(promoAfterUpdate);

        assertThat(promoAfterUpdate.getPromoParams().keys(), hasSize(4));
        assertThat(
                promoAfterUpdate.getPromoParams(PromoParameterName.BUDGET_MODE),
                equalTo(Set.of(BudgetMode.ASYNC))
        );
        assertThat(promoAfterUpdate.getRulesContainer().getAllRules(), hasSize(5));


        Promo updatedPromo = promoDao.getPromo(promoId);
        assertThat(promo, notNullValue());

        // COUPON_VALUE_TYPE, BUDGET_MODE, COUPON_VALUE, PROMO_SOURCE
        assertThat(updatedPromo.getPromoParams().keys(), hasSize(4));
        assertThat(
                updatedPromo.getPromoParams(PromoParameterName.BUDGET_MODE),
                equalTo(Set.of(BudgetMode.ASYNC))
        );
        assertThat(updatedPromo.getRulesContainer().getAllRules(), hasSize(6));
    }

    @Test
    public void shouldNotUpdateShopPromoId() {
        var promoBuilder = PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("COUPON")
                .addPromoRule(RuleType.MAX_PROMO_NOMINAL_FILTER_RULE, RuleParameterName.MAX_PROMO_NOMINAL,
                        Set.of(BigDecimal.valueOf(3)))
                .addPromoRule(RuleType.FIRST_ORDER_CUTTING_RULE)
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                .setBudget(BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .setBudgetMode(BudgetMode.SYNC)
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setPromoSource(ESourceType.LOYALTY_VALUE);

        long promoId = promoDao.insert(promoBuilder.basePromo(), false);

        Promo promo = promoDao.getPromo(promoId);
        assertThat(promo, notNullValue());

        String savedShopPromoId = promo.getShopPromoId();
        assertThat(savedShopPromoId, notNullValue());

        promoBuilder.setId(promoId)
                .setPromoKey(promo.getPromoKey())
                .setBudgetMode(BudgetMode.ASYNC)
                .setShopPromoId("new_shop_promo_id");

        Promo promoUpdate = promoBuilder.basePromo();
        promoDao.update(promoUpdate);

        Promo promoAfterUpdate = promoDao.getPromo(promoId);
        assertThat(promo, notNullValue());

        assertThat(promoAfterUpdate.getShopPromoId(), equalTo(savedShopPromoId));
    }
}
