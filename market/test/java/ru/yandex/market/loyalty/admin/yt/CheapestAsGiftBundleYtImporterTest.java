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
import Market.Promo.Promo.PromoDetails.CheapestAsGift;
import Market.Promo.Promo.PromoDetails.FeedOfferId;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
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
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;

public class CheapestAsGiftBundleYtImporterTest extends MarketLoyaltyAdminMockedDbTest {
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
                .setType(CHEAPEST_AS_GIFT.getReportCode())
                .setCheapestAsGift(CheapestAsGift.newBuilder()
                        .setCount(3)
                        .addFeedOfferIds(FeedOfferId.newBuilder()
                                .setFeedId((int) FEED_ID)
                                .setOfferId(SSKU_1)
                                .build())
                        .addFeedOfferIds(FeedOfferId.newBuilder()
                                .setFeedId((int) FEED_ID)
                                .setOfferId(SSKU_2)
                                .build())
                        .addFeedOfferIds(FeedOfferId.newBuilder()
                                .setFeedId((int) FEED_ID)
                                .setOfferId(SSKU_3)
                                .build())
                        .addFeedOfferIds(FeedOfferId.newBuilder()
                                .setFeedId((int) FEED_ID)
                                .setOfferId(SSKU_4)
                                .build())
                        .build());
    }

    private void prepareData(PromoYtTestHelper.YtreeDataBuilder dataBuilder) {
        promoBundlesTestHelper.addNullRecord(dataBuilder);
        dataBuilder
                .promo(FEED_ID, SOME_PROMO_KEY, typicalDetails.clone().setShopPromoId(randomString()))
                .promo(ANOTHER_FEED_ID, ANOTHER_PROMO_KEY, typicalDetails.clone().setShopPromoId(randomString()));
    }

    @Test
    public void shouldImportCheapestAsGiftBundle() {
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
                        hasProperty("promoBundlesStrategyType", is(CHEAPEST_AS_GIFT)),
                        hasValidProperties()
                ),
                allOf(
                        hasProperty("feedId", is(ANOTHER_FEED_ID)),
                        hasProperty("promoKey", is(ANOTHER_PROMO_KEY)),
                        hasProperty("promoBundlesStrategyType", is(CHEAPEST_AS_GIFT)),
                        hasValidProperties()
                )
        ));

        promoBundleDescriptions.forEach(p -> assertNotNull(promoDao.getPromo(p.getPromoId())));
    }

    private static Matcher<PromoBundleDescription> hasValidProperties() {
//      MARKETDISCOUNT-6684 Do not ship the assortment for 2=3
        return allOf(
                hasProperty("status", is(ACTIVE)),
                hasProperty("items", hasSize(0))
        );
    }
}
