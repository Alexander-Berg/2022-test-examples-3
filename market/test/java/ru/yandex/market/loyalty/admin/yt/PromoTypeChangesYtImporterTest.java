package ru.yandex.market.loyalty.admin.yt;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper.ThrowableConsumer;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.ImportResult;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.PromoImportResult;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import Market.Promo.Promo.PromoDetails;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL.count;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL.discountProportion;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL.set;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueSetDSL.ssku;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.GenericBundleDSL.relation;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.blueSetDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.cheapestAsGiftDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.giftWithPurchaseDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.promoDetails;
import static ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao.activeOn;
import static ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao.withKeys;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;

public class PromoTypeChangesYtImporterTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SHOP_PROMO_ID = "some promo";
    private static final String SOME_PROMO_KEY = "some promo";
    private static final String SSKU_1 = "some ssku 1";
    private static final String SSKU_2 = "some ssku 2";
    private static final String SSKU_3 = "some ssku 3";
    private static final String SSKU_4 = "some ssku 4";
    private static final long FEED_ID = 1234;

    @Autowired
    private PromoBundleDao promoBundleDao;
    @Autowired
    private PromoYtImporter importer;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    private PromoDetails.Builder blueSetDetails;
    private PromoDetails.Builder giftDetails;
    private PromoDetails.Builder cheapestAsGift;

    @Override
    @Before
    public void initMocks() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        cheapestAsGift = promoDetails(
                PromoYtGenerator.PromoDetailsDSL.shopPromoId(SHOP_PROMO_ID),
                PromoYtGenerator.PromoDetailsDSL.starts(current),
                PromoYtGenerator.PromoDetailsDSL.ends(current.plusDays(1)),

                cheapestAsGiftDetails(
                        PromoYtGenerator.CheapestAsGiftDSL.quantityInBundle(3),

                        PromoYtGenerator.CheapestAsGiftDSL.ssku(FEED_ID, SSKU_1),
                        PromoYtGenerator.CheapestAsGiftDSL.ssku(FEED_ID, SSKU_2),
                        PromoYtGenerator.CheapestAsGiftDSL.ssku(FEED_ID, SSKU_3)
                )
        );

        blueSetDetails = promoDetails(
                PromoYtGenerator.PromoDetailsDSL.shopPromoId(SHOP_PROMO_ID),
                PromoYtGenerator.PromoDetailsDSL.starts(current),
                PromoYtGenerator.PromoDetailsDSL.ends(current.plusDays(1)),

                blueSetDetails(
                        set(
                                PromoYtGenerator.BlueSetDSL.item(
                                        ssku(SSKU_1),
                                        count(1),
                                        discountProportion(30)
                                ),
                                PromoYtGenerator.BlueSetDSL.item(
                                        ssku(SSKU_2),
                                        count(1),
                                        discountProportion(30)
                                ),
                                PromoYtGenerator.BlueSetDSL.item(
                                        ssku(SSKU_3),
                                        count(1),
                                        discountProportion(30)
                                )
                        ),
                        set(
                                PromoYtGenerator.BlueSetDSL.item(
                                        ssku(SSKU_4),
                                        count(1),
                                        discountProportion(20)
                                ),
                                PromoYtGenerator.BlueSetDSL.item(
                                        ssku(SSKU_3),
                                        count(1),
                                        discountProportion(20)
                                )
                        )
                )
        );

        giftDetails = promoDetails(
                PromoYtGenerator.PromoDetailsDSL.shopPromoId(SHOP_PROMO_ID),
                PromoYtGenerator.PromoDetailsDSL.starts(current),
                PromoYtGenerator.PromoDetailsDSL.ends(current.plusDays(1)),

                giftWithPurchaseDetails(
                        relation(
                                PromoYtGenerator.GenericBundleDSL.primary(
                                        PromoYtGenerator.GenericBundleDSL.ssku(SSKU_1)
                                ),
                                PromoYtGenerator.GenericBundleDSL.gift(
                                        PromoYtGenerator.GenericBundleDSL.item(
                                                PromoYtGenerator.GenericBundleDSL.ssku(SSKU_2)
                                        )
                                ),
                                PromoYtGenerator.GenericBundleDSL.proportion(100)
                        ),
                        relation(
                                PromoYtGenerator.GenericBundleDSL.primary(
                                        PromoYtGenerator.GenericBundleDSL.ssku(SSKU_3)
                                ),
                                PromoYtGenerator.GenericBundleDSL.gift(
                                        PromoYtGenerator.GenericBundleDSL.item(
                                                PromoYtGenerator.GenericBundleDSL.ssku(SSKU_2)
                                        )
                                ),
                                PromoYtGenerator.GenericBundleDSL.proportion(100)
                        )
                )
        );
    }

    private ThrowableConsumer<PromoYtTestHelper.YtreeDataBuilder> prepareData(PromoDetails.Builder details) {
        return db -> {
            promoBundlesTestHelper.addNullRecord(db);
            db.promo(FEED_ID, SOME_PROMO_KEY, details.clone());
        };
    }

    @Test
    public void shouldChangeBlueSetToGiftPromoType() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(
                prepareData(blueSetDetails),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        Collection<PromoImportResult> results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        PromoBundleDescription promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertBlueSetDetails(promoBundleDescription);

        importResults = promoBundlesTestHelper.withMock(
                prepareData(giftDetails),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertGiftDetails(promoBundleDescription);
    }

    @Test
    public void shouldChangeBlueSetToCheapestAsGiftPromoType() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(
                prepareData(blueSetDetails),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        Collection<PromoImportResult> results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        PromoBundleDescription promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertBlueSetDetails(promoBundleDescription);

        importResults = promoBundlesTestHelper.withMock(
                prepareData(cheapestAsGift),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertCheapestAsGiftDetails(promoBundleDescription);
    }

    @Test
    public void shouldChangeGiftToBlueSetPromoType() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(
                prepareData(giftDetails),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        Collection<PromoImportResult> results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        PromoBundleDescription promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertGiftDetails(promoBundleDescription);


        importResults = promoBundlesTestHelper.withMock(
                prepareData(blueSetDetails),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertBlueSetDetails(promoBundleDescription);
    }

    @Test
    public void shouldChangeGiftToCheapestAsGiftPromoType() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(
                prepareData(giftDetails),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        Collection<PromoImportResult> results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        PromoBundleDescription promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertGiftDetails(promoBundleDescription);


        importResults = promoBundlesTestHelper.withMock(
                prepareData(cheapestAsGift),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertCheapestAsGiftDetails(promoBundleDescription);
    }

    @Test
    public void shouldChangeCheapestAsGiftToBlueSetPromoType() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(
                prepareData(cheapestAsGift),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        Collection<PromoImportResult> results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        PromoBundleDescription promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertCheapestAsGiftDetails(promoBundleDescription);

        importResults = promoBundlesTestHelper.withMock(
                prepareData(blueSetDetails),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertBlueSetDetails(promoBundleDescription);
    }

    @Test
    public void shouldChangeCheapestAsGiftToGiftPromoType() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(
                prepareData(cheapestAsGift),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        Collection<PromoImportResult> results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        PromoBundleDescription promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertCheapestAsGiftDetails(promoBundleDescription);

        importResults = promoBundlesTestHelper.withMock(
                prepareData(giftDetails),
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(1));

        promoBundleDescription = promoBundleDao.selectFirst(
                activeOn(clock.dateTime()),
                withKeys(SHOP_PROMO_ID)
        ).get();

        assertGiftDetails(promoBundleDescription);
    }

    private void assertCheapestAsGiftDetails(PromoBundleDescription bundleDescription) {
        //      MARKETDISCOUNT-6684 Do not ship the assortment for 2=3
        assertThat(bundleDescription, allOf(
                hasProperty("feedId", is(FEED_ID)),
                hasProperty("promoKey", is(SHOP_PROMO_ID)),
                hasProperty("promoBundlesStrategyType", is(CHEAPEST_AS_GIFT)),
                hasProperty("items", hasSize(0))
        ));
    }

    private void assertGiftDetails(PromoBundleDescription bundleDescription) {
        assertThat(bundleDescription, allOf(
                hasProperty("feedId", is(FEED_ID)),
                hasProperty("promoKey", is(SHOP_PROMO_ID)),
                hasProperty("promoBundlesStrategyType", is(GIFT_WITH_PURCHASE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        hasProperty("condition", hasProperty("mappings", hasItems(
                                hasProperty("ssku", is(SSKU_1)),
                                hasProperty("ssku", is(SSKU_3))
                        ))),
                        hasProperty("condition", hasProperty("mappings", hasItems(
                                hasProperty("ssku", is(SSKU_2))
                        )))
                ))
        ));
    }

    private void assertBlueSetDetails(PromoBundleDescription bundleDescription) {
        assertThat(bundleDescription, allOf(
                hasProperty("feedId", is(FEED_ID)),
                hasProperty("promoKey", is(SHOP_PROMO_ID)),
                hasProperty("promoBundlesStrategyType", is(BLUE_SET)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        hasProperty("condition", hasProperty("conditions", hasItems(
                                hasProperty("ssku", is(SSKU_1)),
                                hasProperty("ssku", is(SSKU_2)),
                                hasProperty("ssku", is(SSKU_3))
                        ))),
                        hasProperty("condition", hasProperty("conditions", hasItems(
                                hasProperty("ssku", is(SSKU_3)),
                                hasProperty("ssku", is(SSKU_4))
                        )))
                ))
        ));
    }
}
