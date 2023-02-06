package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.test.CheapestAsGiftTestBase;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.md5;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixedPromocode;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

public class DiscountControllerPromocodesWithCheapestAsGiftTest extends CheapestAsGiftTestBase {

    private static final long USER_ID = 123;
    private static final String PROMOCODE = "some promocode";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromocodeService promocodeService;

    private CoinKey expectedCoin;

    @Before
    public void setUp() {
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode(PROMOCODE)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        expectedCoin = promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build()).getActivationResults().get(0).getCoinKey();
    }

    @Test
    public void shouldApplyCheapestAsGiftForOneItemWithPromocodeTogether() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, md5(FIRST_ITEM_SSKU)),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(1000),
                        quantity(4)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), is(empty()));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());

        final OrderWithBundlesResponse bundlesResponse = firstOrderOf(discountResponse);
        assertThat(bundlesResponse.getItems(), hasSize(1));

        final BundledOrderItemResponse item = getItemByShopSKU(bundlesResponse, FIRST_ITEM_SSKU);
        assertThat(item.getBundleId(), nullValue());
        assertThat(item.getFeedId(), equalTo(FEED_ID));
        assertThat(item.getOfferId(), notNullValue());
        assertThat(item.getPromos(), hasSize(2));

        final ItemPromoResponse coinPromo = getPromo(item.getPromos(), PromoType.MARKET_PROMOCODE);
        assertThat(coinPromo.getUsedCoin(), notNullValue());
        assertThat(coinPromo.getUsedCoin().getId(), is(expectedCoin.getId()));
        assertThat(coinPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(75)));

        final ItemPromoResponse newPromo = getPromo(item.getPromos(), PromoType.CHEAPEST_AS_GIFT);
        assertThat(newPromo.getUsedCoin(), nullValue());
        assertThat(newPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(250)));
    }

    @Test
    public void shouldApplyCheapestAsGiftForTwoItemsWithPromocodeTogether() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, md5(FIRST_ITEM_SSKU)),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(1000),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(FEED_ID, md5(SECOND_ITEM_SSKU)),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(1000),
                        quantity(2)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), is(empty()));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());

        final OrderWithBundlesResponse bundlesResponse = firstOrderOf(discountResponse);
        assertThat(bundlesResponse.getItems(), hasSize(2));

        final BundledOrderItemResponse firstItem = getItemByShopSKU(bundlesResponse, FIRST_ITEM_SSKU);
        assertThat(firstItem.getFeedId(), equalTo(FEED_ID));
        assertThat(firstItem.getOfferId(), notNullValue());
        assertThat(firstItem.getPromos(), hasSize(2));

        final ItemPromoResponse firstCoinPromo = getPromo(firstItem.getPromos(), PromoType.MARKET_PROMOCODE);
        assertThat(firstCoinPromo.getUsedCoin(), notNullValue());
        assertThat(firstCoinPromo.getUsedCoin().getId(), is(expectedCoin.getId()));
        assertThat(firstCoinPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(75)));

        final ItemPromoResponse firstGiftPromo = getPromo(firstItem.getPromos(), PromoType.CHEAPEST_AS_GIFT);
        assertThat(firstGiftPromo.getUsedCoin(), nullValue());
        assertThat(firstGiftPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(250)));

        final BundledOrderItemResponse secondItem = getItemByShopSKU(bundlesResponse, SECOND_ITEM_SSKU);
        assertThat(secondItem.getBundleId(), nullValue());
        assertThat(secondItem.getFeedId(), equalTo(FEED_ID));
        assertThat(secondItem.getOfferId(), notNullValue());
        assertThat(secondItem.getPromos(), hasSize(2));

        final ItemPromoResponse secondCoinPromo = getPromo(secondItem.getPromos(), PromoType.MARKET_PROMOCODE);
        assertThat(secondCoinPromo.getUsedCoin(), notNullValue());
        assertThat(secondCoinPromo.getUsedCoin().getId(), is(expectedCoin.getId()));
        assertThat(secondCoinPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(75)));

        final ItemPromoResponse secondGiftPromo = getPromo(secondItem.getPromos(), PromoType.CHEAPEST_AS_GIFT);
        assertThat(secondGiftPromo.getUsedCoin(), nullValue());
        assertThat(secondGiftPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(250)));
    }
}
