package ru.yandex.market.loyalty.core.service;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.model.ids.CategoryId;
import ru.yandex.market.loyalty.core.model.ids.MskuId;
import ru.yandex.market.loyalty.core.model.ids.PromoId;
import ru.yandex.market.loyalty.core.model.ids.VendorId;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.model.action.PromoActionParameterName.STATIC_PERK_NAME;
import static ru.yandex.market.loyalty.core.model.action.PromoActionType.STATIC_PERK_ADDITION_ACTION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_CREATION;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.NOT_LOGIC;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.VENDOR_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.VENDOR_FILTER_RULE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.FIRST_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PARENT_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.SECOND_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.getActionsMapWithStaticPerkAddition;

public class CashbackCacheServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final String SOME_MSKU = "someMsku";
    private static final String ANOTHER_MSKU = "someMsku2";
    private static final long SOME_VENDOR = 123L;
    private static final long ANOTHER_VENDOR = 124L;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void shouldFindAllPromosWithNotLogicSetFalse() {
        Promo promo1 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(PARENT_CATEGORY_ID))
        );
        Promo promo2 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, ImmutableSet.of(SOME_MSKU))
        );
        Promo promo3 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(VENDOR_FILTER_RULE, VENDOR_ID, ImmutableSet.of(SOME_VENDOR))
        );

        cashbackCacheService.reloadCashbackPromos();

        final Set<PromoId> promos = cashbackCacheService.findPromos(
                new MskuId(SOME_MSKU),
                new CategoryId(SECOND_CHILD_CATEGORY_ID),
                new VendorId(SOME_VENDOR)
        );

        assertEquals(ImmutableSet.of(
                new PromoId(promo1.getId()),
                new PromoId(promo2.getId()),
                new PromoId(promo3.getId())
        ), promos);
    }

    @Test
    public void shouldFindAllPromosForNullVendorIdWithNotLogicSetFalse() {
        Promo promo1 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(PARENT_CATEGORY_ID))
        );
        Promo promo2 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, ImmutableSet.of(SOME_MSKU))
        );
        Promo promo3 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(VENDOR_FILTER_RULE, VENDOR_ID, ImmutableSet.of(SOME_VENDOR))
        );

        cashbackCacheService.reloadCashbackPromos();

        final Set<PromoId> promos = cashbackCacheService.findPromos(
                new MskuId(SOME_MSKU),
                new CategoryId(SECOND_CHILD_CATEGORY_ID),
                new VendorId(null)
        );

        assertEquals(ImmutableSet.of(
                new PromoId(promo1.getId()),
                new PromoId(promo2.getId()),
                new PromoId(promo3.getId())
        ), promos);
    }

    @Test
    public void shouldFindSinglePromoWithNotLogicSetFalse() {
        Promo promo1 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(PARENT_CATEGORY_ID))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, ImmutableSet.of(SOME_MSKU))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(VENDOR_FILTER_RULE, VENDOR_ID, ImmutableSet.of(SOME_VENDOR))
        );

        cashbackCacheService.reloadCashbackPromos();

        final Set<PromoId> promos = cashbackCacheService.findPromos(
                new MskuId(ANOTHER_MSKU),
                new CategoryId(FIRST_CHILD_CATEGORY_ID),
                new VendorId(ANOTHER_VENDOR)
        );

        assertEquals(
                ImmutableSet.of(new PromoId(promo1.getId())),
                promos
        );
    }

    @Test
    public void shouldNotFindPromoWithNotLogicSetFalse() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(SECOND_CHILD_CATEGORY_ID))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(MSKU_FILTER_RULE, MSKU_ID, ImmutableSet.of(SOME_MSKU))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(VENDOR_FILTER_RULE, VENDOR_ID, ImmutableSet.of(SOME_VENDOR))
        );

        cashbackCacheService.reloadCashbackPromos();

        final Set<PromoId> promos = cashbackCacheService.findPromos(
                new MskuId(ANOTHER_MSKU),
                new CategoryId(FIRST_CHILD_CATEGORY_ID),
                new VendorId(ANOTHER_VENDOR)
        );

        assertThat(
                promos,
                is(empty())
        );
    }

    @Test
    public void shouldFindAllPromosWithNotLogicSetTrue() {
        final Promo promo1 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(CATEGORY_FILTER_RULE)
                                .withParams(CATEGORY_ID, ImmutableSet.of(FIRST_CHILD_CATEGORY_ID))
                                .withSingleParam(NOT_LOGIC, true))
        );
        final Promo promo2 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(MSKU_FILTER_RULE)
                                .withParams(MSKU_ID, ImmutableSet.of(SOME_MSKU))
                                .withSingleParam(NOT_LOGIC, true))
        );
        final Promo promo3 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(VENDOR_FILTER_RULE)
                                .withParams(VENDOR_ID, ImmutableSet.of(SOME_VENDOR))
                                .withSingleParam(NOT_LOGIC, true))
        );

        cashbackCacheService.reloadCashbackPromos();

        final Set<PromoId> promos = cashbackCacheService.findPromos(
                new MskuId(ANOTHER_MSKU),
                new CategoryId(SECOND_CHILD_CATEGORY_ID),
                new VendorId(ANOTHER_VENDOR)
        );

        assertEquals(ImmutableSet.of(
                new PromoId(promo1.getId()),
                new PromoId(promo2.getId()),
                new PromoId(promo3.getId())
        ), promos);
    }

    @Test
    public void shouldFindSinglePromoWithNotLogicSetTrue() {
        final Promo promo1 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(CATEGORY_FILTER_RULE)
                                .withParams(CATEGORY_ID, ImmutableSet.of(FIRST_CHILD_CATEGORY_ID))
                                .withSingleParam(NOT_LOGIC, true))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(MSKU_FILTER_RULE)
                                .withParams(MSKU_ID, ImmutableSet.of(SOME_MSKU))
                                .withSingleParam(NOT_LOGIC, true))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(VENDOR_FILTER_RULE)
                                .withParams(VENDOR_ID, ImmutableSet.of(SOME_VENDOR))
                                .withSingleParam(NOT_LOGIC, true))
        );

        cashbackCacheService.reloadCashbackPromos();

        final Set<PromoId> promos = cashbackCacheService.findPromos(
                new MskuId(SOME_MSKU),
                new CategoryId(SECOND_CHILD_CATEGORY_ID),
                new VendorId(SOME_VENDOR)
        );

        assertEquals(ImmutableSet.of(new PromoId(promo1.getId())), promos);
    }

    @Test
    public void shouldNotFindPromoWithNotLogicSetTrue() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(CATEGORY_FILTER_RULE)
                                .withParams(CATEGORY_ID, ImmutableSet.of(FIRST_CHILD_CATEGORY_ID))
                                .withSingleParam(NOT_LOGIC, true))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(MSKU_FILTER_RULE)
                                .withParams(MSKU_ID, ImmutableSet.of(SOME_MSKU))
                                .withSingleParam(NOT_LOGIC, true))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(VENDOR_FILTER_RULE)
                                .withParams(VENDOR_ID, ImmutableSet.of(SOME_VENDOR))
                                .withSingleParam(NOT_LOGIC, true))
        );

        cashbackCacheService.reloadCashbackPromos();

        final Set<PromoId> promos = cashbackCacheService.findPromos(
                new MskuId(SOME_MSKU),
                new CategoryId(FIRST_CHILD_CATEGORY_ID),
                new VendorId(SOME_VENDOR)
        );

        assertThat(
                promos,
                is(empty())
        );
    }

    @Test
    public void shouldCacheExtraCashbackPromos() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EXTRA_CASHBACK)))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
        );

        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();

        assertThat(
                cashbackCacheService.getExtraCashbackPromoIds(),
                contains(equalTo(extraCashbackPromo.getId()))
        );
    }

    @Test
    public void shouldCacheExtraEmployeeCashbackPromos() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EMPLOYEE_EXTRA_CASHBACK)))
        );
        configurationService.set(ConfigurationService.YANDEX_EMPLOYEE_EXTRA_CASHBACK_PROMO_ID,
                extraCashbackPromo.getId());
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
        );

        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();

        assertThat(
                cashbackCacheService.getYandexEmployeeExtraCashbackPromoId(),
                equalTo(Optional.of(extraCashbackPromo.getId()))
        );
    }

    @Test
    public void shouldCacheExtraPharmaCashbackPromos() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EXTRA_PHARMA_CASHBACK)))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
        );

        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();

        assertThat(
                cashbackCacheService.getExtraCashbackPromoIds(),
                contains(extraCashbackPromo.getId())
        );
    }

    @Test
    public void shouldSaveAndGetPromoActionForCashbackProps() {
        var testStaticPerkName = "test_perk";
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.ONE)
                        .setBudget(BigDecimal.TEN)
                        .setEndDate(Date.from(clock.dateTime().plusDays(2).toInstant(ZoneOffset.UTC)))
                        .setCashbackActionsMap(getActionsMapWithStaticPerkAddition(testStaticPerkName, ORDER_CREATION))
        );

        var actionContainers = cashbackCacheService.getCashbackPropsOrEmpty(promo.getPromoId()).get()
                .getPromoActionsMap().getAllContainers();

        assertThat(actionContainers, hasSize(1));
        assertThat(actionContainers,
                hasItem(allOf(
                        hasProperty("actionType", equalTo(STATIC_PERK_ADDITION_ACTION)),
                        hasProperty("orderStage", equalTo(ORDER_CREATION))
                ))
        );
        assertThat(actionContainers.get(0).getSingleParamRequired(STATIC_PERK_NAME), equalTo(testStaticPerkName));
    }
}
