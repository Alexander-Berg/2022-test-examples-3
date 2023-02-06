package ru.yandex.market.adv.promo.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType;
import Market.DataCamp.DataCampPromo.PromoMechanics.MarketPromocode.DiscountType;
import NMarket.Common.Promo.Promo;
import NMarketIndexer.Common.Common;
import org.apache.commons.lang3.StringUtils;

import static ru.yandex.market.adv.promo.datacamp.utils.DatacampOffersUtils.powToIdx;
import static ru.yandex.market.adv.promo.datacamp.utils.DateTimeUtils.getDateTimeInSeconds;

/**
 * Класс для вспомогательных метов для работы с промо с механикой "Промокод" в тестах.
 */
@SuppressWarnings("ParameterNumber")
public final class PromocodeMechanicTestUtils {

    private PromocodeMechanicTestUtils() { }

    public static DataCampPromo.PromoDescription createPartnerPromocodeDescription(String promoId) {
        return createPartnerPromocodeDescription(
                0, promoId,
                LocalDateTime.now().minus(10, ChronoUnit.DAYS),
                LocalDateTime.now().minus(10, ChronoUnit.DAYS),
                true
        );
    }

    public static DataCampPromo.PromoDescription createPartnerPromocodeDescription(
            long businessId,
            String promoId,
            String promocode,
            String promoName,
            long discountValue,
            DiscountType discountType,
            ApplyingType applyingType,
            BigDecimal budgetLimit
    ) {
        return createPartnerPromocodeDescription(
                businessId, promoId, promocode, promoName, discountValue, discountType, applyingType, budgetLimit,
                LocalDateTime.of(2021, Month.JANUARY, 1, 0, 0), LocalDateTime.of(2051, Month.JANUARY, 1, 0, 0), true
        );
    }

    public static DataCampPromo.PromoDescription createPartnerPromocodeDescription(
            String promoId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return createPartnerPromocodeDescription(
                0, promoId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59), true
        );
    }

    public static DataCampPromo.PromoDescription createPartnerPromocodeDescription(
            long businessId,
            String promoId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            boolean enabled
    ) {
        String promocode = StringUtils.substringAfter(promoId, "_");

        return createPartnerPromocodeDescription(
                businessId, promoId, promocode, "Test name", 5, DiscountType.PERCENTAGE, ApplyingType.REUSABLE, null,
                startDateTime, endDateTime, enabled
        );
    }

    public static DataCampPromo.PromoDescription createPartnerPromocodeDescription(
            long businessId,
            String promoId,
            String promocode,
            String promoName,
            long discountValue,
            DiscountType discountType,
            ApplyingType applyingType,
            BigDecimal budgetLimit,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            boolean enabled
    ) {
        return createPromocodeDescription(
                businessId, promoId, Promo.ESourceType.PARTNER_SOURCE, promocode, promoName, discountValue,
                discountType, applyingType, budgetLimit, startDateTime, endDateTime, enabled, null, null, null, null
        );
    }

    public static DataCampPromo.PromoDescription createAnaplanPromocodeDescription(String promoId) {
        String promocode = StringUtils.substringAfter(promoId, "_");

        return createAnaplanPromocodeDescription(
                0, promoId, promocode, "Promo name",
                10, DiscountType.PERCENTAGE, ApplyingType.ONE_TIME,
                true, null, null
        );
    }

    public static DataCampPromo.PromoDescription createAnaplanPromocodeDescription(
            String promoId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String promocode = StringUtils.substringAfter(promoId, "_");

        return createPromocodeDescription(
                0, promoId, Promo.ESourceType.ANAPLAN, promocode, "Test name",
                10, DiscountType.PERCENTAGE, ApplyingType.ONE_TIME, null,
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59),
                true, null, null, null, null
        );
    }

    public static DataCampPromo.PromoDescription createAnaplanPromocodeDescription(
            long businessId,
            String promoId,
            String promocode,
            String promoName,
            long discountValue,
            DiscountType discountType,
            ApplyingType applyingType,
            boolean enabled,
            BigDecimal cartMinPrice,
            BigDecimal orderMaxPrice
    ) {
        return createPromocodeDescription(
                businessId, promoId, Promo.ESourceType.ANAPLAN, promocode, promoName, discountValue,
                discountType, applyingType, null, LocalDateTime.of(2021, Month.JANUARY, 1, 0, 0),
                LocalDateTime.of(2051, Month.JANUARY, 1, 0, 0), enabled, cartMinPrice, orderMaxPrice,
                DataCampPromo.PromoAdditionalInfo.StrategyType.CATEGORY, 1577829600L // 2020 год
        );
    }

    /**
     * Создание [почти полностью] честного описания партнерского промокода в формате АХ.
     */
    public static DataCampPromo.PromoDescription createPromocodeDescription(
            long businessId,
            String promoId,
            NMarket.Common.Promo.Promo.ESourceType sourceType,
            String promocode,
            String promoName,
            long discountValue,
            DiscountType discountType,
            ApplyingType applyingType,
            BigDecimal budgetLimit,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            boolean enabled,
            BigDecimal cartMinPrice,
            BigDecimal orderMaxPrice,
            DataCampPromo.PromoAdditionalInfo.StrategyType strategyType,
            Long publishDatePi
    ) {
        long startDateTimeInSeconds = getDateTimeInSeconds(startDateTime);
        long endDateTimeInSeconds = getDateTimeInSeconds(endDateTime);

        DataCampPromo.PromoMechanics.MarketPromocode.Builder marketPromocodeBuilder =
                DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                        .setPromoCode(promocode)
                        .setDiscountType(discountType)
                        .setApplyingType(applyingType);
        if (discountType == DiscountType.PERCENTAGE) {
            marketPromocodeBuilder.setValue(discountValue);
        } else if (discountType == DiscountType.VALUE) {
            marketPromocodeBuilder.setRatingRub(discountValue);
        }
        if (cartMinPrice != null) {
            marketPromocodeBuilder.setBucketMinPrice(cartMinPrice.longValue());
        }
        if (orderMaxPrice != null) {
            marketPromocodeBuilder.setOrderMaxPrice(orderMaxPrice.longValue());
        }

        DataCampPromo.PromoConstraints.Builder promoConstraintsBuilder =
                DataCampPromo.PromoConstraints.newBuilder()
                        .setStartDate(startDateTimeInSeconds)
                        .setEndDate(endDateTimeInSeconds)
                        .setEnabled(enabled);
        if (budgetLimit != null) {
            promoConstraintsBuilder.setMoneyLimit(
                    Common.PriceExpression.newBuilder()
                            .setPrice(powToIdx(budgetLimit))
            );
        }

        DataCampPromo.PromoAdditionalInfo.Builder promoAdditionalInfoBuilder =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(promoName)
                        .setSendPromoPi(true)
                        .setPriority(1);
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
                                .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                )
                .setConstraints(
                        promoConstraintsBuilder
                )
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setMarketPromocode(marketPromocodeBuilder)
                )
                .setAdditionalInfo(
                        promoAdditionalInfoBuilder
                )
                .build();
    }
}
