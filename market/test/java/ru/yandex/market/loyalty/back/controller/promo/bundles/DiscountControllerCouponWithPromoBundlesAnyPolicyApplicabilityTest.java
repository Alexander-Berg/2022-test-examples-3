package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.utils.CartUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.math.BigDecimal;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_APPLICABLE;
import static ru.yandex.market.loyalty.api.model.PromoType.GENERIC_BUNDLE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.rule.RuleType.DONT_USE_WITH_BUNDLES_FILTER_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_APPLICABILITY_POLICY;
import static ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy.ANY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictBerubonus;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictPromocode;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;

@Deprecated
public class DiscountControllerCouponWithPromoBundlesAnyPolicyApplicabilityTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final String BUNDLE_PROMO_KEY = "some promo bundle";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String GIFT_ITEM_SSKU = "some gift offer";
    private static final BigDecimal INITIAL_CURRENT_BUDGET = BigDecimal.valueOf(700);
    private static final String COUPON_CODE = "SOME COUPON CODE";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private PromoBundleService bundleService;

    @Before
    public void prepare() {
        configurationService.set(
                PROMO_APPLICABILITY_POLICY,
                ANY
        );
    }

    @Test
    public void shouldApplyCouponWhenRestrictedOnlyBundle() {
        createCouponPromo();
        final String expectedPromoBundle = generateBundleIdWithRestrictions();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoupon(COUPON_CODE)
                        .build());

        assertThat(discountResponse.getCouponError(), nullValue());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                ),
                                hasProperty("promoType", is(PromoType.MARKET_COUPON))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", contains(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14999)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyCouponWhenRestrictedOnlyCoupon() {
        createCouponPromoWithRestrictionsOnBundle();
        final String expectedPromoBundle = generateBundle();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoupon(COUPON_CODE)
                        .build());

        assertThat(discountResponse.getCouponError(), nullValue());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                ),
                                hasProperty("promoType", is(PromoType.MARKET_COUPON))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", contains(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14999)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldNotApplyCouponWhenAllRestricted() {
        createCouponPromoWithRestrictionsOnBundle();
        final String expectedPromoBundle = generateBundleIdWithRestrictions();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoupon(COUPON_CODE)
                        .build());

        assertThat(discountResponse.getCouponError(), hasProperty(
                "error",
                hasProperty("code", equalTo(COUPON_NOT_APPLICABLE.toString()))
        ));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14999)))
                                )
                        ))
                )
        ));
    }

    private void createCouponPromo() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(COUPON_CODE)
                        .setBudget(INITIAL_CURRENT_BUDGET)
        );
    }

    private void createCouponPromoWithRestrictionsOnBundle() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(COUPON_CODE)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .addPromoRule(DONT_USE_WITH_BUNDLES_FILTER_RULE)
        );
    }

    private String generateBundleIdWithRestrictions() {
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(BUNDLE_PROMO_KEY),
                        shopPromoId(BUNDLE_PROMO_KEY),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(10)),
                        restrictions(
                                restrictBerubonus(),
                                restrictPromocode()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                ));

        return CartUtils.generateBundleId(bundleDescription, FEED_ID, PROMO_ITEM_SSKU, GIFT_ITEM_SSKU);
    }

    private String generateBundle() {
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(BUNDLE_PROMO_KEY),
                        shopPromoId(BUNDLE_PROMO_KEY),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                ));

        return CartUtils.generateBundleId(bundleDescription, FEED_ID, PROMO_ITEM_SSKU, GIFT_ITEM_SSKU);
    }
}
