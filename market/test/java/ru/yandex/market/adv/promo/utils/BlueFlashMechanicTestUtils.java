package ru.yandex.market.adv.promo.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import Market.DataCamp.DataCampPromo;
import NMarket.Common.Promo.Promo;

import static ru.yandex.market.adv.promo.datacamp.utils.DateTimeUtils.getDateTimeInSeconds;
import static ru.yandex.market.adv.promo.utils.PromoMechanicTestUtils.createWarehouseRestriction;

/**
 * Класс для вспомогательных метов для работы с промо с механикой "Флеш акция" в тестах.
 */
public final class BlueFlashMechanicTestUtils {

    private BlueFlashMechanicTestUtils() { }

    public static DataCampPromo.PromoDescription createAnaplanBlueFlashDescription(
            String promoId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return createAnaplanBlueFlashDescription(
                0, promoId, 145, "Test name",
                getDateTimeInSeconds(startDate.atStartOfDay()),
                getDateTimeInSeconds(endDate.atTime(23, 59, 59))
        );
    }

    public static DataCampPromo.PromoDescription createAnaplanBlueFlashDescription(
            long businessId,
            String promoId,
            Integer warehouseId,
            String promoName
    ) {
        return createAnaplanBlueFlashDescription(
                businessId, promoId, warehouseId, promoName,
                getDateTimeInSeconds(LocalDate.now().minus(10, ChronoUnit.DAYS).atStartOfDay()),
                getDateTimeInSeconds(LocalDate.now().plus(10, ChronoUnit.DAYS).atTime(23, 59, 59))
        );
    }

    /**
     * Создание [почти полностью] честного описания маркетплейсной флеш акции в формате АХ.
     */
    public static DataCampPromo.PromoDescription createAnaplanBlueFlashDescription(
            long businessId,
            String promoId,
            Integer warehouseId,
            String promoName,
            long startTime,
            long endTime
    ) {
        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule = warehouseId == null ?
                DataCampPromo.PromoConstraints.OffersMatchingRule.getDefaultInstance() :
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setWarehouseRestriction(createWarehouseRestriction(warehouseId))
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setEnabled(true)
                        .setStartDate(startTime)
                        .setEndDate(endTime)
                        .build();

        DataCampPromo.PromoAdditionalInfo promoAdditionalInfo =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(promoName)
                        .setSendPromoPi(true)
                        .setStrategyType(DataCampPromo.PromoAdditionalInfo.StrategyType.CROSS_CATEGORY)
                        .setPublishDatePi(1577829600) // 2020 год
                        .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(Math.toIntExact(businessId))
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.BLUE_FLASH)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfo)
                .build();
    }
}
