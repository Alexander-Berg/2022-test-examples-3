package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.fixedPrice;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;

@TestFor(DiscountController.class)
public class DiscountControllerPromoBundlesWithDifferentProportionalDiscountTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final String PROMO_KEY = "some promo";
    private static final String FIRST_SSKU = "first offer";
    private static final String SECOND_SSKU = "second offer";
    private static final String THIRD_SSKU = "third offer";
    private static final String FOURTH_SSKU = "fourth offer";
    private static final String FIFTH_SSKU = "fifth offer";

    @Autowired
    private PromoBundleService bundleService;

    @Before
    public void prepare() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoId(PROMO_KEY),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, FIRST_SSKU, SECOND_SSKU, THIRD_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(FIRST_SSKU),
                        then(FOURTH_SSKU),
                        proportion(40)
                ), directionalMapping(
                        when(SECOND_SSKU),
                        then(FIFTH_SSKU),
                        proportion(60)
                ), directionalMapping(
                        when(THIRD_SSKU),
                        then(FIFTH_SSKU),
                        fixedPrice(200)
                ))
        ));
    }

    @Test
    public void shouldConstructBundlesWithDifferentProportionalDiscount() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        price(100000)
                ).withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        promoKeys(PROMO_KEY),
                        price(8000)
                ).withOrderItem(
                        itemKey(FEED_ID, FOURTH_SSKU),
                        ssku(FOURTH_SSKU),
                        price(2000)
                ).withOrderItem(
                        itemKey(FEED_ID, FIFTH_SSKU),
                        ssku(FIFTH_SSKU),
                        price(3000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        OrderWithBundlesResponse orderResponse = firstOrderOf(discountResponse);
        assertThat(orderResponse.getBundles(), hasItems(
                hasProperty("items", hasItems(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("offerId", is(FOURTH_SSKU))
                )),
                hasProperty("items", hasItems(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("offerId", is(FIFTH_SSKU))
                ))
        ));
        assertThat(orderResponse.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasItem(
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1200)))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(FOURTH_SSKU)),
                        hasProperty("promos", hasItem(
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(800)))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1200)))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(FIFTH_SSKU)),
                        hasProperty("promos", hasItem(
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1800)))
                        ))
                )
        ));
    }

    @Test
    public void shouldConstructBundlesWithDifferentProportionalAndFixedDiscount() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        price(100000)
                ).withOrderItem(
                        itemKey(FEED_ID, THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        promoKeys(PROMO_KEY),
                        price(8000)
                ).withOrderItem(
                        itemKey(FEED_ID, FOURTH_SSKU),
                        ssku(FOURTH_SSKU),
                        price(2000)
                ).withOrderItem(
                        itemKey(FEED_ID, FIFTH_SSKU),
                        ssku(FIFTH_SSKU),
                        price(3000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        OrderWithBundlesResponse orderResponse = firstOrderOf(discountResponse);
        assertThat(orderResponse.getBundles(), hasItems(
                hasProperty("items", hasItems(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("offerId", is(FOURTH_SSKU))
                )),
                hasProperty("items", hasItems(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("offerId", is(FIFTH_SSKU))
                ))
        ));
        assertThat(orderResponse.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasItem(
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1200)))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(FOURTH_SSKU)),
                        hasProperty("promos", hasItem(
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(800)))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("promos", hasItem(
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(FIFTH_SSKU)),
                        hasProperty("promos", hasItem(
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(2800)))
                        ))
                )
        ));
    }

    protected static OrderWithBundlesResponse firstOrderOf(MultiCartWithBundlesDiscountResponse discountResponse) {
        return discountResponse.getOrders().iterator().next();
    }
}
