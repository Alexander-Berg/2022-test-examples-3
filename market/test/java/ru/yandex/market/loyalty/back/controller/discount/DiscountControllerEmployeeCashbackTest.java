package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.OrderItemResponse;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.ydb.CashbackOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.CashbackOrder;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.staff.StaffCacheService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_CASHBACK;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.cashback.CashbackService.EMPLOYEE_MONTH_LIMIT;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerEmployeeCashbackTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private StaffCacheService staffCacheService;
    @Autowired
    public CashbackOrdersDao cashbackOrdersDao;

    @Test
    public void shouldReduceOverlappingEmployeeCashback() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EMPLOYEE_EXTRA_CASHBACK)))
        );
        final Promo defaultPromo =
                promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE));

        configureExtraCashback(extraCashbackPromo);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.TEN));
        assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmountByPromoKey().get(extraCashbackPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(9)));
        assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmountByPromoKey().get(defaultPromo.getPromoKey()), comparesEqualTo(BigDecimal.ONE));
    }

    @Test
    public void shouldReduceMaxAndOverlappingEmployeeCashback() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EMPLOYEE_EXTRA_CASHBACK)))
                        .addCashbackRule(RuleType.MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(3000))
        );
        final Promo alfaPromo =
                promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(2))
                        .setPromoBucketName("alfa"));
        final Promo betaPromo =
                promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(3))
                        .setPromoBucketName("beta"));

        configureExtraCashback(extraCashbackPromo);


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(25000), quantity(2))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(3000)));
        assertThat(getItemEmitCashback(discountResponse, DEFAULT_ITEM_KEY).getAmountByPromoKey().get(extraCashbackPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(500)));
        assertThat(getItemEmitCashback(discountResponse, DEFAULT_ITEM_KEY).getAmountByPromoKey().get(alfaPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(1000)));
        assertThat(getItemEmitCashback(discountResponse, DEFAULT_ITEM_KEY).getAmountByPromoKey().get(betaPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(1500)));
    }

    @Test
    public void shouldReduceMaxAndOverlappingEmployeeCashback2() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EMPLOYEE_EXTRA_CASHBACK)))
                        .addCashbackRule(RuleType.MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(3000))
        );
        final Promo defaultPromo =
                promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5)));

        mockCashbackTotal(extraCashbackPromo.getPromoKey(), EMPLOYEE_MONTH_LIMIT);
        configureExtraCashback(extraCashbackPromo);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(25000), quantity(2))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(2500)));
        assertThat(getItemEmitCashback(discountResponse, DEFAULT_ITEM_KEY).getAmountByPromoKey().get(defaultPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(2500)));
    }

    @Test
    public void shouldReduceMaxAndOverlappingEmployeeCashback3() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EMPLOYEE_EXTRA_CASHBACK)))
                        .addCashbackRule(RuleType.MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(3000))
        );
        final Promo defaultPromo =
                promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5)));

        mockCashbackTotal(extraCashbackPromo.getPromoKey(), EMPLOYEE_MONTH_LIMIT.subtract(BigDecimal.valueOf(300)));
        configureExtraCashback(extraCashbackPromo);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(2500), quantity(2))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(300)));
        assertThat(getItemEmitCashback(discountResponse, DEFAULT_ITEM_KEY).getAmountByPromoKey().get(extraCashbackPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(50)));
        assertThat(getItemEmitCashback(discountResponse, DEFAULT_ITEM_KEY).getAmountByPromoKey().get(defaultPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(250)));
    }

    @Test
    public void shouldUseCashbackTotalThresholdForExtraEmployeeCashback() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EMPLOYEE_EXTRA_CASHBACK)))
                        .addCashbackRule(RuleType.MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(3000))
        );

        mockCashbackTotal(extraCashbackPromo.getPromoKey(), EMPLOYEE_MONTH_LIMIT.subtract(BigDecimal.valueOf(300)));
        configureExtraCashback(extraCashbackPromo);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(2500), quantity(2))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(300)));
        assertThat(getItemEmitCashback(discountResponse, DEFAULT_ITEM_KEY).getAmountByPromoKey().get(extraCashbackPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(300)));
    }

    @Test
    public void shouldChooseStandardCashback() {
        final Promo extraCashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.ONE)
                        .addCashbackRule(RuleContainer.builder(PERKS_ALLOWED_CUTTING_RULE)
                                .withParams(PERK_TYPE, ImmutableSet.of(PerkType.YANDEX_EMPLOYEE_EXTRA_CASHBACK)))
        );
        final Promo defaultPromo =
                promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        configureExtraCashback(extraCashbackPromo);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.TEN));
        assertThat(getItemEmitCashback(discountResponse, DEFAULT_ITEM_KEY).getAmountByPromoKey().get(defaultPromo.getPromoKey()), comparesEqualTo(BigDecimal.TEN));
    }

    private static CashbackOptions getItemEmitCashback(MultiCartWithBundlesDiscountResponse discountResponse,
                                                       ItemKey key) {
        return discountResponse
                .getOrders()
                .get(0)
                .getItems().stream()
                .filter(i -> i.getFeedId().equals(key.getFeedId()) && i.getOfferId().equals(key.getOfferId()))
                .findFirst()
                .map(OrderItemResponse::getCashback)
                .map(CashbackResponse::getEmit)
                .orElseThrow();
    }

    private void mockCashbackTotal(String promoKey, BigDecimal amount) {
        when(cashbackOrdersDao.selectByUidWithBFCondition(
                eq(DEFAULT_UID),
                eq(promoKey),
                any()
        ))
                .thenReturn(Collections.singletonList(new CashbackOrder(DEFAULT_UID,
                        OrderStatus.DELIVERED.toString(),
                        clock.instant(),
                        ANOTHER_ORDER_ID,
                        promoKey,
                        amount
                )));
    }

    private void configureExtraCashback(Promo extraCashbackPromo) {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EMPLOYEE_EXTRA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EMPLOYEE_EXTRA_CASHBACK_PROMO_ID,
                extraCashbackPromo.getId());
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_UIDS_SET, DEFAULT_UID);
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        staffCacheService.reset(Set.of(DEFAULT_UID));
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
    }
}
