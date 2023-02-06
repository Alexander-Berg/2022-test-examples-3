package ru.yandex.market.adv.promo.service.promo.waiting_promo;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.service.promo.waiting_promo.model.WaitingPromo;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;


class WaitingPromoServiceTest extends FunctionalTest {
    @Autowired
    WaitingPromoService waitingPromoService;

    @Test
    @DbUnitDataSet(before = "WaitingPromoServiceTest/waitingPromoTest.before.csv")
    public void getExistingWaitingPromoTest() {
        long partnerId = 111;
        String promoId = "cf_0001";
        String offerId = "offer1";
        LocalDateTime startTime = LocalDateTime.of(2022, Month.JULY, 20, 14, 23, 30, 79000000);
        Optional<WaitingPromo> waitingPromo = waitingPromoService.getWaitingPromo(partnerId, promoId);
        assertThat(waitingPromo).isPresent();
        WaitingPromo expectedWaitingPromo = new WaitingPromo.Builder()
                .withPartnerId(partnerId)
                .withPromoId(promoId)
                .withOfferId(offerId)
                .withIsActive(true)
                .withStartTime(startTime)
                .build();
        assertThat(waitingPromo.get()).isEqualTo(expectedWaitingPromo);
    }

    @Test
    @DbUnitDataSet(before = "WaitingPromoServiceTest/waitingPromoTest.before.csv")
    public void getAbsentWaitingPromoTest() {
        long partnerId = 111;
        String promoId = "cf_0002";
        Optional<WaitingPromo> waitingPromo = waitingPromoService.getWaitingPromo(partnerId, promoId);
        assertThat(waitingPromo).isNotPresent();
    }

    @Test
    @DbUnitDataSet(before = "WaitingPromoServiceTest/waitingPromoTest.before.csv")
    public void startWaitingNewPromoTest() {
        long partnerId = 111;
        String promoId = "cf_0002";
        String offerId = "offer1";
        boolean isActive = false;
        waitingPromoService.startWaitingPromo(partnerId, promoId, offerId, isActive);
        Optional<WaitingPromo> waitingPromo = waitingPromoService.getWaitingPromo(partnerId, promoId);
        assertThat(waitingPromo).isPresent();
        assertThat(waitingPromo.get()).satisfies(
                promo -> {
                    assertThat(promo.getPromoId()).isEqualTo(promoId);
                    assertThat(promo.getOfferId()).isEqualTo(offerId);
                    assertThat(promo.getPartnerId()).isEqualTo(partnerId);
                    assertThat(promo.isActive()).isFalse();
                }
        );
    }

    @Test
    @DbUnitDataSet(before = "WaitingPromoServiceTest/waitingPromoTest.before.csv")
    public void startWaitingExistPromoTest() {
        long partnerId = 111;
        String promoId = "cf_0001";
        String offerId = "offer2";
        boolean isActive = false;
        waitingPromoService.startWaitingPromo(partnerId, promoId, offerId, isActive);
        Optional<WaitingPromo> waitingPromo = waitingPromoService.getWaitingPromo(partnerId, promoId);
        assertThat(waitingPromo).isPresent();
        assertThat(waitingPromo.get()).satisfies(
                promo -> {
                    assertThat(promo.getPromoId()).isEqualTo(promoId);
                    assertThat(promo.getOfferId()).isEqualTo(offerId);
                    assertThat(promo.getPartnerId()).isEqualTo(partnerId);
                    assertThat(promo.isActive()).isFalse();
                }
        );
    }

    @Test
    @DbUnitDataSet(before = "WaitingPromoServiceTest/waitingPromoTest.before.csv")
    public void finishWaitingPromoTest() {
        long partnerId = 111;
        String promoId = "cf_0001";
        waitingPromoService.finishWaitingPromo(partnerId, promoId);
        Optional<WaitingPromo> waitingPromo = waitingPromoService.getWaitingPromo(partnerId, promoId);
        assertThat(waitingPromo).isNotPresent();
    }
}
