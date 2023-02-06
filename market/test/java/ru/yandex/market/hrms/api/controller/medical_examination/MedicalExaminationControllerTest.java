package ru.yandex.market.hrms.api.controller.medical_examination;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "MedicalExaminationControllerTest.before.csv")
public class MedicalExaminationControllerTest extends AbstractApiTest {

    @Test
    void loadExcelWithErrorTest() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "testMoExcelDuplicates.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("testMoExcelDuplicates.xlsx")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/medical-examination/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(loadFromFile("duplication_error.json"), false));
    }

    @Test
    @DbUnitDataSet(after = "MedicalExaminationControllerTest.after.csv")
    void loadExcelSuccessfullyTest() throws Exception {

        mockClock(LocalDateTime.of(2022, 5, 30, 10, 0, 0));

        var file = new MockMultipartFile(
                "file",
                "testMoExcelOk.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("testMoExcelOk.xlsx")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/medical-examination/upload")
                        .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.update.before.csv",
            after = "MedicalExaminationControllerTest.update.after.csv")
    void loadExcelWithUpdateRowsTest() throws Exception {

        mockClock(LocalDateTime.of(2022, 6, 1, 10, 0, 0));

        var file = new MockMultipartFile(
                "file",
                "testMoExcelOk.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("testMoExcelOk.xlsx")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/medical-examination/upload")
                        .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.load_info.before.csv")
    void loadSofMedicalInfoTest() throws Exception {
        mockClock(LocalDate.of(2021, 2, 2));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination")
                        .param("domainId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("all_employees_sof.json"), true));

    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.load_info.before.csv")
    void loadTomMedicalInfoTest() throws Exception {
        mockClock(LocalDate.of(2021, 2, 2));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination")
                        .param("domainId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("all_employees_tom.json"), true));

    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.load_info.before.csv")
    void loadSofMedicalInfoWithNameFilterTest() throws Exception {
        mockClock(LocalDate.of(2021, 2, 2));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination")
                        .param("domainId", "1")
                        .param("employeeName", "Гелл")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("filter_by_name_sof.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.load_info.before.csv")
    void loadSofMedicalInfoWithStaffLoginFilterTest() throws Exception {
        mockClock(LocalDate.of(2021, 2, 2));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination")
                        .param("domainId", "1")
                        .param("employeeName", "test-staff3")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("filter_by_staff_login_sof.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.load_info.before.csv")
    void getMedExamTypesTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination/types"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("medical_examination_types.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.load_info.before.csv")
    void getMedReferralStatusesTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/medical-examination-referral/statuses"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("medical_referral_statuses.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.load_info.before.csv",
            after = "MedicalExaminationControllerTest.single_upload.after.csv")
    void shouldUploadOneRow() throws Exception {
        mockClock(LocalDate.of(2022, 6, 2));

        mockMvc.perform(MockMvcRequestBuilders.post("/lms/medical-examination/single-upload")
                        .param("domainId", "1")
                        .param("employeeId", "1")
                        .param("medicalExamDate", "2022-02-10")
                        .param("hygieneExamDate", "2022-02-10")
                        .param("psychoExamDate", "2022-01-20")
                        .param("status", "Годен")
                        .cookie(new Cookie("yandex_login", "pshevlyakova")))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerTest.load_info.before.csv",
            after = "MedicalExaminationControllerTest.load_info.before.csv")
    void shouldUploadOneRowWithEmptyDate() throws Exception {
        mockClock(LocalDate.of(2022, 6, 2));

        mockMvc.perform(MockMvcRequestBuilders.post("/lms/medical-examination/single-upload")
                        .param("domainId", "1")
                        .param("employeeId", "1")
                        .param("medicalExamDate", "2021-12-08")
                        .param("psychoExamDate", "2021-12-08")
                        .param("status", "Годен")
                        .cookie(new Cookie("yandex_login", "pshevlyakova")))
                .andExpect(status().isOk());
    }
}
