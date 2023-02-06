package ru.yandex.market.loyalty.admin.tms.yt.promo.export;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.PromoYtUpdateProcessor;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleStatus;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.test.TestFor;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.FeedOfferId;
import Market.Promo.Promo.PromoDetails.GenericBundle.BundleContent;
import Market.Promo.Promo.PromoDetails.GenericBundle.PromoItem;
import Market.Promo.Promo.PromoDetails.GenericBundle.SecondaryItem;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.source;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;

@TestFor(PromoYtUpdateProcessor.class)
public class PromoBundlesYtUpdateProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SHOP_PROMO_ID = "some promo";
    private static final String SOME_PROMO_KEY = "some promo";
    private static final String ANOTHER_PROMO_KEY = "another promo";
    private static final String ANOTHER_ONE_PROMO_KEY = "another one promo";
    private static final String PRIMARY_SSKU_1 = "primary ssku 1";
    private static final String PRIMARY_SSKU_2 = "primary ssku 2";
    private static final String PRIMARY_SSKU_3 = "primary ssku 3";
    private static final String PRIMARY_SSKU_4 = "primary ssku 4";
    private static final String SECONDARY_SSKU_1 = "secondary ssku 1";
    private static final long FEED_ID = 1234;
    private static final long ANOTHER_FEED_ID = 1235;
    private static final long ANOTHER_ONE_FEED_ID = 1236;

    @Autowired
    private PromoYtUpdateProcessor bundlesUpdateProcessor;
    @Autowired
    private PromoBundleService promoBundleService;
    @Autowired
    private PromoBundleDao promoBundleDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    private PromoDetails.Builder giftWithPurchaseDetails;
    private PromoDetails.Builder cheapestAsGiftDetails;

    @Before
    public void configure() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        giftWithPurchaseDetails = PromoDetails.newBuilder()
                .setStartDate(current.toEpochSecond())
                .setEndDate(current.plusDays(1).toEpochSecond())
                .setShopPromoId(SHOP_PROMO_ID)
                .setType(GIFT_WITH_PURCHASE.getReportCode())
                .setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                        .addBundlesContent(BundleContent.newBuilder()
                                .setPrimaryItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId(PRIMARY_SSKU_1)
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId(SECONDARY_SSKU_1)
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(BundleContent.newBuilder()
                                .setPrimaryItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId(PRIMARY_SSKU_2)
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId(SECONDARY_SSKU_1)
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(BundleContent.newBuilder()
                                .setPrimaryItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId(PRIMARY_SSKU_3)
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId(SECONDARY_SSKU_1)
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(BundleContent.newBuilder()
                                .setPrimaryItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId(PRIMARY_SSKU_4)
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId(SECONDARY_SSKU_1)
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .build());

        cheapestAsGiftDetails = PromoDetails.newBuilder()
                .setStartDate(current.toEpochSecond())
                .setEndDate(current.plusDays(1).toEpochSecond())
                .setShopPromoId(SHOP_PROMO_ID)
                .setType(CHEAPEST_AS_GIFT.getReportCode())
                .setCheapestAsGift(PromoDetails.CheapestAsGift.newBuilder()
                        .setCount(3)
                        .addFeedOfferIds(FeedOfferId.newBuilder()
                                .setFeedId((int) FEED_ID)
                                .setOfferId(PRIMARY_SSKU_1)
                                .build())
                        .addFeedOfferIds(FeedOfferId.newBuilder()
                                .setFeedId((int) FEED_ID)
                                .setOfferId(PRIMARY_SSKU_2)
                                .build())
                        .addFeedOfferIds(FeedOfferId.newBuilder()
                                .setFeedId((int) FEED_ID)
                                .setOfferId(PRIMARY_SSKU_3)
                                .build())
                        .addFeedOfferIds(FeedOfferId.newBuilder()
                                .setFeedId((int) FEED_ID)
                                .setOfferId(PRIMARY_SSKU_4)
                                .build())
                        .build());
    }

    private void prepareData(PromoYtTestHelper.YtreeDataBuilder dataBuilder) {
        promoBundlesTestHelper.addNullRecord(dataBuilder);
        dataBuilder
                .promo(FEED_ID, SOME_PROMO_KEY, giftWithPurchaseDetails.clone().setShopPromoId(randomString()))
                .promo(ANOTHER_FEED_ID, ANOTHER_PROMO_KEY, giftWithPurchaseDetails.clone().setShopPromoId(randomString()))
                .promo(ANOTHER_ONE_FEED_ID, ANOTHER_ONE_PROMO_KEY, cheapestAsGiftDetails.clone());
    }

    @Test
    public void shouldUpdatePromoBundlesFromYt() {
        promoBundlesTestHelper.usingMock(this::prepareData, bundlesUpdateProcessor::updatePromoFromYt);

        Set<String> persisted = promoBundleService.getActivePromoKeys();
        assertThat(persisted, hasSize(3));
        assertThat(persisted, hasItems(
                SOME_PROMO_KEY,
                ANOTHER_PROMO_KEY,
                ANOTHER_ONE_PROMO_KEY
        ));
    }

    @Test
    public void shouldSavePromoBundleLandingUrl() {
        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "PROMO_KEY", giftWithPurchaseDetails.clone().setLandingUrl("created_landing_url"
                    ));
                },
                bundlesUpdateProcessor::updatePromoFromYt
        );

        Set<PromoBundleDescription> promoBundles = promoBundleService.getActivePromoBundles(Collections.singleton(
                "PROMO_KEY"));

        assertThat(promoBundles, hasSize(1));
        PromoBundleDescription promoBundle = promoBundles.iterator().next();
        assertThat(promoBundle.getPromoId(), notNullValue());

        assertThat(
                promoService.getPromoParamValue(promoBundle.getPromoId(), PromoParameterName.LANDING_URL).orElse(""),
                is("created_landing_url"));
    }

    @Test
    public void shouldUpdatePromoBundleLandingUrl() {
        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "PROMO_KEY", giftWithPurchaseDetails.clone().setLandingUrl("old_landing_url"));
                },
                bundlesUpdateProcessor::updatePromoFromYt
        );

        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "PROMO_KEY", giftWithPurchaseDetails.clone().setLandingUrl("new_landing_url"));
                },
                bundlesUpdateProcessor::updatePromoFromYt
        );

        Set<PromoBundleDescription> promoBundles = promoBundleService.getActivePromoBundles(Collections.singleton(
                "PROMO_KEY"));

        assertThat(promoBundles, hasSize(1));
        PromoBundleDescription promoBundle = promoBundles.iterator().next();
        assertThat(promoBundle.getPromoId(), notNullValue());

        assertThat(
                promoService.getPromoParamValue(promoBundle.getPromoId(), PromoParameterName.LANDING_URL).orElse(""),
                is("new_landing_url"));
    }

    @Test
    public void shouldDeactivatePromoBundles() {
        PromoBundleDescription expectedDescription = promoBundleService.createPromoBundle(getPromoBundleDescription());

        assertThat(expectedDescription, hasProperty("status", is(PromoBundleStatus.ACTIVE)));
        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        promoBundlesTestHelper.usingMock(this::prepareData, bundlesUpdateProcessor::updatePromoFromYt);

        assertThat(
                promoBundleService.getActivePromoBundles(Collections.singleton("some expired promo")), empty()
        );

        assertThat(
                promoBundleDao.getPromoBundleDescriptionById(expectedDescription.getId()),
                hasProperty("status", is(PromoBundleStatus.INACTIVE))
        );

        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.INACTIVE))
        );
    }

    @Test
    public void shouldNotDeactivateSnapshotWhenPromoKeyWasChanged() {
        // Arrange
        final long feedId = 12345L;
        PromoBundleDescription oldDescription = promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(12345),
                promoKey("old_promo_key"),
                shopPromoId(SHOP_PROMO_ID),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(giftWithPurchase(feedId, PRIMARY_SSKU_1)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(feedId, SECONDARY_SSKU_1)))
                )
        );

        assertThat(oldDescription, hasProperty("status", is(PromoBundleStatus.ACTIVE)));
        assertThat(oldDescription, hasProperty("promoKey", is("old_promo_key")));
        assertThat(
                promoService.getPromo(oldDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        // Act
        promoBundlesTestHelper.usingMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(feedId, "new_promo_key", giftWithPurchaseDetails.clone());
                },
                bundlesUpdateProcessor::updatePromoFromYt
        );

        // Assert
        assertThat(
                promoBundleService.getActivePromoBundles(Collections.singleton("new_promo_key")),
                hasItem(allOf(
                        hasProperty("id", is(oldDescription.getId())),
                        hasProperty("status", is(PromoBundleStatus.ACTIVE))
                ))
        );

        assertThat(
                promoService.getPromo(oldDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        // Должен появиться активный фолбэк со старым promo_key
        assertThat(
                promoBundleService.getActivePromoBundles(Collections.singleton("old_promo_key")),
                hasItem(allOf(
                        hasProperty("id", not(oldDescription.getId())),
                        hasProperty("status", is(PromoBundleStatus.ACTIVE)),
                        hasProperty("snapshotVersion", is(true)),
                        hasProperty("deactivationTime", nullValue())
                ))
        );
    }

    @Test
    public void shouldUpdateStatusBeforeInactiveToActivePromoBundles() {
        PromoBundleDescription expectedDescription = promoBundleService.createPromoBundle(getPromoBundleDescription());

        assertNotNull(expectedDescription);
        assertThat(expectedDescription, hasProperty("status", is(PromoBundleStatus.ACTIVE)));
        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        promoBundlesTestHelper.usingMock(this::prepareData, bundlesUpdateProcessor::updatePromoFromYt);

        PromoBundleDescription activeDescription = promoBundleService.createPromoBundle(getPromoBundleDescription());

        assertNotNull(activeDescription);
        assertThat(activeDescription, hasProperty("status", is(PromoBundleStatus.ACTIVE)));
        assertEquals(expectedDescription.getPromoId(), activeDescription.getPromoId());
        assertEquals(expectedDescription, activeDescription);
        assertThat(
                promoService.getPromo(activeDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );

        assertThat(
                promoBundleService.getActivePromoBundles(Collections.singleton("some expired promo")), not(empty())
        );

        assertThat(
                promoBundleDao.getPromoBundleDescriptionById(expectedDescription.getId()),
                hasProperty("status", is(PromoBundleStatus.ACTIVE))
        );

        assertThat(
                promoService.getPromo(expectedDescription.getPromoId()),
                hasProperty("status", is(PromoStatus.ACTIVE))
        );
    }

    private PromoBundleDescription getPromoBundleDescription() {
        return bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey("some expired promo"),
                shopPromoId("some expired promo"),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(giftWithPurchase(FEED_ID, PRIMARY_SSKU_1)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, SECONDARY_SSKU_1)))
        );
    }
}
