package ru.yandex.market.loyalty.admin.yt;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.ImportResult;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.GenericBundle.BundleContent;
import Market.Promo.Promo.PromoDetails.GenericBundle.PromoItem;
import Market.Promo.Promo.PromoDetails.GenericBundle.SecondaryItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PROMO_KEY;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toTimeUnit;
import static NMarket.Common.Promo.Promo.EPromoType;

public class GiftWithPurchaseBundleWithOldDescriptionYtImporterTest extends MarketLoyaltyAdminMockedDbTest {
    private static final long FEED_ID = 1234;

    @Autowired
    private PromoBundleDao promoBundleDao;
    @Autowired
    private PromoYtImporter importer;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

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
                                            .setSpreadDiscount(90.187)
                                            .addBundlesContent(BundleContent.newBuilder()
                                                    .setPrimaryItem(PromoItem.newBuilder()
                                                            .setCount(1)
                                                            .setOfferId(randomString())
                                                            .build())
                                                    .setSecondaryItem(SecondaryItem.newBuilder()
                                                            .setItem(PromoItem.newBuilder()
                                                                    .setCount(1)
                                                                    .setOfferId(randomString())
                                                                    .build())
                                                            .build())
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
}
