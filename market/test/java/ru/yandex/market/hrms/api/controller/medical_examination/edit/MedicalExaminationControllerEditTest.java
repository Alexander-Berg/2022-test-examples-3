package ru.yandex.market.hrms.api.controller.medical_examination.edit;

import java.time.LocalDate;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "MedicalExaminationControllerEditTest.before.csv")
public class MedicalExaminationControllerEditTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(after = "MedicalExaminationControllerEditTest.after.csv")
    void deleteMedicalReferral() throws Exception {
        mockClock(LocalDate.of(2022, 6, 5));

        mockMvc.perform(MockMvcRequestBuilders.delete("/lms/medical-examination-referral/10")
                        .cookie(new Cookie("yandex_login", "pshevlyakova")))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(after = "MedicalExaminationControllerEditTest.update.after.csv")
    void editMedicalReferral() throws Exception {
        mockClock(LocalDate.of(2022, 6, 5));

        mockMvc.perform(MockMvcRequestBuilders.post("/lms/medical-examination-referral/10")
                        .param("domainId", "1")
                        .param("medExaminationDate", "2022-06-20")
                        .param("medExaminationTypes", "ANNUAL_MED_EXAM,HYGIENE_MED_EXAM")
                        .cookie(new Cookie("yandex_login", "pshevlyakova")))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("medical_referral_update_answer.json"), true));
    }
}
