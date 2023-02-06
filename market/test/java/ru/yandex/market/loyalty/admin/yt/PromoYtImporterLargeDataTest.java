package ru.yandex.market.loyalty.admin.yt;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.GenericBundle;

import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PROMO_KEY;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;

public class PromoYtImporterLargeDataTest extends MarketLoyaltyAdminMockedDbTest {

    private static final int FEED_ID = 1234;
    private static final String SOME_PROMO_KEY = "some promo";

    @Autowired
    private PromoYtImporter importer;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;
    @Autowired
    private PromoBundleDao bundleDao;

    private PromoDetails.Builder typicalDetails;

    @Before
    public void initMocks() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());
        typicalDetails = PromoDetails.newBuilder()
                .setFeedId(FEED_ID)
                .setStartDate(current.toEpochSecond())
                .setEndDate(current.plusDays(1).toEpochSecond())
                .setShopPromoId(SOME_PROMO_KEY)
                .setType(GIFT_WITH_PURCHASE.getReportCode())
                .setGenericBundle(GenericBundle.newBuilder()
                        .addAllBundlesContent(generateBundleRelations(configurationService.genericBundleCapacityMax() / 2))
                        .build());
    }


    @Test
    public void shouldImportPromoBundleDescription() {
        promoBundlesTestHelper.usingMock(
                dataBuilder -> {
                    promoBundlesTestHelper.addNullRecord(dataBuilder);
                    dataBuilder
                            .promo(200344511, "VfwhDOEyPAmssXrSTPRtVA==", typicalDetails.clone());
                },
                importer::importPromos
        );

        PromoBundleDescription description = bundleDao.selectFirst(PROMO_KEY.eqTo("VfwhDOEyPAmssXrSTPRtVA==")).get();

        int sskuSize = configurationService.genericBundleCapacityMax() / 2;

        assertThat(description.getItems(), hasItems(
                allOf(
                        hasProperty("primary", is(true)),
                        hasProperty("condition", allOf(
                                hasProperty("mappings", hasSize(sskuSize))
                        ))
                ),
                allOf(
                        hasProperty("primary", is(false)),
                        hasProperty("condition", allOf(
                                hasProperty("mappings", hasSize(sskuSize))
                        ))
                )
        ));
    }

    private Iterable<? extends GenericBundle.BundleContent> generateBundleRelations(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> GenericBundle.BundleContent.newBuilder()
                        .setPrimaryItem(GenericBundle.PromoItem.newBuilder()
                                .setCount(1)
                                .setOfferId(randomString())
                                .build())
                        .setSecondaryItem(GenericBundle.SecondaryItem.newBuilder()
                                .setItem(GenericBundle.PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId(randomString())
                                        .build())
                                .setDiscountPrice(PromoDetails.Money.newBuilder()
                                        .setCurrency(Currency.RUR.name())
                                        .setValue(5000)
                                        .build())
                                .build())
                        .build())
                .collect(Collectors.toList());
    }
}
