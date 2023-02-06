package ru.yandex.market.adv.promo.monitoring.promo.statistic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.adv.promo.service.environment.constant.EnvironmentSettingConstants.PROMO_MAX_COUNT;

public class PromoMaxCountMonitoringTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(
            before = "PromoMaxCountMonitoringTest/check_OK/before.csv"
    )
    @DisplayName("Настройка окружения задана, порог не пробили - всё хорошо")
    void check_OK() {
        String expectedMessage = "0;OK";
        check(expectedMessage);
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Настройка окружения не установлена или некорреткна")
    void check_InvalidEnvironmentSetting() {
        String expectedMessage = String.format(
                "2;<CRIT> Environment setting for the maximum possible count of partner promos " +
                        "is not set or invalid, setting name: %s", PROMO_MAX_COUNT);

        check(expectedMessage);
    }

    @Test
    @DbUnitDataSet(
            before = "PromoMaxCountMonitoringTest/check_CloseToMaxCount/before.csv"
    )
    @DisplayName("Близки к пробитию порога")
    void check_CloseToMaxCount() {
        int currentCount = 149149;
        int maxCount = 150000;
        check_MaxCount(currentCount, maxCount);
    }

    @Test
    @DbUnitDataSet(
            before = "PromoMaxCountMonitoringTest/check_MaxCountExceeded/before.csv"
    )
    @DisplayName("Пробили порог")
    void check_MaxCountExceeded() {
        int currentCount = 150150;
        int maxCount = 150000;
        check_MaxCount(currentCount, maxCount);
    }

    void check_MaxCount(int currentCount, int maxCount) {
        String expectedMessage = String.format(
                "2;<CRIT> The current count of partner promos is close to or exceeds " +
                        "the maximum allowed, current count = %d, max count = %d", currentCount, maxCount);

        check(expectedMessage);
    }

    void check(String expectedMessage) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl() + "/monitoring/partner/promo/max-count");

        assertEquals(expectedMessage, response.getBody());
    }
}
