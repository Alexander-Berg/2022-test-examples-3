package ru.yandex.market.loyalty.admin.yt;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.ImportResult;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.PromoImportResult;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.BlueSet.SetContent;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao.activeOn;
import static ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao.withKeys;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import NMarket.Common.Promo.Promo.EPromoType;

public class BlueSetPromoYtImporterTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SOME_PROMO_KEY = "some promo";
    private static final String ANOTHER_PROMO_KEY = "another promo";
    private static final String SSKU_1 = "some ssku 1";
    private static final String SSKU_2 = "some ssku 2";
    private static final String SSKU_3 = "some ssku 3";
    private static final String SSKU_4 = "some ssku 4";
    private static final long FEED_ID = 1234;
    private static final long ANOTHER_FEED_ID = 12345;

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
                .setType(BLUE_SET.getReportCode())
                .setBlueSet(PromoDetails.BlueSet.newBuilder()
                        .addSetsContent(SetContent.newBuilder()
                                .addItems(SetContent.SetItem.newBuilder()
                                        .setOfferId(SSKU_1)
                                        .setDiscount(30.0)
                                        .setCount(1)
                                        .build())
                                .addItems(SetContent.SetItem.newBuilder()
                                        .setOfferId(SSKU_2)
                                        .setDiscount(30.0)
                                        .setCount(1)
                                        .build())
                                .addItems(SetContent.SetItem.newBuilder()
                                        .setOfferId(SSKU_3)
                                        .setDiscount(30.0)
                                        .setCount(1)
                                        .build())
                                .build())
                        .addSetsContent(SetContent.newBuilder()
                                .addItems(SetContent.SetItem.newBuilder()
                                        .setOfferId(SSKU_4)
                                        .setDiscount(20.0)
                                        .setCount(1)
                                        .build())
                                .addItems(SetContent.SetItem.newBuilder()
                                        .setOfferId(SSKU_3)
                                        .setDiscount(0.0)
                                        .setCount(1)
                                        .build())
                                .build())
                        .build())
                .setRestrictions(PromoDetails.Restrictions.newBuilder()
                        .addRestrictedPromoTypes(EPromoType.PromoCode)
                        .build());
    }

    private void prepareData(PromoYtTestHelper.YtreeDataBuilder dataBuilder) {
        promoBundlesTestHelper.addNullRecord(dataBuilder);
        dataBuilder
                .promo(FEED_ID, SOME_PROMO_KEY, typicalDetails.clone().setShopPromoId(randomString()))
                .promo(ANOTHER_FEED_ID, ANOTHER_PROMO_KEY, typicalDetails.clone().setShopPromoId(randomString()));
    }

    @Test
    public void shouldImportBlueSetBundle() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(
                this::prepareData,
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        Collection<PromoImportResult> results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(2));

        Set<PromoBundleDescription> promoBundleDescriptions = promoBundleDao.select(
                activeOn(clock.dateTime()),
                withKeys(
                        SOME_PROMO_KEY,
                        ANOTHER_PROMO_KEY
                )
        );

        assertThat(promoBundleDescriptions, hasSize(2));
        assertThat(promoBundleDescriptions, hasItems(
                allOf(
                        hasProperty("feedId", is(FEED_ID)),
                        hasProperty("promoKey", is(SOME_PROMO_KEY)),
                        hasProperty("promoBundlesStrategyType", is(BLUE_SET)),
                        hasValidProperties()
                ),
                allOf(
                        hasProperty("feedId", is(ANOTHER_FEED_ID)),
                        hasProperty("promoKey", is(ANOTHER_PROMO_KEY)),
                        hasProperty("promoBundlesStrategyType", is(BLUE_SET)),
                        hasValidProperties()
                )
        ));

        promoBundleDescriptions.forEach(p -> assertNotNull(promoDao.getPromo(p.getPromoId())));
    }

    private static Matcher<PromoBundleDescription> hasValidProperties() {
        return allOf(
                hasProperty("status", is(ACTIVE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("condition", hasProperty("conditions", hasItems(
                                allOf(
                                        hasProperty("ssku", is(SSKU_1)),
                                        hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30)))
                                ),
                                allOf(
                                        hasProperty("ssku", is(SSKU_2)),
                                        hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30)))
                                ),
                                allOf(
                                        hasProperty("ssku", is(SSKU_3)),
                                        hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30)))
                                )
                        )))
                ), allOf(
                        hasProperty("quantityInBundle", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("condition", hasProperty("conditions", hasItems(
                                allOf(
                                        hasProperty("ssku", is(SSKU_4)),
                                        hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(20)))
                                ),
                                allOf(
                                        hasProperty("ssku", is(SSKU_3)),
                                        hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(0)))
                                )
                        )))
                )))
        );
    }
}
