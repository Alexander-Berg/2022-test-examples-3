package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationResult;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodesActivationResult;
import ru.yandex.market.loyalty.core.utils.CartUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.PromoType.GENERIC_BUNDLE;
import static ru.yandex.market.loyalty.api.model.PromoType.MARKET_PROMOCODE;
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
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictPromocode;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixedPromocode;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

public class DiscountControllerPromocodesWithPromoBundlesAnyPolicyApplicabilityTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final long USER_ID = 123;
    private static final String PROMOCODE = "some promocode";
    private static final String BUNDLE_PROMO_KEY = "some promo bundle";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String GIFT_ITEM_SSKU = "some gift offer";
    private static final String SOME_SSKU = "some offer";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromocodeService promocodeService;
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
    public void shouldApplyPromocodeWhenRestrictedOnlyOnBundle() {
        CoinKey expectedCoin = createPromocode(USER_ID);
        String expectedPromoBundle = generateBundleIdWithRestrictions();

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
                )
                .withOrderItem(
                        itemKey(FEED_ID, SOME_SSKU),
                        ssku(SOME_SSKU),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));
        assertThat(discountResponse.getPromocodeErrors(), is(empty()));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                ),
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
                                )
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
                ),
                allOf(
                        hasProperty("offerId", is(SOME_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyPromocodeWhenRestrictedOnlyOnPromocode() {
        CoinKey expectedCoin = createPromocodeWithRestrictions(USER_ID);
        String expectedPromoBundle = generateBundleId();

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
                )
                .withOrderItem(
                        itemKey(FEED_ID, SOME_SSKU),
                        ssku(SOME_SSKU),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), is(empty()));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                ),
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14999)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SOME_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldNotApplyPromocodeWhenRestrictedOnBoth() {
        CoinKey expectedCoin = createPromocodeWithRestrictions(USER_ID);
        String expectedPromoBundle = generateBundleIdWithRestrictions();

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
                )
                .withOrderItem(
                        itemKey(FEED_ID, SOME_SSKU),
                        ssku(SOME_SSKU),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), is(empty()));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
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
                ),
                allOf(
                        hasProperty("offerId", is(SOME_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
                                )
                        ))
                )
        ));
    }

    private CoinKey createPromocode(long uid) {
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode(PROMOCODE)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        PromocodesActivationResult activationResults = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(uid)
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build());

        PromocodeActivationResult promocodeActivationResult = activationResults.getActivationResults().get(0);

        assertThat(promocodeActivationResult, notNullValue());

        return promocodeActivationResult.getCoinKey();
    }

    private CoinKey createPromocodeWithRestrictions(long uid) {
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode(PROMOCODE)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .addCoinRule(RuleContainer.builder(DONT_USE_WITH_BUNDLES_FILTER_RULE))
        );
        PromocodesActivationResult activationResults = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(uid)
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build());

        PromocodeActivationResult promocodeActivationResult = activationResults.getActivationResults().get(0);

        assertThat(promocodeActivationResult, notNullValue());

        return promocodeActivationResult.getCoinKey();
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

    private String generateBundleId() {
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
