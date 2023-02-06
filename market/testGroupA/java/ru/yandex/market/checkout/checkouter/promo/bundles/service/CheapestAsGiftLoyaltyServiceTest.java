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
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.CHEAPEST_AS_GIFT;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.LoyaltyTestUtils.createTestContext;
import static ru.yandex.market.checkout.test.providers.OrderProvider.orderBuilder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;

public class CheapestAsGiftLoyaltyServiceTest extends CheapestAsGiftTestBase {

    @Autowired
    private LoyaltyService loyaltyService;

    @Test
    public void shouldCheckPromoKeyAndReturnPromos() {
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
                                        BigDecimal.valueOf(250),
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
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                ))
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(), createTestContext(cart,
                reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", contains(hasProperty("promoDefinition", allOf(
                                hasProperty("type", equalTo(CHEAPEST_AS_GIFT)),
                                hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                hasProperty("shopPromoId", is(SHOP_PROMO_KEY)),
                                hasProperty("bundleId", nullValue())
                        ))))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", contains(hasProperty("promoDefinition", allOf(
                                hasProperty("type", equalTo(CHEAPEST_AS_GIFT)),
                                hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                hasProperty("shopPromoId", is(SHOP_PROMO_KEY)),
                                hasProperty("bundleId", nullValue())
                        ))))
                )
        ))));
    }

    @Test
    public void shouldNotAddChangesOnPromoApply() {
        final MultiCart cart = MultiCartProvider.single(orderBuilder()
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
                                        BigDecimal.valueOf(250),
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
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                ))
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(), createTestContext(cart,
                reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", nullValue())
                )
        ))));
    }
}
