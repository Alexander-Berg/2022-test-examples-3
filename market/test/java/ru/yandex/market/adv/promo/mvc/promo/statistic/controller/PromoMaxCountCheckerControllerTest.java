package ru.yandex.market.adv.promo.mvc.promo.statistic.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

import static ru.yandex.market.adv.promo.utils.CommonTestUtils.getResource;

public class PromoMaxCountCheckerControllerTest extends FunctionalTest {

    private final static String RESULT_TRUE_JSON = "resultTrue_response.json";
    private final static String RESULT_FALSE_JSON = "resultFalse_response.json";

    /**
     * Тест проверяет корректность работы контроллера
     */
    @Test
    @DbUnitDataSet(
            before = "PromoMaxCountCheckerControllerTest/check_MaxLimitExceeded/before.csv"
    )
    @DisplayName("Кейс, когда текущее количество акций больше максимально допустимого")
    void check_MaxLimitExceeded() {
        check(RESULT_TRUE_JSON);
    }

    @Test
    @DbUnitDataSet(
            before = "PromoMaxCountCheckerControllerTest/check_OK/before.csv"
    )
    @DisplayName("Кейс, когда текущее количество акций меньше максимально допустимого")
    void check_OK() {
        check(RESULT_FALSE_JSON);
    }

    @Test
    @DbUnitDataSet(
            before = "PromoMaxCountCheckerControllerTest/check_StatisticsNotCalculated_OK/before.csv"
    )
    @DisplayName("Кейс, когда текущее количество акций ещё не посчитано")
    void check_StatisticsNotCalculated_OK() {
        check(RESULT_FALSE_JSON);
    }

    private void check(String resultJson) {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() + "/partner/promo/max-count/check");

        String expected = getResource(this.getClass(), resultJson);
        JSONAssert.assertEquals(expected, response.getBody(), true);
    }
}
