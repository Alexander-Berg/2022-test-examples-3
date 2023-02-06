package ru.yandex.market.loyalty.back.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OrderRequestWithoutOrderId;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.coin.BaseCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.CoinApplicationRestrictionType;
import ru.yandex.market.loyalty.api.model.coin.CoinRestrictionType;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinsForCart;
import ru.yandex.market.loyalty.api.model.coin.CoinsForUser;
import ru.yandex.market.loyalty.api.model.coin.FutureCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.FutureCoins;
import ru.yandex.market.loyalty.api.model.coin.PersonTopSortType;
import ru.yandex.market.loyalty.api.model.coin.SmartShoppingImageTypes;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.restrictions.CoinRestrictionsResponseList;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryFeature;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderItemsRequest;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinDescription;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.ExcludedOffersType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.avatar.AvatarImageId;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.constants.DeliveryPartnerType;
import ru.yandex.market.loyalty.core.service.discount.constants.SupplierFlagRestrictionType;
import ru.yandex.market.loyalty.core.service.exclusions.ExcludedOffersService;
import ru.yandex.market.loyalty.core.service.mail.AlertNotificationService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.RegionSettingsUtils;
import ru.yandex.market.loyalty.core.utils.ReportMockUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static java.sql.Timestamp.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.api.model.UsageClientDeviceType.APPLICATION;
import static ru.yandex.market.loyalty.api.model.UsageClientDeviceType.DESKTOP;
import static ru.yandex.market.loyalty.api.model.coin.CoinApplicationRestrictionType.FILTER_RULE_RESTRICTION;
import static ru.yandex.market.loyalty.api.model.coin.CoinApplicationRestrictionType.LSC_RESTRICTION;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.USED;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_PLATFORM;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.EXCLUDED_OFFERS_TYPE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.NOT_LOGIC;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.REGION_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_FLAG_RESTRICTION_TYPE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.VENDOR_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.CLIENT_PLATFORM_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.DELIVERY_REGION_CART_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.EXCLUDED_OFFERS_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PURCHASE_BY_LIST_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.VENDOR_FILTER_RULE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.STICK_CATEGORY;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withFeatures;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.Formatters.NBSP;
import static ru.yandex.market.loyalty.core.utils.Formatters.makeNonBreakingSpaces;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.coinHasKey;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.DEFAULT_REGION;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_MSKU;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.EXCLUDED_SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.THIRD_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EXPIRATION_DAYS;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_IMAGE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultCoinDescriptionBuilder;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFreeDelivery;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFreeDeliveryWithMinOrderTotalRule;
import static ru.yandex.market.loyalty.core.utils.RegionSettingsUtils.DEFAULT_THRESHOLD;
import static ru.yandex.market.loyalty.core.utils.RegionSettingsUtils.LITTLE_LESS_THAN_DEFAULT_THRESHOLD;
import static ru.yandex.market.loyalty.core.utils.ReportMockUtils.makeDefaultItem;
import static ru.yandex.market.loyalty.core.utils.ReportMockUtils.makeLargeDimensionItem;
import static ru.yandex.market.loyalty.core.utils.ReportMockUtils.makeLargeWeightItem;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.THIRD_UID;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(CoinsController.class)
public class CoinsControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long DEFAULT_VENDOR_ID = 100L;
    private static final int DEFAULT_CATEGORY_ID = 1;

    // TODO by almakarov57: добавить исключение для брендов в OffersFilter MARKETDISCOUNT-6365
    private static final ImmutableSet<Long> HARD_EXCLUDED_VENDORS = ImmutableSet.of(206928L);

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinsController coinsController;
    @Autowired
    private AlertNotificationService alertNotificationService;
    @Autowired
    private ReportMockUtils reportMockUtils;
    @Autowired
    private CoinService coinService;
    @Autowired
    private ExcludedOffersService excludedOffersService;
    @Autowired
    private RegionSettingsUtils regionSettingsUtils;

    @Before
    public void reloadFutureCoins() {
        coinsController.reloadFutureCoins();
    }

    @Test
    public void shouldReturnActualExpiredCoinsIfNotActive() {
        int daysToExpire = 30;
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed()
                .setExpiration(ExpirationPolicy.expireByDays(daysToExpire))
        );

        coinService.create.createCoin(promo, defaultAuth().setStatus(INACTIVE).build());

        clock.spendTime(Duration.ofDays(daysToExpire + 1));

        List<UserCoinResponse> coins = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins();
        assertThat(coins, contains(allOf(
                hasProperty("status", equalTo(CoinStatus.INACTIVE)),
                hasProperty("shortTerm", equalTo(false))
        )));

        assertEquals(1, marketLoyaltyClient.getCoinsCount(DEFAULT_UID));
    }

    @Test
    public void shouldReturnShortTermCoin() {
        int hoursToExpire = 30;
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed()
                .setExpiration(ExpirationPolicy.flash(Duration.ofHours(hoursToExpire)))
        );

        coinService.create.createCoin(promo, defaultAuth().setStatus(INACTIVE).build());

        clock.spendTime(Duration.ofHours(hoursToExpire + 1));

        List<UserCoinResponse> coins = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins();
        assertThat(coins, contains(allOf(
                hasProperty("status", equalTo(CoinStatus.INACTIVE)),
                hasProperty("shortTerm", equalTo(true))
        )));

        assertEquals(1, marketLoyaltyClient.getCoinsCount(DEFAULT_UID));
    }

    @Test
    public void shouldReturnShortTermCoinForShortTermToEndOfPromo() {
        int hoursToExpire = 30;
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed()
                .setExpiration(ExpirationPolicy.shortTermToEndOfPromo())
        );

        coinService.create.createCoin(promo, defaultAuth().setStatus(INACTIVE).build());

        clock.spendTime(Duration.ofHours(hoursToExpire + 1));

        List<UserCoinResponse> coins = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins();
        assertThat(coins, contains(allOf(
                hasProperty("status", equalTo(CoinStatus.INACTIVE)),
                hasProperty("shortTerm", equalTo(true))
        )));

        assertEquals(1, marketLoyaltyClient.getCoinsCount(DEFAULT_UID));
    }

    @Test
    public void shouldRoundEndDateToLastSecondOfTheDay() {
        clock.setDate(valueOf("2019-01-01 00:00:00"));
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed()
                .setEndDate(valueOf("2019-01-17 15:00:00"))
                .setExpiration(ExpirationPolicy.toEndOfPromo())
        );
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());
        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);
        assertThat(coin, hasProperty("roundedEndDate", equalTo(new Date(valueOf("2019-01-17 23:59:59").getTime()))));
        CoinsForUser coins = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5);
        assertThat(
                coins,
                hasProperty(
                        "coins",
                        contains(
                                hasProperty("endDate", equalTo(new Date(valueOf("2019-01-17 23:59:59").getTime())))
                        )
                )
        );

        clock.setDate(valueOf("2019-01-17 23:00:00"));
        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), is(not(empty())));

        clock.setDate(valueOf("2019-01-18 00:00:01"));
        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), is(empty()));

        alertNotificationService.processEmailQueue(100);
    }

    @Test
    public void shouldNotReturnUsedCoinsByOrder() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        int orderId = 123123;
        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder()
                                .withOrderItem()
                                .withOrderId(String.valueOf(orderId))
                                .build()
                )
                .withCoins(coinKey)
                .build()
        );

        assertThat(marketLoyaltyClient.getAllCoinsForOrder(orderId), is(empty()));
    }

    @Test
    public void shouldReturnDefaultSubtitle() {
        Promo promoWithoutRestrictionDescription = promoManager.createSmartShoppingPromo(
                defaultFixed(
                        DEFAULT_COIN_FIXED_NOMINAL,
                        defaultCoinDescriptionBuilder().setRestrictionDescription(null)
                )
        );

        String restrictionDescription = "на любой заказ из категории 'Х'";
        String expectedRestrictionDescription = "на" + NBSP + "любой заказ из" + NBSP + "категории 'Х'";
        Promo promoWithRestrictionDescription = promoManager.createSmartShoppingPromo(
                defaultFixed(
                        DEFAULT_COIN_FIXED_NOMINAL,
                        defaultCoinDescriptionBuilder().setRestrictionDescription(restrictionDescription)
                )
        );

        coinService.create.createCoin(promoWithRestrictionDescription, defaultAuth(DEFAULT_UID).build());
        coinService.create.createCoin(promoWithoutRestrictionDescription, defaultAuth(ANOTHER_UID).build());

        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), contains(
                hasProperty("subtitle", equalTo(expectedRestrictionDescription))
        ));
        assertThat(marketLoyaltyClient.getCoinsForUser(ANOTHER_UID, 5).getCoins(), contains(
                hasProperty("subtitle", equalTo(
                        makeNonBreakingSpaces(CoreCoinType.FIXED.getDefaultRestrictionDescription(), "test")
                ))
        ));
    }

    @Test
    public void shouldReturnImages() {
        SmartShoppingPromoBuilder<?> smartShoppingPromoBuilder = defaultFixed();
        CoinDescription.Builder coinDescription = smartShoppingPromoBuilder.getCoinDescription();
        coinDescription.setAvatarImageAltId(new AvatarImageId(123, "AltImageName"));
        Promo promoWithRestrictionDescription = promoManager.createSmartShoppingPromo(smartShoppingPromoBuilder);

        coinService.create.createCoin(promoWithRestrictionDescription, defaultAuth(DEFAULT_UID).build());

        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), contains(allOf(
                hasProperty("image", containsString(DEFAULT_COIN_IMAGE.getImageName())),
                hasProperty("images", hasEntry(
                        equalTo(SmartShoppingImageTypes.STANDARD), containsString(DEFAULT_COIN_IMAGE.getImageName())
                        )
                ),
                hasProperty("images", hasEntry(
                        equalTo(SmartShoppingImageTypes.ALT), containsString("AltImageName")
                        )
                )
        )));
    }

    @Test
    public void shouldReturnImages_AltImage() {
        Promo promoWithRestrictionDescription = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promoWithRestrictionDescription, defaultAuth(DEFAULT_UID).build());

        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), contains(allOf(
                hasProperty("image", containsString(DEFAULT_COIN_IMAGE.getImageName())),
                hasProperty("images", hasEntry(
                        equalTo(SmartShoppingImageTypes.STANDARD), containsString(DEFAULT_COIN_IMAGE.getImageName())
                        )
                )
        )));
    }

    @Test
    public void shouldReturnOneRandomRestriction() {
        Promo promoWithoutRestrictions = promoManager.createSmartShoppingPromo(
                defaultFixed()
        );
        ImmutableSet<Integer> categories = ImmutableSet.of(123, 1123, 12312);
        Promo promoWithCategoryRestriction = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categories)
        );

        Promo promoWithMskuRestriction = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(MSKU_FILTER_RULE, MSKU_ID, ImmutableSet.of("123", "1123", "12312"))
        );

        coinService.create.createCoin(promoWithoutRestrictions, defaultAuth(DEFAULT_UID).build());
        coinService.create.createCoin(promoWithCategoryRestriction, defaultAuth(ANOTHER_UID).build());
        coinService.create.createCoin(promoWithMskuRestriction, defaultAuth(THIRD_UID).build());

        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), contains(
                hasProperty("coinRestrictions", is(empty()))
        ));
        assertThat(marketLoyaltyClient.getCoinsForUser(ANOTHER_UID, 5).getCoins(), contains(
                hasProperty("coinRestrictions", contains(
                        allOf(
                                hasProperty("restrictionType", equalTo(CoinRestrictionType.CATEGORY)),
                                hasProperty("categoryId", is(in(categories))),
                                hasProperty("msku", nullValue())
                        )
                ))
        ));
        assertThat(marketLoyaltyClient.getCoinsForUser(THIRD_UID, 5).getCoins(), contains(
                hasProperty("coinRestrictions", contains(
                        allOf(
                                hasProperty("restrictionType", equalTo(CoinRestrictionType.MSKU)),
                                hasProperty("categoryId", nullValue()),
                                hasProperty("msku", is(in(ImmutableSet.of("123", "1123", "12312"))))
                        )
                ))
        ));
    }

    @Test
    public void shouldNotReturnRestrictionForNotLogic() {
        RuleContainer.Builder<?> rulesContainer = RuleContainer.builder(CATEGORY_FILTER_RULE)
                .withParams(CATEGORY_ID, Collections.singleton(12321))
                .withSingleParam(NOT_LOGIC, true);

        Promo promoWithNotCategoryRestriction = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(rulesContainer)
        );

        coinService.create.createCoin(promoWithNotCategoryRestriction, defaultAuth().build());

        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), contains(
                hasProperty("coinRestrictions", is(empty()))
        ));
    }

    @Test
    public void shouldReturnRandomFutureCoins() {
        @SuppressWarnings("StringConcatenationMissingWhitespace")
        Set<String> titlesOfUserCoins = IntStream.range(0, 5)
                .mapToObj(i -> "toCreateCoin" + i)
                .collect(Collectors.toSet());
        titlesOfUserCoins
                .forEach(title -> {
                    Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                            defaultFixed(
                                    DEFAULT_COIN_FIXED_NOMINAL,
                                    defaultCoinDescriptionBuilder().setTitle(title)
                            )
                    );
                    coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
                });

        @SuppressWarnings("StringConcatenationMissingWhitespace")
        Set<String> titlesOfFutureCoins = IntStream.range(0, 10)
                .mapToObj(i -> "futureCoins" + i)
                .collect(Collectors.toSet());

        titlesOfFutureCoins.forEach(title ->
                promoManager.createSmartShoppingPromo(
                        defaultFixed(
                                DEFAULT_COIN_FIXED_NOMINAL,
                                defaultCoinDescriptionBuilder().setTitle(title)
                        )
                )
        );

        coinsController.reloadFutureCoins();

        int count = 0;
        while (true) {
            try {
                checkThatResponseIsRandom(titlesOfUserCoins);
                return;
            } catch (AssertionError e) {
                count++;
                //одна ошибка может быть случайностью
                if (count > 3) {
                    throw e;
                }
            }
        }
    }

    @SuppressWarnings("ObjectAllocationInLoop")
    private void checkThatResponseIsRandom(Set<String> titlesOfUserCoins) {
        int limitFeatureCoins = 4;
        List<List<String>> responses = IntStream.range(0, 5).mapToObj(
                i -> makeExceptionsUnchecked(() -> marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, limitFeatureCoins)
                        .getFutureCoins()
                        .stream()
                        .map(BaseCoinResponse::getTitle)
                        .collect(Collectors.toList())
                )).collect(Collectors.toList());

        for (List<String> fetchedTitles : responses) {
            assertThat(fetchedTitles, hasSize(limitFeatureCoins));
            assertThat(fetchedTitles, hasSize(new HashSet<>(fetchedTitles).size()));
            assertThat(responses, hasItem(not(contains(fetchedTitles.toArray()))));

            assertThat(fetchedTitles, not(hasItem(is(in(titlesOfUserCoins)))));
        }
    }

    @Test
    public void shouldReturnApplicableCoins() {
        mockReport(makeDefaultItem(DEFAULT_ITEM_KEY));
        OrderRequestWithoutOrderId order = orderRequestBuilder()
                .withOrderItem()
                .buildWithoutOrderId();

        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, order);

        assertThat(coinsForCart.getApplicableCoins(), is(empty()));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }


    @Test
    public void shouldReturnCoinsApplicableOnDevice() {
        mockReport(makeDefaultItem());
        final Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setCoinCreationReason(CoreCoinCreationReason.OTHER)
                        .addCoinRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, APPLICATION)
        );
        coinService.create.createCoin(smartShoppingPromo, defaultAuth(DEFAULT_UID).build());

        OrderRequestWithoutOrderId order = orderRequestBuilder()
                .withClientDeviceType(APPLICATION)
                .withOrderItem()
                .buildWithoutOrderId();

        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, order);

        assertThat(coinsForCart.getApplicableCoins(), hasSize(1));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldReturnNoCoinsIfNotApplicableOnDevice() {
        final Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setCoinCreationReason(CoreCoinCreationReason.OTHER)
                        .addCoinRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, APPLICATION)
        );
        coinService.create.createCoin(smartShoppingPromo, defaultAuth(DEFAULT_UID).build());

        OrderRequestWithoutOrderId order = orderRequestBuilder()
                .withClientDeviceType(DESKTOP)
                .withOrderItem()
                .buildWithoutOrderId();

        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, order);

        assertThat(coinsForCart.getApplicableCoins(), is(empty()));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));
    }

    @Test
    public void shouldReturnAllCoinsIfDeviceNull() {
        mockReport(makeDefaultItem());
        final Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setCoinCreationReason(CoreCoinCreationReason.OTHER)
                        .addCoinRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, APPLICATION)
        );
        coinService.create.createCoin(smartShoppingPromo, defaultAuth(DEFAULT_UID).build());

        OrderRequestWithoutOrderId order = orderRequestBuilder()
                .withClientDeviceType(null)
                .withOrderItem()
                .buildWithoutOrderId();

        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, order);

        assertThat(coinsForCart.getApplicableCoins(), hasSize(1));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldNotReturnFutureCoinsIfReasonNotOrder() {
        promoManager.createSmartShoppingPromo(
                defaultFixed().setCoinCreationReason(CoreCoinCreationReason.OTHER)
        );
        Promo orderPromo = promoManager.createSmartShoppingPromo(
                defaultFixed().setCoinCreationReason(CoreCoinCreationReason.ORDER)
        );

        coinsController.reloadFutureCoins();

        FutureCoins futureCoins = marketLoyaltyClient.getFutureCoins(10);

        assertThat(futureCoins.getFutureCoins(), hasSize(1));
        assertThat(futureCoins.getFutureCoins(), contains(hasProperty("promoId", equalTo(orderPromo.getId()))));
    }

    @Test
    public void shouldReturnNoneCoinsByDefault() {
        OrderRequestWithoutOrderId order = orderRequestBuilder().withOrderItem().buildWithoutOrderId();

        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, order);

        assertThat(coinsForCart.getApplicableCoins(), is(empty()));

        CoinsForUser coinsForUser = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5);
        assertThat(coinsForUser.getCoins(), is(empty()));
        assertThat(coinsForUser.getFutureCoins(), is(empty()));
    }

    @Test
    public void shouldReturnAllCoinsIfNoFilterRules() {
        mockReport(makeDefaultItem());
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFixed()
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderRequestWithoutOrderId order = orderRequestBuilder().withOrderItem().buildWithoutOrderId();

        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, order);

        assertThat(coinsForCart.getApplicableCoins(), contains(coinHasKey(coinKey)));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldReturnAtMostOneFreeDeliveryCoin() {
        mockReport(makeDefaultItem());
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFreeDelivery()),
                defaultAuth(DEFAULT_UID).build()
        );
        coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFreeDelivery()),
                defaultAuth(DEFAULT_UID).build()
        );

        var coinsForCart = getCoinsForCartV2WithDefaultItem();

        assertThat(coinsForCart.getApplicableCoins(), hasSize(1));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));
    }

    @Test
    public void shouldFilterByMinOrderTotal() {
        BigDecimal minOrderTotal = BigDecimal.valueOf(2000);
        BigDecimal chipItemPrice = BigDecimal.valueOf(100);
        BigDecimal expensiveItemPrice = BigDecimal.valueOf(3000);

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, minOrderTotal)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        mockReport(makeDefaultItem(DEFAULT_ITEM_KEY, chipItemPrice));
        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(chipItemPrice)
                )
                .buildWithoutOrderId());
        assertThat(coinsForCart.getApplicableCoins(), is(empty()));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));

        mockReport(makeDefaultItem(DEFAULT_ITEM_KEY, expensiveItemPrice));
        coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(expensiveItemPrice)
                )
                .buildWithoutOrderId());
        assertThat(coinsForCart.getApplicableCoins(), contains(coinHasKey(coinKey)));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldRejectFreeDeliveryCoinByMinOrderTotal() {
        BigDecimal minOrderTotal = BigDecimal.valueOf(699);
        BigDecimal itemPrice = BigDecimal.valueOf(390);

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery().addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, minOrderTotal)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        mockReport(makeDefaultItem(DEFAULT_ITEM_KEY, itemPrice));
        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(itemPrice)
                )
                .buildWithoutOrderId());
        assertThat(coinsForCart.getApplicableCoins(), is(empty()));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));

        final MultiCartWithBundlesDiscountResponse calcRes = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(itemPrice)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withDeliveries(courierDelivery()).build())
                        .withCoins(coinKey)
                        .build());
        assertThat(calcRes.getCoinErrors(), hasSize(1));
    }

    @Test
    public void shouldFilterByMaxOrderTotal() {
        mockReport(makeDefaultItem());
        BigDecimal maxOrderTotal = BigDecimal.valueOf(2000);
        BigDecimal chipItemPrice = BigDecimal.valueOf(100);
        BigDecimal expensiveItemPrice = BigDecimal.valueOf(3000);

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(UPPER_BOUND_DISCOUNT_BASE_RULE, MAX_ORDER_TOTAL, maxOrderTotal)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(expensiveItemPrice)
                )
                .buildWithoutOrderId());
        assertThat(coinsForCart.getApplicableCoins(), contains(coinHasKey(coinKey)));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));

        coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(chipItemPrice)
                )
                .buildWithoutOrderId());
        assertThat(coinsForCart.getApplicableCoins(), contains(coinHasKey(coinKey)));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldReturnAllApplicableCoins() {
        String mskuForFirstCoin = "123141412";
        int categoryForSecondCoin = 879465459;
        Promo firstPromo = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(MSKU_FILTER_RULE, MSKU_ID, mskuForFirstCoin)
        );

        Promo secondPromo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categoryForSecondCoin)
                        .addCoinRule(PURCHASE_BY_LIST_RULE, SUPPLIER_ID, Set.of(1L))
        );

        CoinKey firstCoinKey = coinService.create.createCoin(firstPromo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(secondPromo, defaultAuth().build());

        mockReport(
                ReportMockUtils.offerBuilder()
                        .withItemKey(DEFAULT_ITEM_KEY)
                        .withMsku(mskuForFirstCoin)
                        .build(),
                makeDefaultItem(THIRD_ITEM_KEY)
        );
        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        msku(mskuForFirstCoin),
                        supplier(EXCLUDED_SUPPLIER_ID)
                )
                .withOrderItem(itemKey(THIRD_ITEM_KEY))
                .buildWithoutOrderId());
        assertThat(coinsForCart.getApplicableCoins(), contains(coinHasKey(firstCoinKey)));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));

        mockReport(
                ReportMockUtils.offerBuilder()
                        .withItemKey(ANOTHER_ITEM_KEY)
                        .withCategory(categoryForSecondCoin)
                        .build(),
                makeDefaultItem(THIRD_ITEM_KEY)
        );
        coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        categoryId(categoryForSecondCoin)
                )
                .withOrderItem(itemKey(THIRD_ITEM_KEY))
                .buildWithoutOrderId());
        assertThat(coinsForCart.getApplicableCoins(), contains(coinHasKey(secondCoinKey)));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));

        mockReport(
                ReportMockUtils.offerBuilder()
                        .withItemKey(DEFAULT_ITEM_KEY)
                        .withMsku(mskuForFirstCoin)
                        .build(),
                ReportMockUtils.offerBuilder()
                        .withItemKey(ANOTHER_ITEM_KEY)
                        .withCategory(categoryForSecondCoin)
                        .build()
        );
        coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        msku(mskuForFirstCoin)
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        categoryId(categoryForSecondCoin)
                )
                .withOrderItem(itemKey(THIRD_ITEM_KEY))
                .buildWithoutOrderId());
        assertThat(
                coinsForCart.getApplicableCoins(),
                containsInAnyOrder(
                        coinHasKey(firstCoinKey),
                        coinHasKey(secondCoinKey)
                )
        );
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldGetCoinNotFound() {
        MarketLoyaltyException ex = assertThrows(MarketLoyaltyException.class, () -> marketLoyaltyClient.getCoin(100));
        // TODO нормально прокидывать 404 MARKETDISCOUNT-1130
        assertEquals(((HttpClientErrorException) ex.getCause()).getRawStatusCode(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldGetCoinNotFoundIfNotOwner() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
        );
        final CoinKey coin = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        MarketLoyaltyException ex = assertThrows(
                MarketLoyaltyException.class, () -> marketLoyaltyClient.getCoin(coin.getId(), ANOTHER_UID));
        // TODO нормально прокидывать 404 MARKETDISCOUNT-1130
        assertEquals(((HttpClientErrorException) ex.getCause()).getRawStatusCode(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldGetCoinWithActivationTokenNotFoundIfNotOwner() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
        );
        final CoinKey coin = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        MarketLoyaltyException ex = assertThrows(
                MarketLoyaltyException.class, () -> marketLoyaltyClient.getCoin(coin.getId(), ANOTHER_UID, true));
        // TODO нормально прокидывать 404 MARKETDISCOUNT-1130
        assertEquals(((HttpClientErrorException) ex.getCause()).getRawStatusCode(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldIgnoreLargeDimensionItemWhenUseWeightV2() {
        mockReport(
                makeDefaultItem(DEFAULT_ITEM_KEY),
                makeLargeDimensionItem(ANOTHER_ITEM_KEY)
        );

        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFreeDelivery()),
                defaultAuth(DEFAULT_UID).build()
        );

        var coinsForLargeCart = getCoinsForCartV2(
                DEFAULT_UID, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(anotherItem()))
        );

        var coinsForDefaultCart = getCoinsForCartV2(
                DEFAULT_UID, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItem()))
        );
        assertThat("Wrong coins count for first cart", coinsForLargeCart.getApplicableCoins(), hasSize(1));
        assertThat("Wrong coins count for second cart", coinsForDefaultCart.getApplicableCoins(), hasSize(1));
    }

    @Test
    public void shouldSkipDeliveryCoinIfLargeSizedCartV2() {
        mockReport(
                makeDefaultItem(DEFAULT_ITEM_KEY),
                makeLargeWeightItem(ANOTHER_ITEM_KEY)
        );

        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        var coinKey = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFreeDelivery()),
                defaultAuth().build()
        );

        var coinsForDefaultCart = getCoinsForCartV2(
                DEFAULT_UID, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItem()))
        );

        var coinsForLargeCart = getCoinsForCartV2(
                DEFAULT_UID, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(anotherItem()))
        );

        var coinsForManyDefaultCart = getCoinsForCartV2(
                DEFAULT_UID, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItems(21)))
        );

        ensureCoinIsApplicableAndDisabledCoinsIsEmpty(coinKey, coinsForDefaultCart);

        ensureApplicableCoinsIsEmptyAndCoinIsDisabled(coinKey, coinsForLargeCart, LSC_RESTRICTION);
        assertThat(
                coinsForLargeCart.getDisabledCoins().keySet().iterator().next(),
                equalTo(LSC_RESTRICTION)
        );

        ensureApplicableCoinsIsEmptyAndCoinIsDisabled(coinKey, coinsForManyDefaultCart, LSC_RESTRICTION);
        assertThat(
                coinsForManyDefaultCart.getDisabledCoins().keySet().iterator().next(),
                equalTo(LSC_RESTRICTION)
        );
    }

// [TODO] MARKETDISCOUNT-5480

//    @Test
//    public void shouldSkipCoinsIfDropshipItemsOnly() {
//        Promo promo = promoManager.createSmartShoppingPromo(
//            defaultFreeDelivery()
//        );
//        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());
//
//        mockReport(makeDropshipItem(DEFAULT_ITEM_KEY), makeDefaultItem(ANOTHER_ITEM_KEY));
//        CoinsForCart coinsForCart1 = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
//            .withOrderItem(dropship())
//            .withDeliveries(courierDelivery(SELECTED))
//            .buildWithoutOrderId());
//        assertThat(coinsForCart1.getApplicableCoins(), is(empty()));
//
//        CoinsForCart coinsForCart2 = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
//            .withOrderItem(itemKey(ANOTHER_ITEM_KEY))
//            .withDeliveries(courierDelivery(SELECTED))
//            .buildWithoutOrderId());
//        assertThat(coinsForCart2.getApplicableCoins(), hasSize(1));
//    }

//    @Test
//    public void shouldReturnDisabledCoinsIfDropshipItemsOnly() {
//        Promo promo = promoManager.createSmartShoppingPromo(
//            defaultFreeDelivery()
//        );
//        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());
//
//        mockReport(makeDropshipItem(ANOTHER_ITEM_KEY), makeDefaultItem());
//        CoinsForCart coinsForCart1 = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
//            .withOrderItem(dropship(), itemKey(ANOTHER_ITEM_KEY))
//            .withDeliveries(courierDelivery(SELECTED))
//            .buildWithoutOrderId());
//        assertThat(coinsForCart1.getDisabledCoins().entrySet(), hasSize(1));
//
//        CoinsForCart coinsForCart2 = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
//            .withOrderItem()
//            .withDeliveries(courierDelivery(SELECTED))
//            .buildWithoutOrderId());
//        assertThat(coinsForCart2.getDisabledCoins().entrySet(), is(empty()));
//
//    }

    @Test
    public void shouldReturnCoinsIfNotDropshipItemsOnly() {
        mockReport(makeDefaultItem(), makeDropshipItem(ANOTHER_ITEM_KEY));
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        var coinKey = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFreeDelivery()),
                defaultAuth(DEFAULT_UID).build()
        );

        CoinsForCart coinsForCart = getCoinsForCartV2(
                DEFAULT_UID, DESKTOP, new OrderItemsRequest(Arrays.asList(defaultItem(), anotherItem()))
        );

        ensureCoinIsApplicableAndDisabledCoinsIsEmpty(coinKey, coinsForCart);
    }

    private static FoundOffer makeDropshipItem(ItemKey itemKey) {
        FoundOffer foundOffer = makeDefaultItem(itemKey);
        foundOffer.setDeliveryPartnerTypes(Collections.singletonList("SHOP"));
        return foundOffer;
    }

    private static FoundOffer makeExpressItem(ItemKey itemKey) {
        FoundOffer foundOffer = makeDefaultItem(itemKey);
        foundOffer.setIsExpress(true);
        return foundOffer;
    }
// [TODO] MARKETDISCOUNT-5480

//    @Test
//    public void shouldReturnDropshipRestrictionForDisabledCoinsIfDropshipItemsOnlyAndLDI() {
//        mockReport(makeDropshipItem(DEFAULT_ITEM_KEY), makeDropshipItem(ANOTHER_ITEM_KEY));
//        Promo freeDeliveryPromo = promoManager.createSmartShoppingPromo(
//            defaultFreeDelivery()
//        );
//        coinService.create.createCoin(freeDeliveryPromo, defaultAuth(DEFAULT_UID).build());
//
//        Promo percentPromo = promoManager.createSmartShoppingPromo(
//            defaultPercent()
//        );
//        coinService.create.createCoin(percentPromo, defaultAuth(DEFAULT_UID).build());
//
//        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
//            .withOrderItem(dropship(), b -> b.setLargeDiminsionItem(true))
//            .withOrderItem(dropship(), itemKey(ANOTHER_ITEM_KEY))
//            .withDeliveries(courierDelivery(SELECTED))
//            .buildWithoutOrderId());
//
//        assertThat(coinsForCart.getApplicableCoins(), is(empty()));
//        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));
//        Map.Entry<CoinApplicationRestrictionType, List<UserCoinResponse>> disabledCoins =
//            coinsForCart.getDisabledCoins().entrySet().iterator().next();
//        assertEquals(CoinApplicationRestrictionType.DROPSHIP_RESTRICTION, disabledCoins.getKey());
//        assertThat(disabledCoins.getValue(), hasSize(2));
//    }

    @Test
    public void shouldReturnFreeDeliveryExpressCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
                        .addCoinRule(SUPPLIER_FLAG_RESTRICTION_FILTER_RULE, SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE)
        );
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        mockReport(makeExpressItem(DEFAULT_ITEM_KEY));
        CoinsForCart coinsForCart1 = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem()
                .withDeliveries(courierDelivery(
                        withFeatures(Set.of(DeliveryFeature.EXPRESS)),
                        withPrice(BigDecimal.valueOf(350))))
                .buildWithoutOrderId());
        assertThat(coinsForCart1.getApplicableCoins(), is(not(empty())));
    }

    @Test
    public void shouldNotReturnFreeDeliveryExpressCoinForNotExpressItem() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
                        .addCoinRule(SUPPLIER_FLAG_RESTRICTION_FILTER_RULE, SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE)
        );
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());
        mockReport(makeDefaultItem(ANOTHER_ITEM_KEY));
        CoinsForCart coinsForCart1 = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, orderRequestBuilder()
                .withOrderItem()
                .withDeliveries(courierDelivery(
                        withPrice(BigDecimal.valueOf(350))))
                .buildWithoutOrderId());
        assertThat(coinsForCart1.getApplicableCoins(), is(empty()));
    }

    @Test
    public void shouldGetUsedCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(orderRequestBuilder().withOrderItem().build())
                .withCoins(coinKey)
                .build()
        );

        UserCoinResponse coin = marketLoyaltyClient.getCoin(coinKey.getId());
        assertEquals(CoinStatus.USED, coin.getStatus());
    }

    @Test
    public void shouldNotGetUsedCoinsForUser() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(orderRequestBuilder().withOrderItem().build())
                .withCoins(coinKey)
                .build()
        );
        CoinsForUser coinsForUser = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5);

        assertThat(coinsForUser.getCoins(), is(empty()));
    }

    @Test
    public void shouldSortCoinsByState() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        CoinKey inactiveCoinKey = coinService.create.createCoin(promo, defaultAuth().setStatus(INACTIVE).build());
        CoinKey activeCoinKey = coinService.create.createCoin(promo, defaultAuth().build());


        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), contains(
                hasProperty("id", equalTo(activeCoinKey.getId())),
                hasProperty("id", equalTo(inactiveCoinKey.getId()))
        ));
    }

    @Test
    public void shouldGetFutureCoins() {
        promoManager.createSmartShoppingPromo(defaultFixed());
        coinsController.reloadFutureCoins();

        FutureCoins coinsForUser = marketLoyaltyClient.getFutureCoins(5);

        assertThat(coinsForUser.getFutureCoins(), hasSize(1));
    }

    @Test
    public void shouldNotGetUsedCoinsForCart() {
        mockReport(makeDefaultItem());
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(orderRequestBuilder().withOrderItem().build())
                .withCoins(coinKey)
                .build()
        );
        CoinsForCart coinsForUser = marketLoyaltyClient.getCoinsForCart(
                DEFAULT_UID,
                orderRequestBuilder()
                        .withOrderItem()
                        .buildWithoutOrderId()
        );

        assertThat(coinsForUser.getApplicableCoins(), is(empty()));
        assertThat(coinsForUser.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldGetDeliveryCoin() {
        mockReport(makeDefaultItem());
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());

        coinService.create.createCoin(promo, defaultAuth().setStatus(USED).build());
        var coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        var coinsForCart = getCoinsForCartV2WithDefaultItem();

        ensureCoinIsApplicableAndDisabledCoinsIsEmpty(coinKey, coinsForCart);
    }

    @Test
    public void shouldNotGetDeliveryCoin() {
        mockReport(makeDefaultItem(LITTLE_LESS_THAN_DEFAULT_THRESHOLD));
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        var promo = promoManager.createSmartShoppingPromo(
                defaultFreeDeliveryWithMinOrderTotalRule(BigDecimal.valueOf(10))
        );

        coinService.create.createCoin(promo, defaultAuth().setStatus(USED).build());
        var coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        var coinsForCart = getCoinsForCartV2WithDefaultItem();

        ensureApplicableCoinsIsEmptyAndCoinIsDisabled(coinKey, coinsForCart);
    }

    @Test
    public void shouldGetActiveCoinsCount() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        assertEquals(1, marketLoyaltyClient.getCoinsCount(DEFAULT_UID));

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder().withOrderItem()
                                .build()
                )
                .withCoins(coinKey)
                .build()
        );

        assertEquals(0, marketLoyaltyClient.getCoinsCount(DEFAULT_UID));
    }

    @Test
    public void shouldNotReturnInactiveCoinForOrder() {
        mockReport(makeDefaultItem());
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).setStatus(INACTIVE).build());

        OrderRequestWithoutOrderId order = orderRequestBuilder().withOrderItem().buildWithoutOrderId();

        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(DEFAULT_UID, order);

        assertThat(coinsForCart.getApplicableCoins(), is(empty()));
        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldReturnInactiveCoinForPerson() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).setStatus(INACTIVE).build());

        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(), hasSize(1));
    }

    @Test
    public void shouldGetCoinsCountForInactiveCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).setStatus(INACTIVE).build());

        assertEquals(1, marketLoyaltyClient.getCoinsCount(DEFAULT_UID));
    }

    @Test
    public void shouldGetCoinsCountForMuid() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultNoAuth().build());

        assertEquals(1, marketLoyaltyClient.getCoinsCountForMuid(DEFAULT_MUID));
    }

    @Test
    public void shouldGetCoinsForMuid() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(marketLoyaltyClient.getCoinsForUserForMuid(DEFAULT_MUID, 5).getCoins(), hasSize(1));
    }

    @Test
    public void shouldNotCountExpiredCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultNoAuth().build());

        clock.spendTime(DEFAULT_EXPIRATION_DAYS + 1, ChronoUnit.DAYS);

        assertEquals(0, marketLoyaltyClient.getCoinsCountForMuid(DEFAULT_MUID));
    }

    @Test
    public void shouldNotGetExpiredCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultNoAuth().build());

        clock.spendTime(DEFAULT_EXPIRATION_DAYS + 1, ChronoUnit.DAYS);

        assertThat(marketLoyaltyClient.getCoinsForUserForMuid(DEFAULT_MUID, 5).getCoins(), is(empty()));
    }

    @Test
    public void shouldGetCoinsCountForUnknownUid() {
        assertEquals(0, marketLoyaltyClient.getCoinsCount(DEFAULT_UID));
    }

    @Test
    public void shouldNotFailWhenAllOffersExpired() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
        );
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        CoinsForCart coinsForCart = getCoinsForCartV2(
                DEFAULT_UID, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItem()))
        );

        assertThat(coinsForCart.getApplicableCoins(), is(empty()));

        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));
        assertThat(
                coinsForCart.getDisabledCoins().keySet().iterator().next(),
                equalTo(FILTER_RULE_RESTRICTION)
        );
    }


    @Test
    public void shouldFilterCoinIfDeliveryRegionMismatch() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery().addCoinRule(DELIVERY_REGION_CART_CUTTING_RULE, REGION_ID,
                        Collections.singleton(213))
        );
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        mockReport(makeDefaultItem());

        CoinsForCart coinsForCart = getCoinsForCartV2(
                DEFAULT_UID, 123, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItem()))
        );

        assertThat(coinsForCart.getApplicableCoins(), is(empty()));

        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));
        assertThat(
                coinsForCart.getDisabledCoins().keySet().iterator().next(),
                equalTo(CoinApplicationRestrictionType.REGION_RESTRICTION)
        );
    }

    @Test
    public void shouldAllowBonusOnExludedCategoryIfExcludedCategoriesIgnored() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100)).
                        addCoinRule(EXCLUDED_OFFERS_FILTER_RULE, EXCLUDED_OFFERS_TYPE, ExcludedOffersType.NONE)
        );
        coinService.create.createCoin(
                smartShoppingPromo,
                defaultAuth().build()
        );

        mockReport(makeDefaultItem(STICK_CATEGORY));

        CoinsForCart coinsForCart = getCoinsForCartV2(
                DEFAULT_UID, 123, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItem()))
        );

        assertThat(coinsForCart.getApplicableCoins(), hasSize(1));

        assertThat(coinsForCart.getDisabledCoins().entrySet(), is(empty()));
    }

    @Test
    public void shouldNotFailOnRepeatedItemsWithBundles() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
        );
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        CoinsForCart coinsForCart = getCoinsForCartV2(
                DEFAULT_UID, UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Arrays.asList(
                        defaultItem(),
                        anotherItem(),
                        itemBuilder()
                                .setBundleId("some bundle item")
                                .setBundleId("some bundle")
                                .build()
                ))
        );

        assertThat(coinsForCart.getApplicableCoins(), is(empty()));

        assertThat(coinsForCart.getDisabledCoins().entrySet(), hasSize(1));
        assertThat(
                coinsForCart.getDisabledCoins().keySet().iterator().next(),
                equalTo(FILTER_RULE_RESTRICTION)
        );
    }

    @Test
    public void shouldFailOnDuplicatedItems() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
        );
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                getCoinsForCartV2(
                        DEFAULT_UID, UsageClientDeviceType.DESKTOP,
                        new OrderItemsRequest(Arrays.asList(
                                defaultItem(),
                                defaultItem()
                        ))
                )
        );

        assertThat(exception.getMarketLoyaltyErrorCode(), is(MarketLoyaltyErrorCode.DUPLICATE_ITEMS_IN_REQUEST));
        assertThat(exception.getMessage(), startsWith("Duplicate items in request "));
    }

    @Test
    public void shouldReturnFalseForIsCategoryBonusForCart() {
        mockReport(makeDefaultItem());

        coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                defaultAuth(DEFAULT_UID).build()
        );

        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(), false);
    }

    @Test
    public void shouldReturnFalseForIsCategoryBonusForPerson() {
        coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                defaultAuth(DEFAULT_UID).build()
        );

        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(),
                false
        );
    }

    @Test
    public void shouldReturnTrueForIsCategoryBonusIfPromoHasMskuRule() {
        createPromoAndCoinWithRules(createRulesContainer(
                RuleContainer.builder(MSKU_FILTER_RULE)
                        .withParams(MSKU_ID, Collections.singleton(DEFAULT_MSKU))
                        .build()
        ), DEFAULT_MSKU, DEFAULT_VENDOR_ID);

        CoinsForUser coinsForUser = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5);

        ensureCoinsSingleSizedAndHasCategoryBonusValue(coinsForUser.getCoins(), true);
        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(),
                true
        );
    }

    @Test
    public void shouldReturnTrueForIsCategoryBonusIfPromoHasVendorRule() {
        createPromoAndCoinWithRules(
                createRulesContainer(RuleContainer.builder(RuleType.VENDOR_FILTER_RULE)
                        .withParams(VENDOR_ID, Collections.singleton(DEFAULT_VENDOR_ID))
                        .build()), DEFAULT_MSKU, DEFAULT_VENDOR_ID
        );

        CoinsForUser coinsForUser = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5);
        ensureCoinsSingleSizedAndHasCategoryBonusValue(coinsForUser.getCoins(), true);
        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(),
                true
        );
    }

    @Test
    public void shouldReturnTrueForIsCategoryBonusIfPromoHasCategoryRule() {
        createPromoAndCoinWithRules(createRulesContainer(
                RuleContainer.builder(CATEGORY_FILTER_RULE)
                        .withParams(CATEGORY_ID, Collections.singleton(DEFAULT_CATEGORY_ID))
                        .build()
        ), DEFAULT_MSKU, DEFAULT_VENDOR_ID);

        CoinsForUser coinsForUser = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5);
        List<UserCoinResponse> coins = coinsForUser.getCoins();
        ensureCoinsSingleSizedAndHasCategoryBonusValue(coins, true);
        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(),
                true
        );
    }

    @Test
    public void shouldNotReturnInactiveTopCoinsForUser() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).setStatus(INACTIVE).build());

        CoinsForUser coinsForUser = marketLoyaltyClient.getTopCoinsForUser(DEFAULT_UID, 1, null);

        assertThat(coinsForUser.getCoins(), is(empty()));
    }

    @Test
    public void shouldReturnTopCoinsForUserWithDefaultSortType() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        CoinsForUser coinsForUser = marketLoyaltyClient.getTopCoinsForUser(DEFAULT_UID, 1, null);

        assertThat(coinsForUser.getCoins(), hasSize(1));
    }

    @Test
    public void shouldReturnTopCoinsForUserRightNominalOrder() {
        Promo promo1 = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(100)));
        Promo promo2 = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(300)));
        Promo promo3 = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(1000)));
        coinService.create.createCoin(promo1, defaultAuth(DEFAULT_UID).build());
        coinService.create.createCoin(promo2, defaultAuth(DEFAULT_UID).build());
        coinService.create.createCoin(promo3, defaultAuth(DEFAULT_UID).build());

        CoinsForUser coinsForUser = marketLoyaltyClient.getTopCoinsForUser(DEFAULT_UID, 3, PersonTopSortType.TOP);

        assertThat(coinsForUser.getCoins(), contains(
                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(1000))),
                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(300))),
                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(100)))
        ));
    }

    @Test
    public void shouldReturnTopCoinsForUserWithTopSortType() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        CoinsForUser coinsForUser = marketLoyaltyClient.getTopCoinsForUser(DEFAULT_UID, 1, PersonTopSortType.TOP);

        assertThat(coinsForUser.getCoins(), hasSize(1));
    }

    @Test
    public void shouldReturnTopCoinsForUserWithRandomSortType() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        CoinsForUser coinsForUser = marketLoyaltyClient.getTopCoinsForUser(DEFAULT_UID, 1, PersonTopSortType.RANDOM);

        assertThat(coinsForUser.getCoins(), hasSize(1));
    }

    @Test
    public void shouldReturnFalseForIsCategoryBonusForUserIfPromoHasMskuRuleWithNotLogic() {
        createPromoAndCoinWithRules(createRulesContainer(
                RuleContainer.builder(MSKU_FILTER_RULE)
                        .withParams(NOT_LOGIC, Collections.singleton(true))
                        .withParams(MSKU_ID, Collections.singleton(DEFAULT_MSKU))
                        .build()
        ), "12345", DEFAULT_VENDOR_ID);

        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(),
                false
        );
        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(),
                false
        );
    }

    @Test
    public void shouldReturnFalseForIsCategoryBonusForUserIfPromoHasVendorRuleWithNotLogic() {
        createPromoAndCoinWithRules(createRulesContainer(
                RuleContainer.builder(VENDOR_FILTER_RULE)
                        .withParams(NOT_LOGIC, Collections.singleton(true))
                        .withParams(VENDOR_ID, Collections.singleton(DEFAULT_VENDOR_ID))
                        .build()
        ), DEFAULT_MSKU, 1L);

        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(),
                false
        );
        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(),
                false
        );
    }

    @Test
    public void shouldReturnFalseForIsCategoryBonusForUserIfPromoHasCategoryRuleWithNotLogic() {
        createPromoAndCoinWithRules(createRulesContainer(
                RuleContainer.builder(CATEGORY_FILTER_RULE)
                        .withParams(NOT_LOGIC, Collections.singleton(true))
                        .withParams(CATEGORY_ID, Collections.singleton(1111))
                        .build()
        ), DEFAULT_MSKU, DEFAULT_VENDOR_ID);

        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(),
                false
        );
        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(),
                false
        );
    }

    @Test
    public void shouldReturnTrueForIsCategoryBonusForUserIfPromoHasAnyRuleWithNotLogic() {
        RulesContainer rulesContainer = createRulesContainer(
                RuleContainer.builder(MSKU_FILTER_RULE)
                        .withParams(NOT_LOGIC, Collections.singleton(true))
                        .withParams(MSKU_ID, Collections.singleton("1"))
                        .build()
        );
        rulesContainer.add(
                RuleContainer.builder(VENDOR_FILTER_RULE)
                        .withParams(VENDOR_ID, Collections.singleton(DEFAULT_VENDOR_ID))
                        .build());

        rulesContainer.add(
                RuleContainer.builder(CATEGORY_FILTER_RULE)
                        .withParams(CATEGORY_ID, Collections.singleton(DEFAULT_CATEGORY_ID))
                        .build());

        createPromoAndCoinWithRules(rulesContainer, DEFAULT_MSKU, DEFAULT_VENDOR_ID);

        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(),
                true
        );
        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(),
                true
        );
    }

    @Test
    public void shouldReturnTrueForIsCategoryBonusForUserIfPromoDoesNotHaveNotLogicInAnyRule() {
        RulesContainer rulesContainer = createRulesContainer(
                RuleContainer.builder(MSKU_FILTER_RULE)
                        .withParams(MSKU_ID, Collections.singleton(DEFAULT_MSKU))
                        .build()
        );

        rulesContainer.add(
                RuleContainer.builder(VENDOR_FILTER_RULE)
                        .withParams(VENDOR_ID, Collections.singleton(DEFAULT_VENDOR_ID))
                        .build());

        rulesContainer.add(
                RuleContainer.builder(CATEGORY_FILTER_RULE)
                        .withParams(CATEGORY_ID, Collections.singleton(DEFAULT_CATEGORY_ID))
                        .build());

        createPromoAndCoinWithRules(rulesContainer, DEFAULT_MSKU, DEFAULT_VENDOR_ID);

        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5).getCoins(),
                true
        );
        ensureCoinsSingleSizedAndHasCategoryBonusValue(
                getCoinsForCartV2WithDefaultItem().getApplicableCoins(),
                true
        );
    }

    @Test
    public void shouldReturnApplicableCoinsIfCoinIsNotFreeDeliveryEvenIfCartPriceSatisfyingFreeDelivery() {
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();
        mockReport(makeDefaultItem(DEFAULT_ITEM_KEY, DEFAULT_THRESHOLD));
        CoinKey coinKey = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                defaultAuth(DEFAULT_UID).build()
        );

        CoinsForCart coinsForCart = getCoinsForCartV2(
                DEFAULT_UID,
                DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItem()))
        );

        ensureCoinIsApplicableAndDisabledCoinsIsEmpty(coinKey, coinsForCart);
    }

    @Test
    public void shouldReturnApplicableCoinsIfOneSatisfyingThresholdButAnotherIsDropship() {
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        FoundOffer defaultItem = makeDefaultItem();
        FoundOffer dropshipItem = makeDefaultItem(ANOTHER_ITEM_KEY);
        dropshipItem.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.SHOP.getCode()));

        mockReport(defaultItem, dropshipItem);
        CoinKey coinKey = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFreeDelivery()),
                defaultAuth().build()
        );

        CoinsForCart coinsForCart = getCoinsForCartV2(
                DEFAULT_UID,
                DESKTOP,
                new OrderItemsRequest(
                        Arrays.asList(defaultItem(), anotherItem())
                )
        );

        ensureCoinIsApplicableAndDisabledCoinsIsEmpty(coinKey, coinsForCart);
    }

    @Test
    public void shouldNotReturnApplicableCoinsIfCartSatisfyingThresholdButOneItemIsDropship() {
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        FoundOffer defaultItem = makeDefaultItem(DEFAULT_ITEM_KEY, LITTLE_LESS_THAN_DEFAULT_THRESHOLD);
        FoundOffer dropshipItem = makeDefaultItem(ANOTHER_ITEM_KEY, DEFAULT_THRESHOLD);
        dropshipItem.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.SHOP.getCode()));

        mockReport(defaultItem, dropshipItem);
        CoinKey coinKey = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFreeDeliveryWithMinOrderTotalRule(BigDecimal.ONE)),
                defaultAuth().build()
        );

        CoinsForCart coinsForCart = getCoinsForCartV2(
                DEFAULT_UID,
                DESKTOP,
                new OrderItemsRequest(
                        Arrays.asList(defaultItem(), anotherItem())
                )
        );

        ensureApplicableCoinsIsEmptyAndCoinIsDisabled(coinKey, coinsForCart);
    }

    @Test
    public void shouldGetCoinRestrictions() {
        final ImmutableSet<Integer> categories = ImmutableSet.of(123, 1123, 12312);
        final CoinKey coinKey = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFixed().addCoinRule(
                        CATEGORY_FILTER_RULE,
                        CATEGORY_ID,
                        categories
                )),
                defaultAuth(DEFAULT_UID).build()
        );

        final CoinRestrictionsResponseList response = marketLoyaltyClient.getCoinRestrictions(
                coinKey.getId());

        assertThat(
                response,
                hasProperty(
                        "coinRestrictions",
                        contains(
                                allOf(
                                        hasProperty(
                                                "coinId",
                                                equalTo(Long.toString(coinKey.getId()))
                                        ),
                                        hasProperty(
                                                "restrictions",
                                                allOf(
                                                        hasProperty("hid", equalTo(categories)),
                                                        hasProperty("msku", is(nullValue())),
                                                        hasProperty("vendor", is(nullValue())),
                                                        hasProperty("not", allOf(
                                                                hasProperty("hid", equalTo(getExcludedHids())),
                                                                hasProperty("msku", equalTo(getExcludedMskus())),
                                                                hasProperty("vendor", is(HARD_EXCLUDED_VENDORS))
                                                        ))
                                                )
                                        )
                                ))
                )
        );
    }

    @Test
    public void shouldGetCoinRestrictionsWithoutExcludedOffers() {
        final ImmutableSet<Integer> categories = ImmutableSet.of(STICK_CATEGORY);
        final CoinKey coinKey = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()
                        .addCoinRule(
                                CATEGORY_FILTER_RULE,
                                CATEGORY_ID,
                                categories
                        )
                        .addCoinRule(
                                EXCLUDED_OFFERS_FILTER_RULE,
                                EXCLUDED_OFFERS_TYPE,
                                ExcludedOffersType.NONE
                        )
                ),
                defaultAuth(DEFAULT_UID).build()
        );

        final CoinRestrictionsResponseList response = marketLoyaltyClient.getCoinRestrictions(
                coinKey.getId());

        assertThat(
                response,
                hasProperty(
                        "coinRestrictions",
                        contains(
                                allOf(
                                        hasProperty(
                                                "coinId",
                                                equalTo(Long.toString(coinKey.getId()))
                                        ),
                                        hasProperty(
                                                "restrictions",
                                                allOf(
                                                        hasProperty("hid", equalTo(categories)),
                                                        hasProperty("msku", is(nullValue())),
                                                        hasProperty("vendor", is(nullValue())),
                                                        hasProperty("supplier", is(nullValue())),
                                                        hasProperty("not", allOf(
                                                                hasProperty("hid", equalTo(Collections.emptySet())),
                                                                hasProperty("msku", equalTo(Collections.emptySet())),
                                                                hasProperty("vendor", is(HARD_EXCLUDED_VENDORS)),
                                                                hasProperty("supplier", equalTo(Collections.emptySet()))
                                                                )
                                                        )
                                                )
                                        )
                                ))
                )
        );
    }

    @Test
    public void shouldAddPromoKeyToCoinsCoinsForCartResponse() {
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();
        mockReport(makeDefaultItem(DEFAULT_ITEM_KEY, DEFAULT_THRESHOLD));
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinService.create.createCoin(
                promo,
                defaultAuth(DEFAULT_UID).build()
        );


        CoinsForCart coinsForCart = marketLoyaltyClient.getCoinsForCart(
                DEFAULT_UID,
                orderRequestBuilder().withOrderItem().buildWithoutOrderId()
        );

        checkPromoKeyInCoins(promo, coinsForCart.getApplicableCoins());
    }

    @Test
    public void shouldAddPromoKeyToUserCoinResponse() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        CoinsForUser coinsForUser = marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5);

        checkPromoKeyInCoins(promo, coinsForUser.getCoins());
    }

    @Test
    public void shouldAddPromoKeyToFutureCoinResponse() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinsController.reloadFutureCoins();
        List<FutureCoinResponse> futureCoins = marketLoyaltyClient.getFutureCoins(5).getFutureCoins();
        assertThat(
                futureCoins,
                everyItem(
                        hasProperty("promoKey", equalTo(promo.getPromoKey()))
                )
        );
    }

    private static void checkPromoKeyInCoins(Promo promo, List<UserCoinResponse> applicableCoins) {
        assertThat(
                applicableCoins,
                everyItem(
                        hasProperty("promoKey", equalTo(promo.getPromoKey()))
                )
        );
    }

    private Set<String> getExcludedMskus() {
        return excludedOffersService.getExcluded().getMskus();
    }

    @NotNull
    private Set<Integer> getExcludedHids() {
        return excludedOffersService.getExcluded().getHids().stream().map(Long::intValue).collect(Collectors.toSet());
    }

    private static void ensureCoinIsApplicableAndDisabledCoinsIsEmpty(CoinKey coinKey, CoinsForCart coinsForCart) {
        assertThat(
                "Applicable coins problem:",
                coinsForCart.getApplicableCoins(),
                allOf(
                        hasSize(1),
                        everyItem(hasProperty("id", equalTo(coinKey.getId())))
                )
        );
        assertThat(
                "Disabled coins map isn't empty",
                coinsForCart.getDisabledCoins(),
                anEmptyMap()
        );
    }

    private static void ensureApplicableCoinsIsEmptyAndCoinIsDisabled(CoinKey coinKey, CoinsForCart coinsForCart) {
        ensureApplicableCoinsIsEmptyAndCoinIsDisabled(coinKey, coinsForCart, FILTER_RULE_RESTRICTION);
    }

    private static void ensureApplicableCoinsIsEmptyAndCoinIsDisabled(
            CoinKey coinKey,
            CoinsForCart coinsForCart,
            CoinApplicationRestrictionType restriction
    ) {
        assertThat(
                "Applicable coins map isn't empty",
                coinsForCart.getApplicableCoins(),
                emptyIterable()
        );
        assertThat(
                "There is a problem with the disabled coins map:",
                coinsForCart.getDisabledCoins(),
                allOf(
                        aMapWithSize(1),
                        hasKey(restriction)
                )
        );
        assertThat(
                "There is a problem with the free delivery threshold restriction:",
                coinsForCart.getDisabledCoins().get(restriction),
                allOf(
                        hasSize(1),
                        everyItem(hasProperty("id", equalTo(coinKey.getId()))
                        )
                )
        );
    }

    @NotNull
    private static RulesContainer createRulesContainer(RuleContainer<?> innerRule) {
        RulesContainer rulesContainer = new RulesContainer();
        rulesContainer.add(innerRule);
        return rulesContainer;
    }

    private void createPromoAndCoinWithRules(RulesContainer rulesContainer, String mskuId, long vendorId) {
        mockReport(makeDefaultItem(mskuId, vendorId));

        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setPromoKey("somePromoKey")
                        .setRulesContainer(rulesContainer)
        );
        coinService.create.createCoin(
                promo,
                defaultAuth(DEFAULT_UID)
                        .setCoinProps(
                                CoinProps.builder()
                                        .setPromoData(promo)
                                        .setType(CoreCoinType.FIXED)
                                        .setExpirationPolicy(ExpirationPolicy.toEndOfPromo())
                                        .build()
                        )
                        .build()
        );
    }

    private static void ensureCoinsSingleSizedAndHasCategoryBonusValue(
            List<UserCoinResponse> coins, boolean isCategoryBonusValue
    ) {
        assertThat(
                coins,
                allOf(
                        hasSize(1),
                        everyItem(hasProperty("categoryBonus", equalTo(isCategoryBonusValue))
                        )
                )
        );
    }

    private CoinsForCart getCoinsForCartV2WithDefaultItem() {
        return getCoinsForCartV2(DEFAULT_UID, DESKTOP, new OrderItemsRequest(Collections.singletonList(defaultItem())));
    }

    private CoinsForCart getCoinsForCartV2(
            @SuppressWarnings("SameParameterValue") long uid,
            @SuppressWarnings("SameParameterValue") UsageClientDeviceType clientDeviceType,
            OrderItemsRequest request
    ) {
        return getCoinsForCartV2(uid, DEFAULT_REGION, clientDeviceType, request);
    }

    private CoinsForCart getCoinsForCartV2(
            @SuppressWarnings("SameParameterValue") long uid,
            long regionId, @SuppressWarnings("SameParameterValue") UsageClientDeviceType clientDeviceType,
            OrderItemsRequest request
    ) {
        return marketLoyaltyClient.getCoinsForCartV2(
                uid, regionId, false, MarketPlatform.BLUE, clientDeviceType, request);
    }

    private void mockReportWithDefaultItemsWithKeys(ItemKey... keys) {
        mockReport(
                Arrays.stream(keys).map(ReportMockUtils::makeDefaultItem).toArray(FoundOffer[]::new)
        );
    }


    private void mockReport(FoundOffer... fos) {
        try {
            reportMockUtils.mockReportService(fos);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("ReportMock exception!", e);
        }
    }

    private static OrderItemsRequest.Item.Builder itemBuilder() {
        return OrderItemsRequest.Item.builder()
                .setFeedId(DEFAULT_ITEM_KEY.getFeedId())
                .setOfferId(DEFAULT_ITEM_KEY.getOfferId())
                .setCount(1);
    }

    private static OrderItemsRequest.Item defaultItem() {
        return itemBuilder().build();
    }

    private static OrderItemsRequest.Item defaultItems(int count) {
        return itemBuilder()
                .setCount(count)
                .build();
    }

    private static OrderItemsRequest.Item anotherItem() {
        return itemBuilder()
                .setFeedId(ANOTHER_ITEM_KEY.getFeedId())
                .setOfferId(ANOTHER_ITEM_KEY.getOfferId())
                .build();
    }
}
