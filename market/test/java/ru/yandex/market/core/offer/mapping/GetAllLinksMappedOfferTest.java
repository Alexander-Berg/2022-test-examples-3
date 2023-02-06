package ru.yandex.market.core.offer.mapping;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@ParametersAreNonnullByDefault
class GetAllLinksMappedOfferTest {
    @Test
    void testGetAllLinksOnEmpty() {
        MappedOffer mappedOffer = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(661)
                                .setShopSku("shopSKU1")
                                .setTitle("TestShopSKU1")
                                .build()
                )
                .build();
        MatcherAssert.assertThat(mappedOffer.getAllLinks(), Matchers.hasSize(0));
    }

    @Test
    void testGetAllLinksActiveOnly() {
        MappedOffer mappedOffer = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(661)
                                .setShopSku("shopSKU1")
                                .setTitle("TestShopSKU1")
                                .build()
                )
                .setActiveLink(MarketEntityInfo.marketSku(
                        MarketSkuInfo.of(123L, "MarketSku123",
                                MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                ))
                .build();
        MatcherAssert.assertThat(
                mappedOffer.getAllLinks(),
                Matchers.contains(
                        MappedOfferMatchers.isModeratedLink(
                                ModerationStatus.ACCEPTED,
                                MappedOfferMatchers.isMarketSku(123)
                        )
                )
        );
    }

    @Test
    void testGetAllLinksPartnerOnly() {
        MappedOffer mappedOffer = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(661)
                                .setShopSku("shopSKU1")
                                .setTitle("TestShopSKU1")
                                .build()
                )
                .setPartnerLink(
                        MarketEntityInfo.marketSku(
                                MarketSkuInfo.of(124L, "MarketSku124",
                                        MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                        ),
                        ModerationStatus.MODERATION
                )
                .build();
        MatcherAssert.assertThat(
                mappedOffer.getAllLinks(),
                Matchers.contains(
                        MappedOfferMatchers.isModeratedLink(
                                ModerationStatus.MODERATION,
                                MappedOfferMatchers.isMarketSku(124)
                        )
                )
        );
    }

    @Test
    void testGetAllLinksActiveAndModerated() {
        MappedOffer mappedOffer = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(661)
                                .setShopSku("shopSKU1")
                                .setTitle("TestShopSKU1")
                                .build()
                )
                .setPartnerLink(
                        MarketEntityInfo.marketSku(
                                MarketSkuInfo.of(124L, "MarketSku124",
                                        MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                        ),
                        ModerationStatus.MODERATION
                )
                .setActiveLink(MarketEntityInfo.marketSku(
                        MarketSkuInfo.of(123L, "MarketSku123",
                                MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                ))
                .build();
        MatcherAssert.assertThat(
                mappedOffer.getAllLinks(),
                Matchers.containsInAnyOrder(
                        MappedOfferMatchers.isModeratedLink(
                                ModerationStatus.ACCEPTED,
                                MappedOfferMatchers.isMarketSku(123)
                        ),
                        MappedOfferMatchers.isModeratedLink(
                                ModerationStatus.MODERATION,
                                MappedOfferMatchers.isMarketSku(124)
                        )
                )
        );
    }

    @Test
    void testGetAllLinksActiveByPartner() {
        MappedOffer mappedOffer = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(661)
                                .setShopSku("shopSKU1")
                                .setTitle("TestShopSKU1")
                                .build()
                )
                .setPartnerLink(
                        MarketEntityInfo.marketSku(
                                MarketSkuInfo.of(123L, "MarketSku123",
                                        MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                        ),
                        ModerationStatus.ACCEPTED
                )
                .setActiveLink(MarketEntityInfo.marketSku(
                        MarketSkuInfo.of(123L, "MarketSku123",
                                MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                ))
                .build();
        MatcherAssert.assertThat(
                mappedOffer.getAllLinks(),
                Matchers.contains(
                        MappedOfferMatchers.isModeratedLink(
                                ModerationStatus.ACCEPTED,
                                MappedOfferMatchers.isMarketSku(123)
                        )
                )
        );
    }

    @Test
    void testGetAllLinksIgnoreAcceptedConflict() {
        MappedOffer mappedOffer = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(661)
                                .setShopSku("shopSKU1")
                                .setTitle("TestShopSKU1")
                                .build()
                )
                .setPartnerLink(
                        MarketEntityInfo.marketSku(
                                MarketSkuInfo.of(123L, "MarketSku123",
                                        MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                        ),
                        ModerationStatus.ACCEPTED
                )
                .setActiveLink(MarketEntityInfo.marketSku(
                        MarketSkuInfo.of(124L, "MarketSku124",
                                MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                ))
                .build();
        MatcherAssert.assertThat(
                mappedOffer.getAllLinks(),
                Matchers.contains(
                        MappedOfferMatchers.isModeratedLink(
                                ModerationStatus.ACCEPTED,
                                MappedOfferMatchers.isMarketSku(124)
                        )
                )
        );
    }

    @Test
    void testGetAllLinksConfilctingAcceptedLinkExistence() {
        MappedOffer mappedOffer = new MappedOffer.Builder()
                .setShopOffer(
                        new ShopOffer.Builder()
                                .setSupplierId(661)
                                .setShopSku("shopSKU1")
                                .setTitle("TestShopSKU1")
                                .build()
                )
                .setPartnerLink(
                        MarketEntityInfo.marketSku(
                                MarketSkuInfo.of(123L, "MarketSku123",
                                        MarketCategoryInfo.of(12L, "Category12"), SkuType.MARKET)
                        ),
                        ModerationStatus.ACCEPTED
                )
                .build();
        MatcherAssert.assertThat(
                mappedOffer.getAllLinks(),
                Matchers.contains(
                        MappedOfferMatchers.isModeratedLink(
                                ModerationStatus.ACCEPTED,
                                MappedOfferMatchers.isMarketSku(123)
                        )
                )
        );
    }
}
