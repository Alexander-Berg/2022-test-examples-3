package ru.yandex.market.adv.promo.utils;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule;

/**
 * Класс для вспомогательных метов, общих для различных механик, для работы с промо в тестах.
 */
public final class PromoMechanicTestUtils {

    private PromoMechanicTestUtils() { }

    /**
     * Создание ограничения по складам.
     */
    public static OffersMatchingRule.WarehouseRestriction createWarehouseRestriction(int warehouseId) {
        return DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                        .addId(warehouseId)
                        .build()
                )
                .build();
    }

    public static DataCampPromo.PromoDescription addUpdateTimeToPromoDescription(
            DataCampPromo.PromoDescription description,
            long createdAt,
            long updatedAt
    ) {
        return description.toBuilder()
                .setUpdateInfo(
                        DataCampPromo.UpdateInfo.newBuilder()
                                .setCreatedAt(createdAt)
                                .setUpdatedAt(updatedAt)
                                .build()
                )
                .build();
    }
}
