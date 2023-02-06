package ru.yandex.market.checkout.checkouter.promo.multipromo;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.promo.PromoConfigurer;
import ru.yandex.market.checkout.helpers.BundleOrderHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SECOND_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.THIRD_OFFER;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;

public class CheapestAsGiftMultiPromoTest extends AbstractWebTestBase {

    private static final String DD_PROMO = "direct discount";
    private static final String PD_PROMO = "price drop";

    @Autowired
    private PromoConfigurer promoConfigurer;
    @Autowired
    private BundleOrderHelper bundleOrderHelper;

    @Test
    void shouldApplyWithDirectDiscount() {
        AtomicReference<Parameters> propertiesRef = new AtomicReference<>();
        Order order = bundleOrderHelper.createTypicalOrderWithCheapestAsGift(parameters -> {
            propertiesRef.set(parameters);

            promoConfigurer.importFrom(parameters);

            OrderItem item1 = itemOf(parameters.getOrder(), FIRST_OFFER);

            promoConfigurer.applyDirectDiscount(
                    item1,
                    DD_PROMO,
                    DD_PROMO,
                    SHOP_PROMO_KEY,
                    BigDecimal.valueOf(1000), null, true, true);

            promoConfigurer.applyTo(parameters);
        });

        assertThat(order, notNullValue());
        assertThat(order.getPromos(), hasItems(
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.CHEAPEST_AS_GIFT))),
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.DIRECT_DISCOUNT)))
        ));
        assertThat(order.getItems(), hasItem(
                allOf(
                        hasProperty("offerId", is(FIRST_OFFER)),
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(1000))),
                        hasProperty("promos", hasItems(allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.DIRECT_DISCOUNT))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1000)))
                        ), allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.CHEAPEST_AS_GIFT))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1000)))
                        )))
                )
        ));
        assertThat(order.getItems(), everyItem(
                hasProperty("primaryInBundle", is(false))
        ));

        var discountRequest = propertiesRef.get().getLoyaltyParameters().getLastDiscountRequest();
        var itemRequest = discountRequest.getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getOfferId().equals(FIRST_OFFER))
                .findFirst().orElseThrow();

        assertThat(discountRequest, notNullValue());
        assertThat(itemRequest.getPrice(), comparesEqualTo(BigDecimal.valueOf(2000)));
        assertThat(itemRequest.getPromoDiscounts(), empty());
    }

    @Test
    void shouldApplyWithBlueDiscount() {
        AtomicReference<Parameters> propertiesRef = new AtomicReference<>();
        Order order = bundleOrderHelper.createTypicalOrderWithCheapestAsGift(parameters -> {
            propertiesRef.set(parameters);

            promoConfigurer.importFrom(parameters);

            OrderItem item1 = itemOf(parameters.getOrder(), FIRST_OFFER);

            promoConfigurer.applyBlueDiscount(
                    item1,
                    BigDecimal.valueOf(1000));

            promoConfigurer.applyTo(parameters);
        });

        assertThat(order, notNullValue());
        assertThat(order.getPromos(), hasItems(
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.CHEAPEST_AS_GIFT))),
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.MARKET_BLUE)))
        ));
        assertThat(order.getItems(), hasItem(
                allOf(
                        hasProperty("offerId", is(FIRST_OFFER)),
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(1000))),
                        hasProperty("promos", hasItems(allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.MARKET_BLUE))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1000)))
                        ), allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.CHEAPEST_AS_GIFT))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1000)))
                        )))
                )
        ));
        assertThat(order.getItems(), everyItem(
                hasProperty("primaryInBundle", is(false))
        ));

        var discountRequest = propertiesRef.get().getLoyaltyParameters().getLastDiscountRequest();
        var itemRequest = discountRequest.getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getOfferId().equals(FIRST_OFFER))
                .findFirst().orElseThrow();

        assertThat(discountRequest, notNullValue());
        assertThat(itemRequest.getPrice(), comparesEqualTo(BigDecimal.valueOf(2000)));
        assertThat(itemRequest.getPromoDiscounts(), empty());
    }

    @Test
    void shouldApplyWithPriceDrop() {
        AtomicReference<Parameters> propertiesRef = new AtomicReference<>();
        Order order = bundleOrderHelper.createTypicalOrderWithCheapestAsGift(parameters -> {
            propertiesRef.set(parameters);

            promoConfigurer.importFrom(parameters);

            OrderItem item1 = itemOf(parameters.getOrder(), FIRST_OFFER);
            OrderItem item2 = itemOf(parameters.getOrder(), SECOND_OFFER);
            OrderItem item3 = itemOf(parameters.getOrder(), THIRD_OFFER);

            promoConfigurer.applyPriceDropDiscount(
                    item1,
                    PD_PROMO,
                    PD_PROMO,
                    BigDecimal.valueOf(1000));

            promoConfigurer.applyTo(parameters);

            parameters.getLoyaltyParameters().expectResponseItems(
                    itemResponseFor(item1)
                            .promo(new ItemPromoResponse(
                                    BigDecimal.valueOf(1000),
                                    ru.yandex.market.loyalty.api.model.PromoType.EXTERNAL,
                                    null,
                                    null,
                                    PD_PROMO,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            )),
                    itemResponseFor(item2),
                    itemResponseFor(item3)
            );
        });

        assertThat(order, notNullValue());
        assertThat(order.getPromos(), hasItems(
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.CHEAPEST_AS_GIFT))),
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.PRICE_DROP_AS_YOU_SHOP)))
        ));
        assertThat(order.getItems(), hasItem(
                allOf(
                        hasProperty("offerId", is(FIRST_OFFER)),
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(1000))),
                        hasProperty("promos", hasItems(allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.PRICE_DROP_AS_YOU_SHOP))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1000)))
                        ), allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.CHEAPEST_AS_GIFT))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1000)))
                        )))
                )
        ));
        assertThat(order.getItems(), everyItem(
                hasProperty("primaryInBundle", is(false))
        ));

        var discountRequest = propertiesRef.get().getLoyaltyParameters().getLastDiscountRequest();
        var itemRequest = discountRequest.getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getOfferId().equals(FIRST_OFFER))
                .findFirst().orElseThrow();

        assertThat(itemRequest.getPrice(), comparesEqualTo(BigDecimal.valueOf(3000)));
        assertThat(itemRequest.getPromoDiscounts(), hasSize(1));
        assertThat(itemRequest.getPromoDiscounts(), hasItem(allOf(
                hasProperty("promoType", is(PromoType.PRICE_DROP_AS_YOU_SHOP.getCode())),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1000)))
        )));
    }

    @Test
    void shouldApplyWithPriceDropWithBluePromo() {
        AtomicReference<Parameters> propertiesRef = new AtomicReference<>();
        Order order = bundleOrderHelper.createTypicalOrderWithCheapestAsGift(parameters -> {
            propertiesRef.set(parameters);

            promoConfigurer.importFrom(parameters);

            OrderItem item1 = itemOf(parameters.getOrder(), FIRST_OFFER);
            OrderItem item2 = itemOf(parameters.getOrder(), SECOND_OFFER);
            OrderItem item3 = itemOf(parameters.getOrder(), THIRD_OFFER);

            promoConfigurer.applyBlueDiscount(
                    item1,
                    BigDecimal.valueOf(200)
            );
            promoConfigurer.applyPriceDropDiscount(
                    item1,
                    PD_PROMO,
                    PD_PROMO,
                    BigDecimal.valueOf(100));

            promoConfigurer.applyTo(parameters);

            parameters.getLoyaltyParameters().expectResponseItems(
                    itemResponseFor(item1)
                            .promo(new ItemPromoResponse(
                                    BigDecimal.valueOf(100),
                                    ru.yandex.market.loyalty.api.model.PromoType.EXTERNAL,
                                    null,
                                    null,
                                    PD_PROMO,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            )),
                    itemResponseFor(item2),
                    itemResponseFor(item3)
            );
        });

        assertThat(order, notNullValue());
        assertThat(order.getPromos(), hasItems(
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.CHEAPEST_AS_GIFT))),
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.PRICE_DROP_AS_YOU_SHOP))),
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.MARKET_BLUE)))
        ));
        assertThat(order.getItems(), hasItem(
                allOf(
                        hasProperty("offerId", is(FIRST_OFFER)),
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(1700))),
                        hasProperty("promos", hasItems(allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.PRICE_DROP_AS_YOU_SHOP))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(100)))
                        ), allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.CHEAPEST_AS_GIFT))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1000)))
                        ), allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.MARKET_BLUE))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(200)))
                        )))
                )
        ));
        assertThat(order.getItems(), everyItem(
                hasProperty("primaryInBundle", is(false))
        ));

        var discountRequest = propertiesRef.get().getLoyaltyParameters().getLastDiscountRequest();
        var itemRequest = discountRequest.getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getOfferId().equals(FIRST_OFFER))
                .findFirst().orElseThrow();

        assertThat(itemRequest.getPrice(), comparesEqualTo(BigDecimal.valueOf(2800)));
        assertThat(itemRequest.getPromoDiscounts(), hasSize(1));
        assertThat(itemRequest.getPromoDiscounts(), hasItem(allOf(
                hasProperty("promoType", is(PromoType.PRICE_DROP_AS_YOU_SHOP.getCode())),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
        )));
    }

    @Nonnull
    private OrderItem itemOf(@Nonnull Order order, @Nonnull String offerId) {
        return order.getItems().stream()
                .filter(item -> item.getOfferId().equals(offerId))
                .findFirst().orElseThrow();
    }
}
