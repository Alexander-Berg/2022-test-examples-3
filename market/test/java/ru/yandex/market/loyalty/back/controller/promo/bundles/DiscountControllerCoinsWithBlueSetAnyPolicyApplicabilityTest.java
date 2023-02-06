package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PromoBundleDescriptionBuilder;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.Rule;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;

import java.time.LocalDate;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_APPLICABILITY_POLICY;
import static ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy.ANY;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.mixin;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
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
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictBerubonus;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

public class DiscountControllerCoinsWithBlueSetAnyPolicyApplicabilityTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final String PROMO_KEY = "some promo bundle";
    private static final String FIRST_SSKU = "first offer";
    private static final String SECOND_SSKU = "second ffer";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private PromoBundleService bundleService;

    @Before
    public void prepare() {
        configurationService.set(PROMO_APPLICABILITY_POLICY, ANY);
    }

    @Test
    public void shouldApplyCoinWhenRestrictedOnlyOnBundle() {
        CoinKey expectedCoin = createCoin();
        bundleService.createPromoBundle(blueSetDescription(
                restrictions(
                        restrictBerubonus()
                )
        ));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        promoKeys(PROMO_KEY),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(discountResponse.getUnusedCoins(), is(empty()));
        assertThat(discountResponse.getCoinErrors(), is(empty()));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                ),
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                ),
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyCoinWhenRestrictedOnlyOnCoin() {
        CoinKey expectedCoin = createCoinWithRestrictions(RuleType.DONT_USE_WITH_BLUE_SET_FILTER_RULE);
        bundleService.createPromoBundle(blueSetDescription());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        promoKeys(PROMO_KEY),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(discountResponse.getUnusedCoins(), is(empty()));
        assertThat(discountResponse.getCoinErrors(), is(empty()));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                ),
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                ),
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldNotApplyCoinWhenRestrictedOnBoth() {
        CoinKey expectedCoin = createCoinWithRestrictions(RuleType.DONT_USE_WITH_BLUE_SET_FILTER_RULE);
        bundleService.createPromoBundle(blueSetDescription(
                restrictions(
                        restrictBerubonus()
                )
        ));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        promoKeys(PROMO_KEY),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(discountResponse.getUnusedCoins(), empty());
        assertThat(discountResponse.getCoinErrors(), hasSize(1));
        assertThat(discountResponse.getCoinErrors(), contains(
                hasProperty(
                        "error",
                        hasProperty("code", equalTo(MarketLoyaltyErrorCode.NOT_SUITABLE_COIN.name()))
                )
        ));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyCoinWhenRestrictedOnBothButWithWrongRuleType() {
        // Wrong rule type
        CoinKey expectedCoin = createCoinWithRestrictions(RuleType.DONT_USE_WITH_CHEAPEST_AS_GIFT_FILTER_RULE);
        bundleService.createPromoBundle(blueSetDescription(
                restrictions(
                        restrictBerubonus()
                )
        ));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        promoKeys(PROMO_KEY),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        promoKeys(PROMO_KEY),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(discountResponse.getUnusedCoins(), is(empty()));
        assertThat(discountResponse.getCoinErrors(), is(empty()));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                ),
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                ),
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
                                )
                        ))
                )
        ));
    }

    private CoinKey createCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        return coinService.create.createCoin(promo, defaultAuth().build());
    }

    private <T extends Rule> CoinKey createCoinWithRestrictions(RuleType<T> ruleType) {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .addCoinRule(RuleContainer.builder(ruleType))
        );

        return coinService.create.createCoin(promo, defaultAuth().build());
    }

    @SafeVarargs
    private PromoBundleDescription blueSetDescription(
            BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder>... customizers
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
