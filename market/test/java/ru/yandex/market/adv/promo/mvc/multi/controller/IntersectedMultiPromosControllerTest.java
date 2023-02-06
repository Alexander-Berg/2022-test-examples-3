package ru.yandex.market.adv.promo.mvc.multi.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

class IntersectedMultiPromosControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosControllerTest/unknownProcessingIdSpecifiedTest.before.csv")
    @DisplayName("Передан несуществующий процессинг")
    void unknownProcessingIdSpecifiedTest() {
        String requestBody =
                CommonTestUtils.getResource(this.getClass(), "unknownProcessingIdSpecifiedTest_request.json");
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getIntersectedPromosRequest(1111, 2222, requestBody)
        );
    }

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosControllerTest/incorrectStatusProcessingIdSpecifiedTest.before.csv")
    @DisplayName("Передан процессинг, который находится в некорректном статусе")
    void incorrectStatusProcessingIdSpecifiedTest() {
        String requestBody =
                CommonTestUtils.getResource(this.getClass(), "incorrectStatusProcessingIdSpecifiedTest_request.json");
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getIntersectedPromosRequest(1111, 2222, requestBody)
        );
    }

    private ResponseEntity<String> getIntersectedPromosRequest(long partnerId, long businessId, String body) {
        return FunctionalTestHelper.post(
                baseUrl() + "/partner/promo/multi/intersected-promos?partnerId=" +
                        partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }
}
