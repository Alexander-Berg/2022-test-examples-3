package ru.yandex.market.adv.promo.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;

import Market.DataCamp.DataCampPromo;
import NMarket.Common.Promo.Promo;

import static ru.yandex.market.adv.promo.datacamp.utils.DateTimeUtils.getDateTimeInSeconds;
import static ru.yandex.market.adv.promo.utils.PromoMechanicTestUtils.createWarehouseRestriction;

/**
 * Класс для вспомогательных метов для работы с промо с механикой "Самый дешевый в подарок" в тестах.
 */
@SuppressWarnings("ParameterNumber")
public final class CheapestAsGiftMechanicTestUtils {

    private CheapestAsGiftMechanicTestUtils() { }

    public static DataCampPromo.PromoDescription createPartnerCheapestAsGiftDescription(String promoId) {
        return createPartnerCheapestAsGiftDescription(
                promoId,
                LocalDate.now().minus(10, ChronoUnit.DAYS),
                LocalDate.now().minus(10, ChronoUnit.DAYS)
        );
    }

    public static DataCampPromo.PromoDescription createPartnerCheapestAsGiftDescription(
            String promoId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return createCheapestAsGiftDescription(
                0, promoId, Promo.ESourceType.PARTNER_SOURCE, 145, 3, "Test name",
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59),
                null, null
        );
    }

    public static DataCampPromo.PromoDescription createAnaplanCheapestAsGiftDescription(String promoId) {
        return createAnaplanCheapestAsGiftDescription(
                promoId, LocalDateTime.now().minus(10, ChronoUnit.DAYS), LocalDateTime.now().minus(10, ChronoUnit.DAYS)
        );
    }

    public static DataCampPromo.PromoDescription createAnaplanCheapestAsGiftDescription(
            String promoId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return createAnaplanCheapestAsGiftDescription(promoId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    public static DataCampPromo.PromoDescription createAnaplanCheapestAsGiftDescription(
            String promoId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return createAnaplanCheapestAsGiftDescription(0, promoId, 145, 3, "Test name", startDateTime, endDateTime);
    }

    public static DataCampPromo.PromoDescription createAnaplanCheapestAsGiftDescription(
            long businessId,
            String promoId,
            int warehouseId,
            int count,
            String promoName
    ) {
        return createAnaplanCheapestAsGiftDescription(
                businessId, promoId, warehouseId, count, promoName,
                LocalDateTime.of(2021, Month.JANUARY, 1, 0, 0), LocalDateTime.of(2051, Month.JANUARY, 1, 0, 0)
        );
    }

    public static DataCampPromo.PromoDescription createAnaplanCheapestAsGiftDescription(
            long businessId,
            String promoId,
            int warehouseId,
            int count,
            String promoName,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return createCheapestAsGiftDescription(
                businessId,
                promoId,
                Promo.ESourceType.ANAPLAN,
                warehouseId,
                count,
                promoName,
                startDateTime,
                endDateTime,
                DataCampPromo.PromoAdditionalInfo.StrategyType.CATEGORY,
                1577829600L // 2020 год
        );
    }

    /**
     * Создание [почти полностью] честного описания маркетплейсной акции "Самый дешевый в подарок" в формате АХ.
     */
    public static DataCampPromo.PromoDescription createCheapestAsGiftDescription(
            long businessId,
            String promoId,
            Promo.ESourceType sourceType,
            int warehouseId,
            int count,
            String promoName,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            DataCampPromo.PromoAdditionalInfo.StrategyType strategyType,
            Long publishDatePi
    ) {
        long startDateTimeInSeconds = getDateTimeInSeconds(startDateTime);
        long endDateTimeInSeconds = getDateTimeInSeconds(endDateTime);

        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setWarehouseRestriction(createWarehouseRestriction(warehouseId))
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setStartDate(startDateTimeInSeconds)
                        .setEndDate(endDateTimeInSeconds)
                        .setEnabled(true)
                        .build();

        DataCampPromo.PromoAdditionalInfo.Builder promoAdditionalInfoBuilder =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(promoName)
                        .setSendPromoPi(true);
        if (strategyType != null) {
            promoAdditionalInfoBuilder
                    .setStrategyType(strategyType);
        }
        if (publishDatePi != null) {
            promoAdditionalInfoBuilder
                    .setPublishDatePi(publishDatePi);

        }

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(Math.toIntExact(businessId))
                                .setSource(sourceType)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfoBuilder)
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setCheapestAsGift(
                                        DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                                .setCount(count)
                                                .build()
                                )
                                .build()
                )
                .build();
    }
}
