package ru.yandex.market.adv.promo.mvc.promo.waiting_promo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

class StartWaitingPromoControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(after = "StartWaitingPromoControllerTest/testStartWaitingForPromo.after.csv")
    public void testStartWaitingForPromo() {
        String requestBody = CommonTestUtils.getResource(this.getClass(), "testStartWaitingForPromo.json");
        makeWaitingPromoRequest(requestBody);
    }

    private void makeWaitingPromoRequest(String body) {
        FunctionalTestHelper.post(baseUrl() + "/promo/start-waiting-promo", new HttpEntity<>(body, getDefaultHeaders()));
    }
}
