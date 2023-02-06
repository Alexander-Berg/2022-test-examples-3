package ru.yandex.market.global.index.domain.enrichment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import util.RandomDataGenerator;

import ru.yandex.market.global.common.datacamp.DataCampClient;
import ru.yandex.market.global.index.BaseFunctionalTest;
import ru.yandex.market.global.index.domain.dixtionary.MarketCategoriesDictionary;
import ru.yandex.mj.generated.client.partner.api.ShopExportApiClient;
import ru.yandex.mj.generated.client.partner.model.ListShopsExportResponseDto;
import ru.yandex.mj.generated.client.partner.model.ShopExportDto;
import ru.yandex.mj.generated.server.model.CategoryEnrichmentCacheDto;
import ru.yandex.mj.generated.server.model.MarketCategoryDto;
import ru.yandex.mj.generated.server.model.ShopEnrichmentCacheDto;
import ru.yandex.mj.generated.server.model.ShopMarketCategoryDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EnrichmentCalculatorTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(EnrichmentCalculatorTest.class).build();

    private static final long SHOP_ID_1 = 1L;
    private static final long BUSINESS_ID_1 = 11L;
    private static final long SHOP_ID_2 = 2L;
    private static final long BUSINESS_ID_2 = 22L;
    private static final long SHOP_ID_3 = 3L;
    private static final long BUSINESS_ID_3 = 33L;

    private static final long CAT_ERR = 9999;

    private static final long CAT_1 = 10;
    private static final long CAT_1_1 = 1010;
    private static final long CAT_1_2 = 1020;

    private static final long CAT_2 = 20;
    private static final long CAT_2_1 = 2010;
    private static final long CAT_2_2 = 2020;
    private static final int ROOT = 0;

    private final ShopExportApiClient shopExportApiClient;
    private final DataCampClient dataCampClient;
    private final MarketCategoriesDictionary marketCategoriesDictionary;

    private final EnrichmentCalculator enrichmentCalculator;

    @BeforeEach
    public void setup() {
        mockShops();
        mockCategories();
        mockOffers(BUSINESS_ID_1, SHOP_ID_1);
        mockOffers(BUSINESS_ID_2, SHOP_ID_2);
    }

    @Test
    public void testCalculate() {
        mockOffers(BUSINESS_ID_1, SHOP_ID_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_2,
                CAT_1_2,
                CAT_1_2,
                CAT_1_2,
                CAT_1_2,
                CAT_1_2,
                CAT_1_2,
                CAT_1_2,
                CAT_1_2,
                CAT_1_2,
                CAT_2_1
        );
        mockOffers(BUSINESS_ID_3, SHOP_ID_3,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1,
                CAT_1_1
        );

        mockOffers(BUSINESS_ID_2, SHOP_ID_2, CAT_2_1);

        EnrichmentContext context = enrichmentCalculator.calculate();

        Assertions.assertThat(context)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreCollectionOrder(true)
                        .build()
                )
                .isEqualTo(new EnrichmentContext()
                        .setShopCache(Map.of(
                                SHOP_ID_1, new ShopEnrichmentCacheDto().shopId(SHOP_ID_1)
                                        .marketCategories(List.of(
                                                new ShopMarketCategoryDto()
                                                        .id(CAT_1)
                                                        .code("cat_" + CAT_1)
                                                        .sortOrder(1)
                                                        .sortOrderForCustomer(19),
                                                new ShopMarketCategoryDto()
                                                        .id(CAT_1_1)
                                                        .code("cat_" + CAT_1_1)
                                                        .sortOrder(1)
                                                        .sortOrderForCustomer(4),
                                                new ShopMarketCategoryDto()
                                                        .id(CAT_1_2)
                                                        .code("cat_" + CAT_1_2)
                                                        .sortOrder(1)
                                                        .sortOrderForCustomer(4),
                                                new ShopMarketCategoryDto()
                                                        .id(CAT_2_1)
                                                        .code("cat_" + CAT_2_1)
                                                        .sortOrder(1)
                                                        .sortOrderForCustomer(0)
                                        )),
                                SHOP_ID_2, new ShopEnrichmentCacheDto().shopId(SHOP_ID_2)
                                        .marketCategories(List.of(
                                                new ShopMarketCategoryDto()
                                                        .id(CAT_2_1)
                                                        .code("cat_" + CAT_2_1)
                                                        .sortOrder(1)
                                                        .sortOrderForCustomer(1)
                                        ))
                        ))
                        .setCategoryCache(Map.of(
                                CAT_1, new CategoryEnrichmentCacheDto()
                                        .categoryId(CAT_1)
                                        .categoryCode("cat_" + CAT_1)
                                        .offersCount(20)
                                        .visibleForCustomer(true)
                                        .sortOrderForCustomer(20),
                                CAT_1_1, new CategoryEnrichmentCacheDto()
                                        .categoryId(CAT_1_1)
                                        .categoryCode("cat_" + CAT_1_1)
                                        .offersCount(10)
                                        .visibleForCustomer(true)
                                        .sortOrderForCustomer(10),
                                CAT_1_2, new CategoryEnrichmentCacheDto()
                                        .categoryId(CAT_1_2)
                                        .categoryCode("cat_" + CAT_1_2)
                                        .offersCount(10)
                                        .visibleForCustomer(true)
                                        .sortOrderForCustomer(10),
                                CAT_2, new CategoryEnrichmentCacheDto()
                                        .categoryId(CAT_2)
                                        .categoryCode("cat_" + CAT_2)
                                        .offersCount(2)
                                        .visibleForCustomer(false)
                                        .sortOrderForCustomer(1000),
                                CAT_2_1, new CategoryEnrichmentCacheDto()
                                        .categoryId(CAT_2_1)
                                        .categoryCode("cat_" + CAT_2_1)
                                        .offersCount(2)
                                        .visibleForCustomer(true)
                                        .sortOrderForCustomer(2)
                                )
                        )
                );
    }

    @Test
    public void testSkipNotExistingCategory() {
        mockOffers(BUSINESS_ID_1, SHOP_ID_1, CAT_ERR);
        mockOffers(BUSINESS_ID_2, SHOP_ID_2, CAT_ERR);

        EnrichmentContext context = enrichmentCalculator.calculate();

        Assertions.assertThat(context)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreCollectionOrder(true)
                        .build()
                )
                .isEqualTo(new EnrichmentContext());
    }

    private void mockCategories() {
        mockCategory(CAT_1, ROOT, null, null);
        mockCategory(CAT_1_1, CAT_1, null, null);
        mockCategory(CAT_1_2, CAT_1, null, null);
        mockCategory(CAT_2, ROOT, false, 1000);
        mockCategory(CAT_2_1, CAT_2, null, null);
        mockCategory(CAT_2_2, CAT_2, null, null);
        mockNoCategory(CAT_ERR);
    }

    private void mockCategory(
            long marketCategoryId, long parentCategoryId,
            Boolean fixedVisibleForCustomer, Integer fixedSortOrderForCustomer
    ) {
        String code = marketCategoryId == ROOT ? "root" : "cat_" + marketCategoryId;
        String parentCode = parentCategoryId == ROOT ? "root" : "cat_" + parentCategoryId;
        Mockito.when(marketCategoriesDictionary.get(
                Mockito.eq(String.valueOf(marketCategoryId))
        )).thenReturn(RANDOM.nextObject(MarketCategoryDto.class)
                .id(marketCategoryId)
                .code(code)
                .parentId(parentCategoryId)
                .parentCode(parentCode)
                .fixedSortOrderForCustomer(fixedSortOrderForCustomer)
                .fixedVisibleForCustomer(fixedVisibleForCustomer)
        );
    }

    private void mockNoCategory(long marketCategoryId) {
        Mockito.when(marketCategoriesDictionary.get(
                Mockito.eq(String.valueOf(marketCategoryId))
        )).thenReturn(null);
    }

    private void mockOffers(long businessId, long shopId, Long... categories) {
        SyncGetOffer.GetUnitedOffersResponse.Builder builder = SyncGetOffer.GetUnitedOffersResponse.newBuilder();

        Arrays.stream(categories).forEach(
                c -> builder.addOffers(
                        createOffer((int) businessId, (int) shopId, "OFFER_" + c, c)
                )
        );

        Mockito.when(dataCampClient.getOffersBatch(
                Mockito.eq(businessId), Mockito.eq(shopId), Mockito.isNull(), Mockito.anyInt(), Mockito.anyBoolean()
        )).thenReturn(
                builder.build()
        );

        SyncGetOffer.GetUnitedOffersResponse empty = SyncGetOffer.GetUnitedOffersResponse.newBuilder().build();
        Mockito.when(dataCampClient.getOffersBatch(
                Mockito.eq(businessId), Mockito.eq(shopId), Mockito.isNotNull(), Mockito.anyInt(), Mockito.anyBoolean()
        )).thenReturn(
                empty
        );
    }

    @NotNull
    private DataCampUnitedOffer.UnitedOffer createOffer(int businessId, int shopId, String offerId, Long categryId) {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId(offerId)
                                .setBusinessId(businessId)
                                .setShopId(shopId)
                                .build()
                        )
                        .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                                .setOfferParams(DataCampOfferContent.ProductYmlParams.newBuilder()
                                                        .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                                                .setName("marketCategoryId")
                                                                .setValue(categryId.toString())
                                                                .build())
                                                )
                                        )
                                        .build()
                                )
                                .build()
                        )
                )
                .putService(shopId, DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setBusinessId(businessId)
                                .setShopId(shopId)
                                .setOfferId(offerId)
                                .buildPartial()
                        )
                        .build()
                )
                .build();
    }

    private void mockShops() {
        Mockito.when(shopExportApiClient.apiV1ShopExportAllGet().schedule().join())
                .thenReturn(
                        new ListShopsExportResponseDto().items(List.of(
                                new ShopExportDto()
                                        .id(SHOP_ID_1)
                                        .businessId(BUSINESS_ID_1)
                                        .hidden(false),
                                new ShopExportDto()
                                        .id(SHOP_ID_2)
                                        .businessId(BUSINESS_ID_2)
                                        .hidden(false)
                                ,
                                new ShopExportDto()
                                        .id(SHOP_ID_3)
                                        .businessId(BUSINESS_ID_3)
                                        .hidden(true)
                                )
                        )
                );
    }
}
