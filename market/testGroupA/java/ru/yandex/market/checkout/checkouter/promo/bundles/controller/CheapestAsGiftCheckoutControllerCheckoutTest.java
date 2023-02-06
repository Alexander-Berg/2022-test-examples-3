package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemRemovalPermission;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.CheapestAsGiftTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class CheapestAsGiftCheckoutControllerCheckoutTest extends CheapestAsGiftTestBase {

    @Test
    void shouldCheckoutOnValidPromo() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(firstOffer))
                .itemBuilder(similar(secondOffer))
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(
                fbyRequestFor(cart, reportOffers, config ->
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

        assertThat(multiOrder, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiOrder).getItems(), hasItems(
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

    @Test
    void itemsWithCheapestAsGiftPromoCannotBeRemovedTest() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(firstOffer))
                .itemBuilder(similar(secondOffer))
                .itemBuilder(similar(offerWithoutPromo))
        );

        Parameters parameters = fbyRequestFor(cart, reportOffers, config ->
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
                ));
        parameters.setExperiments("items_removal_if_missing");
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        Order order = CollectionUtils.extractSingleton(multiOrder.getOrders());

        Set<Long> promoItemIds = order.getItems().stream()
                .filter(item -> item.getPromos().stream().map(ItemPromo::getType).anyMatch(CHEAPEST_AS_GIFT::equals))
                .map(OrderItem::getId)
                .collect(Collectors.toSet());
        assertFalse(promoItemIds.isEmpty());

        Set<Long> nonPromoItemIds = order.getItems().stream()
                .filter(item -> item.getPromos().stream().map(ItemPromo::getType).noneMatch(CHEAPEST_AS_GIFT::equals))
                .map(OrderItem::getId)
                .collect(Collectors.toSet());
        assertFalse(nonPromoItemIds.isEmpty());

        OrderItemsRemovalPermissionResponse removalPermission = client.getOrderItemsRemovalPermissions(order.getId());

        Set<Long> allowedItemIds = removalPermission.getItemRemovalPermissions().stream()
                .filter(OrderItemRemovalPermission::isRemovalAllowed)
                .map(OrderItemRemovalPermission::getItemId)
                .collect(Collectors.toSet());
        assertEquals(allowedItemIds, nonPromoItemIds);

        Set<Long> forbiddenItemIds = removalPermission.getItemRemovalPermissions().stream()
                .filter(itemPermission -> !itemPermission.isRemovalAllowed())
                .map(OrderItemRemovalPermission::getItemId)
                .collect(Collectors.toSet());
        assertEquals(forbiddenItemIds, promoItemIds);
    }
}
