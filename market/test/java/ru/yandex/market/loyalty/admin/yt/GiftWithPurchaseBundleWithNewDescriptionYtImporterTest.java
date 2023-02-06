package ru.yandex.market.loyalty.admin.yt;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper.YtreeDataBuilder;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.ImportResult;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.GenericBundle.BundleContent;
import Market.Promo.Promo.PromoDetails.GenericBundle.PromoItem;
import Market.Promo.Promo.PromoDetails.GenericBundle.SecondaryItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao.activeOn;
import static ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao.withKeys;
import static ru.yandex.market.loyalty.core.dao.promo.Queries.PROMO_TYPE_FIELD;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PROMO_KEY;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.model.promo.CorePromoType.GENERIC_BUNDLE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toTimeUnit;
import static NMarket.Common.Promo.Promo.EPromoType;

public class GiftWithPurchaseBundleWithNewDescriptionYtImporterTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SHOP_PROMO_ID = "some promo";
    private static final String SOME_PROMO_KEY = "some promo";
    private static final String ANOTHER_PROMO_KEY = "another promo";
    private static final String ANOTHER_ONE_PROMO_KEY = "another one promo";
    private static final String PRIMARY_SSKU_1 = "primary ssku 1";
    private static final String PRIMARY_SSKU_2 = "primary ssku 2";
    private static final String SECONDARY_SSKU_1 = "secondary ssku 1";
    private static final String SECONDARY_SSKU_2 = "secondary ssku 2";
    private static final long FEED_ID = 1234;
    private static final long ANOTHER_FEED_ID = 1235;
    private static final long ANOTHER_ONE_FEED_ID = 1236;

    @Autowired
    private PromoBundleDao promoBundleDao;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private PromoYtImporter importer;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    private PromoDetails.Builder typicalDetails;

    @Override
    @Before
    public void initMocks() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        typicalDetails = PromoDetails.newBuilder()
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
                                        .setOfferId(PRIMARY_SSKU_1)
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId(SECONDARY_SSKU_2)
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
                        .build())
                .setRestrictions(PromoDetails.Restrictions.newBuilder()
                        .addRestrictedPromoTypes(EPromoType.MarketBonus)
                        .build());

    }

    private void prepareData(YtreeDataBuilder dataBuilder) {
        promoBundlesTestHelper.addNullRecord(dataBuilder);
        dataBuilder
                .promo(FEED_ID, SOME_PROMO_KEY, typicalDetails.clone())
                .promo(ANOTHER_FEED_ID, ANOTHER_PROMO_KEY, typicalDetails.clone())
                .promo(ANOTHER_ONE_FEED_ID, ANOTHER_ONE_PROMO_KEY, typicalDetails.clone());
    }

    @Test
    public void shouldImportPromoBundleDescription() {
        promoBundlesTestHelper.usingMock(this::prepareData, importer::importPromos);

        Set<PromoBundleDescription> promoBundleDescriptions = promoBundleDao.select(
                activeOn(clock.dateTime()),
                withKeys(
                        SOME_PROMO_KEY,
                        ANOTHER_PROMO_KEY,
                        ANOTHER_ONE_PROMO_KEY
                )
        );

        assertThat(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)), hasSize(3));
        assertThat(promoBundleDescriptions, hasSize(3));
        assertThat(promoBundleDescriptions, everyItem(
                hasProperty("shopPromoId", is(SHOP_PROMO_ID))));
        assertThat(promoBundleDescriptions, hasItems(
                allOf(
                        hasProperty("promoKey", is(SOME_PROMO_KEY)),
                        hasProperty("items", hasSize(2)),
                        hasProperty("items", hasItems(
                                allOf(
                                        hasProperty("primary", is(true)),
                                        hasProperty("condition", hasProperty("mappings", hasItems(
                                                hasProperty("ssku", is(PRIMARY_SSKU_1)),
                                                hasProperty("ssku", is(PRIMARY_SSKU_2))
                                        )))
                                ),
                                allOf(
                                        hasProperty("primary", is(false)),
                                        hasProperty("condition", hasProperty("mappings", hasItems(
                                                allOf(
                                                        hasProperty("ssku", is(SECONDARY_SSKU_1)),
                                                        hasProperty("requiredSskus", contains(PRIMARY_SSKU_1))
                                                ),
                                                allOf(
                                                        hasProperty("ssku", is(SECONDARY_SSKU_2)),
                                                        hasProperty("requiredSskus", contains(PRIMARY_SSKU_1))
                                                ),
                                                allOf(
                                                        hasProperty("ssku", is(SECONDARY_SSKU_1)),
                                                        hasProperty("requiredSskus", contains(PRIMARY_SSKU_2))
                                                )
                                        )))
                                )
                        ))
                ),
                hasProperty("promoKey", is(ANOTHER_PROMO_KEY)),
                hasProperty("promoKey", is(ANOTHER_ONE_PROMO_KEY))
        ));
    }

    @Test
    public void shouldImportRestrictionFlags() {
        promoBundlesTestHelper.usingMock(this::prepareData, importer::importPromos);

        Set<PromoBundleDescription> promoBundleDescriptions = promoBundleDao.select(
                activeOn(clock.dateTime()),
                withKeys(
                        SOME_PROMO_KEY,
                        ANOTHER_PROMO_KEY,
                        ANOTHER_ONE_PROMO_KEY
                )
        );

        assertThat(promoBundleDescriptions, everyItem(hasProperty("restrictions", allOf(
                hasProperty("allowBerubonus", is(false)),
                hasProperty("allowPromocode", is(true))
        ))));
    }

    @Test
    public void shouldUpdateIdempotently() {
        promoBundlesTestHelper.usingMock(this::prepareData, importer::importPromos);

        assertThat(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)), hasSize(3));
        assertThat(
                promoBundleDao.selectPromoKeysBy(activeOn(clock.dateTime())),
                hasSize(3)
        );

        promoBundlesTestHelper.usingMock(this::prepareData, importer::importPromos);

        assertThat(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)), hasSize(3));
        assertThat(
                promoBundleDao.selectPromoKeysBy(activeOn(clock.dateTime())),
                hasSize(3)
        );

        promoBundlesTestHelper.usingMock(this::prepareData, importer::importPromos);

        assertThat(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)), hasSize(3));
        assertThat(
                promoBundleDao.selectPromoKeysBy(activeOn(clock.dateTime())),
                hasSize(3)
        );

        promoBundlesTestHelper.usingMock(this::prepareData, importer::importPromos);

        assertThat(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)), hasSize(3));
        assertThat(
                promoBundleDao.selectPromoKeysBy(activeOn(clock.dateTime())),
                hasSize(3)
        );
    }

    @Test
    public void shouldUpdatePromoBundleDescription() {
        promoBundlesTestHelper.usingMock(this::prepareData, importer::importPromos);

        assertThat(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)), hasSize(3));

        assertTrue(promoBundleDao.findPromoBundlesBy(
                SOME_PROMO_KEY,
                clock.dateTime(), true
        ).isPresent());

        promoBundlesTestHelper.usingMock(dataBuilder -> {
            promoBundlesTestHelper.addNullRecord(dataBuilder);

            dataBuilder.promo(FEED_ID, "changed md5 key",
                    typicalDetails.clone().setGenericBundle(PromoDetails.GenericBundle.newBuilder()
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
                            .build())
                            .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                    .addRestrictedPromoTypes(EPromoType.PromoCode)
                                    .addRestrictedPromoTypes(EPromoType.MarketBonus)
                                    .build())
            );
        }, importer::importPromos);

        assertThat(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)), hasSize(3));

        assertThat(promoBundleDao.findPromoBundlesBy(
                SOME_PROMO_KEY,
                clock.dateTime(), true
        ).get(), hasProperty("snapshotVersion", is(true)));

        final Optional<PromoBundleDescription> descriptionOptional = promoBundleDao.findPromoBundlesBy(
                "changed md5 key",
                clock.dateTime(), true
        );
        assertTrue(descriptionOptional.isPresent());
    }

    @Test
    public void shouldImportPromoBundlesWithProportionalDiscount() {
        promoBundlesTestHelper.usingMock(
                dataBuilder -> {
                    promoBundlesTestHelper.addNullRecord(dataBuilder);
                    dataBuilder
                            .promo(FEED_ID, "promo", PromoDetails.newBuilder()
                                    .setStartDate(toTimeUnit(LocalDateTime.now(), TimeUnit.SECONDS))
                                    .setEndDate(toTimeUnit(LocalDateTime.now().plusDays(1), TimeUnit.SECONDS))
                                    .setShopPromoId(randomString())
                                    .setType(GIFT_WITH_PURCHASE.getReportCode())
                                    .setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                                            .addBundlesContent(BundleContent.newBuilder()
                                                    .setSpreadDiscount(90.187)
                                                    .setPrimaryItem(PromoItem.newBuilder()
                                                            .setCount(1)
                                                            .setOfferId(randomString())
                                                            .build())
                                                    .setSecondaryItem(SecondaryItem.newBuilder()
                                                            .setItem(PromoItem.newBuilder()
                                                                    .setCount(1)
                                                                    .setOfferId(randomString())
                                                                    .build()))
                                                    .build())
                                            .build())
                                    .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                            .addRestrictedPromoTypes(EPromoType.PromoCode)
                                            .build()));
                },
                importer::importPromos
        );

        PromoBundleDescription description = promoBundleDao.get(
                PROMO_KEY.eqTo("promo").and(PromoBundleDescription.FEED_ID.eqTo(FEED_ID)));

        assertThat(description.getItems(), hasItems(
                allOf(
                        hasProperty("primary", is(true)),
                        hasProperty("condition", hasProperty("mappings", hasItem(allOf(
                                hasProperty("proportion", nullValue()),
                                hasProperty("fixedPrice", nullValue())
                        ))))
                ),
                allOf(
                        hasProperty("primary", is(false)),
                        hasProperty("condition", hasProperty("mappings", hasItem(allOf(
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(9.813))),
                                hasProperty("fixedPrice", nullValue())
                        ))))
                )
        ));
    }

    @Test
    public void shouldReturnInvalidRecords() {
        List<PromoYtImporter.ImportResult> importResults = promoBundlesTestHelper.withMock(dataBuilder -> {
            promoBundlesTestHelper.addNullRecord(dataBuilder);
            dataBuilder.promo(200344511L, "changed md5 key",
                    typicalDetails.clone().setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                            .addBundlesContent(BundleContent.newBuilder()
                                    .setPrimaryItem(PromoItem.newBuilder()
                                            .setCount(1)
                                            .setOfferId("00065.00026.100126176813")
                                            .build())
                                    .setSecondaryItem(SecondaryItem.newBuilder()
                                            .setItem(PromoItem.newBuilder()
                                                    .setCount(1)
                                                    .setOfferId("00065.00023.100126176176")
                                                    .build())
                                            .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                    .setCurrency(Currency.RUR.name())
                                                    .setValue(100)
                                                    .build())
                                            .build())
                                    .build())
                            .build())
            );

            dataBuilder.promo(200344512L, "invalid promo",
                    typicalDetails.clone().setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                            .clearBundlesContent()
                            .build())
            );
        }, importer::importPromos);

        assertThat(importResults, not(empty()));

        assertThat(importResults.get(0).getImportResults(), hasItems(
                hasProperty("promoKey", is("changed md5 key")),
                allOf(
                        hasProperty("promoKey", is("invalid promo")),
                        hasProperty("valid", is(false))
                )
        ));
    }

    @Test
    public void shouldImportRecordsWithZeroSpreadDiscount() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(dataBuilder -> {
            promoBundlesTestHelper.addNullRecord(dataBuilder);
            dataBuilder.promo(FEED_ID, "promo with zero spread discount",
                    PromoDetails.newBuilder()
                            .setStartDate(1566680400)
                            .setEndDate(1577825999)
                            .setType(GIFT_WITH_PURCHASE.getReportCode())
                            .setShopPromoId("promo with zero spread discount")
                            .setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                                    .addBundlesContent(BundleContent.newBuilder()
                                            .setSpreadDiscount(0.0)
                                            .setPrimaryItem(PromoItem.newBuilder()
                                                    .setCount(1)
                                                    .setOfferId("00065.00026.100126174633")
                                                    .build())
                                            .setSecondaryItem(SecondaryItem.newBuilder()
                                                    .setItem(PromoItem.newBuilder()
                                                            .setCount(1)
                                                            .setOfferId("00065.00023.100126176175")
                                                            .build())
                                                    .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                            .setCurrency(Currency.RUR.name())
                                                            .setValue(100)
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                    .addRestrictedPromoTypes(EPromoType.MarketBonus)
                                    .build())
            );
        }, importer::importPromos);

        assertThat(importResults, not(empty()));

        assertThat(importResults.get(0).getImportResults(), hasItems(
                hasProperty("promoKey", is("promo with zero spread discount"))
        ));
    }

    @Test
    public void shouldImportRecordsWith100PercentSpreadDiscount() {
        List<PromoYtImporter.ImportResult> importResults = promoBundlesTestHelper.withMock(dataBuilder -> {
            promoBundlesTestHelper.addNullRecord(dataBuilder);
            dataBuilder.promo(FEED_ID, "promo with zero spread discount",
                    PromoDetails.newBuilder()
                            .setStartDate(1566680400)
                            .setEndDate(1577825999)
                            .setType(GIFT_WITH_PURCHASE.getReportCode())
                            .setShopPromoId("promo with zero spread discount")
                            .setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                                    .addBundlesContent(BundleContent.newBuilder()
                                            .setSpreadDiscount(100.0)
                                            .setPrimaryItem(PromoItem.newBuilder()
                                                    .setCount(1)
                                                    .setOfferId("00065.00026.100126174633")
                                                    .build())
                                            .setSecondaryItem(SecondaryItem.newBuilder()
                                                    .setItem(PromoItem.newBuilder()
                                                            .setCount(1)
                                                            .setOfferId("00065.00023.100126176175")
                                                            .build())
                                                    .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                            .setCurrency(Currency.RUR.name())
                                                            .setValue(100)
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                    .addRestrictedPromoTypes(EPromoType.MarketBonus)
                                    .build())
            );
        }, importer::importPromos);

        assertThat(importResults, not(empty()));

        assertThat(importResults.get(0).getImportResults(), hasItems(
                hasProperty("promoKey", is("promo with zero spread discount"))
        ));
    }

    @Test
    public void shouldCreateOneSnapshotVersionForEachImport() {
        promoBundlesTestHelper.withMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "some promo key", typicalDetails.clone());
                },
                importer::importPromos
        );

        promoBundlesTestHelper.withMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "changed promo key", typicalDetails.clone());
                },
                importer::importPromos
        );

        promoBundlesTestHelper.withMock(
                db -> {
                    promoBundlesTestHelper.addNullRecord(db);
                    db.promo(FEED_ID, "changed twice promo key", typicalDetails.clone());
                },
                importer::importPromos
        );

        assertThat(promoBundleDao.select(
                PromoBundleDescription.SHOP_PROMO_ID.eqTo(SHOP_PROMO_ID),
                PromoBundleDescription.FEED_ID.eqTo(FEED_ID),
                PromoBundleDescription.SNAPSHOT_VERSION.eqTo(true)
        ), hasSize(2));
    }
}
