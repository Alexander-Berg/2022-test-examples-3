package ru.yandex.market.checkout.checkouter.promo.bundles.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.CheapestAsGiftTestBase;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.LoyaltyTestUtils.createTestContext;
import static ru.yandex.market.checkout.test.providers.OrderProvider.orderBuilder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class CheapestAsGiftWithGiftCountLoyaltyServiceTest extends CheapestAsGiftTestBase {

    @Autowired
    private LoyaltyService loyaltyService;

    @Test
    public void shouldCheckPromoKeyAndReturnPromos() {
        OrderItemProvider.OrderItemBuilder firstOffer = OrderItemProvider.orderItemWithSortingCenter()
                .offer(PRIMARY_OFFER)
                .price(600)
                .count(2);

        OrderItemProvider.OrderItemBuilder secondOffer = OrderItemProvider.orderItemWithSortingCenter()
                .offer(GIFT_OFFER)
                .price(300)
                .count(1);
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(firstOffer)
                .itemBuilder(secondOffer)
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectResponseItems(
                        itemResponseFor(firstOffer)
                                .quantity(2)
                                .promo(new ItemPromoResponse(
                                        BigDecimal.valueOf(120),
                                        PromoType.CHEAPEST_AS_GIFT,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                ))
                                .primaryInBundle(true),
                        itemResponseFor(secondOffer)
                                .quantity(1)
                                .promo(new ItemPromoResponse(
                                        BigDecimal.valueOf(60),
                                        PromoType.CHEAPEST_AS_GIFT,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                ))
                                .primaryInBundle(false)
                )
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(firstOffer).primaryInBundle(true), 2)
                        .item(similar(secondOffer).primaryInBundle(false), 1));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(), createTestContext(cart,
                reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", contains(
                                hasProperty("giftCount", comparesEqualTo(BigDecimal.ZERO))
                        ))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", contains(
                                hasProperty("giftCount", comparesEqualTo(BigDecimal.ONE))
                        ))
                )
        ))));
    }
}
