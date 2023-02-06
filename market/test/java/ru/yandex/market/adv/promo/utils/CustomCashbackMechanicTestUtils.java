package ru.yandex.market.adv.promo.utils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalBrandRestriction;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction;
import Market.DataCamp.DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.market.adv.promo.datacamp.utils.DateTimeUtils;
import ru.yandex.market.adv.promo.datacamp.utils.PromoStorageUtils;

import static Market.DataCamp.DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK;
import static ru.yandex.market.core.util.DateTimes.MOSCOW_TIME_ZONE;

@SuppressWarnings("ParameterNumber")
public final class CustomCashbackMechanicTestUtils {

    private CustomCashbackMechanicTestUtils() { }

    public static SyncGetPromo.GetPromoBatchResponse createCustomCashbackGetResponse() {
        DataCampPromo.PromoDescription promo1 = createPromo(
                "12345_promo",
                1_600_500_000,
                1_600_700_000,
                List.of(1L, 2L, 3L),
                List.of(5L, 6L, 7L),
                123,
                2,
                CreationTab.DYNAMIC_GROUPS,
                false);

        DataCampPromo.PromoDescription promo2 = createPromo(
                "12345_qwe",
                1_600_500_000,
                DateTimeUtils.getDateTimeInSeconds(PromoStorageUtils.TERMLESS_PROMO_DATE_TIME),
                Collections.emptyList(),
                Collections.emptyList(),
                567,
                1,
                CreationTab.FILE,
                true);

        DataCampPromo.PromoDescriptionBatch batch = DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addPromo(promo1)
                .addPromo(promo2)
                .build();

        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(batch)
                .build();
    }

    public static SyncGetPromo.GetPromoBatchResponse createGetResponseForUpdateRequest() {
        DataCampPromo.PromoDescription promo = createPromo(
                "12345_promo",
                1_600_500_000,
                DateTimeUtils.getDateTimeInSeconds(PromoStorageUtils.TERMLESS_PROMO_DATE_TIME),
                Collections.emptyList(),
                Collections.emptyList(),
                123,
                1,
                CreationTab.FILE,
                true);

        DataCampPromo.PromoDescriptionBatch batch = DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addPromo(promo)
                .build();

        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(batch)
                .build();
    }

    public static SyncGetPromo.GetPromoBatchResponse createGetResponseForDeleteRequest() {
        return createGetResponseForUpdateRequest();
    }

    public static SyncGetPromo.DeletePromoBatchResponse createDeletePromoBatchResponse() {
        return SyncGetPromo.DeletePromoBatchResponse.newBuilder()
                .build();
    }

    public static SyncGetPromo.GetPromoBatchResponse createGetResponseForUpdatePrioritiesRequest() {
        return createCustomCashbackGetResponse();
    }

    public static DataCampPromo.PromoDescription createCustomCashbackPromo(
            String promoId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return createCustomCashbackPromo(promoId, startDate, endDate, 10);
    }

    public static DataCampPromo.PromoDescription createCustomCashbackPromo(
            String promoId,
            LocalDate startDate,
            LocalDate endDate,
            Integer cashbackValue
    ) {
        return createPromo(
                promoId,
                startDate.atStartOfDay().atZone(MOSCOW_TIME_ZONE).toEpochSecond(),
                endDate.atTime(23, 59, 59).atZone(MOSCOW_TIME_ZONE).toEpochSecond(),
                null, null,
                cashbackValue, 3, CreationTab.FILE, true
        );
    }

    private static DataCampPromo.PromoDescription createPromo(
            String promoId,
            long startDate,
            long endDate,
            List<Long> categoryIds,
            List<Long> brandIds,
            Integer cashbackValue,
            int promoPriority,
            CreationTab creationTab,
            boolean enabled
    ) {
        DataCampPromo.PromoConstraints.OffersMatchingRule.Builder offersMatchingRuleBuilder =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder();

        if (CollectionUtils.isNotEmpty(categoryIds)) {
            offersMatchingRuleBuilder.setOrigionalCategoryRestriction(
                    createOriginalCategoryRestriction(categoryIds)
            );
        }
        if (CollectionUtils.isNotEmpty(brandIds)) {
            offersMatchingRuleBuilder.setOriginalBrandRestriction(
                    createOriginalBrandRestriction(brandIds)
            );
        }

        DataCampPromo.PromoConstraints promoConstraints = DataCampPromo.PromoConstraints.newBuilder()
                .setStartDate(startDate)
                .setEndDate(endDate)
                .addOffersMatchingRules(offersMatchingRuleBuilder.build())
                .setEnabled(enabled)
                .build();

        DataCampPromo.PromoMechanics.PartnerCustomCashback customCashback =
                DataCampPromo.PromoMechanics.PartnerCustomCashback.newBuilder()
                        .setMarketTariffsVersionId(0)
                        .setCashbackValue(cashbackValue)
                        .setPriority(promoPriority)
                        .setSource(creationTab)
                        .build();

        DataCampPromo.PromoMechanics mechanics = DataCampPromo.PromoMechanics.newBuilder()
                .setPartnerCustomCashback(customCashback)
                .build();

        DataCampPromo.PromoAdditionalInfo additionalInfo = DataCampPromo.PromoAdditionalInfo.newBuilder()
                .setName(promoId + "_" + promoPriority)
                .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(PARTNER_CUSTOM_CASHBACK)
                )
                .setConstraints(promoConstraints)
                .setMechanicsData(mechanics)
                .setAdditionalInfo(additionalInfo)
                .build();
    }

     public static OriginalCategoryRestriction createOriginalCategoryRestriction(
            List<Long> categoryIds
    ) {
        OriginalCategoryRestriction.Builder originalCategoryRestrictionBuilder =
                DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder();
        categoryIds.forEach(categoryId ->
                originalCategoryRestrictionBuilder.addIncludeCategegoryRestriction(
                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                .setId(categoryId)
                                .build()
                )
        );
        return originalCategoryRestrictionBuilder.build();
    }

    private static OriginalBrandRestriction createOriginalBrandRestriction(
            List<Long> brandIds
    ) {
        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.Builder promoBrandsBuilder =
                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.newBuilder();
        brandIds.forEach(brandId ->
                promoBrandsBuilder.addBrands(
                        DataCampPromo.PromoBrand.newBuilder()
                                .setId(brandId)
                                .build()
                )
        );
        return DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalBrandRestriction.newBuilder()
                .setIncludeBrands(promoBrandsBuilder)
                .build();
    }

    public static SyncGetPromo.GetPromoBatchResponse createResponseForPriorityRequest(int priority) {

        DataCampPromo.PromoDescription promo = DataCampPromo.PromoDescription.newBuilder()
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setPartnerCustomCashback(
                                        DataCampPromo.PromoMechanics.PartnerCustomCashback.newBuilder()
                                                .setPriority(priority)
                                                .build()
                                )
                                .build()
                )
                .build();


        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(
                        DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(promo)
                                .build()
                )
                .build();
    }
}
