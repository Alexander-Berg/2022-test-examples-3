package ru.yandex.market.adv.promo.utils;

import Market.DataCamp.DataCampPromo;
import NMarket.Common.Promo.Promo;

public final class CashbackMechanicTestUtils {

    private CashbackMechanicTestUtils() { }

    public static DataCampPromo.PromoDescription createStandardCashback(String promoId, int businessId) {
        return createStandardCashback(promoId, businessId, 0, 12, 5, 18, true);
    }

    public static DataCampPromo.PromoDescription createStandardCashback(
            String promoId,
            int businessId,
            int marketTariffsVersionId,
            int cehacValue,
            int diyValue,
            int otherValue,
            boolean enabled
    ) {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                                .setBusinessId(businessId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK)
                )
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setPartnerStandartCashback(
                                        DataCampPromo.PromoMechanics.PartnerStandartCashback.newBuilder()
                                                .setMarketTariffsVersionId(marketTariffsVersionId)
                                                .addStandartGroup(
                                                        DataCampPromo.PromoMechanics.
                                                                PartnerStandartCashback.StandartGroup.newBuilder()
                                                                .setCodeName("cehac")
                                                                .setValue(cehacValue)
                                                )
                                                .addStandartGroup(
                                                        DataCampPromo.PromoMechanics.
                                                                PartnerStandartCashback.StandartGroup.newBuilder()
                                                                .setCodeName("diy")
                                                                .setValue(diyValue)
                                                )
                                                .addStandartGroup(
                                                        DataCampPromo.PromoMechanics.
                                                                PartnerStandartCashback.StandartGroup.newBuilder()
                                                                .setCodeName("default")
                                                                .setValue(otherValue)
                                                )
                                )
                )
                .setConstraints(
                        DataCampPromo.PromoConstraints.newBuilder()
                                .setEnabled(enabled)
                                .setStartDate(1577826000L) // 2020-01-01
                                .setEndDate(4102434000L) // 2100-01-01
                )
                .build();
    }
}
