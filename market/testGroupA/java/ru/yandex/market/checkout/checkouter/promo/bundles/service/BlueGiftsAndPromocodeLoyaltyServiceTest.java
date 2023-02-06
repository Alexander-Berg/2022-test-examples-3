package ru.yandex.market.checkout.checkouter.promo.bundles.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.model.PromocodeDiscountEntry;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.GENERIC_BUNDLE;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.MARKET_PROMOCODE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.LoyaltyTestUtils.createTestContextWithBuilders;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemBuilder;
import static ru.yandex.market.checkout.test.providers.OrderProvider.orderBuilder;

public class BlueGiftsAndPromocodeLoyaltyServiceTest extends AbstractWebTestBase {

    private static final String PROMOCODE = "PROMOCODE";
    private static final String PROMOCODE_PROMO_KEY = "PROMOCODE_PROMO_KEY";
    private static final String PROMOCODE_SHOP_PROMO_ID = "PROMOCODE_SHOP_PROMO_ID";
    private final List<FoundOfferBuilder> reportOffers = new ArrayList<>();
    @Autowired
    private LoyaltyService loyaltyService;
    private OrderItemProvider.OrderItemBuilder primaryOffer;
    private OrderItemProvider.OrderItemBuilder secondaryOffer;

    @BeforeEach
    public void configure() {
        primaryOffer = orderItemBuilder()
                .offer(PRIMARY_OFFER)
                .price(10000);

        secondaryOffer = orderItemBuilder()
                .offer(GIFT_OFFER)
                .price(2000);

        //report should return promo key for primary item
        reportOffers.add(FoundOfferBuilder.createFrom(primaryOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.GENERIC_BUNDLE.getCode()));

        reportOffers.add(FoundOfferBuilder.createFrom(secondaryOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.GENERIC_BUNDLE_SECONDARY.getCode()));
    }

    @Test
    public void shouldPromocodeShopPromoIdNotEqualBundleShopPromoId() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
        );

        Parameters requestParameters = defaultBlueOrderParameters();
        requestParameters.getLoyaltyParameters().setExpectedPromoCode(PROMOCODE);
        requestParameters.configureMultiCart(multiCart -> multiCart.setPromoCode(PROMOCODE));

        OfferItemKey primaryOfferItemKey = similar(primaryOffer)
                .promoBundle(PROMO_BUNDLE).build().getOfferItemKey();

        requestParameters.getLoyaltyParameters()
                .expectPromocodes(
                        PromocodeDiscountEntry
                                .promocode(PROMOCODE, PROMOCODE_PROMO_KEY)
                                .discount(Map.of(primaryOfferItemKey, BigDecimal.valueOf(101)))
                                .shopPromoId(PROMOCODE_SHOP_PROMO_ID))

                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1, 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("promos", containsInAnyOrder(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", equalTo(GENERIC_BUNDLE)),
                                        hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                                        hasProperty("shopPromoId", equalTo(SHOP_PROMO_KEY))
                                )), hasProperty("promoDefinition", allOf(
                                        hasProperty("type", equalTo(MARKET_PROMOCODE)),
                                        hasProperty("marketPromoId", equalTo(PROMOCODE_PROMO_KEY)),
                                        hasProperty("promoCode", equalTo(PROMOCODE)),
                                        hasProperty("shopPromoId", equalTo(PROMOCODE_SHOP_PROMO_ID))
                                ))))),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("promos", contains(
                                        hasProperty("promoDefinition", allOf(
                                                hasProperty("type", equalTo(GENERIC_BUNDLE)),
                                                hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                                hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                                                hasProperty("shopPromoId", equalTo(SHOP_PROMO_KEY))
                                        ))
                                )
                        ))))));
    }

    private OrderItemProvider.OrderItemBuilder similar(OrderItemProvider.OrderItemBuilder item) {
        return item.clone();
    }
}
