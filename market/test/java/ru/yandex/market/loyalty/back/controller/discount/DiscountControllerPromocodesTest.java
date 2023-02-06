package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.mock.MockForShooting;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.promocode.StorePromocodeDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserReferralPromocodeDao;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoCodeGeneratorType;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.ydb.UserReferralPromocode;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.ClidCacheService;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationResult;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodesActivationResult;
import ru.yandex.market.loyalty.core.service.promocode.StorePromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.UserPromocodeInfo;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static java.sql.Timestamp.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_APPLICABLE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_EXISTS;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.MIN_ORDER_TOTAL_VIOLATED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_SUITABLE_EXCLUDED_RULES;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_SUITABLE_FILTER_RULES;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.PROMOCODE_IS_EXPIRED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.REFERRAL_PROMOCODE_NOT_APPLICABLE_WITH_OTHER_PROMOCODES;
import static ru.yandex.market.loyalty.api.model.PromoType.MARKET_COUPON;
import static ru.yandex.market.loyalty.api.model.PromoType.MARKET_PROMOCODE;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COUPON_ERROR;
import static ru.yandex.market.loyalty.core.logbroker.EventType.ORDER_REQUEST;
import static ru.yandex.market.loyalty.core.logbroker.EventType.PROMOCODE_ACTIVATED;
import static ru.yandex.market.loyalty.core.logbroker.EventType.PROMOCODE_DISCOUNT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.PROMOCODE_ERROR;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.NOT_LOGIC;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.SUPPLIER_FILTER_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.EXCLUSIONS_CONFIG_ENABLED;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.SUPPLIER_EXCLUSION_ID;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.yandexUidOperationContext;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.ANOTHER_COUPON_CODE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_CODE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_VALUE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SHOP_PROMO_ID;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultPercentPromocode;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_YANDEX_UID;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;
import static ru.yandex.market.sdk.userinfo.service.UidConstants.NO_SIDE_EFFECT_EMAIL;

@TestFor(DiscountController.class)
public class DiscountControllerPromocodesTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final String PROMOCODE = "some_promocode";
    private static final String ANOTHER_PROMOCODE = "ANOTHER_PROMOCODE";
    private static final String PARTNER_PROMOCODE = "partner_promocode";
    private static final String FIRST_ORDER_PROMOCODE = "first_order_promocode";
    private static final String REGIONAL_PROMOCODE = "regionsl_promocode";
    private static final String ANAPLAN_ID = "anaplan id";
    private static final long USER_ID = 123L;
    private static final long USER_ID_WITH_ORDERS = 234L;
    private static final long FEED_ID = 123;
    private static final String FIRST_SSKU = "first offer";
    private static final String SECOND_SSKU = "second offer";
    private static final int REGION_1 = 213;
    private static final int REGION_2 = 11;
    private static final String MSKU_1 = "MSKU_1";
    private static final String MSKU_2 = "MSKU_2";

    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ClidCacheService clidCacheService;
    @Autowired
    private StorePromocodeService storePromocodeService;
    @Autowired
    private PromoService promoService;
    @Autowired
    private StorePromocodeDao storePromocodeDao;
    @Autowired
    protected ClockForTests clock;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private UserReferralPromocodeDao userReferralPromocodeDao;

    private Promo promocode;
    private Promo partnerPromocode;
    private Promo promocodeFirstOrder;

    @Before
    public void configure() {
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.ALLOWED_REGION_CUTTING_RULE)
                .withSingleParam(RuleParameterName.REGION_ID, REGION_1)
                .build());
        rc.add(RuleContainer.builder(RuleType.FORBIDDEN_REGION_CUTTING_RULE)
                .withSingleParam(RuleParameterName.REGION_ID, REGION_2)
                .build());
        promocode = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setClid(12345L)
                .setShopPromoId(SHOP_PROMO_ID)
                .setAnaplanId(ANAPLAN_ID)
                .setCode(PROMOCODE));
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setClid(12346L)
                .setCode(ANOTHER_PROMOCODE));
        partnerPromocode = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE_VALUE)
                .setCode(PARTNER_PROMOCODE));
        promocodeFirstOrder = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFirstOrderPromocode()
                .setCode(FIRST_ORDER_PROMOCODE));
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(REGIONAL_PROMOCODE)
        );
        promoService.reloadActiveSmartShoppingPromoIdsCache();
    }

    @Test
    public void shouldApplyFixedDiscountOnCalc() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)),
                        hasProperty("promocode", is(PROMOCODE))
                )))
        )));
    }

    @Test
    public void shouldApplyPartnerFixedDiscountOnCalc() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PARTNER_PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000),
                        promoKeys(partnerPromocode.getPromoKey())
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PARTNER_PROMOCODE)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));
    }

    @Test
    public void shouldApplyPromocodeInDifferentCaseOnCalc() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE.toLowerCase()))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE.toLowerCase())
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));
    }

    @Test
    public void shouldApplyFixedDiscountFirstOrderOnCalc() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(FIRST_ORDER_PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(FIRST_ORDER_PROMOCODE)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));
    }

    @Test
    public void shouldApplyFixedDiscountWithRegionRestrictionsOnCalc() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(REGIONAL_PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        OperationContextDto operationContextDto = uidOperationContextDto(USER_ID);
        operationContextDto.setRegionId((long) REGION_2);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoupon(REGIONAL_PROMOCODE)
                        .withOperationContext(operationContextDto)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), empty());

        operationContextDto = uidOperationContextDto(USER_ID);
        operationContextDto.setRegionId((long) REGION_1);

        MultiCartWithBundlesDiscountResponse discountResponseFixed = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoupon(REGIONAL_PROMOCODE)
                        .withOperationContext(operationContextDto)
                        .build());

        assertThat(discountResponseFixed.getPromocodeErrors(), empty());
        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponseFixed);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));
    }

    @Test
    public void shouldNotApplyFixedDiscountFirstOrderOnCalc() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID_WITH_ORDERS)
                .externalPromocodes(Set.of(FIRST_ORDER_PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID_WITH_ORDERS))
                        .withCoupon(FIRST_ORDER_PROMOCODE)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", empty())
        )));
    }

    @Test
    public void shouldApplyFixedDiscountOnSpend() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        Set<UserPromocodeInfo> promocodeInfos = promocodeService.retrievePromocodeInfo(USER_ID, PROMOCODE);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));


        var promocodeInfo = promocodeInfos.stream()
                .findFirst();
        assertThat(promocodeInfo.isPresent(), is(true));
        Coin coin = coinService.search.getCoin(promocodeInfo.get().getCoinKey()).get();

        assertThat(coin.getStatus(), is(CoreCoinStatus.USED));

        promocodeInfos = promocodeService.retrievePromocodeInfo(USER_ID, PROMOCODE);

        promocodeInfo = promocodeInfos.stream()
                .findFirst();

        assertThat(promocodeInfo.get().getCode(), is(PROMOCODE));
        assertThat(promocodeInfo.get().getErrorCode(), nullValue());
    }

    @Test
    public void shouldApplyFixedDiscountOnSpendWithAsyncCoinActivateBudget() {
        configurationService.set(ConfigurationService.ASYNC_BUDGET_ONLY_FOR_ACTIVATE_COIN_EMISSION, true);
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        Set<UserPromocodeInfo> promocodeInfos = promocodeService.retrievePromocodeInfo(USER_ID, PROMOCODE);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));


        var promocodeInfo = promocodeInfos.stream()
                .findFirst();
        assertThat(promocodeInfo.isPresent(), is(true));
        Coin coin = coinService.search.getCoin(promocodeInfo.get().getCoinKey()).get();

        assertThat(coin.getStatus(), is(CoreCoinStatus.USED));

        promocodeInfos = promocodeService.retrievePromocodeInfo(USER_ID, PROMOCODE);

        promocodeInfo = promocodeInfos.stream()
                .findFirst();

        assertThat(promocodeInfo.get().getCode(), is(PROMOCODE));
        assertThat(promocodeInfo.get().getErrorCode(), nullValue());
    }

    @Test
    public void shouldApplyReusedInDifferentDatesPromoPromocode() {
        clock.setDate(valueOf("2019-01-10 15:00:00"));
        final String somePromoCode = promocodeService.generateNewPromocode();
        BigDecimal firstPromoDiscount = BigDecimal.valueOf(100L);

        final BigDecimal defaultBudget = BigDecimal.valueOf(10000L);
        final Promo firstMonthPromo = promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode(firstPromoDiscount)
                        .setStartDate(valueOf("2019-01-01 15:00:00"))
                        .setEndDate(valueOf("2019-02-01 15:00:00"))
                        .setBudget(defaultBudget)
                        .setCode(somePromoCode)
        );

        BigDecimal secondPromoDiscount = BigDecimal.valueOf(200L);
        final Promo secondMonthPromo = promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode(secondPromoDiscount)
                        .setStartDate(valueOf("2019-02-01 15:00:01"))
                        .setEndDate(valueOf("2019-03-01 15:00:00"))
                        .setCode(somePromoCode)
        );

        final PromocodesActivationResult firstActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(somePromoCode))
                        .build());

        assertThat(firstActivationResult.getActivationResults(), hasSize(1));
        assertThat(
                firstActivationResult.getActivationResults().get(0).getPromoId(),
                equalTo(firstMonthPromo.getPromoId().getId())
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(somePromoCode)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(firstPromoDiscount))
                )))
        )));

        clock.setDate(valueOf("2019-02-10 15:00:00"));
        final PromocodesActivationResult secondActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(somePromoCode))
                        .build());

        assertThat(secondActivationResult.getActivationResults(), hasSize(1));
        assertThat(
                secondActivationResult.getActivationResults().get(0).getPromoId(),
                equalTo(secondMonthPromo.getPromoId().getId())
        );

        OrderWithBundlesResponse secondOrder = firstOrderOf(marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(somePromoCode)
                        .build()));

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(secondOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(secondPromoDiscount))
                )))
        )));
    }

    @Test
    public void shouldAddClidInPromoResponse() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        clidCacheService.reloadCache();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("clid", comparesEqualTo(12345L)),
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));
    }

    @Test
    public void shouldAddPartnerPromoFlagFor3pPartner() {
        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());
        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)),
                        hasProperty("partnerPromo", equalTo(false))
                )))
        )));

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PARTNER_PROMOCODE))
                .build());

        OrderWithBundlesRequest order3p = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000),
                        promoKeys(partnerPromocode.getPromoKey())

                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse3p = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order3p)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PARTNER_PROMOCODE)

                        .build());

        assertThat(discountResponse3p.getCoins(), empty());
        assertThat(discountResponse3p.getUnusedPromocodes(), empty());
        assertThat(discountResponse3p.getPromocodeErrors(), empty());

        OrderWithBundlesResponse firstOrder3p = firstOrderOf(discountResponse3p);

        assertThat(firstOrder3p.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)),
                        hasProperty("partnerPromo", equalTo(true))
                )))
        )));
    }

    @Test
    public void shouldSpendLastPromocodeSuitableToItem() {
        String msku = "34534";
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, msku)
                .build());
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode("PROMO_FOR_34534")
        );
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode("PROMO_FOR_34534_2")
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMO_FOR_34534", "PROMO_FOR_34534_2")
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));
        assertThat(promocodesActivationResult.getActivationResults(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("code", equalTo("PROMO_FOR_34534"))
                        ),
                        allOf(
                                hasProperty("code", equalTo("PROMO_FOR_34534_2"))
                        )
                )
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasSize(1))
        )));

        var promoDiscountSum = firstOrder.getItems()
                .get(0)
                .getPromos()
                .stream()
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum, comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
    }

    @Test
    public void shouldCalcLastPromocodesSuitableToItem() {
        String msku1 = "123";
        String msku2 = "234";
        String msku3 = "345";
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(getMskuFilterRuleContainer(Set.of(msku1, msku2)))
                .setCode("PROMO1_MSKU1_MSKU2")
        );
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(getMskuFilterRuleContainer(Set.of(msku1, msku3)))
                .setCode("PROMO2_MSKU1_MSKU3")
        );
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(getMskuFilterRuleContainer(Set.of(msku3)))
                .setCode("PROMO3_MSKU3")
        );

        clock.setDate(valueOf("2021-01-01 00:00:27"));
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMO3_MSKU3")
        );
        clock.setDate(valueOf("2021-01-01 00:00:38"));
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMO2_MSKU1_MSKU3")
        );
        clock.setDate(valueOf("2021-01-01 00:00:57"));
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMO1_MSKU1_MSKU2")
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(3));
        assertThat(promocodesActivationResult.getActivationResults(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("code", equalTo("PROMO1_MSKU1_MSKU2"))
                        ),
                        allOf(
                                hasProperty("code", equalTo("PROMO2_MSKU1_MSKU3"))
                        ),
                        allOf(
                                hasProperty("code", equalTo("PROMO3_MSKU3"))
                        )
                )
        );

        long secondFeedId = 234L;
        long thirdFeedId = 345L;
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku1),
                        price(1000)
                ).withOrderItem(
                        itemKey(secondFeedId, FIRST_SSKU),
                        msku(msku2),
                        price(1100)
                ).withOrderItem(
                        itemKey(thirdFeedId, FIRST_SSKU),
                        msku(msku3),
                        price(1300)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), hasSize(1));
        assertThat(discountResponse.getUnusedPromocodes(), hasItem(equalTo("PROMO2_MSKU1_MSKU3")));
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), containsInAnyOrder(
                allOf(
                        hasProperty("feedId", is(FEED_ID)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", containsInAnyOrder(
                                allOf(
                                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                                        hasProperty("promocode", is("PROMO1_MSKU1_MSKU2")))))),
                allOf(
                        hasProperty("feedId", is(secondFeedId)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", containsInAnyOrder(
                                allOf(
                                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                                        hasProperty("promocode", is("PROMO1_MSKU1_MSKU2")))))),
                allOf(
                        hasProperty("feedId", is(thirdFeedId)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", containsInAnyOrder(
                                allOf(
                                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                                        hasProperty("promocode", is("PROMO3_MSKU3"))))))
        ));

        var promoDiscountSum = firstOrder.getItems()
                .stream().flatMap(p -> p.getPromos().stream())
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum, comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL.add(DEFAULT_COIN_FIXED_NOMINAL)));
    }

    @Test
    public void shouldDeclineNotReferralPromoCode() {
        String COUPON_CODE = "REF_ABCD123";
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
        promoService.setPromoParam(couponPromo.getPromoId().getId(), PromoParameterName.GENERATOR_TYPE,
                PromoCodeGeneratorType.REFERRAL);

        var userReferralPromocode = UserReferralPromocode.builder()
                .setUid(0L)
                .setPromocode(COUPON_CODE)
                .setAssignTime(clock.instant())
                .setExpireTime(clock.instant().plus(10, ChronoUnit.DAYS))
                .build();

        userReferralPromocodeDao.insertNewEntry(userReferralPromocode);
        reloadPromoCache();

        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode("PROMOCODE")
        );
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMOCODE")
        );
        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var coinKey = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build()).getActivationResults().get(0).getCoinKey();
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(6000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(operationContext)
                        .useInternalPromocode(false)
                        .withCoupon(COUPON_CODE)
                        .withCoins(coinKey)
                        .build());
        assertThat(discountResponse, hasProperty(
                        "coinErrors", allOf(
                                hasSize(1),
                                everyItem(
                                        hasProperty("error",
                                                hasProperty("code",
                                                        equalTo(REFERRAL_PROMOCODE_NOT_APPLICABLE_WITH_OTHER_PROMOCODES.name()))))
                        )
                )
        );
    }

    @Test
    public void shouldAllowReferralPromocodeWith3pPromocode() {
        String COUPON_CODE = "REF_ABCD123";
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
        promoService.setPromoParam(couponPromo.getPromoId().getId(), PromoParameterName.GENERATOR_TYPE,
                PromoCodeGeneratorType.REFERRAL);

        var userReferralPromocode = UserReferralPromocode.builder()
                .setUid(0L)
                .setPromocode(COUPON_CODE)
                .setAssignTime(clock.instant())
                .setExpireTime(clock.instant().plus(10, ChronoUnit.DAYS))
                .build();

        userReferralPromocodeDao.insertNewEntry(userReferralPromocode);
        reloadPromoCache();

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        CoinKey coinKey = promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(operationContext.getUid())
                .externalPromocodes(Set.of(PARTNER_PROMOCODE))
                .build()).getActivationResults().get(0).getCoinKey();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(6000),
                        promoKeys(partnerPromocode.getPromoKey())
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(operationContext)
                        .useInternalPromocode(false)
                        .withCoupon(COUPON_CODE)
                        .withCoins(coinKey)
                        .build());
        assertThat(discountResponse, allOf(
                hasProperty(
                        "coinErrors", hasSize(0)
                ),
                hasProperty("orders", everyItem(
                        hasProperty("items", everyItem(
                                hasProperty("promos",
                                        allOf(
                                                hasSize(2),
                                                containsInAnyOrder(
                                                        hasProperty("promoType", equalTo(MARKET_PROMOCODE)),
                                                        hasProperty("promoType", equalTo(MARKET_COUPON))
                                                )
                                        )
                                )
                        ))
                ))
        ));
    }

    @NotNull
    private RulesContainer getMskuFilterRuleContainer(Set<String> mskus) {
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withParams(RuleParameterName.MSKU_ID, mskus)
                .build());
        return rc;
    }

    @Test
    public void shouldCalcPromocodesAndCoupon() {
        long secondFeedId = 234L;
        String msku1 = "123";
        String msku2 = "234";

        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(getMskuFilterRuleContainer(Set.of(msku1)))
                .setCode("PROMO1_MSKU1")
        );
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(getMskuFilterRuleContainer(Set.of(msku2)))
                .setCode("PROMO2_MSKU2")
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMO1_MSKU1", "PROMO2_MSKU2", DEFAULT_COUPON_CODE)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));
        assertThat(promocodesActivationResult.getActivationResults(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("code", equalTo("PROMO1_MSKU1"))
                        ),
                        allOf(
                                hasProperty("code", equalTo("PROMO2_MSKU2"))
                        )
                )
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku1),
                        price(10000)
                ).withOrderItem(
                        itemKey(secondFeedId, FIRST_SSKU),
                        msku(msku2),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), containsInAnyOrder(
                allOf(
                        hasProperty("feedId", is(FEED_ID)),
                        hasProperty("promos", hasSize(2))),
                allOf(
                        hasProperty("feedId", is(234L)),
                        hasProperty("promos", hasSize(2))
                )));

        var promoDiscountSum = firstOrder.getItems()
                .stream().flatMap(p -> p.getPromos().stream())
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum,
         comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL.multiply(BigDecimal.valueOf(2)).add(DEFAULT_COUPON_VALUE)));
    }

    @Test
    public void shouldSaveLastAddedSuitableCoupons() {
        String msku = "34534";
        BigDecimal anotherCouponValue = BigDecimal.valueOf(400);
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(msku)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode(ANOTHER_COUPON_CODE)
                .setCouponValue(anotherCouponValue, CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(msku)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("anotherCouponCode2")
                .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton("808989")));

        clock.setDate(valueOf("2021-01-01 00:00:47"));
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(DEFAULT_COUPON_CODE)
        );

        clock.setDate(valueOf("2021-01-01 00:00:48"));
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(ANOTHER_COUPON_CODE)
        );

        clock.setDate(valueOf("2021-01-01 00:00:50"));
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("anotherCouponCode2")
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), hasSize(1));
        assertThat(discountResponse.getUnusedPromocodes(), hasItem(equalTo(DEFAULT_COUPON_CODE.toUpperCase())));
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), hasItem(allOf(
                hasProperty(
                        "promocode",
                        equalTo("ANOTHERCOUPONCODE2")
                ), hasProperty(
                        "error",
                        hasProperty(
                                "error",
                                allOf(
                                        hasProperty("code", equalTo(COUPON_NOT_APPLICABLE.name())),
                                        hasProperty("message", equalTo(COUPON_NOT_APPLICABLE.getDefaultDescription())),
                                        hasProperty("userMessage", nullValue())
                                )
                        )
                )
        )));

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasSize(1))
        )));

        var promoDiscountSum = firstOrder.getItems()
                .get(0)
                .getPromos()
                .stream()
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum, comparesEqualTo(anotherCouponValue));
    }

    @Test
    public void shouldSaveLastAddedSuitableCouponsForYandexUid() {
        String msku = "34534";
        BigDecimal anotherCouponValue = BigDecimal.valueOf(400);
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(msku)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode(ANOTHER_COUPON_CODE)
                .setCouponValue(anotherCouponValue, CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(msku)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("anotherCouponCode2")
                .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton("808989")));

        clock.setDate(valueOf("2021-01-01 00:00:47"));
        storePromocodeService.savePromocodes(
                Identity.Type.YANDEX_UID.buildIdentity(DEFAULT_YANDEX_UID),
                Set.of(DEFAULT_COUPON_CODE)
        );

        clock.setDate(valueOf("2021-01-01 00:00:48"));
        storePromocodeService.savePromocodes(
                Identity.Type.YANDEX_UID.buildIdentity(DEFAULT_YANDEX_UID),
                Set.of(ANOTHER_COUPON_CODE)
        );

        clock.setDate(valueOf("2021-01-01 00:00:50"));
        storePromocodeService.savePromocodes(
                Identity.Type.YANDEX_UID.buildIdentity(DEFAULT_YANDEX_UID),
                Set.of("anotherCouponCode2")
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(yandexUidOperationContext())
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), hasSize(1));
        assertThat(discountResponse.getUnusedPromocodes(), hasItem(equalTo(DEFAULT_COUPON_CODE.toUpperCase())));
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), hasItem(allOf(
                hasProperty(
                        "promocode",
                        equalTo("ANOTHERCOUPONCODE2")
                ), hasProperty(
                        "error",
                        hasProperty(
                                "error",
                                allOf(
                                        hasProperty("code", equalTo(COUPON_NOT_APPLICABLE.name())),
                                        hasProperty("message", equalTo(COUPON_NOT_APPLICABLE.getDefaultDescription())),
                                        hasProperty("userMessage", nullValue())
                                )
                        )
                )
        )));

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasSize(1))
        )));

        var promoDiscountSum = firstOrder.getItems()
                .get(0)
                .getPromos()
                .stream()
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum, comparesEqualTo(anotherCouponValue));
    }

    @Test
    public void shouldSaveLastAddedSuitableCouponsForYandexUidWhenUidIsTheSameAsMuid() {
        String msku = "34534";
        BigDecimal anotherCouponValue = BigDecimal.valueOf(400);
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(msku)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode(ANOTHER_COUPON_CODE)
                .setCouponValue(anotherCouponValue, CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(msku)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("anotherCouponCode2")
                .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton("808989")));

        clock.setDate(valueOf("2021-01-01 00:00:47"));
        storePromocodeService.savePromocodes(
                Identity.Type.YANDEX_UID.buildIdentity(DEFAULT_YANDEX_UID),
                Set.of(DEFAULT_COUPON_CODE)
        );

        clock.setDate(valueOf("2021-01-01 00:00:48"));
        storePromocodeService.savePromocodes(
                Identity.Type.YANDEX_UID.buildIdentity(DEFAULT_YANDEX_UID),
                Set.of(ANOTHER_COUPON_CODE)
        );

        clock.setDate(valueOf("2021-01-01 00:00:50"));
        storePromocodeService.savePromocodes(
                Identity.Type.YANDEX_UID.buildIdentity(DEFAULT_YANDEX_UID),
                Set.of("anotherCouponCode2")
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                )
                .build();

        OperationContextDto operationContextDto = new OperationContextDto();
        operationContextDto.setYandexUid(DEFAULT_YANDEX_UID);
        operationContextDto.setMuid(DEFAULT_MUID);
        operationContextDto.setUid(DEFAULT_MUID);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(operationContextDto)
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), hasSize(1));
        assertThat(discountResponse.getUnusedPromocodes(), hasItem(equalTo(DEFAULT_COUPON_CODE.toUpperCase())));
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), hasItem(allOf(
                hasProperty(
                        "promocode",
                        equalTo("ANOTHERCOUPONCODE2")
                ), hasProperty(
                        "error",
                        hasProperty(
                                "error",
                                allOf(
                                        hasProperty("code", equalTo(COUPON_NOT_APPLICABLE.name())),
                                        hasProperty("message", equalTo(COUPON_NOT_APPLICABLE.getDefaultDescription())),
                                        hasProperty("userMessage", nullValue())
                                )
                        )
                )
        )));

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasSize(1))
        )));

        var promoDiscountSum = firstOrder.getItems()
                .get(0)
                .getPromos()
                .stream()
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum, comparesEqualTo(anotherCouponValue));
    }

    @Test
    public void shouldSaveLastAddedSuitableCouponsWhenCreationDateIsNull() {
        String msku = "34534";
        BigDecimal anotherCouponValue = BigDecimal.valueOf(400);
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(msku)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode(ANOTHER_COUPON_CODE)
                .setCouponValue(anotherCouponValue, CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton(msku)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("anotherCouponCode2")
                .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
                .addPromoRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Collections.singleton("808989")));

        clock.setDate(valueOf("2021-01-01 00:00:47"));
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(DEFAULT_COUPON_CODE)
        );

        storePromocodeDao.insertPromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Collections.singleton(ANOTHER_COUPON_CODE), null);

        storePromocodeDao.insertPromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Collections.singleton("anotherCouponCode2"), null);

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), hasSize(2));
        assertThat(discountResponse.getUnusedPromocodes(), allOf(
                hasItem(equalTo(ANOTHER_COUPON_CODE.toUpperCase())),
                hasItem(equalTo("anotherCouponCode2".toUpperCase()))));
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasSize(1))
        )));

        var promoDiscountSum = firstOrder.getItems()
                .get(0)
                .getPromos()
                .stream()
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum, comparesEqualTo(DEFAULT_COUPON_VALUE));
    }

    @Test
    public void shouldReturnNotSuitablePromocodes() {
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE, PARTNER_PROMOCODE)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));


        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(),
                hasItem(allOf(
                        hasProperty("promocode", is(PARTNER_PROMOCODE.toUpperCase())))
                ));

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasSize(1)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)),
                        hasProperty("promocode", is(PROMOCODE))
                )))
        )));

        var promoDiscountSum = firstOrder.getItems()
                .get(0)
                .getPromos()
                .stream()
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum, comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is(promocode.getPromoKey())),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_DISCOUNT)),
                hasProperty("couponCode", is(PROMOCODE.toUpperCase())),
                hasProperty("isError", nullValue())
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is(partnerPromocode.getPromoKey())),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_ERROR)),
                hasProperty("errorType", is(NOT_SUITABLE_FILTER_RULES.name())),
                hasProperty("couponCode", is(PARTNER_PROMOCODE.toUpperCase())),
                hasProperty("isError", is(true))
        )));
    }

    @Test
    public void shouldReturnExternalIdsOnItemPromo() {
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE)
        );

        promoManager.updatePromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE)
                .setId(promocode.getPromoId().getId())
                .setPromoKey(promocode.getPromoKey())
                .setPromoStorageId(SHOP_PROMO_ID)
                .setAnaplanId(ANAPLAN_ID)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(1));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoinErrors(), empty());
        assertThat(firstOrder.getItems(), hasItem(
                hasProperty("promos", hasItem(allOf(
                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                        hasProperty("anaplanId", is(ANAPLAN_ID))
                )))
        ));
    }

    @Test
    public void shouldApplyPromocodeOnExternalRulesWithRearrFlag() {

        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, MSKU_1)
                .withSingleParam(NOT_LOGIC, true)
                .build());
        promoManager.updatePromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE)
                .setId(promocode.getPromoId().getId())
                .setPromoKey(promocode.getPromoKey())
                .setRulesContainer(rc)
        );

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku(MSKU_1),
                        price(10000),
                        promoKeys(promocode.getPromoKey())
                )
                .build();

        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "loyalty-promo-report");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build(), experiments);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());
    }

    @Test
    public void shouldApplyPromocodeWithNotSuitableRegionOnExternalRulesWithRearrFlag() {

        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.ALLOWED_REGION_CUTTING_RULE)
                .withSingleParam(RuleParameterName.REGION_ID, 65)
                .build());
        promoManager.updatePromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE)
                .setId(promocode.getPromoId().getId())
                .setPromoKey(promocode.getPromoKey())
                .setRulesContainer(rc)
        );

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000),
                        promoKeys(promocode.getPromoKey())
                )
                .build();

        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "loyalty-promo-report");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(OperationContextFactory
                                .withUidBuilder(USER_ID)
                                .withRegionId(1)
                                .buildOperationContext())
                        .withCoupon(PROMOCODE)
                        .build(), experiments);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());
    }

    @Test
    public void shouldApplyPromocodeOnExternalRulesWithRearrFlagIfPromoNotActive() {
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, MSKU_1)
                .withSingleParam(NOT_LOGIC, true)
                .build());
        promoManager.updatePromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE)
                .setId(promocode.getPromoId().getId())
                .setPromoKey(promocode.getPromoKey())
                .setRulesContainer(rc)
        );

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        Promo promo = promoService.getPromo(promocode.getPromoId().getId());
        promoService.updateStatus(promo, PromoStatus.INACTIVE);

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku(MSKU_1),
                        price(10000),
                        promoKeys(promocode.getPromoKey())
                )
                .build();

        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "loyalty-promo-report");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build(), experiments);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());
    }

    @Test
    public void shouldNotApplyPromocodeOnExternalRulesWithoutRearrFlag() {

        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, MSKU_1)
                .withSingleParam(NOT_LOGIC, true)
                .build());
        promoManager.updatePromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE)
                .setId(promocode.getPromoId().getId())
                .setPromoKey(promocode.getPromoKey())
                .setRulesContainer(rc)
        );

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(PROMOCODE))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku(MSKU_1),
                        price(10000),
                        promoKeys(promocode.getPromoKey())
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), not(empty()));
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));

    }

    @Test
    public void shouldReturnNotSuitablePromocodesWithExp() {
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE, PARTNER_PROMOCODE)
        );

        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, MSKU_1)
                .withSingleParam(NOT_LOGIC, true)
                .build());
        promoManager.updatePromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE)
                .setId(promocode.getPromoId().getId())
                .setPromoKey(promocode.getPromoKey())
                .setRulesContainer(rc)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));


        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku(MSKU_1),
                        price(10000)
                )
                .build();

        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "loyalty-promo-report");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build(), experiments);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), hasSize(2));

    }

    @Test
    public void shouldApplySeveralPromocodesWithExp() {
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE, PARTNER_PROMOCODE)
        );

        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, MSKU_1)
                .withSingleParam(NOT_LOGIC, true)
                .build());
        promoManager.updatePromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE)
                .setPromoStorageId(SHOP_PROMO_ID)
                .setAnaplanId(ANAPLAN_ID)
                .setId(promocode.getPromoId().getId())
                .setPromoKey(promocode.getPromoKey())
                .setRulesContainer(rc)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));


        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        promoKeys(promocode.getPromoKey()),
                        ssku(FIRST_SSKU),
                        msku(MSKU_1),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        promoKeys(partnerPromocode.getPromoKey()),
                        ssku(SECOND_SSKU),
                        msku(MSKU_2),
                        price(15000))
                .build();

        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "loyalty-promo-report");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build(), experiments);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promocode", is(PROMOCODE)),
                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                        hasProperty("promoKey", is(promocode.getPromoKey()))
                )))
        )));

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promocode", is(PARTNER_PROMOCODE)),
                        hasProperty("shopPromoId", is(partnerPromocode.getShopPromoId())),
                        hasProperty("promoKey", is(partnerPromocode.getPromoKey()))
                )))
        )));

    }

    @Test
    public void shouldApplyHiddenPromocodesWithExp() {
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE)
        );

        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, MSKU_1)
                .withSingleParam(NOT_LOGIC, false)
                .build());
        promoManager.updatePromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setDontUploadToIdx(true)
                .setCode(PROMOCODE)
                .setPromoStorageId(SHOP_PROMO_ID)
                .setAnaplanId(ANAPLAN_ID)
                .setId(promocode.getPromoId().getId())
                .setPromoKey(promocode.getPromoKey())
                .setRulesContainer(rc)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(1));


        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku(MSKU_1),
                        price(10000)
                )
                .build();

        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "loyalty-promo-report");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build(), experiments);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promocode", is(PROMOCODE)),
                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                        hasProperty("promoKey", is(promocode.getPromoKey()))
                )))
        )));
    }

    @Test
    public void shouldApplyPromocodesForFirstOrderWithExp() {
        configurationService.set(ConfigurationService.TRACING_PROMO_TYPES_ENABLED, true);
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(FIRST_ORDER_PROMOCODE)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(1));


        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku(MSKU_1),
                        price(10000)
                )
                .build();

        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "loyalty-promo-report");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build(), experiments);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promocode", is(FIRST_ORDER_PROMOCODE)),
                        hasProperty("promoKey", is(promocodeFirstOrder.getPromoKey()))
                )))
        )));
    }

    @Test
    public void shouldSendNotSuitableFilterRules() {
        final String someCode10 = "SOME_CODE_10";

        final Promo compositePromo = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.TEN)
                .addCoinRule(MSKU_FILTER_RULE, MSKU_ID, "123")
                .setCode(someCode10)
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(someCode10)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(1));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(FEED_ID, FIRST_SSKU),
                                                price(185),
                                                quantity(5),
                                                msku(FIRST_SSKU),
                                                ssku(FIRST_SSKU),
                                                promoKeys(compositePromo.getPromoKey())
                                        )
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build()
        );

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), hasItem(allOf(
                hasProperty("promocode", is(someCode10))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is(compositePromo.getPromoKey())),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_ERROR)),
                hasProperty("errorType", is(NOT_SUITABLE_FILTER_RULES.name())),
                hasProperty("couponCode", is(someCode10)),
                hasProperty("isError", is(true))
        )));
    }

    @Test
    public void shouldNotDropSecondPromocodeOnFirstNotSuitable() {
        configurationService.enable(ConfigurationService.PROMOCODE_ASSORTMENT_FROM_REPORT_ENABLED);

        final String someCode10 = "SOME_CODE_10";
        final String simplePromocode = "simplePromocode";
        int categoryId = 123141412;

        final Promo simplePromo = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.valueOf(12))
                .setBindOnlyOnce(true)
                .setDontUploadToIdx(false)
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE_VALUE)
                .setCode(simplePromocode)
        );

        final Promo compositePromo = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.TEN)
                .addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(1000))
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categoryId)
                .setBindOnlyOnce(true)
                .setDontUploadToIdx(false)
                .setCode(someCode10)
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(simplePromocode, someCode10)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(FEED_ID, SECOND_SSKU),
                                                promoKeys(simplePromo.getPromoKey()),
                                                msku(SECOND_SSKU),
                                                ssku(SECOND_SSKU),
                                                supplier(1234L),
                                                quantity(1),
                                                price(3200))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(FEED_ID, FIRST_SSKU),
                                                price(185),
                                                quantity(5),
                                                msku(FIRST_SSKU),
                                                ssku(FIRST_SSKU),
                                                categoryId(categoryId),
                                                promoKeys(compositePromo.getPromoKey())
                                        )
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build()
        );

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), hasItem(allOf(
                hasProperty("promocode", is(someCode10))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is(compositePromo.getPromoKey())),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_ERROR)),
                hasProperty("errorType", is(MIN_ORDER_TOTAL_VIOLATED.name())),
                hasProperty("couponCode", is(someCode10)),
                hasProperty("isError", is(true))
        )));
    }

    @Test
    public void shouldNotApplyPromocodeOnItemWithTheSameShopSku() {
        configurationService.enable(ConfigurationService.PROMOCODE_ASSORTMENT_FROM_REPORT_ENABLED);

        final String someCode10 = "SOME_CODE_10";
        final String someCode20 = "SOME_CODE_20";
        Long supplierFirst = 123141412L;
        Long supplierSecond = 89898989L;
        Long supplierOther = 6767678L;

        final Promo promoFirst = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.TEN)
                .addCoinRule(SUPPLIER_FILTER_RULE, SUPPLIER_ID, supplierFirst)
                .setBindOnlyOnce(true)
                .setDontUploadToIdx(false)
                .setCode(someCode10)
        );

        final Promo promoSecond = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.TEN)
                .addCoinRule(SUPPLIER_FILTER_RULE, SUPPLIER_ID, supplierOther)
                .addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(6000L))
                .setBindOnlyOnce(true)
                .setDontUploadToIdx(false)
                .setCode(someCode20)
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(someCode10, someCode20)
        );

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(FEED_ID, FIRST_SSKU),
                                                promoKeys(promoFirst.getPromoKey()),
                                                msku(FIRST_SSKU),
                                                ssku(FIRST_SSKU),
                                                supplier(supplierFirst),
                                                quantity(1),
                                                price(3200))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(FEED_ID, SECOND_SSKU),
                                                promoKeys(promoSecond.getPromoKey()),
                                                msku(FIRST_SSKU),
                                                ssku(FIRST_SSKU),
                                                supplier(supplierSecond),
                                                quantity(1),
                                                price(2220)
                                        )
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build()
        );

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), hasItem(allOf(
                hasProperty("promocode", is(someCode20))
        )));

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("items", contains(
                        allOf(
                                hasProperty("offerId", equalTo(FIRST_SSKU)),
                                hasProperty("promos", contains(
                                        allOf(
                                                hasProperty("promoType", is(MARKET_PROMOCODE)),
                                                hasProperty("promocode", equalTo(someCode10))
                                        )
                                ))
                        )
                )),
                hasProperty("items", contains(
                        allOf(
                                hasProperty("offerId", equalTo(SECOND_SSKU)),
                                hasProperty("promos", empty())
                        )
                ))
        ));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is(promoFirst.getPromoKey())),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_DISCOUNT)),
                hasProperty("couponCode", is(someCode10.toUpperCase())),
                hasProperty("isError", nullValue())
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is(promoSecond.getPromoKey())),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_ERROR)),
                hasProperty("errorType", is(MIN_ORDER_TOTAL_VIOLATED.name())),
                hasProperty("couponCode", is(someCode20)),
                hasProperty("isError", is(true))
        )));
    }

    @Test
    public void shouldApplyPromocodeOnItemWithTheSameShopSkuWhenConfigEnabled() {
        configurationService.enable(ConfigurationService.PROMOCODE_ASSORTMENT_FROM_REPORT_ENABLED);
        configurationService.enable(ConfigurationService.PROMOCODE_ASSORTMENT_FROM_REPORT_BY_SSKU_ENABLED);

        final String someCode10 = "SOME_CODE_10";
        final String someCode20 = "SOME_CODE_20";
        Long supplierFirst = 123141412L;
        Long supplierSecond = 89898989L;
        Long supplierOther = 6767678L;

        final Promo promoFirst = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.TEN)
                .addCoinRule(SUPPLIER_FILTER_RULE, SUPPLIER_ID, supplierFirst)
                .setBindOnlyOnce(true)
                .setDontUploadToIdx(false)
                .setCode(someCode10)
        );

        final Promo promoSecond = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.TEN)
                .addCoinRule(SUPPLIER_FILTER_RULE, SUPPLIER_ID, supplierOther)
                .addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(6000L))
                .setBindOnlyOnce(true)
                .setDontUploadToIdx(false)
                .setCode(someCode20)
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(someCode10, someCode20)
        );

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(FEED_ID, FIRST_SSKU),
                                                promoKeys(promoFirst.getPromoKey()),
                                                msku(FIRST_SSKU),
                                                ssku(FIRST_SSKU),
                                                supplier(supplierFirst),
                                                quantity(1),
                                                price(3200))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(FEED_ID, SECOND_SSKU),
                                                promoKeys(promoSecond.getPromoKey()),
                                                msku(FIRST_SSKU),
                                                ssku(FIRST_SSKU),
                                                supplier(supplierSecond),
                                                quantity(1),
                                                price(2220)
                                        )
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build()
        );

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(), hasItem(allOf(
                hasProperty("promocode", is(someCode20))
        )));

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("items", contains(
                        allOf(
                                hasProperty("offerId", equalTo(FIRST_SSKU)),
                                hasProperty("promos", contains(
                                        allOf(
                                                hasProperty("promoType", is(MARKET_PROMOCODE)),
                                                hasProperty("promocode", equalTo(someCode10))
                                        )
                                ))
                        )
                )),
                hasProperty("items", contains(
                        allOf(
                                hasProperty("offerId", equalTo(SECOND_SSKU)),
                                hasProperty("promos", contains(
                                        allOf(
                                                hasProperty("promoType", is(MARKET_PROMOCODE)),
                                                hasProperty("promocode", equalTo(someCode10))
                                        )
                                ))
                        )
                ))
        ));
    }

    @Test
    public void shouldReturnMockForShooting() {
        configurationService.enable(ConfigurationService.PROMOCODE_ASSORTMENT_FROM_REPORT_ENABLED);
        configurationService.enable(ConfigurationService.MOCK_CALC_FOR_SHOOTING_ENABLED);

        final String simplePromocode = "simplePromocode";

        final Promo promo = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.valueOf(12))
                .setBindOnlyOnce(true)
                .setDontUploadToIdx(false)
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE_VALUE)
                .setCode(simplePromocode)
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(2_190_550_858_753_437_195L)),
                Set.of(simplePromocode)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(2_190_550_858_753_437_195L);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(2_190_550_858_753_437_195L)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(1));

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder
                .builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(FEED_ID, SECOND_SSKU),
                                        promoKeys(promo.getPromoKey()),
                                        msku(SECOND_SSKU),
                                        ssku(SECOND_SSKU),
                                        supplier(1234L),
                                        quantity(1),
                                        price(3200))
                                .build()
                )
                .withOperationContext(operationContext)
                .useInternalPromocode(true)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                request
        );

        assertThat(discountResponse.getPromocodeErrors(), empty());
        assertThat(discountResponse, is(MockForShooting.getMockForShooting(request)));
    }

    @Test
    public void shouldReturnActivateMockForShooting() {
        configurationService.enable(ConfigurationService.PROMOCODE_ASSORTMENT_FROM_REPORT_ENABLED);
        configurationService.enable(ConfigurationService.MOCK_ACTIVATE_FOR_SHOOTING_ENABLED);

        final String simplePromocode = "simplePromocode";

        promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.valueOf(12))
                .setBindOnlyOnce(true)
                .setDontUploadToIdx(false)
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE_VALUE)
                .setCode(simplePromocode)
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(simplePromocode)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(2_190_550_858_753_437_195L);
        operationContext.setEmail(NO_SIDE_EFFECT_EMAIL);

        var promocodesActivationResult = marketLoyaltyClient.activatePromocodes(
                ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationRequest.builder()
                        .userId(2_190_550_858_753_437_195L)
                        .useInternalPromocodes(true)
                        .operationContext(operationContext)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), empty());
    }

    @Test
    public void shouldSuccessAsPartnerPromoForSubsidyDisabledPromo() {

        final String simplePromocode = "simplePromocode";
        final String noSubsidyPromocode = "no_subsidy_promocode";

        Promo promo = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.valueOf(12))
                .setBindOnlyOnce(false)
                .setDontUploadToIdx(false)
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN_VALUE)
                .setMarketPlacePromo(true)
                .setCode(simplePromocode)
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(simplePromocode)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodeActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodeActivationResult.getActivationResults(), not(empty()));

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder
                .builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(FEED_ID, FIRST_SSKU),
                                        promoKeys(promo.getPromoKey()),
                                        msku(FIRST_SSKU),
                                        ssku(FIRST_SSKU),
                                        supplier(1234L),
                                        quantity(1),
                                        price(3200))
                                .build()
                )
                .withOperationContext(operationContext)
                .useInternalPromocode(true)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                request
        );

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promocode", is(simplePromocode)),
                        hasProperty("shopPromoId", is(promo.getShopPromoId())),
                        hasProperty("promoKey", is(promo.getPromoKey())),
                        hasProperty("partnerPromo", equalTo(true))
                )))
        )));

        Promo noSubsidyPromo = promoManager.createPromocodePromo(defaultPercentPromocode(BigDecimal.valueOf(12))
                .setBindOnlyOnce(false)
                .setDontUploadToIdx(false)
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN_VALUE)
                .setCode(noSubsidyPromocode));

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID_WITH_ORDERS)),
                Set.of(noSubsidyPromocode)
        );

        operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID_WITH_ORDERS);

        promocodeActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID_WITH_ORDERS)
                        .useSaved(true)
                        .build());

        assertThat(promocodeActivationResult.getActivationResults(), not(empty()));

        request = DiscountRequestWithBundlesBuilder
                .builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(FEED_ID, FIRST_SSKU),
                                        promoKeys(noSubsidyPromo.getPromoKey()),
                                        msku(FIRST_SSKU),
                                        ssku(FIRST_SSKU),
                                        supplier(1234L),
                                        quantity(1),
                                        price(3200))
                                .build()
                )
                .withOperationContext(operationContext)
                .useInternalPromocode(true)
                .build();

        discountResponse = marketLoyaltyClient.calculateDiscount(
                request
        );

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());

        firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promocode", is(noSubsidyPromocode)),
                        hasProperty("shopPromoId", is(noSubsidyPromo.getShopPromoId())),
                        hasProperty("promoKey", is(noSubsidyPromo.getPromoKey())),
                        hasProperty("partnerPromo", equalTo(false))
                )))
        )));
    }

    @Test
    public void shouldReturnErrorForExcludedSupplier() throws InterruptedException {
        configurationService.set(EXCLUSIONS_CONFIG_ENABLED, true);
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(1));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        supplier(SUPPLIER_EXCLUSION_ID),
                        price(10000)
                )
                .build();
        Thread.sleep(2 * 1000);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));
        assertThat(discountResponse.getPromocodeErrors(),
                hasItem(allOf(
                        hasProperty("promocode", is(PROMOCODE.toUpperCase())))
                ));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is(promocode.getPromoKey())),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_ERROR)),
                hasProperty("errorType", is(NOT_SUITABLE_EXCLUDED_RULES.name())),
                hasProperty("couponCode", is(PROMOCODE.toUpperCase())),
                hasProperty("isError", is(true))
        )));
    }

    @Test
    public void testPromocodeIsExpiredDiscountEvent() {
        String msku = "34534";
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, msku)
                .build());
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setEndDate(toDate(LocalDateTime.now().plus(1, ChronoUnit.HOURS)))
                .setRulesContainer(rc)
                .setCode("PROMOCODE_IS_EXPIRED")
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMOCODE_IS_EXPIRED")
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        List<PromocodeActivationResult> activationResults = promocodesActivationResult.getActivationResults();
        assertThat(activationResults, hasSize(1));
        assertThat(activationResults,
                containsInAnyOrder(
                        allOf(
                                hasProperty("code", equalTo("PROMOCODE_IS_EXPIRED"))
                        )
                )
        );

        clock.setDate(toDate(LocalDateTime.now().plus(2, ChronoUnit.HOURS)));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("eventType", is(PROMOCODE_ACTIVATED)),
                hasProperty("promoId", is(activationResults.get(0).getPromoId())),
                hasProperty("promoKey", is(activationResults.get(0).getPromoKey())),
                hasProperty("shopPromoId", is(activationResults.get(0).getShopPromoId())),
                hasProperty("clientId", is(activationResults.get(0).getClientId())),
                hasProperty("couponCode", is(activationResults.get(0).getCode())),
                hasProperty("uid", is(USER_ID))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is("unknown")),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_ERROR)),
                hasProperty("errorType", is(PROMOCODE_IS_EXPIRED.name())),
                hasProperty("couponCode", is("PROMOCODE_IS_EXPIRED")),
                hasProperty("isError", is(true))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_COUPON)),
                hasProperty("promoKey", is("unknown")),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(COUPON_ERROR)),
                hasProperty("errorType", is(COUPON_NOT_EXISTS.name())),
                hasProperty("couponCode", is("PROMOCODE_IS_EXPIRED")),
                hasProperty("isError", is(true))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(ORDER_REQUEST)),
                hasProperty("coinsCount", is(0)),
                hasProperty("orderTotalPrice", is(BigDecimal.valueOf(10000))),
                hasProperty("orderTotalDiscount", is(BigDecimal.ZERO))
        )));
    }

    @Test
    @Ignore("flaky")
    public void testPromocodeIsExpiredDiscountEventForLastAddedPromo() {
        String msku = "34534";
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, msku)
                .build());
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setStartDate(toDate(LocalDateTime.now().minus(1, ChronoUnit.HOURS)))
                .setEndDate(toDate(LocalDateTime.now().plus(1, ChronoUnit.HOURS)))
                .setRulesContainer(rc)
                .setCode("TWIN_PROMOCODE")
                .setBindOnlyOnce(true)
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("TWIN_PROMOCODE")
        );

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());
        var activationResults = promocodesActivationResult.getActivationResults();
        assertThat(activationResults, hasSize(1));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                ).build();

        // Тратим монетку. В мониторинге для данного промо ошибка бы была DISCOUNT_NOT_ACTIVE
        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setStartDate(toDate(LocalDateTime.now().plus(1, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES)))
                .setEndDate(toDate(LocalDateTime.now().plus(3, ChronoUnit.HOURS)))
                .setRulesContainer(rc)
                .setCode("TWIN_PROMOCODE")
        );

        clock.setDate(toDate(LocalDateTime.now().plus(2, ChronoUnit.HOURS)));

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("TWIN_PROMOCODE")
        );

        promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());
        activationResults = promocodesActivationResult.getActivationResults();
        assertThat(activationResults, hasSize(1));

        clock.setDate(toDate(LocalDateTime.now().plus(4, ChronoUnit.HOURS)));

        order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(5000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        firstOrderOf(discountResponse);

        assertThat(discountResponse.getPromocodeErrors(), hasSize(1));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_PROMOCODE)),
                hasProperty("promoKey", is("unknown")),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(PROMOCODE_ERROR)),
                hasProperty("errorType", is(PROMOCODE_IS_EXPIRED.name())),
                hasProperty("couponCode", is("TWIN_PROMOCODE")),
                hasProperty("isError", is(true))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(MARKET_COUPON)),
                hasProperty("promoKey", is("unknown")),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(COUPON_ERROR)),
                hasProperty("errorType", is(COUPON_NOT_EXISTS.name())),
                hasProperty("couponCode", is("TWIN_PROMOCODE")),
                hasProperty("isError", is(true))
        )));
    }

    @Ignore("Disabled after project https://st.yandex-team.ru/MADV-795 was done")
    @Test
    public void shouldNotBePromocodeIsExpiredDiscountEventWhenCouponExists() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("PROMOCODE_IS_EXPIRED")
        );

        String msku = "34534";
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, msku)
                .build());
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setEndDate(toDate(LocalDateTime.now().plus(1, ChronoUnit.HOURS)))
                .setRulesContainer(rc)
                .setCode("PROMOCODE_IS_EXPIRED")
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMOCODE_IS_EXPIRED")
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        List<PromocodeActivationResult> activationResults = promocodesActivationResult.getActivationResults();
        assertThat(activationResults, hasSize(1));
        assertThat(activationResults,
                containsInAnyOrder(
                        allOf(
                                hasProperty("code", equalTo("PROMOCODE_IS_EXPIRED"))
                        )
                )
        );

        clock.setDate(toDate(LocalDateTime.now().plus(2, ChronoUnit.HOURS)));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), empty());

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("eventType", is(PROMOCODE_ACTIVATED)),
                hasProperty("promoId", is(activationResults.get(0).getPromoId())),
                hasProperty("promoKey", is(activationResults.get(0).getPromoKey())),
                hasProperty("shopPromoId", is(activationResults.get(0).getShopPromoId())),
                hasProperty("clientId", is(activationResults.get(0).getClientId())),
                hasProperty("couponCode", is(activationResults.get(0).getCode())),
                hasProperty("uid", is(USER_ID))
        )));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(PROMOCODE_ERROR)),
                hasProperty("errorType", is(PROMOCODE_IS_EXPIRED.name()))
        )));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(COUPON_ERROR)),
                hasProperty("errorType", is(COUPON_NOT_EXISTS.name()))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("uid", is(USER_ID)),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(ORDER_REQUEST)),
                hasProperty("coinsCount", is(0)),
                hasProperty("orderTotalPrice", is(BigDecimal.valueOf(10000))),
                hasProperty("orderTotalDiscount", is(DEFAULT_COUPON_VALUE.setScale(
                        2, RoundingMode.FLOOR)))
        )));
    }

    @Test
    public void shouldApplyDiscountOnCalcWithPending() {
        String msku = "34534";
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, msku)
                .build());
        Promo promo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode("PROMO_FOR_34534")
        );

        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of("PROMO_FOR_34534")
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());

        promoService.updateStatusFromPromoStorage(promo, PromoStatus.PENDING);

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        msku(msku),
                        price(10000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasSize(1))
        )));

        var promoDiscountSum = firstOrder.getItems()
                .get(0)
                .getPromos()
                .stream()
                .map(ItemPromoResponse::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(promoDiscountSum, comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
    }
}
