package ru.yandex.market.rg.asyncreport.migration.promo;

import java.util.Map;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampUnitedOffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.rg.asyncreport.unitedcatalog.migration.UnitedOfferBuilder;
import ru.yandex.market.rg.config.FunctionalTest;

class OfferPromoUpdaterTest extends FunctionalTest {

    @Autowired
    private OfferPromoUpdater offerPromoUpdater;

    @ParameterizedTest(name = "{1}")
    @DisplayName("Тест обновления промо в офере.")
    @MethodSource("getPromoTypes")
    void createUpdatedOffer(DataCampPromo.PromoType promoType, String testName) {
        int businessId = 100;
        int serviceId = 200;
        String offerId = "someOfferId";
        int warehouseId = 300;
        String oldPromoOld = "someOldPromoId";
        String newPromoId = "someNewPromoId";
        var sourceOffer = generateSourceOffer(businessId, serviceId, offerId, warehouseId, promoType, oldPromoOld);
        var promoIdMap = Map.of(oldPromoOld, newPromoId);
        var actual = offerPromoUpdater.createUpdatedOffer(sourceOffer, promoIdMap);
        var expected = generateExpectedOffer(businessId, serviceId, offerId, warehouseId, promoType, newPromoId);
        Assertions.assertEquals(expected, actual);
    }

    private DataCampUnitedOffer.UnitedOffer generateSourceOffer(int businessId, int serviceId, String offerId,
                                                                int warehouseId, DataCampPromo.PromoType type,
                                                                String promoId) {
        return UnitedOfferBuilder
                .offerBuilder(businessId, serviceId, offerId, warehouseId, true, false, null)
                .withPromo(type, promoId)
                .build();
    }

    private DataCampUnitedOffer.UnitedOffer generateExpectedOffer(int businessId, int serviceId, String offerId,
                                                                  int warehouseId, DataCampPromo.PromoType type,
                                                                  String promoId) {
        var offerMeta = DataCampOfferMeta.OfferMeta.newBuilder()
                .setScope(DataCampOfferMeta.OfferScope.SERVICE)
                .build();
        return UnitedOfferBuilder
                .offerBuilder(businessId, serviceId, offerId, warehouseId, false, false, offerMeta)
                .withPromo(type, promoId)
                .build();
    }

    private static Stream<Arguments>  getPromoTypes() {
        return Stream.of(
                Arguments.of(DataCampPromo.PromoType.MARKET_PROMOCODE, "Обновление офера с акций-промокодом"),
                Arguments.of(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK, "Обновление офера с акций-кешбеком")
        );
    }


}
