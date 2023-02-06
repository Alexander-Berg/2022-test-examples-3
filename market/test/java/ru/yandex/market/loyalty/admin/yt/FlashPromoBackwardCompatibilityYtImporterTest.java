package ru.yandex.market.loyalty.admin.yt;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BlueFlashDSL;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.PromoDetailsDSL;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.ImportResult;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.PromoImportResult;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.flash.FlashPromoDao;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoStatus;
import Market.Promo.Promo.PromoDetails;

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
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.blueFlashDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.promoDetails;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;

public class FlashPromoBackwardCompatibilityYtImporterTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SOME_PROMO_KEY = "some promo";
    private static final String ANOTHER_PROMO_KEY = "another promo";
    private static final String SSKU_1 = "some ssku 1";
    private static final String SSKU_2 = "some ssku 2";
    private static final String SSKU_3 = "some ssku 3";
    private static final String SSKU_4 = "some ssku 4";
    private static final long FEED_ID = 1234;
    private static final long ANOTHER_FEED_ID = 12345;

    @Autowired
    private FlashPromoDao flashPromoDao;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private PromoYtImporter importer;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    private void prepareData(PromoYtTestHelper.YtreeDataBuilder dataBuilder) {
        promoBundlesTestHelper.addNullRecord(dataBuilder);
        dataBuilder
                .promo(FEED_ID, SOME_PROMO_KEY, flashPromoDetails(FEED_ID, randomString()))
                .promo(ANOTHER_FEED_ID, ANOTHER_PROMO_KEY, flashPromoDetails(ANOTHER_FEED_ID, randomString()));
    }

    private PromoDetails.Builder flashPromoDetails(long feedId, String shopPromoId) {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        return promoDetails(
                PromoDetailsDSL.shopPromoId(shopPromoId),
                PromoDetailsDSL.starts(current),
                PromoDetailsDSL.ends(current.plusDays(1)),

                blueFlashDetails(
                        BlueFlashDSL.item(
                                BlueFlashDSL.offer(feedId, SSKU_1),
                                BlueFlashDSL.fixedPrice(10),
                                BlueFlashDSL.quantityBudget(1000)
                        ),
                        BlueFlashDSL.item(
                                BlueFlashDSL.offer(feedId, SSKU_2),
                                BlueFlashDSL.fixedPrice(20),
                                BlueFlashDSL.quantityBudget(1000)
                        ),
                        BlueFlashDSL.item(
                                BlueFlashDSL.offer(feedId, SSKU_3),
                                BlueFlashDSL.fixedPrice(30),
                                BlueFlashDSL.quantityBudget(1000)
                        ),
                        BlueFlashDSL.item(
                                BlueFlashDSL.offer(feedId, SSKU_4),
                                BlueFlashDSL.fixedPrice(40),
                                BlueFlashDSL.moneyBudget(300)
                        )
                )

        );
    }

    @Test
    public void shouldImportFlashPromoDescription() {
        List<ImportResult> importResults = promoBundlesTestHelper.withMock(
                this::prepareData,
                importer::importPromos
        );

        assertThat(importResults, not(empty()));

        Collection<PromoImportResult> results = importResults.get(0).getImportResults();

        assertThat(results, hasSize(2));

        Set<FlashPromoDescription> promoDescriptions = flashPromoDao.select(
                FlashPromoDescription.PROMO_KEY.in(
                        SOME_PROMO_KEY,
                        ANOTHER_PROMO_KEY
                )
        );

        assertThat(promoDescriptions, hasSize(2));
        assertThat(promoDescriptions, hasItems(
                allOf(
                        hasProperty("feedId", is(FEED_ID)),
                        hasProperty("promoKey", is(SOME_PROMO_KEY)),
                        hasProperty("status", is(FlashPromoStatus.ACTIVE))
                ),
                allOf(
                        hasProperty("feedId", is(ANOTHER_FEED_ID)),
                        hasProperty("promoKey", is(ANOTHER_PROMO_KEY)),
                        hasProperty("status", is(FlashPromoStatus.ACTIVE))
                )
        ));

        promoDescriptions.forEach(p -> assertNotNull(promoDao.getPromo(p.getPromoId())));
    }
}
