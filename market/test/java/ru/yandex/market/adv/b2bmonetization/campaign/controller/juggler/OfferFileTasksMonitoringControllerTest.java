package ru.yandex.market.adv.b2bmonetization.campaign.controller.juggler;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 29.03.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
@DisplayName("Тесты на endpoint GET juggler/file/offer/tasks/status.")
class OfferFileTasksMonitoringControllerTest extends AbstractMonetizationTest {

    @Autowired
    private MockMvc mvc;

    @DbUnitDataSet(
            before = "OfferFileTasksMonitoringController/csv/" +
                    "checkCountOfActiveTasks_twoTasks_ok.before.csv"
    )
    @DisplayName("Мониторинг на активные задачи загрузки ставок по товарам из файла вернул OK.")
    @Test
    void checkCountOfActiveTasks_twoTasks_ok() throws Exception {
        checkCountOfActiveTasks("checkCountOfActiveTasks_twoTasks_ok", status().isOk());
    }

    @DbUnitDataSet(
            before = "OfferFileTasksMonitoringController/csv/" +
                    "checkCountOfActiveTasks_threeTasks_warn.before.csv"
    )
    @DisplayName("Мониторинг на активные задачи загрузки ставок по товарам из файла вернул WARN.")
    @Test
    void checkCountOfActiveTasks_threeTasks_warn() throws Exception {
        checkCountOfActiveTasks("checkCountOfActiveTasks_threeTasks_warn", status().isBadRequest());
    }

    @DbUnitDataSet(
            before = "OfferFileTasksMonitoringController/csv/" +
                    "checkCountOfActiveTasks_fiveTasks_error.before.csv"
    )
    @DisplayName("Мониторинг на активные задачи загрузки ставок по товарам из файла вернул ERROR.")
    @Test
    void checkCountOfActiveTasks_fiveTasks_error() throws Exception {
        checkCountOfActiveTasks("checkCountOfActiveTasks_fiveTasks_error", status().isExpectationFailed());
    }

    private void checkCountOfActiveTasks(String methodName, ResultMatcher resultMatcher) throws Exception {
        mvc.perform(
                        get("/juggler/file/offer/tasks/status")
                                .contentType(MediaType.TEXT_PLAIN_VALUE)
                )
                .andExpect(resultMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string(
                                loadFile("OfferFileTasksMonitoringController/json/response/" + methodName + ".txt")
                                        .trim()
                        )
                );
    }
}
