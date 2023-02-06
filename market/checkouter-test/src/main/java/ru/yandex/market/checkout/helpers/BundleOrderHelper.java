package ru.yandex.market.checkout.helpers;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.loyalty.api.model.PromoType;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SECOND_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.THIRD_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.offerPromo;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

@WebTestHelper
public class BundleOrderHelper extends MockMvcAware {

    @Autowired
    private OrderCreateHelper orderCreateHelper;

    public BundleOrderHelper(WebApplicationContext webApplicationContext,
                             TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    @Nonnull
    public Order createTypicalOrderWithBundles() {
        return createTypicalOrderWithBundles(parameters -> {
        });
    }

    @Nonnull
    public Order createTypicalOrderWithBundles(@Nonnull Consumer<Parameters> parametersModifier) {
        OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(PRIMARY_OFFER)
                .price(10000);

        OrderItemProvider.OrderItemBuilder secondaryOffer = orderItemWithSortingCenter()
                .label("some-id-2")
                .offer(GIFT_OFFER)
                .price(2000);

        Parameters parameters = fbyRequestFor(single(orderWithYandexDelivery()
                        .itemBuilder(similar(primaryOffer)
                                .count(2))
                        .itemBuilder(secondaryOffer)
                ),
                List.of(FoundOfferBuilder.createFrom(primaryOffer.build())
                        .promoKey(PROMO_KEY)
                        .promoType(ReportPromoType.GENERIC_BUNDLE.getCode())
                        .promo(offerPromo(PROMO_KEY, ReportPromoType.GENERIC_BUNDLE))
                        .build()),
                config ->
                        config.expectPromoBundle(OrderBundleBuilder.create()
                                .bundleId(PROMO_BUNDLE)
                                .promo(PROMO_KEY)
                                .clientId(CLIENT_ID)
                                .anaplanId(ANAPLAN_ID)
                                .item(similar(primaryOffer).primaryInBundle(true), 1, 1)
                                .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                                .expectResponseItems(
                                        itemResponseFor(primaryOffer)
                                                .bundleId(PROMO_BUNDLE)
                                                .primaryInBundle(true),
                                        itemResponseFor(secondaryOffer)
                                                .bundleId(PROMO_BUNDLE)
                                                .primaryInBundle(false),
                                        itemResponseFor(primaryOffer)
                                ));

        parametersModifier.accept(parameters);

        return orderCreateHelper.createOrder(parameters);
    }

    @Nonnull
    public Order createTypicalOrderWithCheapestAsGift(@Nonnull Consumer<Parameters> parametersModifier) {
        OrderItemProvider.OrderItemBuilder firstOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(FIRST_OFFER)
                .price(3000);

        OrderItemProvider.OrderItemBuilder secondOffer = orderItemWithSortingCenter()
                .label("some-id-2")
                .offer(SECOND_OFFER)
                .price(3000);

        OrderItemProvider.OrderItemBuilder thirdOffer = orderItemWithSortingCenter()
                .label("some-id-3")
                .offer(THIRD_OFFER)
                .price(3000);

        Parameters parameters = fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(firstOffer)
                .itemBuilder(secondOffer)
                .itemBuilder(thirdOffer)
        ), Stream.of(firstOffer.build(), secondOffer.build(), thirdOffer.build())
                .map(FoundOfferBuilder::createFrom)
                .peek(builder -> builder
                        .promoKey(PROMO_KEY)
                        .promoType(ReportPromoType.CHEAPEST_AS_GIFT.getCode())
                        .promo(offerPromo(PROMO_KEY, ReportPromoType.CHEAPEST_AS_GIFT)))
                .map(FoundOfferBuilder::build)
                .collect(toList()), config ->
                config.expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .promoType(PromoType.CHEAPEST_AS_GIFT)
                        .anaplanId(ANAPLAN_ID)
                        .item(firstOffer, 1, 1000)
                        .item(secondOffer, 1, 1000)
                        .item(thirdOffer, 1, 1000))
                        .expectResponseItems(
                                itemResponseFor(firstOffer),
                                itemResponseFor(secondOffer),
                                itemResponseFor(thirdOffer)
                        ));

        parametersModifier.accept(parameters);

        return orderCreateHelper.createOrder(parameters);
    }

    @Nonnull
    public Order createTypicalOrderWithBlueSet(@Nonnull Consumer<Parameters> parametersModifier) {
        OrderItemProvider.OrderItemBuilder firstOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(FIRST_OFFER)
                .price(5000);

        OrderItemProvider.OrderItemBuilder secondOffer = orderItemWithSortingCenter()
                .label("some-id-2")
                .offer(SECOND_OFFER)
                .price(4000);

        OrderItemProvider.OrderItemBuilder thirdOffer = orderItemWithSortingCenter()
                .label("some-id-3")
                .offer(THIRD_OFFER)
                .price(3000);

        Parameters parameters = fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(firstOffer)
                .itemBuilder(secondOffer)
                .itemBuilder(thirdOffer)
        ), Stream.of(firstOffer.build(), secondOffer.build(), thirdOffer.build())
                .map(FoundOfferBuilder::createFrom)
                .peek(builder -> builder
                        .promoKey(PROMO_KEY)
                        .promoType(ReportPromoType.BLUE_SET.getCode())
                        .promo(offerPromo(PROMO_KEY, ReportPromoType.BLUE_SET)))
                .map(FoundOfferBuilder::build)
                .collect(toList()), config ->
                config.expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .promoType(PromoType.BLUE_SET)
                        .anaplanId(ANAPLAN_ID)
                        .item(firstOffer, 1, 100)
                        .item(secondOffer, 1, 100)
                        .item(thirdOffer, 1, 100))
                        .expectResponseItems(
                                itemResponseFor(firstOffer),
                                itemResponseFor(secondOffer),
                                itemResponseFor(thirdOffer)
                        ));

        parametersModifier.accept(parameters);

        return orderCreateHelper.createOrder(parameters);
    }
}
