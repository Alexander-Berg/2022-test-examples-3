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
import Market.Promo.Promo.PromoDetails.GenericBundle.BundleContent;
import Market.Promo.Promo.PromoDetails.GenericBundle.PromoItem;
import Market.Promo.Promo.PromoDetails.GenericBundle.SecondaryItem;

import java.util.Set;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.in;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.SHOP_PROMO_ID;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;

public class PromoYtImporterEmptyDataTest extends MarketLoyaltyAdminMockedDbTest {

    private static long START_DATE = 1566680400;
    private static long END_DATE = 1577825999;

    @Autowired
    private PromoYtImporter importer;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;
    @Autowired
    private PromoBundleDao bundleDao;

    private PromoDetails.Builder typicalDetails;

    @Before
    public void initMocks() {
        typicalDetails = PromoDetails.newBuilder()
                .setStartDate(START_DATE)
                .setEndDate(END_DATE)
                .setShopPromoId("altuninf_test4")
                .setType(GIFT_WITH_PURCHASE.getReportCode())
                .setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                        .addBundlesContent(BundleContent.newBuilder()
                                .setPrimaryItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00026.100126175417")
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126175475")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(5000)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(BundleContent.newBuilder()
                                .setPrimaryItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00026.100126175459")
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126175475")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(5000)
                                                .build())
                                        .build())
                                .build())
                        .addBundlesContent(BundleContent.newBuilder()
                                .setPrimaryItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00026.100126175777")
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126175475")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(5000)
                                                .build())
                                        .build())
                                .build())
                        .build());
    }

    @Test
    public void shouldImportPromoBundleDescription() {
        promoBundlesTestHelper.usingMock(
                dataBuilder -> {
                    promoBundlesTestHelper.addNullRecord(dataBuilder);
                    dataBuilder
                            .promo(200344511, "VfwhDOEyPAmssXrSTPRtVA==", typicalDetails.clone())
                            .promo(200396943, "3fJfW1AHHtwlnkkVzPHBKA==", typicalDetails.clone())
                            .promo(200396944, "2I2twvNOdzohJiNFSQN11g==", typicalDetails.clone())
                            .promo(200369699, "Mx1eU11lHxZmxeI9SvP1Lg==", typicalDetails.clone());
                },
                importer::importPromos
        );

        typicalDetails.setShopPromoId("altuninf_test4")
                .setGenericBundle(PromoDetails.GenericBundle.newBuilder()
                        .addBundlesContent(BundleContent.newBuilder()
                                .setPrimaryItem(PromoItem.newBuilder()
                                        .setCount(1)
                                        .setOfferId("00065.00026.100126175417")
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126177079")
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
                                        .setOfferId("00065.00026.100126175459")
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126177079")
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
                                        .setOfferId("00065.00026.100126175777")
                                        .build())
                                .setSecondaryItem(SecondaryItem.newBuilder()
                                        .setItem(PromoItem.newBuilder()
                                                .setCount(1)
                                                .setOfferId("00065.00026.100126177079")
                                                .build())
                                        .setDiscountPrice(PromoDetails.Money.newBuilder()
                                                .setCurrency(Currency.RUR.name())
                                                .setValue(100)
                                                .build())
                                        .build())
                                .build())
                        .build());

        promoBundlesTestHelper.usingMock(dataBuilder -> {
            promoBundlesTestHelper.addNullRecord(dataBuilder);

            dataBuilder.promo(200344511, "PP8uzJankfi0gckoQuQFPA==",
                    typicalDetails.clone());

            dataBuilder.promo(200396943, "2gNuzeFkzVuH71549+DiKg==",
                    typicalDetails.clone());

            dataBuilder.promo(200396944, "yCvkct5NyJRsJEYMIGI5ow==",
                    typicalDetails.clone());

            dataBuilder.promo(200369699, "eQAs6x0xDGx76AB5QeE0qQ==",
                    typicalDetails.clone());
        }, importer::importPromos);

        Set<PromoBundleDescription> result = bundleDao.select(SHOP_PROMO_ID.eqTo("altuninf_test4"));

        String[] primarySkus = new String[]
                {"00065.00026.100126175417", "00065.00026.100126175459", "00065.00026.100126175777"};

        String secondarySku = "00065.00026.100126177079";

        assertThat(result, hasItems(
                allOf(
                        hasProperty("feedId", is(200344511L)),
                        hasProperty("promoKey", is("PP8uzJankfi0gckoQuQFPA==")),
                        hasProperty("items", hasItems(
                                hasProperty("condition",
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", in(primarySkus))
                                        ))
                                ),
                                hasProperty("condition",
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", is(secondarySku))
                                        ))
                                )
                        ))
                ),
                allOf(
                        hasProperty("feedId", is(200396943L)),
                        hasProperty("promoKey", is("2gNuzeFkzVuH71549+DiKg==")),
                        hasProperty("items", hasItems(
                                hasProperty("condition",
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", in(primarySkus))
                                        ))
                                ),
                                hasProperty("condition",
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", is(secondarySku))
                                        ))
                                )
                        ))
                ),
                allOf(
                        hasProperty("feedId", is(200396944L)),
                        hasProperty("promoKey", is("yCvkct5NyJRsJEYMIGI5ow==")),
                        hasProperty("items", hasItems(
                                hasProperty("condition",
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", in(primarySkus))
                                        ))
                                ),
                                hasProperty("condition",
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", is(secondarySku))
                                        ))
                                )
                        ))
                ),
                allOf(
                        hasProperty("feedId", is(200369699L)),
                        hasProperty("promoKey", is("eQAs6x0xDGx76AB5QeE0qQ==")),
                        hasProperty("items", hasItems(
                                hasProperty("condition",
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", in(primarySkus))
                                        ))
                                ),
                                hasProperty("condition",
                                        hasProperty("mappings", hasItem(
                                                hasProperty("ssku", is(secondarySku))
                                        ))
                                )
                        ))
                )
        ));
    }
}
