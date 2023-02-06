package ru.yandex.market.partner.mvc.controller.asyncreport;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

public class AsyncReportsStatusYtControllerTest extends FunctionalTest {

    @Autowired
    private SalesDynamicsYtStorage salesDynamicsYtStorage;

    @Test
    @DisplayName("Тест на выбор правильного сервиса")
    void testGetCorrectService() {
        sendRequest(ReportsType.SALES_DYNAMICS);
        Mockito.verify(salesDynamicsYtStorage).findAllAvailableReportDates();
    }

    @Test
    @DisplayName("Запрос на данные для неподдерживаемого отчета")
    void testGetIncorrectReport() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendRequest(ReportsType.ORDERS_RETURNS)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorMessage("Option is not supported for report type ORDERS_RETURNS")
        );
    }

    private ResponseEntity<String> sendRequest(ReportsType reportsType) {
        return FunctionalTestHelper.get(baseUrl + "/async-reports/status/available-dates?report=" + reportsType);
    }
}
