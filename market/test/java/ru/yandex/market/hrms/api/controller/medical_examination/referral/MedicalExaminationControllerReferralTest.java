package ru.yandex.market.hrms.api.controller.medical_examination.referral;

import java.time.LocalDate;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "../MedicalExaminationReferral.before.csv")
public class MedicalExaminationControllerReferralTest extends AbstractApiTest {

    @Test
    void shouldLoadMedicalReferralsPageTest() throws Exception {
        mockClock(LocalDate.of(2022, 6, 1));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination-referral")
                        .param("domainId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("medical_referral_page.json"), true));
    }

    @Test
    void loadMedicalExaminationReferralWithNameFilterTest() throws Exception {
        mockClock(LocalDate.of(2022, 6, 1));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination-referral")
                        .param("domainId", "1")
                        .param("employeeName", "Буффе")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("medical_referral_name_filter_page.json"), true));
    }

    @Test
    void loadMedicalExaminationReferralWithStatusFilterTest() throws Exception {
        mockClock(LocalDate.of(2022, 6, 1));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination-referral")
                        .param("domainId", "2")
                        .param("status", "WAITING")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("medical_referral_status_filter_page.json"), true));
    }

    @Test
    void shouldSuggestMeByStaffLogin() throws Exception {
        mockClock(LocalDate.of(2022, 6, 1));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination/suggest")
                        .param("domainId", "1")
                        .param("staffLogin", "test-staff-")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("staff_login_suggest.json"), true));
    }

    @Test
    @DbUnitDataSet(after = "MedicalExaminationReferral.add_referral.after.csv")
    void shouldAddPersonToReferral() throws Exception {
        mockClock(LocalDate.of(2022, 6, 10));

        mockMvc.perform(MockMvcRequestBuilders.post("/lms/medical-examination-referral")
                        .param("domainId", "1")
                        .param("medicalExaminationDate", "2022-07-30")
                        .param("medicalExaminationTypes", "ANNUAL_MED_EXAM,PSYCHO_MED_EXAM")
                        .param("medicalExaminationIds", "9,10")
                        .cookie(new Cookie("yandex_login", "pshevlyakova"))
                )
                .andExpect(status().isOk());
    }
}
