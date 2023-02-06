package ru.yandex.market.adv.promo.mvc.promo.waiting_promo.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;

class WaitingForSaasPromosControllerTest extends FunctionalTest {
    @Test
    @DbUnitDataSet(before = "WaitingForSaasPromosControllerTest/testWaitingPromos.before.csv")
    public void testPresentWaitingPromos() {
        ResponseEntity<String> response = makeWaitingPromoRequest(222, "cf_0002");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JSONAssert.assertEquals("{\"hasProcessingAssortment\": true}", response.getBody(), true);
    }

    @Test
    @DbUnitDataSet(before = "WaitingForSaasPromosControllerTest/testWaitingPromos.before.csv")
    public void testNotPresentWaitingPromos() {
        ResponseEntity<String> response = makeWaitingPromoRequest(111, "cf_0001");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JSONAssert.assertEquals("{\"hasProcessingAssortment\": false}", response.getBody(), true);
    }

    private ResponseEntity<String> makeWaitingPromoRequest(long partnerId, String promoId) {
        return FunctionalTestHelper.get(
                baseUrl() + "/partner/promo/check-invisible-assortment-existence?partnerId=" + partnerId + "&promoId="+promoId
        );
    }
}
