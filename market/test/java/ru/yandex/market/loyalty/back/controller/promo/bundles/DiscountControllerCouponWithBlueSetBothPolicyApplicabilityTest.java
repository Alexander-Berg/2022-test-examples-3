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
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.math.BigDecimal;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_APPLICABLE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_APPLICABILITY_POLICY;
import static ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy.BOTH;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.mixin;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.md5;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.blueSet;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictPromocode;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;

@Deprecated
public class DiscountControllerCouponWithBlueSetBothPolicyApplicabilityTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final String PROMO_KEY = "some promo bundle";
    private static final String FIRST_SSKU = "first offer";
    private static final String SECOND_SSKU = "second ffer";
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
        configurationService.set(PROMO_APPLICABILITY_POLICY, BOTH);
    }

    @Test
    public void shouldNotApplyCouponWhenRestrictedOnBundle() {
        createCouponPromo();
        bundleService.createPromoBundle(blueSetDescription(
                restrictions(
                        restrictPromocode()
                )
        ));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, md5(FIRST_SSKU)),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, md5(SECOND_SSKU)),
                        ssku(SECOND_SSKU),
                        promoKeys(PROMO_KEY),
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
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), everyItem(
                allOf(
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldNotApplyCouponWhenRestrictedOnCoupon() {
        createCouponPromoWithRestrictionsOnBundle();
        bundleService.createPromoBundle(blueSetDescription());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, md5(FIRST_SSKU)),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, md5(SECOND_SSKU)),
                        ssku(SECOND_SSKU),
                        promoKeys(PROMO_KEY),
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
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), everyItem(
                allOf(
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyCouponWhenNoRestrictions() {
        createCouponPromo();
        bundleService.createPromoBundle(blueSetDescription());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, md5(FIRST_SSKU)),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, md5(SECOND_SSKU)),
                        ssku(SECOND_SSKU),
                        promoKeys(PROMO_KEY),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoupon(COUPON_CODE)
                        .build());

        assertThat(discountResponse.getCouponError(), nullValue());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), everyItem(
                allOf(
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "promoType",
                                        is(PromoType.MARKET_COUPON)
                                ),
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
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
                        .addPromoRule(RuleType.DONT_USE_WITH_BLUE_SET_FILTER_RULE)
        );
    }

    @SafeVarargs
    private PromoBundleDescription blueSetDescription(
            BuildCustomizer<PromoBundleDescription, PromoBundleDescription.PromoBundleDescriptionBuilder>... customizers
    ) {
        return bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(PROMO_KEY),
                shopPromoId(PROMO_KEY),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_SSKU, 10),
                                proportion(SECOND_SSKU, 20)
                        )),
                        primary()
                ),
                mixin(customizers)
        );
    }
}
