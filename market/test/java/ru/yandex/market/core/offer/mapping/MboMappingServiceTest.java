package ru.yandex.market.core.offer.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.mbi.util.MbiMatchers;

@ParametersAreNonnullByDefault
class MboMappingServiceTest {
    private static final Logger log = LoggerFactory.getLogger(MboMappingServiceTest.class);

    @Test
    void testMappingsByMarketSkuAsMap() throws MboMappingServiceException {
        List<MappedOffer> offers =
                Arrays.asList(
                        new MappedOffer.Builder()
                                .setShopOffer(
                                        new ShopOffer.Builder()
                                                .setSupplierId(661)
                                                .setShopSku("Shop661SKU1")
                                                .setTitle("Test 1")
                                                .build()
                                )
                                .setActiveLink(MarketEntityInfo.marketSku(
                                        MarketSkuInfo.of(12345L, "MarketSku12345",
                                                MarketCategoryInfo.of(123L,
                                                        "Category123"), SkuType.MARKET
                                        ))
                                )
                                .build(),
                        new MappedOffer.Builder()
                                .setShopOffer(
                                        new ShopOffer.Builder()
                                                .setSupplierId(661)
                                                .setShopSku("Shop661SKU2")
                                                .setTitle("Test 2")
                                                .build()
                                )
                                .setActiveLink(MarketEntityInfo.marketSku(
                                        MarketSkuInfo.of(12347L, "MarketSku12347",
                                                MarketCategoryInfo.of(123L, "Category123"), SkuType.MARKET
                                        )))
                                .setPartnerLink(
                                        MarketEntityInfo.marketSku(
                                                MarketSkuInfo.of(12345L, "MarketSku12345",
                                                        MarketCategoryInfo.of(123L,
                                                                "Category123"), SkuType.MARKET
                                                )),
                                        ModerationStatus.MODERATION
                                )
                                .build(),
                        new MappedOffer.Builder()
                                .setShopOffer(
                                        new ShopOffer.Builder()
                                                .setSupplierId(661)
                                                .setShopSku("Shop661SKU3")
                                                .setTitle("Test 3")
                                                .build()
                                )
                                .setPartnerLink(
                                        MarketEntityInfo.marketSku(
                                                MarketSkuInfo.of(12346L, "MarketSku12346",
                                                        MarketCategoryInfo.of(123L,
                                                                "Category123"), SkuType.MARKET
                                                )),
                                        ModerationStatus.MODERATION
                                )
                                .build(),
                        new MappedOffer.Builder()
                                .setShopOffer(
                                        new ShopOffer.Builder()
                                                .setSupplierId(662)
                                                .setShopSku("Shop662SKU1")
                                                .setTitle("Test 1")
                                                .build()
                                )
                                .setActiveLink(MarketEntityInfo.marketSku(
                                        MarketSkuInfo.of(12345L, "MarketSku12345",
                                                MarketCategoryInfo.of(123L,
                                                        "Category123"), SkuType.MARKET
                                        ))
                                ).build());
        MboMappingService testService = Mockito.mock(MboMappingService.class);
        Mockito.when(testService.mappingsByMarketSkuAsMap(Mockito.anyLong(), Mockito.anyCollection()))
                .thenCallRealMethod();
        Mockito.when(testService.mappingsByMarketSku(Mockito.anyCollection()))
                .thenReturn(offers);
        Map<Long, List<ModeratedLink<ShopOffer>>> result =
                testService.mappingsByMarketSkuAsMap(661, Arrays.asList(12345L, 12346L));
        System.out.println(result);
        log.info("Result = {}", result);
        MatcherAssert.assertThat(
                result.entrySet(),
                Matchers.contains(
                        MbiMatchers.isEntry(
                                Matchers.is(12345L),
                                Matchers.contains(
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.ACCEPTED,
                                                MappedOfferMatchers.isShopOffer(661,
                                                        "Shop661SKU1", "Test 1")
                                        ),
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.MODERATION,
                                                MappedOfferMatchers.isShopOffer(661,
                                                        "Shop661SKU2", "Test 2")
                                        )
                                )
                        ),
                        MbiMatchers.isEntry(
                                Matchers.is(12346L),
                                Matchers.contains(
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.MODERATION,
                                                MappedOfferMatchers.isShopOffer(661,
                                                        "Shop661SKU3", "Test 3")
                                        )
                                )
                        )
                )
        );
    }
}
