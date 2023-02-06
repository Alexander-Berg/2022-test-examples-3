package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PromoBundleDescriptionBuilder;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.rule.Rule;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationResult;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodesActivationResult;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.Formatters;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.MIN_ORDER_TOTAL_VIOLATED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_SUITABLE_APPLICABILITY_FILTER;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_SUITABLE_FILTER_RULES;
import static ru.yandex.market.loyalty.api.model.PromoType.MARKET_PROMOCODE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_APPLICABILITY_POLICY;
import static ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy.ANY;
import static ru.yandex.market.loyalty.core.service.coin.CoinPromoCalculator.INSUFFICIENT_TOTAL_MESSAGE;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.mixin;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
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
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixedPromocode;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

public class DiscountControllerPromocodeWithBlueSetAnyPolicyApplicabilityTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final long USER_ID = 123;
    private static final String PROMOCODE = "some promocode";
    private static final String PROMO_KEY = "some promo bundle";
    private static final String FIRST_SSKU = "first offer";
    private static final String SECOND_SSKU = "second ffer";

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
        configurationService.set(PROMO_APPLICABILITY_POLICY, ANY);
    }

    @Test
    public void shouldApplyPromocodeWhenRestrictedOnlyOnBundle() {
        CoinKey expectedCoin = createPromocode(USER_ID);
        bundleService.createPromoBundle(blueSetDescription(
                restrictions(
                        restrictPromocode()
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
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), is(empty()));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
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
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
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
    public void shouldApplyPromocodeWhenRestrictedOnlyOnCoin() {
        CoinKey expectedCoin = createPromocodeWithRestrictions(USER_ID, RuleType.DONT_USE_WITH_BLUE_SET_FILTER_RULE);
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
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), is(empty()));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
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
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
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
    public void shouldShowInsufficientTotalMessageWhenMinOrderTotalViolated() {
        String msku = "12332";
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode(PROMOCODE)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(1500))
                        .addCoinRule(MSKU_FILTER_RULE, MSKU_ID, msku));

        PromocodesActivationResult activationResults = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build());

        PromocodeActivationResult promocodeActivationResult = activationResults.getActivationResults().get(0);
        assertThat(promocodeActivationResult, notNullValue());

        bundleService.createPromoBundle(blueSetDescription());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        quantity(1),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), contains(
                hasProperty(
                        "error",
                        hasProperty(
                                "error",
                                allOf(
                                        hasProperty("code", equalTo(MIN_ORDER_TOTAL_VIOLATED.name())),
                                        hasProperty("message",
                                                equalTo(MIN_ORDER_TOTAL_VIOLATED.getDefaultDescription())),
                                        hasProperty("userMessage", equalTo(Formatters.makeNonBreakingSpaces(
                                                String.format(INSUFFICIENT_TOTAL_MESSAGE, "1 500"), "userMessage")))
                                )
                        )
                )
        ));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());
    }

    @Test
    public void shouldShowNotSuitablePromocodeMessageWhenMskuFilterViolatedAndMinOrderTotalIsNot() {
        String msku = "12332";
        String anotherMsku = "789789";
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode(PROMOCODE)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(1500))
                        .addCoinRule(MSKU_FILTER_RULE, MSKU_ID, msku));

        PromocodesActivationResult activationResults = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build());

        PromocodeActivationResult promocodeActivationResult = activationResults.getActivationResults().get(0);
        assertThat(promocodeActivationResult, notNullValue());

        bundleService.createPromoBundle(blueSetDescription());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(anotherMsku),
                        quantity(1),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), contains(
                hasProperty(
                        "error",
                        hasProperty(
                                "error",
                                allOf(
                                        hasProperty("code", equalTo(NOT_SUITABLE_FILTER_RULES.name())),
                                        hasProperty("message",
                                                equalTo(NOT_SUITABLE_FILTER_RULES.getDefaultDescription())),
                                        hasProperty("userMessage",
                                                equalTo(NOT_SUITABLE_FILTER_RULES.getCodeForFront().getDefaultDescription()))
                                )
                        )
                )
        ));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());
    }

    @Test
    public void shouldNotApplyPromocodeWhenRestrictedOnBoth() {
        createPromocodeWithRestrictions(USER_ID, RuleType.DONT_USE_WITH_BLUE_SET_FILTER_RULE);
        bundleService.createPromoBundle(blueSetDescription(
                restrictions(
                        restrictPromocode()
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
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), contains(
                hasProperty(
                        "error",
                        hasProperty(
                                "error",
                                hasProperty("code", equalTo(NOT_SUITABLE_APPLICABILITY_FILTER.name()))
                        )
                )
        ));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());
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
    public void shouldApplyPromocodeWhenRestrictedOnBothButWithWrongRuleType() {
        // Wrong rule type
        CoinKey expectedCoin = createPromocodeWithRestrictions(
                USER_ID, RuleType.DONT_USE_WITH_CHEAPEST_AS_GIFT_FILTER_RULE);
        bundleService.createPromoBundle(blueSetDescription(
                restrictions(
                        restrictPromocode()
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
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), is(empty()));
        assertThat(discountResponse.getUnusedPromocodes(), is(empty()));
        assertThat(discountResponse.getCouponError(), nullValue());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
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
                                allOf(
                                        hasProperty(
                                                "usedCoin",
                                                hasProperty("id", is(expectedCoin.getId()))
                                        ),
                                        hasProperty("promoType", is(MARKET_PROMOCODE))
                                ),
                                hasProperty(
                                        "promoType",
                                        is(PromoType.BLUE_SET)
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

    private <T extends Rule> CoinKey createPromocodeWithRestrictions(long uid, RuleType<T> ruleType) {
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode(PROMOCODE)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .addCoinRule(RuleContainer.builder(ruleType))
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
