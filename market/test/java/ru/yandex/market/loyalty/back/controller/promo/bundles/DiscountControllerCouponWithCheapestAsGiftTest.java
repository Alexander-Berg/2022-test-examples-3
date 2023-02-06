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
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.md5;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;

@Deprecated
public class DiscountControllerCouponWithCheapestAsGiftTest extends CheapestAsGiftTestBase {

    private static final BigDecimal INITIAL_CURRENT_BUDGET = BigDecimal.valueOf(700);
    private static final String COUPON_CODE = "SOME COUPON CODE";

    @Autowired
    private PromoManager promoManager;

    @Before
    public void setUp() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(COUPON_CODE)
                        .setBudget(INITIAL_CURRENT_BUDGET)
        );
    }

    @Test
    public void shouldApplyCheapestAsGiftForOneItemWithCouponTogether() {
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
                        .withCoupon(COUPON_CODE)
                        .build()
        );

        assertThat(discountResponse.getCouponError(), nullValue());

        final OrderWithBundlesResponse bundlesResponse = firstOrderOf(discountResponse);
        assertThat(bundlesResponse.getItems(), hasSize(1));

        final BundledOrderItemResponse item = getItemByShopSKU(bundlesResponse, FIRST_ITEM_SSKU);
        assertThat(item.getBundleId(), nullValue());
        assertThat(item.getFeedId(), equalTo(FEED_ID));
        assertThat(item.getOfferId(), notNullValue());
        assertThat(item.getPromos(), hasSize(2));

        final ItemPromoResponse couponPromo = getPromo(item.getPromos(), PromoType.MARKET_COUPON);
        assertThat(couponPromo.getUsedCoin(), nullValue());
        assertThat(couponPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(75)));

        final ItemPromoResponse newPromo = getPromo(item.getPromos(), PromoType.CHEAPEST_AS_GIFT);
        assertThat(newPromo.getUsedCoin(), nullValue());
        assertThat(newPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(250)));
    }

    @Test
    public void shouldApplyCheapestAsGiftForTwoItemsWithCouponTogether() {
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
                        .withCoupon(COUPON_CODE)
                        .build());

        assertThat(discountResponse.getCouponError(), nullValue());

        final OrderWithBundlesResponse bundlesResponse = firstOrderOf(discountResponse);
        assertThat(bundlesResponse.getItems(), hasSize(2));

        final BundledOrderItemResponse firstItem = getItemByShopSKU(bundlesResponse, FIRST_ITEM_SSKU);
        assertThat(firstItem.getFeedId(), equalTo(FEED_ID));
        assertThat(firstItem.getOfferId(), notNullValue());
        assertThat(firstItem.getPromos(), hasSize(2));

        final ItemPromoResponse firstCoinPromo = getPromo(firstItem.getPromos(), PromoType.MARKET_COUPON);
        assertThat(firstCoinPromo.getUsedCoin(), nullValue());
        assertThat(firstCoinPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(75)));

        final ItemPromoResponse firstGiftPromo = getPromo(firstItem.getPromos(), PromoType.CHEAPEST_AS_GIFT);
        assertThat(firstGiftPromo.getUsedCoin(), nullValue());
        assertThat(firstGiftPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(250)));

        final BundledOrderItemResponse secondItem = getItemByShopSKU(bundlesResponse, SECOND_ITEM_SSKU);
        assertThat(secondItem.getBundleId(), nullValue());
        assertThat(secondItem.getFeedId(), equalTo(FEED_ID));
        assertThat(secondItem.getOfferId(), notNullValue());
        assertThat(secondItem.getPromos(), hasSize(2));

        final ItemPromoResponse secondCoinPromo = getPromo(secondItem.getPromos(), PromoType.MARKET_COUPON);
        assertThat(secondCoinPromo.getUsedCoin(), nullValue());
        assertThat(secondCoinPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(75)));

        final ItemPromoResponse secondGiftPromo = getPromo(secondItem.getPromos(), PromoType.CHEAPEST_AS_GIFT);
        assertThat(secondGiftPromo.getUsedCoin(), nullValue());
        assertThat(secondGiftPromo.getDiscount(), comparesEqualTo(BigDecimal.valueOf(250)));
    }
}
