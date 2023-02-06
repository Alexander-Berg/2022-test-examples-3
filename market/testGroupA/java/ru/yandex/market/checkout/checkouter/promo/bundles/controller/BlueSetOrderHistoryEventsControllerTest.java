package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueSetTestBase;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.BLUE_SET;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class BlueSetOrderHistoryEventsControllerTest extends BlueSetTestBase {

    @Test
    public void shouldReturnPromoInfoInEvents() {
        Order order = createTypicalOrderWithBundles();
        assertThat(order.getRgb(), equalTo(Color.BLUE));

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null,
                0, 10
        );

        assertThat(events.getItems(), everyItem(hasProperty("orderAfter", allOf(
                hasProperty("promos", contains(hasProperty("promoDefinition", allOf(
                        hasProperty("type", equalTo(BLUE_SET)),
                        hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY)),
                        hasProperty("anaplanId", is(ANAPLAN_ID))
                )))),
                hasProperty("items", everyItem(
                        allOf(
                                hasProperty("bundleId", nullValue()),
                                hasProperty("promos", contains(hasProperty("promoDefinition", allOf(
                                        hasProperty("type", equalTo(BLUE_SET)),
                                        hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                        hasProperty("bundleId", nullValue()),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID))
                                ))))
                        )
                ))
        ))));
    }

    private Order createTypicalOrderWithBundles() {
        return orderCreateHelper.createOrder(fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(similar(firstOffer))
                .itemBuilder(similar(secondOffer))
                .itemBuilder(similar(thirdOffer))
        ), PROMO_KEY, config ->
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
    }
}
