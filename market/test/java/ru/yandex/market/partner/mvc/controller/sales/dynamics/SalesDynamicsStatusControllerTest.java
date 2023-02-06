package ru.yandex.market.partner.mvc.controller.sales.dynamics;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link SalesDynamicsStatusController}
 */
class SalesDynamicsStatusControllerTest extends FunctionalTest {

    @Autowired
    private SalesDynamicsYtStorage salesDynamicsYtStorage;

    @Test
    @DisplayName("Тест на получение доступных данных")
    void testGetAvailableDates() {
        when(salesDynamicsYtStorage.findAllAvailableReportDates()).thenReturn(List.of(
                LocalDate.parse("2021-06-30"),
                LocalDate.parse("2021-07-29"),
                LocalDate.parse("2021-08-01"),
                LocalDate.parse("2021-08-04"),
                LocalDate.parse("2021-08-29")
        ));
        ResponseEntity<String> response = sendRequest();
        JsonTestUtil.assertEquals(response, getClass(), "testGetAvailableDates.json");
    }

    @Test
    @DisplayName("Тест на получение доступных данных при недоступности YT")
    void testGetAvailableDatesWithErrors() {
        when(salesDynamicsYtStorage.findAllAvailableReportDates()).thenThrow(RuntimeException.class);
        ResponseEntity<String> response = sendRequest();
        JsonTestUtil.assertEquals(response, "{\"dates\":[]}");
    }

    private ResponseEntity<String> sendRequest() {
        return FunctionalTestHelper.get(baseUrl + "/sales-dynamics/available-dates");
    }
}
