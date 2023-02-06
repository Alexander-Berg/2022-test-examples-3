package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueSetTestBase;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.BLUE_SET;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SECOND_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.THIRD_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class BlueSetCheckoutControllerCheckoutTest extends BlueSetTestBase {

    @Test
    public void shouldCheckoutOnValidPromo() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(firstOffer).price(900))
                .itemBuilder(similar(secondOffer).price(1800))
                .itemBuilder(similar(thirdOffer).price(2700))
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(
                fbyRequestFor(cart, reportOffers, config ->
                        config.expectResponseItems(
                                itemResponseFor(firstOffer)
                                        .quantity(2)
                                        .promo(new ItemPromoResponse(
                                                BigDecimal.valueOf(100),
                                                PromoType.BLUE_SET,
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
                                                BigDecimal.valueOf(200),
                                                PromoType.BLUE_SET,
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
                                itemResponseFor(thirdOffer)
                                        .quantity(2)
                                        .promo(new ItemPromoResponse(
                                                BigDecimal.valueOf(300),
                                                PromoType.BLUE_SET,
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

        assertThat(multiOrder, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiOrder).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(BLUE_SET)),
                                        hasProperty("marketPromoId", is(PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(100)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(BLUE_SET)),
                                        hasProperty("marketPromoId", is(PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(200)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(THIRD_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(BLUE_SET)),
                                        hasProperty("marketPromoId", is(PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(300)))
                        )))
                )
        ));
    }

    @Test
    public void shouldIgnorePriceChangeIfNoLoyaltyDiscount() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(firstOffer).price(900))
                .itemBuilder(similar(secondOffer).price(1800))
                .itemBuilder(similar(thirdOffer).price(2700))
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(
                fbyRequestFor(cart, reportOffers, config ->
                        config.expectResponseItems(
                                itemResponseFor(firstOffer)
                                        .quantity(2),
                                itemResponseFor(secondOffer)
                                        .quantity(2),
                                itemResponseFor(thirdOffer)
                                        .quantity(2)
                        )));

        assertThat(multiOrder, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiOrder).getItems(), everyItem(
                allOf(
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", empty())
                )
        ));
    }

    @Test
    public void shouldIgnorePriceChangeOnInvalidClientPrice() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(firstOffer).price(1000))
                .itemBuilder(similar(secondOffer).price(2000))
                .itemBuilder(similar(thirdOffer).price(3000))
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(
                fbyRequestFor(cart, reportOffers, config ->
                        config.expectResponseItems(
                                itemResponseFor(firstOffer)
                                        .quantity(2)
                                        .promo(new ItemPromoResponse(
                                                BigDecimal.valueOf(100),
                                                PromoType.BLUE_SET,
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
                                                BigDecimal.valueOf(200),
                                                PromoType.BLUE_SET,
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
                                itemResponseFor(thirdOffer)
                                        .quantity(2)
                                        .promo(new ItemPromoResponse(
                                                BigDecimal.valueOf(300),
                                                PromoType.BLUE_SET,
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

        assertThat(multiOrder, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiOrder).getItems(), everyItem(
                allOf(
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(
                                hasProperty(
                                        "promoDefinition",
                                        hasProperty("type", is(BLUE_SET))
                                )
                        ))
                )
        ));
    }
}
