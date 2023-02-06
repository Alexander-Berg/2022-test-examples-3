package ru.yandex.market.loyalty.back.controller.discount;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;

@TestFor(DiscountController.class)
public class DiscountControllerPromocodeWithFiltrationTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final long USER_ID = 2133L;
    public static final String FIRST_SSKU = "FIRST_SSKU";
    public static final String SECOND_SSKU = "SECOND_SSKU";
    public static final String THIRD_SSKU = "THIRD_SSKU";
    public static final String FOURTH_SSKU = "FOURTH_SSKU";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromocodeService promocodeService;

    private String firstPromoCode;
    private Promo firstPromo;
    private Promo secondPromo;

    @Before
    public void setUp() {
        firstPromoCode = promocodeService.generateNewPromocode();
        firstPromo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE_VALUE)
                .setCode(firstPromoCode));

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(firstPromoCode))
                .build());


        var secondPromocode = promocodeService.generateNewPromocode();

        secondPromo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE_VALUE)
                .setCode(secondPromocode));

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(secondPromocode))
                .build());
    }

    @Test
    public void shouldNotApplyPromoCodeForNotParticipatingItems() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(123, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000),
                        promoKeys(firstPromo.getPromoKey(), secondPromo.getPromoKey())
                )
                .withOrderItem(
                        itemKey(124, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(10000),
                        promoKeys(secondPromo.getPromoKey())
                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(firstPromoCode)
                        .build());

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", Matchers.is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", Matchers.is(PromoType.MARKET_PROMOCODE)),
                                hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                        )))
                ),
                allOf(
                        hasProperty("offerId", Matchers.is(SECOND_SSKU)),
                        hasProperty("promos", empty())
                )
        ));

        MultiCartWithBundlesDiscountResponse discountResponseSecondary = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(firstPromoCode)
                        .build());

        assertThat(discountResponseSecondary, notNullValue());
    }

}
