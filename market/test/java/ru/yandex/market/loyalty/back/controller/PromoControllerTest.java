package ru.yandex.market.loyalty.back.controller;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.AccrualInfoResponse;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PagedResponse;
import ru.yandex.market.loyalty.api.model.PromoBalance;
import ru.yandex.market.loyalty.api.model.PromoDescriptionResponse;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.bundle.PromoBundleDescriptionResponse;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.api.model.coin.CoinPromoDto;
import ru.yandex.market.loyalty.api.model.coin.FutureCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.PromoGroupCoinResponseList;
import ru.yandex.market.loyalty.api.model.coin.SmartShoppingImageTypes;
import ru.yandex.market.loyalty.api.model.flash.FlashPromoDescriptionResponse;
import ru.yandex.market.loyalty.api.model.promogroup.PromoGroupTokenAndType;
import ru.yandex.market.loyalty.api.model.promogroup.PromoGroupType;
import ru.yandex.market.loyalty.api.model.promogroup.PromosByUidAndPromoGroupTokenRequest;
import ru.yandex.market.loyalty.api.model.promogroup.PromosByUidAndPromoGroupTokenResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.coin.UserInfo;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoStatus;
import ru.yandex.market.loyalty.core.model.ids.PromoId;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroup;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupImpl;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupPromo;
import ru.yandex.market.loyalty.core.model.spread.SpreadDiscountPromoDescription;
import ru.yandex.market.loyalty.core.rule.HiddenDataUntilBindFilterRule;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.avatar.AvatarImageId;
import ru.yandex.market.loyalty.core.service.bundle.RegularPromoBundleService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.constants.DefaultCurrencyUnit;
import ru.yandex.market.loyalty.core.service.flash.FlashPromoService;
import ru.yandex.market.loyalty.core.service.promogroup.PromoGroupService;
import ru.yandex.market.loyalty.core.service.spread.SpreadPromoService;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;
import ru.yandex.market.loyalty.core.utils.FlashPromoUtils;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.DateUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.api.model.coin.CoinType.FIXED;
import static ru.yandex.market.loyalty.api.model.coin.InactivityReason.ALREADY_BOUND;
import static ru.yandex.market.loyalty.api.model.coin.InactivityReason.PROMO_NOT_AVAILABLE_FOR_BIND;
import static ru.yandex.market.loyalty.api.model.coin.InactivityReason.PROMO_NOT_STARTED;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason.OTHER;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason.PARTNER;
import static ru.yandex.market.loyalty.core.model.promo.CorePromoType.SMART_SHOPPING;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.PROMO_SOURCE;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.SHOP_ID;
import static ru.yandex.market.loyalty.core.model.promo.PromoSubType.PROMOCODE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.HIDDEN_DATA_UNTIL_BIND;
import static ru.yandex.market.loyalty.core.rule.RuleType.HIDDEN_DATA_UNTIL_BIND_RESTRICTION_RULE;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.flashDescription;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_BACKGROUND;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_OUTGOING_LINK;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultCoinDescriptionBuilder;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@TestFor(PromoController.class)
public class PromoControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final String TITLE = "title";
    private static final String RESTRICTION_DESCRIPTION = "RESTRICTION_DESCRIPTION";
    private static final AvatarImageId IMAGE = new AvatarImageId(0, "image");
    public static final String DEFAULT_PROMO_GROUP_TOKEN = "promoGroup";
    public static final String DEFAULT_PROMO_GROUP_NAME = "promoGroup";
    private static final long FEED_ID = 123;
    private static final String BUNDLE_PROMO_KEY = "someBundleKey";
    private static final String BUNDLE_SHOP_PROMO_ID = "<shopPromoId>";
    private static final String FLASH_PROMO_KEY = "someFlashKey";
    private static final String FLASH_PROMO_KEY_WITH_SNAPSHOT = "someFlashKeyWithSnapshot";
    private static final String FLASH_SHOP_PROMO_ID = "<shopPromoId>";
    private static final String FLASH_SHOP_PROMO_ID_WITH_SNAPSHOT = "<shopPromoIdWithSnapshot>";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String GIFT_ITEM_SSKU = "some gift offer";
    private static final String ANOTHER_GIFT_ITEM_SSKU = "another gift offer";
    private static final String FLASH_URL = "flash url";
    private static final String SPREAD_PROMO_KEY = "someSpreadKey";
    private static final String PROMOCODE_PROMO_KEY = "somePromocodeKey";
    private static final String SPREAD_PROMO_DESCRIPTION = "someSpreadDescription";
    private static final String PROMOCODE_PROMO_DESCRIPTION = "somePromocodeDescription";
    private static final String SPREAD_SHOP_PROMO_ID = "<shopSpreadPromoId>";
    private static final String PROMOCODE_SHOP_PROMO_ID = "<shopPromocodePromoId>";
    private static final String LANDING_URL = "landing url";
    private static final long SHOP_ID_VALUE = 151251;
    private static final String PROMO_OFFER_AND_ACCEPTANCE_VALUE = "promo.ru/special/conditions";
    private static final String ADDITIONAL_CONDITIONS_TEXT = "additional conditions text";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoGroupService promoGroupService;
    @Autowired
    private RegularPromoBundleService promoBundleService;
    @Autowired
    private FlashPromoService flashPromoService;
    @Autowired
    private SpreadPromoService spreadPromoService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void shouldReturnPromoBundleDescription() {
        promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(BUNDLE_SHOP_PROMO_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU, ANOTHER_GIFT_ITEM_SSKU))
                )
        ));

        PromoBundleDescriptionResponse result = marketLoyaltyClient.getBundlePromoByShopPromoId(BUNDLE_SHOP_PROMO_ID);

        assertThat(result.getShopPromoId(), comparesEqualTo(BUNDLE_SHOP_PROMO_ID));
    }

    @Test
    public void shouldReturnPromoBundleDescriptionWithPromoSource() {
        PromoBundleDescription promoBundle = promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(BUNDLE_SHOP_PROMO_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU, ANOTHER_GIFT_ITEM_SSKU))
                )
        ));

        promoService.setPromoParam(promoBundle.getPromoId(), PROMO_SOURCE, 67);
        PromoBundleDescriptionResponse result = marketLoyaltyClient.getBundlePromoByShopPromoId(BUNDLE_SHOP_PROMO_ID);

        assertThat(result.getShopPromoId(), comparesEqualTo(BUNDLE_SHOP_PROMO_ID));
        assertThat(result.getPromoSource(), comparesEqualTo(67));
    }

    @Test
    public void shouldReturnPromoBundleDescriptionWithPromoSourceAndShopId() {
        PromoBundleDescription promoBundle = promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(BUNDLE_SHOP_PROMO_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU, ANOTHER_GIFT_ITEM_SSKU))
                )
        ));

        promoService.setPromoParam(promoBundle.getPromoId(), PROMO_SOURCE, 67);
        promoService.setPromoParam(promoBundle.getPromoId(), SHOP_ID, 48L);
        PromoBundleDescriptionResponse result = marketLoyaltyClient.getBundlePromoByShopPromoId(BUNDLE_SHOP_PROMO_ID);

        assertThat(result.getShopPromoId(), comparesEqualTo(BUNDLE_SHOP_PROMO_ID));
        assertThat(result.getPromoSource(), comparesEqualTo(67));
        assertThat(result.getShopId(), comparesEqualTo(48L));
    }

    @Test
    public void shouldReturnFlashPromoDescription() {

        flashPromoService.createPromo(flashDescription(
                FlashPromoUtils.promoSource(LOYALTY_VALUE),
                FlashPromoUtils.feedId(FEED_ID),
                promoKey(FLASH_PROMO_KEY),
                FlashPromoUtils.shopPromoId(FLASH_SHOP_PROMO_ID),
                FlashPromoUtils.starts(clock.dateTime()),
                FlashPromoUtils.ends(clock.dateTime().plusDays(10L)),
                FlashPromoUtils.status(FlashPromoStatus.ACTIVE)
                ).toBuilder().url(FLASH_URL).snapshotVersion(false).build()
        );

        flashPromoService.createPromo(flashDescription(
                FlashPromoUtils.promoSource(LOYALTY_VALUE),
                FlashPromoUtils.feedId(FEED_ID),
                promoKey(FLASH_PROMO_KEY_WITH_SNAPSHOT),
                FlashPromoUtils.shopPromoId(FLASH_SHOP_PROMO_ID_WITH_SNAPSHOT),
                FlashPromoUtils.starts(clock.dateTime()),
                FlashPromoUtils.ends(clock.dateTime().plusDays(10L)),
                FlashPromoUtils.status(FlashPromoStatus.ACTIVE)
                ).toBuilder().url(FLASH_URL).snapshotVersion(true).build()
        );

        FlashPromoDescriptionResponse result = marketLoyaltyClient.getFlashPromoByShopPromoId(FLASH_SHOP_PROMO_ID);
        FlashPromoDescriptionResponse emptyResult = marketLoyaltyClient
                .getFlashPromoByShopPromoId(FLASH_SHOP_PROMO_ID_WITH_SNAPSHOT);

        assertThat(result.getShopPromoId(), comparesEqualTo(FLASH_SHOP_PROMO_ID));
        assertThat(emptyResult.getPromoKey(), nullValue());
    }

    @Test
    public void testGetBalance() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        PromoBalance promoBalance = marketLoyaltyClient.getBalance(promo.getPromoId().getId());
        assertThat(promoBalance.getBalance(), comparesEqualTo(promo.getCurrentBudget()));
        assertThat(promoBalance.getEmissionBalance(), comparesEqualTo(promo.getCurrentEmissionBudget()));
    }

    @Test
    public void shouldFilterSmartshoppingPromoWhenGetPromoPaged() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());
        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setCoinCreationReason(PARTNER));
        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setCoinCreationReason(OTHER));
        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFreeMsku());

        PagedResponse<CoinPromoDto> result =
                marketLoyaltyClient.getCoinPromosPaged(1, 5, null, CoinCreationReason.PARTNER);

        assertThat(result.getData(), hasSize(1));
    }

    @Test
    public void shouldGetSmartshoppingPromoByPromoId() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinPromoDto result = marketLoyaltyClient.getCoinPromosById(promo.getPromoId().getId(), MarketPlatform.BLUE);

        assertThat(result.getName(), equalTo(promo.getName()));
    }

    @Test
    public void shouldFailGetPromoGroupCoinsByNotExistPromoGroupToken() {
        assertThrows(
                MarketLoyaltyException.class,
                () -> marketLoyaltyClient.getPromoGroupCoins("promoGroup", null)
        );
    }

    @Test
    public void shouldGetPromoGroupCoinsByPromoGroupToken() {
        final Promo promo1 = setupDefaultPromoGroupReturningFirstPromo(false);

        final PromoGroupCoinResponseList response = marketLoyaltyClient.getPromoGroupCoins(
                DEFAULT_PROMO_GROUP_TOKEN, null
        );


        assertThat(
                response.getFutureCoins(),
                hasItem(
                        allOf(
                                hasProperty("title", equalTo("title")),
                                hasProperty("subtitle", equalTo("subtitle")),
                                hasProperty("coinType", equalTo(FIXED)),
                                hasProperty("nominal", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)),
                                hasProperty("description", equalTo("ОЧЕНЬ выгодная монетка")),
                                hasProperty("inactiveDescription", is(nullValue())),
                                hasProperty("backgroundColor", equalTo(DEFAULT_COIN_BACKGROUND)),
                                hasProperty("endDate", is(nullValue())),
                                hasProperty("promoEndDate", equalTo(promo1.getEndDate())),
                                hasProperty("outgoingLink", equalTo(DEFAULT_OUTGOING_LINK)),
                                hasProperty("bindingStatus", allOf(
                                        hasProperty("activationCode", equalTo("actionCode")),
                                        hasProperty("reason", is(nullValue())),
                                        hasProperty("isAvailable", equalTo(true))
                                ))
                        ))
        );

        assertThat(response.getFutureCoins(), containsInAnyOrder(
                hasProperty("bindingStatus", hasProperty("isAvailable", equalTo(true))),
                hasProperty("bindingStatus", hasProperty("isAvailable", equalTo(false)))
        ));
        assertThat(response.getBoundCoins(), empty());
        assertEquals(response.getCoinsOrder().size(), 2);
    }

    @Test
    public void shouldHideValuesGetPromoGroupCoinsByPromoGroupToken() {
        final Promo promo1 = setupDefaultPromoGroupReturningFirstPromo(true);

        final PromoGroupCoinResponseList response = marketLoyaltyClient.getPromoGroupCoins(
                DEFAULT_PROMO_GROUP_TOKEN, null
        );


        assertThat(
                response.getFutureCoins(),
                hasItem(
                        allOf(
                                hasProperty("title", equalTo("")),
                                hasProperty("subtitle", equalTo("")),
                                hasProperty("coinType", equalTo(FIXED)),
                                hasProperty("nominal", nullValue()),
                                hasProperty("description", equalTo("")),
                                hasProperty("inactiveDescription", equalTo("")),
                                hasProperty("backgroundColor", equalTo(DEFAULT_COIN_BACKGROUND)),
                                hasProperty("endDate", is(nullValue())),
                                hasProperty("promoEndDate", equalTo(promo1.getEndDate())),
                                hasProperty("outgoingLink", equalTo(DEFAULT_OUTGOING_LINK)),
                                hasProperty("hiddenDataUntilBind", equalTo(true)),
                                hasProperty("bindingStatus", allOf(
                                        hasProperty("activationCode", equalTo("actionCode")),
                                        hasProperty("reason", is(nullValue())),
                                        hasProperty("isAvailable", equalTo(true))
                                ))
                        ))
        );

        assertThat(response.getFutureCoins(), containsInAnyOrder(
                hasProperty("bindingStatus", hasProperty("isAvailable", equalTo(true))),
                hasProperty("bindingStatus", hasProperty("isAvailable", equalTo(false)))
        ));
        assertThat(response.getBoundCoins(), empty());
    }

    @Test
    public void shouldSkipPromoGroupCoinsByPromoGroupToken() {
        final Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setActionCode("actionCode")
                .setCoinDescription(
                        defaultCoinDescriptionBuilder()
                                .setTitle("title")
                                .setRestrictionDescription("subtitle")
                )
        );

        PromoGroup promoGroup = PromoGroupImpl.builder()
                .setPromoGroupType(PromoGroupType.EFIM)
                .setToken(DEFAULT_PROMO_GROUP_TOKEN)
                .setName(DEFAULT_PROMO_GROUP_NAME)
                .setStartDate(LocalDateTime.now(clock))
                .setEndDate(LocalDateTime.now(clock).plusDays(1))
                .build();

        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        promoGroupService.replacePromoGroupPromos(
                promoGroupId,
                ImmutableList.of(PromoGroupPromo.builder()
                        .setPromoGroupId(promoGroupId)
                        .setPromoId(promo.getPromoId().getId())
                        .setSortOrder(1)
                        .build()
                )
        );


        coinService.create.createCoin(promo, CoinInsertRequest
                .authMarketBonus(DEFAULT_UID)
                .setSourceKey("sourceKey")
                .setReason(OTHER)
                .setStatus(CoreCoinStatus.ACTIVE)
                .build());

        final PromoGroupCoinResponseList response = marketLoyaltyClient.getPromoGroupCoins(
                DEFAULT_PROMO_GROUP_TOKEN, DEFAULT_UID);


        assertThat(
                response.getFutureCoins(),
                empty()
        );
        assertThat(response.getBoundCoins(), not(empty()));
        assertThat(response.getCoinsOrder(), not(empty()));
    }

    @Test
    public void shouldFindSmartshoppingPromoByTerm() {
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setName("test").setCoinCreationReason(PARTNER));
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setName("rest").setCoinCreationReason(PARTNER));

        PagedResponse<CoinPromoDto> result =
                marketLoyaltyClient.getCoinPromosPaged(1, 5, "test", CoinCreationReason.PARTNER);

        assertThat(result.getData(), hasSize(1));
    }

    @Test
    public void shouldFindPromoByActionCode() {
        String someCode = "someCode";
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setActionCode(someCode)
        );

        assertThat(marketLoyaltyClient.getPromoIdByCode(someCode), equalTo(promo.getPromoId().getId()));
    }

    @Test
    public void shouldFindPromoByActivationToken() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        coinService.create.createCoin(promo, CoinInsertRequest
                .noAuthMarketBonus(UserInfo.builder().build(), "activationToken")
                .setSourceKey("sourceKey")
                .setReason(OTHER)
                .setStatus(CoreCoinStatus.ACTIVE)
                .build());

        assertThat(marketLoyaltyClient.getPromoIdByActivationToken("activationToken"), equalTo(promo.getPromoId().getId()));
    }

    @Test
    public void shouldReturnInfoOnSmartshoppingPromo() {
        Date endDate = new GregorianCalendar(2030, Calendar.FEBRUARY, 1, 23, 59, 59).getTime();

        String offerAndAcceptance = "beru.ru/special/conditions";
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setCoinDescription(defaultCoinDescriptionBuilder()
                        .setTitle(TITLE)
                        .setRestrictionDescription(RESTRICTION_DESCRIPTION)
                        .setAvatarImageId(IMAGE)
                        .setOutgoingLink(DEFAULT_OUTGOING_LINK)
                )
                .setExpiration(ExpirationPolicy.toEndOfPromo())
                .setEndDate(endDate)
                .setPromoOfferAndAcceptance(offerAndAcceptance)
                .setName("test")
        );

        FutureCoinResponse result = marketLoyaltyClient.getCoinPromoInfo(promo.getPromoId().getId());

        assertThat(result, allOf(
                hasProperty("title", equalTo(TITLE)),
                hasProperty("subtitle", equalTo(RESTRICTION_DESCRIPTION)),
                hasProperty("images", hasEntry(SmartShoppingImageTypes.STANDARD, IMAGE.toString())),
                hasProperty("promoOfferAndAcceptance", equalTo(offerAndAcceptance)),
                hasProperty("endDate", equalTo(endDate)),
                hasProperty("outgoingLink", equalTo(DEFAULT_OUTGOING_LINK)),
                hasProperty("bindingStatus", allOf(
                        hasProperty("activationCode", is(nullValue())),
                        hasProperty("reason", is(nullValue())),
                        hasProperty("isAvailable", equalTo(true))
                ))
        ));
    }

    @Test
    public void shouldReturnInfoOnSmartshoppingPromoForBoundCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setBindOnlyOnce(true)
        );

        coinService.create.createCoin(promo, CoinInsertRequest
                .authMarketBonus(DEFAULT_UID)
                .setSourceKey("sourceKey")
                .setReason(OTHER)
                .setStatus(CoreCoinStatus.ACTIVE)
                .build());

        FutureCoinResponse result = marketLoyaltyClient.getCoinPromoInfo(promo.getPromoId().getId(), DEFAULT_UID);

        assertThat(result, allOf(
                hasProperty("bindingStatus", allOf(
                        hasProperty("activationCode", is(nullValue())),
                        hasProperty("reason", equalTo(ALREADY_BOUND)),
                        hasProperty("isAvailable", equalTo(false))
                ))
        ));
    }

    @Test
    public void shouldReturnInfoOnSmartshoppingPromoForUnboundCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setBindOnlyOnce(true)
        );

        FutureCoinResponse result = marketLoyaltyClient.getCoinPromoInfo(promo.getPromoId().getId(), DEFAULT_UID);

        assertThat(result, allOf(
                hasProperty("bindingStatus", allOf(
                        hasProperty("activationCode", is(nullValue())),
                        hasProperty("reason", is(nullValue())),
                        hasProperty("isAvailable", equalTo(true))
                ))
        ));
    }

    @Test
    public void shouldAddPromoKeyToCoinsResponseByPromoGroupToken() {
        setupDefaultPromoGroupReturningFirstPromo(false);
        PromoGroupCoinResponseList promoGroupResponse = marketLoyaltyClient.getPromoGroupCoins(
                DEFAULT_PROMO_GROUP_TOKEN, null
        );

        assertThat(
                promoGroupResponse.getFutureCoins(),
                everyItem(
                        hasProperty("promoKey", not(nullValue()))
                )
        );
    }

    @Test
    public void shouldAddPromoKeyToCoinPromo() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        assertEquals(
                marketLoyaltyClient.getCoinPromosById(promo.getPromoId().getId(), MarketPlatform.BLUE).getPromoKey(),
                promo.getPromoKey()
        );
    }

    @Test
    public void shouldReturnAccrualPromoById() {
        final Promo promo = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        );

        final AccrualInfoResponse accrualPromo = marketLoyaltyClient.getAccrualPromo(promo.getPromoId().getId());

        assertThat(accrualPromo.getPromoId(), equalTo(promo.getPromoId().getId()));
        assertThat(accrualPromo.getName(), equalTo(promo.getName()));
    }

    @Test
    public void shouldFailReturnAccrualPromoById() {
        final Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.getAccrualPromo(smartShoppingPromo.getPromoId().getId()));
    }

    @Test
    public void shouldReturnAccrualPromoPaged() {
        final Promo unitTests1 = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
                        .setPromoKey("unitTests1")
        );
        final Promo unitTests2 = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
                        .setPromoKey("unitTests2")
        );

        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        PagedResponse<AccrualInfoResponse> accrualPromoPaged = marketLoyaltyClient.getAccrualPromoPaged(
                1, 1, null);

        assertThat(accrualPromoPaged.getData(), hasSize(1));
        assertThat(
                accrualPromoPaged.getData().get(0),
                hasProperty("promoId", oneOf(
                        unitTests1.getPromoId().getId(),
                        unitTests2.getPromoId().getId()
                ))
        );

        accrualPromoPaged = marketLoyaltyClient.getAccrualPromoPaged(
                2, 1, null
        );
        assertThat(accrualPromoPaged.getData(), hasSize(1));
        assertThat(
                accrualPromoPaged.getData().get(0),
                hasProperty("promoId", oneOf(
                        unitTests1.getPromoId().getId(),
                        unitTests2.getPromoId().getId()
                ))
        );

        accrualPromoPaged = marketLoyaltyClient.getAccrualPromoPaged(
                1, 4, null
        );
        assertThat(accrualPromoPaged.getData(), hasSize(2));
        assertThat(accrualPromoPaged.getData(), everyItem(
                hasProperty("promoId", oneOf(
                        unitTests1.getPromoId().getId(),
                        unitTests2.getPromoId().getId()
                ))
        ));

    }

    @Test
    public void getPromosByTokenList() {
        configurationService.set(ConfigurationService.BRAND_DAY_ENABLED, true);
        List<Promo> listPromoOne = createListPromo(5);
        List<Promo> listPromoTwo = createListPromo(5);
        createGroupAndInsertPromos(listPromoOne, PromoGroupType.BRAND_DAY,
                DEFAULT_PROMO_GROUP_TOKEN + 1);
        createGroupAndInsertPromos(listPromoTwo, PromoGroupType.BRAND_DAY,
                DEFAULT_PROMO_GROUP_TOKEN + 2);

        PromoGroupTokenAndType promoGroupTokenAndTypeOne =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 1, PromoGroupType.BRAND_DAY);

        PromoGroupTokenAndType promoGroupTokenAndTypeTwo =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 2, PromoGroupType.BRAND_DAY);

        PromosByUidAndPromoGroupTokenRequest promosByUidAndPromoGroupTokenRequest =
                new PromosByUidAndPromoGroupTokenRequest(DEFAULT_UID, List.of(promoGroupTokenAndTypeOne,
                        promoGroupTokenAndTypeTwo));
        PromosByUidAndPromoGroupTokenResponse response =
                marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);

        assertFalse(response.getPromoGroupCoinsState().isEmpty());
        assertEquals(5, response.getPromoGroupCoinsState().get(0).getFutureCoins().size());
        assertEquals(0, response.getPromoGroupCoinsState().get(0).getBoundCoins().size());

        //привязываем 1 монетку к пользователю
        coinService.create.createCoin(listPromoOne.get(0), CoinInsertRequest
                .authMarketBonus(DEFAULT_UID)
                .setSourceKey("sourceKey")
                .setReason(OTHER)
                .setStatus(CoreCoinStatus.ACTIVE)
                .build());

        response = marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);
        assertFalse(response.getPromoGroupCoinsState().isEmpty());
        assertEquals(4, response.getPromoGroupCoinsState().get(0).getFutureCoins().size());
        assertEquals(1, response.getPromoGroupCoinsState().get(0).getBoundCoins().size());
    }

    @Test
    public void getPromosByTokenListWithoutUid() {
        configurationService.set(ConfigurationService.BRAND_DAY_ENABLED, true);
        List<Promo> listPromoOne = createListPromo(5);
        List<Promo> listPromoTwo = createListPromo(5);
        createGroupAndInsertPromos(listPromoOne, PromoGroupType.BRAND_DAY,
                DEFAULT_PROMO_GROUP_TOKEN + 1);
        createGroupAndInsertPromos(listPromoTwo, PromoGroupType.BRAND_DAY,
                DEFAULT_PROMO_GROUP_TOKEN + 2);

        PromoGroupTokenAndType promoGroupTokenAndTypeOne =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 1, PromoGroupType.BRAND_DAY);

        PromoGroupTokenAndType promoGroupTokenAndTypeTwo =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 2, PromoGroupType.BRAND_DAY);

        //UID не передаем
        PromosByUidAndPromoGroupTokenRequest promosByUidAndPromoGroupTokenRequest =
                new PromosByUidAndPromoGroupTokenRequest(null, List.of(promoGroupTokenAndTypeOne,
                        promoGroupTokenAndTypeTwo));
        PromosByUidAndPromoGroupTokenResponse response =
                marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);

        assertFalse(response.getPromoGroupCoinsState().isEmpty());
        assertEquals(5, response.getPromoGroupCoinsState().get(0).getFutureCoins().size());
        assertEquals(0, response.getPromoGroupCoinsState().get(0).getBoundCoins().size());

        //привязываем 1 монетку к пользователю
        coinService.create.createCoin(listPromoOne.get(0), CoinInsertRequest
                .authMarketBonus(DEFAULT_UID)
                .setSourceKey("sourceKey")
                .setReason(OTHER)
                .setStatus(CoreCoinStatus.ACTIVE)
                .build());

        response = marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);
        assertFalse(response.getPromoGroupCoinsState().isEmpty());
        assertEquals(5, response.getPromoGroupCoinsState().get(0).getFutureCoins().size());
        assertEquals(0, response.getPromoGroupCoinsState().get(0).getBoundCoins().size());
    }

    @Test
    public void getPromosByTokenListAndCheckHiddenParam() {
        configurationService.set(ConfigurationService.BRAND_DAY_ENABLED, true);
        RuleContainer<HiddenDataUntilBindFilterRule> rc = RuleContainer
                .builder(RuleType.HIDDEN_DATA_UNTIL_BIND_RESTRICTION_RULE)
                .withSingleParam(RuleParameterName.HIDDEN_DATA_UNTIL_BIND, true)
                .build();
        SmartShoppingPromoBuilder<?> smartShoppingPromoBuilder = PromoUtils.SmartShopping.defaultFixed();
        smartShoppingPromoBuilder.getCoinProps().addRule(rc);
        smartShoppingPromoBuilder.setEmissionStartDate(DateUtils.toDate(LocalDateTime.now(clock).plusDays(1)));
        smartShoppingPromoBuilder.setEmissionEndDate(DateUtils.toDate(LocalDateTime.now(clock).plusDays(2)));
        Promo smartShoppingPromo =
                promoManager.createSmartShoppingPromo(
                        smartShoppingPromoBuilder
                                .setCoinDescription(
                                        defaultCoinDescriptionBuilder()
                                                .setTitle("title")
                                                .setInactiveDescription("inactiveSubtitle")
                                                .setRestrictionDescription("restrictionDescription")
                                )
                );

        createGroupAndInsertPromos(Collections.singletonList(smartShoppingPromo), PromoGroupType.BRAND_DAY,
                DEFAULT_PROMO_GROUP_TOKEN + 1);

        PromoGroupTokenAndType promoGroupTokenAndTypeOne =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 1, PromoGroupType.BRAND_DAY);

        PromosByUidAndPromoGroupTokenRequest promosByUidAndPromoGroupTokenRequest =
                new PromosByUidAndPromoGroupTokenRequest(DEFAULT_UID, List.of(promoGroupTokenAndTypeOne));
        PromosByUidAndPromoGroupTokenResponse response =
                marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);

        assertFalse(response.getPromoGroupCoinsState().isEmpty());
        assertEquals(1, response.getPromoGroupCoinsState().get(0).getFutureCoins().size());
        assertEquals(0, response.getPromoGroupCoinsState().get(0).getBoundCoins().size());
        assertTrue(response.getPromoGroupCoinsState().get(0).getFutureCoins().get(0).getHiddenDataUntilBind());
        assertEquals("", response.getPromoGroupCoinsState().get(0).getFutureCoins().get(0).getTitle());
        assertEquals("", response.getPromoGroupCoinsState().get(0).getFutureCoins().get(0).getSubtitle());
        assertEquals("", response.getPromoGroupCoinsState().get(0).getFutureCoins().get(0).getInactiveDescription());
        assertEquals("", response.getPromoGroupCoinsState().get(0).getFutureCoins().get(0).getDescription());
        assertNull(response.getPromoGroupCoinsState().get(0).getFutureCoins().get(0).getNominal());
    }

    @Test
    public void getPromosByTokenListAndCheckHiddenParamAfterBind() {
        configurationService.set(ConfigurationService.BRAND_DAY_ENABLED, true);
        RuleContainer<HiddenDataUntilBindFilterRule> rc = RuleContainer
                .builder(RuleType.HIDDEN_DATA_UNTIL_BIND_RESTRICTION_RULE)
                .withSingleParam(RuleParameterName.HIDDEN_DATA_UNTIL_BIND, true)
                .build();
        SmartShoppingPromoBuilder<?> smartShoppingPromoBuilder = PromoUtils.SmartShopping.defaultFixed();
        smartShoppingPromoBuilder.getCoinProps().addRule(rc);
        smartShoppingPromoBuilder.setEmissionStartDate(DateUtils.toDate(LocalDateTime.now(clock).minusDays(1)));
        smartShoppingPromoBuilder.setEmissionEndDate(DateUtils.toDate(LocalDateTime.now(clock).plusDays(1)));
        Promo smartShoppingPromo =
                promoManager.createSmartShoppingPromo(
                        smartShoppingPromoBuilder
                                .setCoinDescription(
                                        defaultCoinDescriptionBuilder()
                                                .setTitle("title")
                                                .setInactiveDescription("inactiveSubtitle")
                                                .setRestrictionDescription("restrictionDescription")
                                )
                );

        createGroupAndInsertPromos(Collections.singletonList(smartShoppingPromo), PromoGroupType.BRAND_DAY,
                DEFAULT_PROMO_GROUP_TOKEN + 1);

        PromoGroupTokenAndType promoGroupTokenAndTypeOne =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 1, PromoGroupType.BRAND_DAY);

        PromosByUidAndPromoGroupTokenRequest promosByUidAndPromoGroupTokenRequest =
                new PromosByUidAndPromoGroupTokenRequest(DEFAULT_UID, List.of(promoGroupTokenAndTypeOne));
        //привязываем 1 монетку к пользователю
        coinService.create.createCoin(smartShoppingPromo, CoinInsertRequest
                .authMarketBonus(DEFAULT_UID)
                .setSourceKey("sourceKey")
                .setReason(OTHER)
                .setStatus(CoreCoinStatus.ACTIVE)
                .build());

        PromosByUidAndPromoGroupTokenResponse response =
                marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);
        assertFalse(response.getPromoGroupCoinsState().isEmpty());
        assertEquals(0, response.getPromoGroupCoinsState().get(0).getFutureCoins().size());
        assertEquals(1, response.getPromoGroupCoinsState().get(0).getBoundCoins().size());
        assertTrue(response.getPromoGroupCoinsState().get(0).getBoundCoins().get(0).getHiddenDataUntilBind());
        assertNotEquals("", response.getPromoGroupCoinsState().get(0).getBoundCoins().get(0).getTitle());
        assertNotEquals("", response.getPromoGroupCoinsState().get(0).getBoundCoins().get(0).getSubtitle());
        assertNotEquals("", response.getPromoGroupCoinsState().get(0).getBoundCoins().get(0).getInactiveDescription());
        assertNotEquals("", response.getPromoGroupCoinsState().get(0).getBoundCoins().get(0).getDescription());
        assertNotNull(response.getPromoGroupCoinsState().get(0).getBoundCoins().get(0).getNominal());
    }

    @Test
    public void shouldReturnInfoOnSmartshoppingPromoForFuturePromo() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setBindOnlyOnce(true)
                .setStatus(PromoStatus.ACTIVE)
                .setStartDate(DateUtils.toDate(LocalDateTime.now().plusDays(1)))
                .setEndDate(DateUtils.toDate(LocalDateTime.now().plusDays(2)))
        );

        FutureCoinResponse result = marketLoyaltyClient.getCoinPromoInfo(promo.getPromoId().getId(), DEFAULT_UID);

        assertThat(result, allOf(
                hasProperty("bindingStatus", allOf(
                        hasProperty("activationCode", is(nullValue())),
                        hasProperty("reason", equalTo(PROMO_NOT_STARTED)),
                        hasProperty("isAvailable", equalTo(false))
                ))
        ));
    }

    @Test
    public void getEmptyBrandDayPromosByTokenListWithDisableConfiguration() {
        configurationService.set(ConfigurationService.BRAND_DAY_ENABLED, false);
        List<Promo> listPromoOne = createListPromo(5);
        List<Promo> listPromoTwo = createListPromo(5);
        createGroupAndInsertPromos(listPromoOne, PromoGroupType.BRAND_DAY,
                DEFAULT_PROMO_GROUP_TOKEN + 1);
        createGroupAndInsertPromos(listPromoTwo, PromoGroupType.BRAND_DAY,
                DEFAULT_PROMO_GROUP_TOKEN + 2);

        PromoGroupTokenAndType promoGroupTokenAndTypeOne =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 1, PromoGroupType.BRAND_DAY);

        PromoGroupTokenAndType promoGroupTokenAndTypeTwo =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 2, PromoGroupType.BRAND_DAY);

        PromosByUidAndPromoGroupTokenRequest promosByUidAndPromoGroupTokenRequest =
                new PromosByUidAndPromoGroupTokenRequest(DEFAULT_UID, List.of(promoGroupTokenAndTypeOne,
                        promoGroupTokenAndTypeTwo));
        PromosByUidAndPromoGroupTokenResponse response =
                marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);

        assertTrue(response.getPromoGroupCoinsState().isEmpty());

        //привязываем 1 монетку к пользователю
        coinService.create.createCoin(listPromoOne.get(0), CoinInsertRequest
                .authMarketBonus(DEFAULT_UID)
                .setSourceKey("sourceKey")
                .setReason(OTHER)
                .setStatus(CoreCoinStatus.ACTIVE)
                .build());

        response = marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);
        assertTrue(response.getPromoGroupCoinsState().isEmpty());
    }


    @Test
    public void shouldReturnPromoNotAvailableForBind() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setBindOnlyOnce(true)
                .setStatus(PromoStatus.ACTIVE)
                .setStartDate(DateUtils.toDate(LocalDateTime.now().minusDays(4)))
                .setEndDate(DateUtils.toDate(LocalDateTime.now().plusDays(2)))
        );
        promoService.setPromoParam(promo.getPromoId().getId(), PromoParameterName.COUPON_EMISSION_DATE_FROM,
                DateUtils.toDate(LocalDateTime.now(clock).minusDays(2)));
        promoService.setPromoParam(promo.getPromoId().getId(), PromoParameterName.COUPON_EMISSION_DATE_TO,
                DateUtils.toDate(LocalDateTime.now(clock).minusDays(1)));
        FutureCoinResponse result = marketLoyaltyClient.getCoinPromoInfo(promo.getPromoId().getId(), DEFAULT_UID);

        assertThat(result, allOf(
                hasProperty("bindingStatus", allOf(
                        hasProperty("activationCode", is(nullValue())),
                        hasProperty("reason", equalTo(PROMO_NOT_AVAILABLE_FOR_BIND)),
                        hasProperty("isAvailable", equalTo(false))
                ))
        ));
    }

    @Test
    public void shouldReturnPromoWithHiddenData() {
        Promo promo = setupDefaultPromoGroupReturningFirstPromo(true);
        FutureCoinResponse result = marketLoyaltyClient.getCoinPromoInfo(promo.getPromoId().getId(), DEFAULT_UID);
        assertEquals(result.getTitle(), Strings.EMPTY);
        assertEquals(result.getSubtitle(), Strings.EMPTY);
        assertNull(result.getNominal());
        assertEquals(result.getDescription(), Strings.EMPTY);
        assertEquals(result.getInactiveDescription(), Strings.EMPTY);
        assertTrue(result.getHiddenDataUntilBind());
    }

    @Test
    public void shouldReturnPromoWithOnlyForPlusParam() {
        Promo promo = setupDefaultPromoGroupReturningFirstPromo(false);
        promoService.setPromoParam(promo.getPromoId().getId(), PromoParameterName.ONLY_FOR_PLUS, true);
        FutureCoinResponse result = marketLoyaltyClient.getCoinPromoInfo(promo.getPromoId().getId(), DEFAULT_UID);
        assertTrue(result.isForPlus());
    }

    @Test
    public void shouldReturnPromoDescriptionByShopPromoId() {
        SpreadDiscountPromoDescription spreadDiscountPromoDescription =
                spreadPromoService.createOrUpdateSpreadPromo(SpreadDiscountPromoDescription.builder()
                        .promoSource(LOYALTY_VALUE)
                        .feedId(FEED_ID)
                        .promoKey(SPREAD_PROMO_KEY)
                        .description(SPREAD_PROMO_DESCRIPTION)
                        .source(SPREAD_PROMO_KEY)
                        .shopPromoId(SPREAD_SHOP_PROMO_ID)
                        .startTime(clock.dateTime().minusYears(10))
                        .endTime(clock.dateTime().plusYears(1))
                        .name(SPREAD_PROMO_KEY)
                        .budgetLimit(BigDecimal.ZERO)
                        .promoType(ReportPromoType.SPREAD_COUNT)
                        .build()
                );

        promoService.setPromoParam(
                spreadDiscountPromoDescription.getPromoId(),
                PromoParameterName.LANDING_URL, "http://market.yandex.ru/special-landing-xxx");

        PromoDescriptionResponse result = marketLoyaltyClient.getPromoByShopPromoId(SPREAD_SHOP_PROMO_ID);

        assertThat(result.getPromoKey(), comparesEqualTo(SPREAD_PROMO_KEY));
        assertThat(result.getDescription(), comparesEqualTo(SPREAD_PROMO_DESCRIPTION));
        assertThat(result.getLandingUrl(), comparesEqualTo("http://market.yandex.ru/special-landing-xxx"));
    }

    @Test
    public void shouldReturnPromocodeDescriptionByShopPromoId() {
        Promo promocode = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setPromoSource(LOYALTY_VALUE)
                .setShopPromoId(PROMOCODE_SHOP_PROMO_ID)
                .setShopId(SHOP_ID_VALUE)
                .setCode("PROMOCODE")
                .setLandingUrl(LANDING_URL)
                .setPromoKey(PROMOCODE_PROMO_KEY)
                .setDescription(PROMOCODE_PROMO_DESCRIPTION)
                .setPromoOfferAndAcceptance(PROMO_OFFER_AND_ACCEPTANCE_VALUE)
                .setAdditionalConditionsText(ADDITIONAL_CONDITIONS_TEXT)
                .addCoinRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        Set.of(DEFAULT_MIN_ORDER_TOTAL))
                .addCoinRule(
                        RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE,
                        RuleParameterName.MAX_ORDER_TOTAL,
                        Set.of(DEFAULT_MAX_ORDER_TOTAL)));

        PromoDescriptionResponse result = marketLoyaltyClient.getPromoByShopPromoId(PROMOCODE_SHOP_PROMO_ID);

        assertThat(result, allOf(
                hasProperty("promoType", is(SMART_SHOPPING.getCode())),
                hasProperty("promoSubType", is(PROMOCODE.getCode())),
                hasProperty("shopPromoId", is(URLEncoder.encode(PROMOCODE_SHOP_PROMO_ID, StandardCharsets.UTF_8))),
                hasProperty("promoKey", is(PROMOCODE_PROMO_KEY)),
                hasProperty("status", is(PromoStatus.ACTIVE.getCode())),
                hasProperty("description", is(PROMOCODE_PROMO_DESCRIPTION)),
                hasProperty("promoId", is(promocode.getPromoId().getId())),
                hasProperty("startDate", is(notNullValue())),
                hasProperty("endDate", is(notNullValue())),
                hasProperty("promoSource", is(LOYALTY_VALUE)),
                hasProperty("shopId", is(SHOP_ID_VALUE)),
                hasProperty("landingUrl", is(LANDING_URL)),
                hasProperty("promoRulesUrl", is(PROMO_OFFER_AND_ACCEPTANCE_VALUE)),
                hasProperty("promocode", allOf(
                        hasProperty("code", is("PROMOCODE")),
                        hasProperty("discountCurrency", is(DefaultCurrencyUnit.RUB.getCode())),
                        hasProperty("discountValue", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)),
                        hasProperty("orderMaxPrice", comparesEqualTo(DEFAULT_MAX_ORDER_TOTAL)),
                        hasProperty("orderMinPrice", comparesEqualTo(DEFAULT_MIN_ORDER_TOTAL)),
                        hasProperty("conditions", is(ADDITIONAL_CONDITIONS_TEXT))
                ))
                )
        );
    }

    @Test
    public void shouldReturnListWithoutNPEIfCoinPropsNotFound() {
        List<Promo> listPromoOne = createListPromo(5);
        removeCoinPropsId(listPromoOne);
        createGroupAndInsertPromos(listPromoOne, PromoGroupType.EFIM,
                DEFAULT_PROMO_GROUP_TOKEN + 1);
        PromoGroupTokenAndType promoGroupTokenAndTypeOne =
                new PromoGroupTokenAndType(DEFAULT_PROMO_GROUP_TOKEN + 1, PromoGroupType.EFIM);
        PromosByUidAndPromoGroupTokenRequest promosByUidAndPromoGroupTokenRequest =
                new PromosByUidAndPromoGroupTokenRequest(DEFAULT_UID, List.of(promoGroupTokenAndTypeOne));
        PromosByUidAndPromoGroupTokenResponse response =
                marketLoyaltyClient.getPromoGroupBoundPromos(promosByUidAndPromoGroupTokenRequest);
        assertTrue(response.getPromoGroupCoinsState().stream().allMatch(state -> state.getFutureCoins().isEmpty()));
    }

    private void removeCoinPropsId(List<Promo> promoIds) {
        promoIds
                .stream()
                .map(Promo::getPromoId)
                .map(PromoId::getId)
                .forEach(promoId -> jdbcTemplate.update("update promo set coin_props_id = null where id = ?", promoId));
    }

    @NotNull
    private Promo setupDefaultPromoGroupReturningFirstPromo(boolean hideData) {
        RuleContainer.Builder<HiddenDataUntilBindFilterRule> ruleBuilder =
                RuleContainer.builder(HIDDEN_DATA_UNTIL_BIND_RESTRICTION_RULE).withSingleParam(HIDDEN_DATA_UNTIL_BIND
                        , hideData);

        SmartShoppingPromoBuilder<?> smartShoppingPromoBuilder = PromoUtils.SmartShopping.defaultFixed();
        smartShoppingPromoBuilder.getCoinProps().addRule(ruleBuilder.build());
        if (hideData) {
            smartShoppingPromoBuilder.setEmissionStartDate(DateUtils.toDate(LocalDateTime.now(clock).plusDays(1)));
        } else {
            smartShoppingPromoBuilder.setEmissionStartDate(DateUtils.toDate(LocalDateTime.now(clock).minusDays(1)));
        }
        smartShoppingPromoBuilder.setEmissionEndDate(DateUtils.toDate(LocalDateTime.now(clock).plusDays(2)));
        final Promo promo1 = promoManager.createSmartShoppingPromo(smartShoppingPromoBuilder
                .setActionCode("actionCode")
                .setCoinDescription(
                        defaultCoinDescriptionBuilder()
                                .setTitle("title")
                                .setRestrictionDescription("subtitle")
                )
        );
        SmartShoppingPromoBuilder<?> smartShoppingPromoBuilder2 = PromoUtils.SmartShopping.defaultFixed();
        smartShoppingPromoBuilder2.getCoinProps().addRule(ruleBuilder.build());
        if (hideData) {
            smartShoppingPromoBuilder2.setEmissionStartDate(DateUtils.toDate(LocalDateTime.now(clock).plusDays(1)));
        } else {
            smartShoppingPromoBuilder2.setEmissionStartDate(DateUtils.toDate(LocalDateTime.now(clock).minusDays(1)));
        }
        final Promo promo2 = promoManager.createSmartShoppingPromo(smartShoppingPromoBuilder2
                .setActionCode("actionCode2")
                .setCoinDescription(
                        defaultCoinDescriptionBuilder()
                                .setTitle("title")
                                .setRestrictionDescription("subtitle")
                )
                .setEmissionBudget(BigDecimal.ZERO)
        );

        PromoGroup promoGroup = PromoGroupImpl.builder()
                .setPromoGroupType(PromoGroupType.EFIM)
                .setToken(DEFAULT_PROMO_GROUP_TOKEN)
                .setName(DEFAULT_PROMO_GROUP_NAME)
                .setStartDate(LocalDateTime.now(clock))
                .setEndDate(LocalDateTime.now(clock).plusDays(1))
                .build();

        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        promoGroupService.replacePromoGroupPromos(
                promoGroupId,
                ImmutableList.of(
                        PromoGroupPromo.builder()
                                .setPromoGroupId(promoGroupId)
                                .setPromoId(promo1.getPromoId().getId())
                                .setSortOrder(1)
                                .build(),
                        PromoGroupPromo.builder()
                                .setPromoGroupId(promoGroupId)
                                .setPromoId(promo2.getPromoId().getId())
                                .setSortOrder(0)
                                .build()
                )
        );
        return promo1;
    }

    private List<Promo> createListPromo(int size) {
        List<Promo> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                    .setActionCode("actionCode" + UUID.randomUUID())
                    .setCoinDescription(
                            defaultCoinDescriptionBuilder()
                                    .setTitle("title" + i)
                                    .setRestrictionDescription("subtitle" + i)
                    )
            ));
        }
        return result;
    }

    private PromoGroup createGroupAndInsertPromos(List<Promo> promos, PromoGroupType type, String token) {
        PromoGroup promoGroup = PromoGroupImpl.builder()
                .setPromoGroupType(type)
                .setToken(token)
                .setName(DEFAULT_PROMO_GROUP_NAME)
                .setStartDate(LocalDateTime.now(clock))
                .setEndDate(LocalDateTime.now(clock).plusDays(1))
                .build();
        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        List<PromoGroupPromo> promoGroupPromos = new ArrayList<>();
        for (int i = 0; i < promos.size(); i++) {
            promoGroupPromos.add(PromoGroupPromo.builder()
                    .setPromoGroupId(promoGroupId)
                    .setPromoId(promos.get(i).getPromoId().getId())
                    .setSortOrder(i)
                    .build());
        }
        promoGroupService.replacePromoGroupPromos(promoGroupId, promoGroupPromos);
        return promoGroup;
    }
}
