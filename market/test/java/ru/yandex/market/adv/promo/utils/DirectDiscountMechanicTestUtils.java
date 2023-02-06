package ru.yandex.market.adv.promo.utils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule;
import NMarket.Common.Promo.Promo;
import org.apache.commons.lang3.tuple.Pair;

import static ru.yandex.market.adv.promo.datacamp.utils.DateTimeUtils.getDateTimeInSeconds;

public final class DirectDiscountMechanicTestUtils {

    private DirectDiscountMechanicTestUtils() { }

    public static DataCampPromo.PromoDescription createAnaplanDirectDiscountDescription(
            String promoId
    ) {
        return createAnaplanDirectDiscountDescription(promoId, LocalDate.of(2020, 1, 1), LocalDate.of(2040, 1, 1));
    }

    public static DataCampPromo.PromoDescription createAnaplanDirectDiscountDescription(
            String promoId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return createAnaplanDirectDiscountDescription(promoId, Collections.emptySet(), startDate, endDate);
    }

    public static DataCampPromo.PromoDescription createAnaplanDirectDiscountDescription(
            String promoId,
            Set<Pair<Long, Integer>> categoriesInfo
    ) {
        return createAnaplanDirectDiscountDescription(
                promoId, categoriesInfo,
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2040, 1, 1)
        );
    }

    /**
     * Создание [почти полностью] честного описания маркетплейсной флеш акции в формате АХ.
     *
     * @param categoriesInfo пары (идентификатор категории, значение мин скидки в этой категории)
     */
    public static DataCampPromo.PromoDescription createAnaplanDirectDiscountDescription(
            String promoId,
            Set<Pair<Long, Integer>> categoriesInfo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<OffersMatchingRule.PromoCategory> promoCategories = categoriesInfo.stream()
                .map(categoryInfo ->
                        OffersMatchingRule.PromoCategory.newBuilder()
                                .setId(categoryInfo.getKey())
                                .setMinDiscount(categoryInfo.getValue())
                                .build()
                )
                .collect(Collectors.toList());
        OffersMatchingRule offersMatchingRule =
                OffersMatchingRule.newBuilder()
                        .setCategoryRestriction(
                                OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addAllPromoCategory(promoCategories)
                                        .build()
                        )
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setEnabled(true)
                        .setStartDate(getDateTimeInSeconds(startDate.atStartOfDay()))
                        .setEndDate(getDateTimeInSeconds(endDate.atTime(23, 59, 59)))
                        .build();

        DataCampPromo.PromoAdditionalInfo promoAdditionalInfo =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo name")
                        .setSendPromoPi(true)
                        .setStrategyType(DataCampPromo.PromoAdditionalInfo.StrategyType.CROSS_CATEGORY)
                        .setPublishDatePi(1577829600) // 2020 год
                        .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfo)
                .build();
    }
}
