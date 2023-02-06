package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackPromoResponse;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.ydb.CashbackOrdersDao;
import ru.yandex.market.loyalty.core.model.GenericParam;
import ru.yandex.market.loyalty.core.model.ids.PromoId;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.discount.ThresholdId;
import ru.yandex.market.loyalty.core.service.threshold.PromoThreshold;
import ru.yandex.market.loyalty.core.service.threshold.PromoThresholdService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.loyalty.core.model.threshold.PromoThresholdParamName.THRESHOLD_VALUE;
import static ru.yandex.market.loyalty.core.model.threshold.PromoThresholdStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.threshold.PromoThresholdType.MAX_TOTAL_CASHBACK;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerThresholdCashbackTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    public CashbackOrdersDao cashbackOrdersDao;
    @Autowired
    public PromoThresholdService promoThresholdService;

    @Test
    public void shouldReduceItemLevelAndMultiLevelCashback() {
        Promo firstPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(8))
                        .setPromoBucketName("base")
        );
        Promo secondPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(12))
                        .setPromoBucketName("plus")
        );
        Promo thirdPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(3), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("meta")
        );

        promoThresholdService.addThreshold(PromoThreshold.builder()
                .setName("system_global")
                .setStatus(ACTIVE)
                .setType(MAX_TOTAL_CASHBACK)
                .setParams(Map.of(THRESHOLD_VALUE, GenericParam.of(BigDecimal.valueOf(1000))))
                .build());

        promoThresholdService.updatePromoThresholds(PromoId.of(firstPromo.getId()), Set.of(ThresholdId.of(
                "system_global")));
        promoThresholdService.updatePromoThresholds(PromoId.of(secondPromo.getId()), Set.of(ThresholdId.of(
                "system_global")));
        promoThresholdService.updatePromoThresholds(PromoId.of(thirdPromo.getId()), Set.of(ThresholdId.of(
                "system_global")));

        configureCashback();

        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder
                                .builder(orderRequestWithBundlesBuilder()
                                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(10_000))
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .build())
                                .build()
                );

        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getType(),
                equalTo(CashbackPermision.ALLOWED));
        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(1000)));
        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getPromos().size(), equalTo(1));
//        пофиксить в рамках https://st.yandex-team.ru/MARKETDISCOUNT-7375
//        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getPromos().get(0).getAmount(),
//        comparesEqualTo(BigDecimal.valueOf(130)));

        OrderWithBundlesResponse orderWithBundlesResponse = discountResponse.getOrders().get(0);
        MatcherAssert.assertThat(orderWithBundlesResponse.getCashback().getEmit().getType(),
                equalTo(CashbackPermision.ALLOWED));
//        пофиксить в рамках https://st.yandex-team.ru/MARKETDISCOUNT-7375
//        MatcherAssert.assertThat(orderWithBundlesResponse.getCashback().getEmit().getAmount(), comparesEqualTo
//        (BigDecimal.valueOf(870)));

        BundledOrderItemResponse bundledOrderItemResponse = orderWithBundlesResponse.getItems().get(0);
        MatcherAssert.assertThat(bundledOrderItemResponse.getCashback().getEmit().getType(),
                equalTo(CashbackPermision.ALLOWED));
//        пофиксить в рамках https://st.yandex-team.ru/MARKETDISCOUNT-7375
//        MatcherAssert.assertThat(bundledOrderItemResponse.getCashback().getEmit().getAmount(), comparesEqualTo
//        (BigDecimal.valueOf(869)));
        MatcherAssert.assertThat(bundledOrderItemResponse.getCashback().getEmit().getPromos().size(), equalTo(2));

        for (CashbackPromoResponse promo : bundledOrderItemResponse.getCashback().getEmit().getPromos()) {
            long amount = promo.getAmount().longValue();
            assertThat(amount, oneOf(
                    521L,
                    522L,
                    348L,
                    349L,
                    347L
            ));
        }
    }

    @Test
    public void shouldReduceItemLevelCashbackForPromoWithThresholdOnly() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(8))
                        .setPromoBucketName("base")
        );
        Promo secondPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(12))
                        .setPromoBucketName("plus")
        );

        promoThresholdService.addThreshold(PromoThreshold.builder()
                .setName("system_global")
                .setStatus(ACTIVE)
                .setType(MAX_TOTAL_CASHBACK)
                .setParams(Map.of(THRESHOLD_VALUE, GenericParam.of(BigDecimal.valueOf(1000))))
                .build());

        promoThresholdService.updatePromoThresholds(PromoId.of(secondPromo.getId()), Set.of(ThresholdId.of(
                "system_global")));

        configureCashback();


        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder
                                .builder(orderRequestWithBundlesBuilder()
                                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(10_000))
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .build())
                                .build()
                );

        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getType(),
                equalTo(CashbackPermision.ALLOWED));
        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(1800)));
        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getPromos().size(), equalTo(0));

        OrderWithBundlesResponse orderWithBundlesResponse = discountResponse.getOrders().get(0);
        MatcherAssert.assertThat(orderWithBundlesResponse.getCashback().getEmit().getType(),
                equalTo(CashbackPermision.ALLOWED));
        MatcherAssert.assertThat(orderWithBundlesResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(1800)));

        BundledOrderItemResponse bundledOrderItemResponse = orderWithBundlesResponse.getItems().get(0);
        MatcherAssert.assertThat(bundledOrderItemResponse.getCashback().getEmit().getType(),
                equalTo(CashbackPermision.ALLOWED));
        MatcherAssert.assertThat(bundledOrderItemResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(1800)));
        MatcherAssert.assertThat(bundledOrderItemResponse.getCashback().getEmit().getPromos().size(), equalTo(2));
        MatcherAssert.assertThat(bundledOrderItemResponse.getCashback().getEmit().getPromos(), containsInAnyOrder(
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(1000))),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(800)))
        ));
    }

    private void configureCashback() {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.refreshThresholds();
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
    }
}
