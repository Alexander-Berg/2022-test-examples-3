package ru.yandex.market.adv.b2bmonetization.programs.controller.juggler;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 05.05.2022
 * Project: b2bmarketmonetization
 *
 * @author eogoreltseva
 */
@ParametersAreNonnullByDefault
@DisplayName("Тесты на endpoint GET juggler/program/status и GET juggler/program/status/enqueued.")
class ProgramStatusMonitoringControllerTest extends AbstractMonetizationTest {

    @Autowired
    private MockMvc mvc;

    @DbUnitDataSet(
            before = "ProgramStatusMonitoringControllerTest/csv/" +
                    "checkProgramTasks_nonTerminalStatus_ok.before.csv"
    )
    @DisplayName("Мониторинг на незавершенные заявки на программы вернул OK.")
    @Test
    void checkProgramTasks_nonTerminalStatus_ok() throws Exception {
        checkProgramTasks("/juggler/program/status", "checkProgramTasks_nonTerminalStatus_ok");
    }

    @DbUnitDataSet(
            before = "ProgramStatusMonitoringControllerTest/csv/" +
                    "checkProgramTasks_enqueuedStatus_ok.before.csv"
    )
    @DisplayName("Мониторинг на незавершенные заявки в статусе ENQUEUED вернул OK.")
    @Test
    void checkProgramTasks_enqueuedStatus_ok() throws Exception {
        checkProgramTasks("/juggler/program/status/enqueued", "checkProgramTasks_enqueuedStatus_ok");
    }

    @DbUnitDataSet(
            before = "ProgramStatusMonitoringControllerTest/csv/" +
                    "checkProgramTasks_nonTerminalStatus_crit.before.csv"
    )
    @DisplayName("Мониторинг на незавершенные заявки на программы вернул CRIT.")
    @Test
    void checkProgramTasks_nonTerminalStatus_crit() throws Exception {
        checkProgramTasks("/juggler/program/status", "checkProgramTasks_nonTerminalStatus_crit");
    }

    @DbUnitDataSet(
            before = "ProgramStatusMonitoringControllerTest/csv/" +
                    "checkProgramTasks_enqueuedStatus_crit.before.csv"
    )
    @DisplayName("Мониторинг на незавершенные заявки в статусе ENQUEUED вернул CRIT.")
    @Test
    void checkProgramTasks_enqueuedStatus_crit() throws Exception {
        checkProgramTasks("/juggler/program/status/enqueued", "checkProgramTasks_enqueuedStatus_crit");
    }

    private void checkProgramTasks(String path, String methodName) throws Exception {
        mvc.perform(
                        get(path).contentType(MediaType.TEXT_PLAIN_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string(
                                loadFile("ProgramStatusMonitoringControllerTest/txt/response/"
                                        + methodName + ".txt"
                                )
                                        .trim()
                        )
                );
    }
}
