package ru.yandex.market.checkout.checkouter.antifraud.detector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.checkout.util.mstat.MstatAntifraudConfigurer;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.ORDER_ITEM_CHANGE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class MstatAntifraudOrderWithBundleSupportDetectorTest extends AbstractWebTestBase {

    @Autowired
    private MstatAntifraudConfigurer mstatAntifraudConfigurer;

    @AfterEach
    private void clearContext() {
        mstatAntifraudConfigurer.resetAll();
    }

    @Test
    public void shouldReturnPrimaryInBundleFields() {
        MultiCart multiCart = actualizeTypicalOrderWithBundles(3);

        Order order = multiCart.getCarts().get(0);

        assertThat(order.getItems(), hasSize(3));
        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(3)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("changes", hasItems(
                                ItemChange.COUNT
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(3)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("changes", hasItems(
                                ItemChange.COUNT
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(5)),
                        hasProperty("changes", not(hasItems(
                                ItemChange.COUNT
                        )))
                )
        ));
    }

    private MultiCart actualizeTypicalOrderWithBundles(int antiFraudLimit) {
        final OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .offer(PRIMARY_OFFER)
                .promoBundle(PROMO_BUNDLE)
                .count(5)
                .price(10000);

        final OrderItemProvider.OrderItemBuilder secondaryOffer = orderItemWithSortingCenter()
                .offer(GIFT_OFFER)
                .promoBundle(PROMO_BUNDLE)
                .count(5)
                .price(2000);

        final OrderBundleBuilder bundleBuilder = OrderBundleBuilder.create()
                .bundleId(PROMO_BUNDLE)
                .promo(PROMO_KEY)
                .item(similar(primaryOffer).primaryInBundle(true), 1)
                .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999);

        Parameters parameters = BlueGiftsOrderUtils.fbyRequestFor(single(orderWithYandexDelivery()
                        .itemBuilder(primaryOffer)
                        .itemBuilder(secondaryOffer)
                        .itemBuilder(similar(secondaryOffer).setNoPromoBundle())
                ), Arrays.asList(
                FoundOfferBuilder.createFrom(primaryOffer.build())
                        .promoKey(PROMO_KEY)
                        .promoType(ReportPromoType.GENERIC_BUNDLE.getCode())
                        .build(),
                FoundOfferBuilder.createFrom(secondaryOffer.build())
                        .promoKey(PROMO_KEY)
                        .promoType(ReportPromoType.GENERIC_BUNDLE_SECONDARY.getCode())
                        .build()
                ), config ->
                        config.expectPromoBundle(bundleBuilder)
                                .expectResponseItems(
                                        itemResponseFor(primaryOffer)
                                                .primaryInBundle(true)
                                                .quantity(3),
                                        itemResponseFor(secondaryOffer)
                                                .primaryInBundle(false)
                                                .quantity(3),
                                        itemResponseFor(similar(secondaryOffer).setNoPromoBundle())
                                                .quantity(5)
                                )
        );

        mstatAntifraudConfigurer.mockVerdict(createVerdict(antiFraudLimit, primaryOffer, secondaryOffer));

        return orderCreateHelper.cart(parameters);
    }

    private static OrderVerdict createVerdict(int countLimit, OrderItemProvider.OrderItemBuilder... items) {
        List<OrderItemResponseDto> resultItems = new ArrayList<>();

        for (OrderItemProvider.OrderItemBuilder itemBuilder : items) {
            OrderItem item = itemBuilder.build();
            resultItems.add(new OrderItemResponseDto(
                    item.getId(),
                    item.getFeedId(),
                    item.getOfferId(),
                    item.getBundleId(),
                    countLimit,
                    Set.of(OrderItemChange.COUNT)
            ));
        }
        return OrderVerdict.builder()
                .checkResults(Set.of(new AntifraudCheckResult(ORDER_ITEM_CHANGE,
                        "something happened", "something happened")))
                .fixedOrder(new OrderResponseDto(resultItems))
                .build();
    }
}
