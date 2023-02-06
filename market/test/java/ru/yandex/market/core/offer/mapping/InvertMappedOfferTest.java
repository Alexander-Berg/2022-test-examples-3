package ru.yandex.market.core.offer.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.util.MbiMatchers;

@ParametersAreNonnullByDefault
class InvertMappedOfferTest {

    private static final String SHOP_SKU = "shopSKU1";
    private static final String TEST_SHOP_SKU = "TestShopSKU1";
    private static final long SUPPLIER_ID = 661L;
    private static final long MARKET_SKU_ID_1 = 123L;
    private static final long MARKET_SKU_ID_2 = 124L;
    private static final long CATEGORY_ID = 12L;
    private static final String CATEGORY_NAME = "Category12";
    private static final String SKU_NAME_1 = "MarketSku123";
    private static final String SKU_NAME_2 = "MarketSku124";

    @Test
    void testInvertOnEmpty() {
        Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(SUPPLIER_ID)
                                .setShopSku(SHOP_SKU)
                                .setTitle(TEST_SHOP_SKU)
                                .build()
                )
                .build()
                .toMapByMarketSku();
        MatcherAssert.assertThat(mapByMarketSku, MbiMatchers.satisfy(Map::isEmpty));
    }

    @Test
    void testInvertActiveOnly() {
        Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku = createMapByMarketSku(MARKET_SKU_ID_1, SKU_NAME_1);

        assertMappedOffer(mapByMarketSku, 1, Collections.singletonList(MARKET_SKU_ID_1), Collections.singletonList(SUPPLIER_ID), Collections.singletonList(ModerationStatus.ACCEPTED));
    }

    @Test
    void testInvertPartnerOnly() {
        Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku = createMapByMarketSku(MARKET_SKU_ID_2, SKU_NAME_2, ModerationStatus.MODERATION);

        assertMappedOffer(mapByMarketSku, 1, Collections.singletonList(MARKET_SKU_ID_2), Collections.singletonList(SUPPLIER_ID), Collections.singletonList(ModerationStatus.MODERATION));
    }

    @Test
    void testInvertActiveAndModerated() {
        Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku = createMapByMarketSku(MARKET_SKU_ID_2, SKU_NAME_2, ModerationStatus.MODERATION, MARKET_SKU_ID_1, SKU_NAME_1);

        assertMappedOffer(mapByMarketSku, 2, Arrays.asList(MARKET_SKU_ID_1, MARKET_SKU_ID_2), Arrays.asList(SUPPLIER_ID, SUPPLIER_ID), Arrays.asList(ModerationStatus.ACCEPTED, ModerationStatus.MODERATION));
    }

    @Test
    void testInvertActiveByPartner() {
        Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku = createMapByMarketSku(MARKET_SKU_ID_1, SKU_NAME_1, ModerationStatus.ACCEPTED, MARKET_SKU_ID_1, SKU_NAME_1);

        assertMappedOffer(mapByMarketSku, 1, Collections.singletonList(MARKET_SKU_ID_1), Collections.singletonList(SUPPLIER_ID), Collections.singletonList(ModerationStatus.ACCEPTED));
    }

    @Test
    void testInvertIgnoreAcceptedConflict() {
        Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku = createMapByMarketSku(MARKET_SKU_ID_1, SKU_NAME_1, ModerationStatus.ACCEPTED, MARKET_SKU_ID_2, SKU_NAME_2);

        assertMappedOffer(mapByMarketSku, 1, Collections.singletonList(MARKET_SKU_ID_2), Collections.singletonList(SUPPLIER_ID), Collections.singletonList(ModerationStatus.ACCEPTED));
    }

    private Map<Long, ModeratedLink<ShopOffer>> createMapByMarketSku(
            long partnerLinkMSkuId, String partnerLinkSkuName, ModerationStatus partnerLinkStatus,
            long activeLinkMSkuId, String activeLinkSkuName
    ) {
        return new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(SUPPLIER_ID)
                                .setShopSku(SHOP_SKU)
                                .setTitle(TEST_SHOP_SKU)
                                .build()
                )
                .setPartnerLink(MarketEntityInfo.marketSku(
                        MarketSkuInfo.of(partnerLinkMSkuId, partnerLinkSkuName,
                                MarketCategoryInfo.of(CATEGORY_ID, CATEGORY_NAME), null)
                        ),
                        partnerLinkStatus
                )
                .setActiveLink(MarketEntityInfo.marketSku(
                        MarketSkuInfo.of(activeLinkMSkuId, activeLinkSkuName,
                                MarketCategoryInfo.of(CATEGORY_ID, CATEGORY_NAME), null)
                        )
                )
                .build()
                .toMapByMarketSku();
    }

    @Test
    void testInvertConflictingAcceptedLinkExistence() {
        Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(SUPPLIER_ID)
                                .setShopSku(SHOP_SKU)
                                .setTitle(TEST_SHOP_SKU)
                                .build()
                )
                .setPartnerLink(
                        MarketEntityInfo.marketSku(
                                MarketSkuInfo.of(MARKET_SKU_ID_1, SKU_NAME_1,
                                        MarketCategoryInfo.of(CATEGORY_ID, CATEGORY_NAME), null)
                        ),
                        ModerationStatus.ACCEPTED
                )
                .build()
                .toMapByMarketSku();

        assertMappedOffer(mapByMarketSku, 1, Collections.singletonList(MARKET_SKU_ID_1), Collections.singletonList(SUPPLIER_ID), Collections.singletonList(ModerationStatus.ACCEPTED));
    }

    @Test
    void testInvertSameOffers() {
        MarketEntityInfo info = MarketEntityInfo.marketSku(
                MarketSkuInfo.of(MARKET_SKU_ID_1, SKU_NAME_1,
                        MarketCategoryInfo.of(CATEGORY_ID, CATEGORY_NAME), null)
        );

        ModeratedLink<MarketEntityInfo> link = ModeratedLink.of(info, ModerationStatus.MODERATION);

        Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(SUPPLIER_ID)
                                .setShopSku(SHOP_SKU)
                                .setTitle(TEST_SHOP_SKU)
                                .build()
                )
                .setPartnerLink(link)
                .setActiveLink(info)
                .build()
                .toMapByMarketSku();

        Assertions.assertEquals(1, mapByMarketSku.size());

        assertMappedOffer(mapByMarketSku, 1, Collections.singletonList(MARKET_SKU_ID_1), Collections.singletonList(SUPPLIER_ID), Collections.singletonList(ModerationStatus.ACCEPTED));
    }

    private Map<Long, ModeratedLink<ShopOffer>> createMapByMarketSku(long activeLinkMSkuId, String activeLinkSkuName) {
        return new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(SUPPLIER_ID)
                                .setShopSku(SHOP_SKU)
                                .setTitle(TEST_SHOP_SKU)
                                .build()
                )
                .setActiveLink(MarketEntityInfo.marketSku(
                        MarketSkuInfo.of(
                                activeLinkMSkuId,
                                activeLinkSkuName,
                                MarketCategoryInfo.of(CATEGORY_ID, CATEGORY_NAME),
                                null
                        )))
                .build()
                .toMapByMarketSku();
    }

    private Map<Long, ModeratedLink<ShopOffer>> createMapByMarketSku(long partnerLinkMSkuId, String partnerLinkSkuName, ModerationStatus partnerLinkStatus) {
        return new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(SUPPLIER_ID)
                                .setShopSku(SHOP_SKU)
                                .setTitle(TEST_SHOP_SKU)
                                .build()
                )
                .setPartnerLink(MarketEntityInfo.marketSku(
                        MarketSkuInfo.of(
                                partnerLinkMSkuId,
                                partnerLinkSkuName,
                                MarketCategoryInfo.of(CATEGORY_ID, CATEGORY_NAME),
                                null
                        )),
                        partnerLinkStatus
                )
                .build()
                .toMapByMarketSku();
    }

    private void assertMappedOffer(
            Map<Long, ModeratedLink<ShopOffer>> mapByMarketSku,
            int size,
            List<Long> targetIds,
            List<Long> shopIds,
            List<ModerationStatus> statuses
    ) {
        List<Matcher<Map.Entry<Long, ModeratedLink<ShopOffer>>>> matchers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Matcher<Map.Entry<Long, ModeratedLink<ShopOffer>>> matcher = MbiMatchers.isEntry(
                    Matchers.is(targetIds.get(i)),
                    MappedOfferMatchers.isModeratedLink(
                            statuses.get(i),
                            MappedOfferMatchers.isShopOffer(shopIds.get(i), SHOP_SKU, TEST_SHOP_SKU)
                    )
            );
            matchers.add(matcher);
        }

        Assertions.assertEquals(size, mapByMarketSku.size());

        MatcherAssert.assertThat(mapByMarketSku.entrySet(), Matchers.containsInAnyOrder(matchers.toArray(new Matcher[matchers.size()])));
    }
}
