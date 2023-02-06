package ru.yandex.market.adv.shop.integration.checkouter.logbroker.controller.juggler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 05.07.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
@DisplayName("Тесты на juggler ручки для logbroker.")
class LogbrokerMonitoringControllerTest extends AbstractShopIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @DbUnitDataSet(
            before = "LogbrokerMonitoringController/csv/" +
                    "checkLogbrokerStatus_errorExist_error.csv"
    )
    @DisplayName("За последние 30 минут в logbroker была ошибка на одном из instance.")
    @Test
    void checkLogbrokerStatus_errorExist_error() {
        check("checkLogbrokerStatus_errorExist_error", status().isExpectationFailed());
    }

    @DbUnitDataSet(
            before = "LogbrokerMonitoringController/csv/" +
                    "checkLogbrokerStatus_multiplyErrorExist_error.csv"
    )
    @DisplayName("За последние 30 минут в logbroker было несколько ошибок.")
    @Test
    void checkLogbrokerStatus_multiplyErrorExist_error() {
        check("checkLogbrokerStatus_multiplyErrorExist_error", status().isExpectationFailed());
    }

    @DbUnitDataSet(
            before = "LogbrokerMonitoringController/csv/" +
                    "checkLogbrokerStatus_empty_ok.csv"
    )
    @DisplayName("Таблица с ошибками logbroker не содержит данных.")
    @Test
    void checkLogbrokerStatus_empty_ok() {
        check("checkLogbrokerStatus_empty_ok", status().isOk());
    }

    @DbUnitDataSet(
            before = "LogbrokerMonitoringController/csv/" +
                    "checkLogbrokerStatus_existed_ok.csv"
    )
    @DisplayName("За последние 30 минут в logbroker не было ошибок.")
    @Test
    void checkLogbrokerStatus_existed_ok() {
        check("checkLogbrokerStatus_existed_ok", status().isOk());
    }

    private void check(String methodName, ResultMatcher resultMatcher) {
        try {
            mvc.perform(
                            get("/juggler/logbroker/status")
                                    .contentType(MediaType.TEXT_PLAIN_VALUE)
                    )
                    .andExpect(resultMatcher)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                    .andExpect(content().string(
                                    loadFile("LogbrokerMonitoringController/txt/" + methodName + ".txt")
                                            .trim()
                            )
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
