package ru.yandex.market.adv.b2bmonetization.programs.interactor.sorryManual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты на endpoint GET /v1/program для программ SORRY и MANUAL
 */
class GetSorryManualProgramApiServiceTest extends AbstractMonetizationTest {

    @DisplayName("Успешно получили информацию, что партнер может участвовать в программе SORRY")
    @DbUnitDataSet(
            before = "Get/csv/getProgram_sorry_success.before.csv"
    )
    @Test
    void getProgram_sorry_success() throws Exception {
        check("SORRY", "getProgram_sorry_success", status().isOk());
    }

    @DisplayName("Успешно получили информацию, что партнер может участвовать в программе MANUAL")
    @DbUnitDataSet(
            before = "Get/csv/getProgram_manual_success.before.csv"
    )
    @Test
    void getProgram_manual_success() throws Exception {
        check("MANUAL", "getProgram_manual_success", status().isOk());
    }

    @DisplayName("Успешно получили информацию, что партнер не может участвовать в программе MANUAL")
    @DbUnitDataSet(
            before = "Get/csv/getProgram_manual_notParticipant.before.csv"
    )
    @Test
    void getProgram_manual_notParticipant() throws Exception {
        check("MANUAL", "getProgram_manual_notParticipant", status().isOk());
    }

    @DisplayName("Успешно получили информацию, что партнер не может участвовать в программе SORRY")
    @DbUnitDataSet(
            before = "Get/csv/getProgram_sorry_notParticipant.before.csv"
    )
    @Test
    void getProgram_sorry_notParticipant() throws Exception {
        check("SORRY", "getProgram_sorry_notParticipant", status().isOk());
    }

    private void check(String programType, String fileName, ResultMatcher resultMatcher) throws Exception {
        mvc.perform(
                        get("/v1/program")
                                .param("programType", programType)
                                .param("partner_id", "1")
                                .param("color", "WHITE")
                                .param("business_id", "1")
                )
                .andExpect(resultMatcher)
                .andExpect(content().json(
                                loadFile("Get/json/response/" +
                                        fileName + ".json"),
                                true
                        )
                );
    }
}
