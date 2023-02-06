package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.CheapestAsGiftTestBase;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.CHEAPEST_AS_GIFT;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class CheapestAsGiftCheckoutControllerCartTest extends CheapestAsGiftTestBase {

    @Test
    public void shouldBeNoChangesOnValidPromo() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(firstOffer))
                .itemBuilder(similar(secondOffer))
        );

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
                config.expectResponseItems(
                        itemResponseFor(firstOffer)
                                .quantity(2)
                                .promo(new ItemPromoResponse(
                                        BigDecimal.valueOf(250),
                                        PromoType.CHEAPEST_AS_GIFT,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        ANAPLAN_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                        itemResponseFor(secondOffer)
                                .quantity(2)
                                .promo(new ItemPromoResponse(
                                        BigDecimal.valueOf(250),
                                        PromoType.CHEAPEST_AS_GIFT,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        ANAPLAN_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                ))
                )));

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", nullValue()));
        assertThat(firstOrder(multiCart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(CHEAPEST_AS_GIFT)),
                                        hasProperty("marketPromoId", is(PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(250)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(CHEAPEST_AS_GIFT)),
                                        hasProperty("marketPromoId", is(PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(250)))
                        )))
                )
        ));
    }
}
