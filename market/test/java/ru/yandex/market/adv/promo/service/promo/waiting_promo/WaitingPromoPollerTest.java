package ru.yandex.market.adv.promo.service.promo.waiting_promo;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.service.promo.waiting_promo.model.WaitingPromo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;

class WaitingPromoPollerTest extends FunctionalTest {
    @Autowired
    private WaitingPromoPoller waitingPromoPoller;
    @Autowired
    private SaasService saasDataCampShopService;
    @Autowired
    private WaitingPromoService waitingPromoService;

    @Test
    @DbUnitDataSet(before = "WaitingPromoPollerTest/processingWaitingPromosTest.before.csv")
    public void processingWaitingPromosTest() {
        mockSaasResponseWithOffer(111);
        mockSaasResponseWithoutOffer(222);
        mockSaasResponseWithOffer(333);
        mockSaasResponseWithoutOffer(444);
        waitingPromoPoller.processWaitingPromos();
        List<WaitingPromo> allWaitingPromos = waitingPromoService.getAllWaitingPromos();
        LocalDateTime startTime = LocalDateTime.of(2022, Month.JULY, 20, 14, 23, 30, 79000000);
        WaitingPromo expectedWaitingPromo1 = new WaitingPromo.Builder()
                .withPartnerId(333)
                .withPromoId("cf_0003")
                .withOfferId("offer3")
                .withIsActive(false)
                .withStartTime(startTime)
                .build();
        WaitingPromo expectedWaitingPromo2 = new WaitingPromo.Builder()
                .withPartnerId(444)
                .withPromoId("cf_0004")
                .withOfferId("offer4")
                .withIsActive(true)
                .withStartTime(startTime)
                .build();
        assertThat(allWaitingPromos).containsExactlyInAnyOrder(expectedWaitingPromo1, expectedWaitingPromo2);
    }

    private void mockSaasResponseWithOffer(long partnerId) {
        SaasOfferInfo saasOfferInfo1 = SaasOfferInfo.newBuilder()
                .addShopId(partnerId)
                .addOfferId("offerId")
                .build();
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo1))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult).when(saasDataCampShopService)
                .searchBusinessOffers(argThat(request -> request.getPrefix() == partnerId));
    }

    private void mockSaasResponseWithoutOffer(long partnerId) {
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(Collections.emptyList())
                .setTotalCount(0)
                .build();
        doReturn(saasSearchResult).when(saasDataCampShopService)
                .searchBusinessOffers(argThat(request -> request.getPrefix() == partnerId));
    }
}
