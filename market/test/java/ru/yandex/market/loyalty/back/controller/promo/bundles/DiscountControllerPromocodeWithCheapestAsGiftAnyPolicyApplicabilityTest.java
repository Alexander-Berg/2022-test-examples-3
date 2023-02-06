package ru.yandex.market.loyalty.back.controller.promo.bundles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.ydb.UserReferralPromocodeDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PromoBundleDescriptionBuilder;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.ydb.UserReferralPromocode;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationResult;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodesActivationResult;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.PromoType.MARKET_PROMOCODE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.model.promo.PromoCodeGeneratorType.REFERRAL;
import static ru.yandex.market.loyalty.core.rule.RuleType.DONT_USE_WITH_CHEAPEST_AS_GIFT_FILTER_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_APPLICABILITY_POLICY;
import static ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy.ANY;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.mixin;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.cheapestAsGift;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.quantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictPromocode;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.withQuantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixedPromocode;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

public class DiscountControllerPromocodeWithCheapestAsGiftAnyPolicyApplicabilityTest
        extends MarketLoyaltyBackMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final long USER_ID = 123;
    private static final String PROMOCODE = "some promocode";
    private static final String PROMO_KEY = "some promo bundle";
    private static final String FIRST_SSKU = "first offer";
    private static final String SECOND_SSKU = "second offer";
    private static final String THIRD_SSKU = "third offer";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private PromoBundleService bundleService;
    @Autowired
    private PromoService promoService;
    @Autowired
    private UserReferralPromocodeDao userReferralPromocodeDao;

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
        bundleService.createPromoBundle(cheapestAsGiftDescription(
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
                .withOrderItem(
                        itemKey(FEED_ID, THIRD_SSKU),
                        ssku(THIRD_SSKU),
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

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
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
                                        is(PromoType.CHEAPEST_AS_GIFT)
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
                                        is(PromoType.CHEAPEST_AS_GIFT)
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
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
    public void shouldApplyPromocodeWhenRestrictedOnlyOnCoin() {
        CoinKey expectedCoin = createPromocodeWithRestrictions(USER_ID);
        bundleService.createPromoBundle(cheapestAsGiftDescription());

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
                .withOrderItem(
                        itemKey(FEED_ID, THIRD_SSKU),
                        ssku(THIRD_SSKU),
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

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
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
                                        is(PromoType.CHEAPEST_AS_GIFT)
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
                                        is(PromoType.CHEAPEST_AS_GIFT)
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
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
        bundleService.createPromoBundle(cheapestAsGiftDescription(
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
                .withOrderItem(
                        itemKey(FEED_ID, THIRD_SSKU),
                        ssku(THIRD_SSKU),
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

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty(
                                        "promoType",
                                        is(PromoType.CHEAPEST_AS_GIFT)
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty(
                                        "promoType",
                                        is(PromoType.CHEAPEST_AS_GIFT)
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
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
    // MARKETDISCOUNT-8740
    public void shouldNotApplyReferralPromocodeEvenNotRestricted() {
        final String COUPON_CODE = "REF_COUPON_CODE";
        final long FRIEND_UID = 0L;

        PromoBundleDescription promoBundle = bundleService.createPromoBundle(cheapestAsGiftDescription());

        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode(COUPON_CODE)
                        .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
                        .setBudget(BigDecimal.valueOf(10000))
                        .setEmissionBudget(BigDecimal.valueOf(10000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_PROMOCODE, couponPromo.getPromoKey());
        promoService.setPromoParam(couponPromo.getPromoId().getId(), PromoParameterName.GENERATOR_TYPE, REFERRAL);

        var userReferralPromocode = UserReferralPromocode.builder()
                .setUid(USER_ID)
                .setPromocode(COUPON_CODE)
                .setAssignTime(clock.instant())
                .setExpireTime(clock.instant().plus(10, ChronoUnit.DAYS))
                .build();

        userReferralPromocodeDao.insertNewEntry(userReferralPromocode);
        reloadPromoCache();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        promoKeys(promoBundle.getPromoKey()),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        promoKeys(promoBundle.getPromoKey()),
                        price(15000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(FRIEND_UID))
                        .withCoupon(COUPON_CODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty("promoType", is(PromoType.CHEAPEST_AS_GIFT))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty("promoType", is(PromoType.CHEAPEST_AS_GIFT))
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("promos", empty())
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
                        .addCoinRule(RuleContainer.builder(DONT_USE_WITH_CHEAPEST_AS_GIFT_FILTER_RULE))
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
    private PromoBundleDescription cheapestAsGiftDescription(
            BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder>... customizers
    ) {
        return bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(PROMO_KEY),
                shopPromoId(PROMO_KEY),
                strategy(CHEAPEST_AS_GIFT),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                withQuantityInBundle(3),
                item(
                        condition(cheapestAsGift(FeedSskuSet.of(FEED_ID, List.of(FIRST_SSKU, SECOND_SSKU)))),
                        primary(),
                        quantityInBundle(3)
                ),
                mixin(customizers)
        );
    }
}
