package ru.yandex.market.adv.promo.monitoring.promo.assortment;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.tms.command.dao.PromoYTDao;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.adv.promo.service.assortment.PotentialAssortmentService.MAXIMUM_HOURS_WITHOUT_ASSORTMENT;

class PotentialAssortmentMonitoringTest extends FunctionalTest {
    @Autowired
    private PromoYTDao promoYTDao;

    @DisplayName("Проверка, что у мониторинга статус ОК, если нашлась таблица в необходимом диапазоне времени.")
    @Test
    void testAssortmentStatsOK() {
        doReturn(Instant.now().minus(MAXIMUM_HOURS_WITHOUT_ASSORTMENT - 1, HOURS)).when(promoYTDao).getPathCreationTime(any(), any());

        ResponseEntity<String> resp = FunctionalTestHelper.get(baseUrl() + "/monitoring/assortment");
        int statusCodeValue = resp.getStatusCodeValue();
        assertEquals(200, statusCodeValue);
        String response = resp.getBody();
        assertNotNull(response);
        String expectedMessage = "0;OK";

        assertEquals(expectedMessage, response);
    }

    @DisplayName("Проверка, что у мониторинга статус CRIT, если не нашлась таблица в необходимом диапазоне времени.")
    @Test
    void testAssortmentStatsCritAtTableCreation() {
        doReturn(Instant.now().minus(MAXIMUM_HOURS_WITHOUT_ASSORTMENT + 1, HOURS)).when(promoYTDao).getPathCreationTime(any(), any());

        ResponseEntity<String> resp = FunctionalTestHelper.get(baseUrl() + "/monitoring/assortment");
        int statusCodeValue = resp.getStatusCodeValue();
        assertEquals(200, statusCodeValue);
        String response = resp.getBody();
        assertNotNull(response);
        String expectedMessage =
                "2;CRIT It's been more than " + MAXIMUM_HOURS_WITHOUT_ASSORTMENT +
                        " hours since the last Potential Assortment table was created.";

        assertEquals(expectedMessage, response);
    }
}
