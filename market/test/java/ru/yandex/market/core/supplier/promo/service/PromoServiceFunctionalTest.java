package ru.yandex.market.core.supplier.promo.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import Market.DataCamp.DataCampPromo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.FunctionalTest;

//todo перенести директории
class PromoServiceFunctionalTest extends FunctionalTest {

    @Test
    void promoFinishedByPartnerTest() {
        DataCampPromo.PromoDescription promoDescription = DataCampPromo.PromoDescription.newBuilder()
                .setConstraints(
                        DataCampPromo.PromoConstraints.newBuilder()
                                .setStartDate(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond())
                                .setEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                                .setEnabled(false)
                                .build()
                )
                .build();
        boolean isPromoFinished = PromoService.isPromoFinished(promoDescription);
        Assertions.assertTrue(isPromoFinished);
    }

    @Test
    void promoFinishedByTimeTest() {
        DataCampPromo.PromoDescription promoDescription = DataCampPromo.PromoDescription.newBuilder()
                .setConstraints(
                        DataCampPromo.PromoConstraints.newBuilder()
                                .setStartDate(Instant.now().minus(10, ChronoUnit.DAYS).getEpochSecond())
                                .setEndDate(Instant.now().minus(5, ChronoUnit.DAYS).getEpochSecond())
                                .setEnabled(true)
                                .build()
                )
                .build();
        boolean isPromoFinished = PromoService.isPromoFinished(promoDescription);
        Assertions.assertTrue(isPromoFinished);
    }
}
