package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.helpers.BundleOrderHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;

public class BlueGiftsOrderHistoryEventsControllerTest extends AbstractWebTestBase {

    @Autowired
    private BundleOrderHelper bundleOrderHelper;

    @Test
    public void shouldReturnBundledItemsInEvents() {
        Order order = bundleOrderHelper.createTypicalOrderWithBundles();

        assertThat(order.getRgb(), equalTo(Color.BLUE));

        Set<Long> orderItemIds = order.getItems().stream()
                .map(OrderItem::getId)
                .collect(Collectors.toSet());

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null,
                0, 10
        );

        assertThat(events.getItems(), everyItem(hasProperty("orderAfter", allOf(
                hasProperty("id", equalTo(order.getId())),
                hasProperty("delivery", hasProperty("parcels", hasItem(
                        allOf(
                                hasProperty("parcelItems", hasSize(3)),
                                hasProperty("parcelItems", everyItem(
                                        hasProperty("itemId", in(orderItemIds))
                                ))
                        )
                ))),
                hasProperty("items", hasSize(3)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                                hasProperty("bundleId", equalTo(PROMO_BUNDLE))
                        ),
                        allOf(
                                hasProperty("offerId", equalTo(GIFT_OFFER)),
                                hasProperty("bundleId", equalTo(PROMO_BUNDLE))
                        ),
                        allOf(
                                hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                                hasProperty("bundleId", nullValue())
                        )
                ))
        ))));
    }

    @Test
    public void shouldReturnShopPromoIdInEvents() {
        Order order = bundleOrderHelper.createTypicalOrderWithBundles();

        assertThat(order.getRgb(), equalTo(Color.BLUE));

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null,
                0, 10
        );

        assertThat(events.getItems(), everyItem(hasProperty("orderAfter", allOf(
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoDefinition", allOf(
                                hasProperty("type", is(PromoType.GENERIC_BUNDLE)),
                                hasProperty("shopPromoId", is(SHOP_PROMO_KEY))
                        ))
                ))),
                hasProperty("items", hasItems(
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(PromoType.GENERIC_BUNDLE)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY))
                                ))
                        )))
                ))
        ))));
    }

    @Test
    public void shouldReturnClientIdInEvents() {
        Order order = bundleOrderHelper.createTypicalOrderWithBundles();

        assertThat(order.getRgb(), equalTo(Color.BLUE));

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null,
                0, 10
        );

        assertThat(events.getItems(), everyItem(hasProperty("orderAfter", allOf(
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoDefinition", allOf(
                                hasProperty("type", is(PromoType.GENERIC_BUNDLE)),
                                hasProperty("clientId", is(CLIENT_ID))
                        ))
                ))),
                hasProperty("items", hasItems(
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(PromoType.GENERIC_BUNDLE)),
                                        hasProperty("clientId", is(CLIENT_ID))
                                ))
                        )))
                ))
        ))));
    }

    @Test
    public void shouldReturnAnaplanIdInEvents() {
        Order order = bundleOrderHelper.createTypicalOrderWithBundles();

        assertThat(order.getRgb(), equalTo(Color.BLUE));

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null,
                0, 10
        );

        assertThat(events.getItems(), everyItem(hasProperty("orderAfter", allOf(
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoDefinition", allOf(
                                hasProperty("type", is(PromoType.GENERIC_BUNDLE)),
                                hasProperty("anaplanId", is(ANAPLAN_ID))
                        ))
                ))),
                hasProperty("items", hasItems(
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(PromoType.GENERIC_BUNDLE)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID))
                                ))
                        )))
                ))
        ))));
    }
}
