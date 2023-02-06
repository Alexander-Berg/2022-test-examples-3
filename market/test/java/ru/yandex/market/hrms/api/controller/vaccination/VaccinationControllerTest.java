package ru.yandex.market.hrms.api.controller.vaccination;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.domain.vaccination.repo.Vaccination;
import ru.yandex.market.hrms.core.domain.vaccination.repo.VaccinationRepo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VaccinationControllerTest extends AbstractApiTest {

    @Autowired
    private VaccinationRepo vaccinationRepo;
    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.before.csv")
    void getVaccinations() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(get("/lms/vaccination"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("VaccinationControllerTest.GetVaccinations.after.json")));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.before.csv")
    void getVaccination() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(get("/lms/vaccination/3"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("VaccinationControllerTest.GetVaccination.after.json")));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.before.csv")
    void setFactDate1() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(put("/lms/vaccination/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "id": 1,
                            "employeeId": 1,
                            "oldFactDate1": null,
                            "oldFactDate2": null,
                            "employeeName": "Алиса",
                            "staffLogin": "login1",
                            "position": "Кладовщик",
                            "shift": "",
                            "planDate1": null,
                            "factDate1": "2021-07-02",
                            "planDate2": null,
                            "factDate2": null,
                            "nonappearances": 0
                        }
                        """))
                .andExpect(status().isOk());

        Vaccination vaccination = vaccinationRepo.findByEmployeeId(1).get();
        Assertions.assertEquals(vaccination.getFactDate1(), LocalDate.of(2021, 7, 2));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.before.csv")
    void setFactDate1AndFactDate2() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(put("/lms/vaccination/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "id": 1,
                            "employeeId": 1,
                            "oldFactDate1": null,
                            "oldFactDate2": null,
                            "employeeName": "Алиса",
                            "staffLogin": "login1",
                            "position": "Кладовщик",
                            "shift": "",
                            "planDate1": null,
                            "factDate1": "2021-07-02",
                            "planDate2": null,
                            "factDate2": "2021-07-23",
                            "nonappearances": 0
                        }
                        """))
                .andExpect(status().isOk());

        Optional<Vaccination> vaccination = vaccinationRepo.findByEmployeeId(1);
        Assertions.assertTrue(vaccination.isPresent());
        Assertions.assertEquals(vaccination.get().getFactDate1(), LocalDate.of(2021, 7, 2));
        Assertions.assertEquals(vaccination.get().getFactDate2(), LocalDate.of(2021, 7, 23));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.before.csv")
    void setFactDate1WhenFactDate2NotNull() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(put("/lms/vaccination/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "id": 3,
                            "employeeId": 3,
                            "oldFactDate1": "2021-07-02",
                            "oldFactDate2": "2021-07-23",
                            "employeeName": "Виктор",
                            "staffLogin": "login3",
                            "position": "Бригадир",
                            "shift": "",
                            "planDate1": null,
                            "factDate1": "2021-07-01",
                            "planDate2": null,
                            "factDate2": "2021-07-23",
                            "nonappearances": 0
                        }
                        """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.before.csv")
    void setFactDate2WhenFactDate1Null() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(put("/lms/vaccination/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "id": 1,
                            "employeeId": 1,
                            "oldFactDate1": null,
                            "oldFactDate2": null,
                            "employeeName": "Алиса",
                            "staffLogin": "login1",
                            "position": "Кладовщик",
                            "shift": "",
                            "planDate1": null,
                            "factDate1": null,
                            "planDate2": null,
                            "factDate2": "2021-07-23",
                            "nonappearances": 0
                        }
                        """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.V2.before.csv")
    void getVaccinationsV2() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(get("/lms/vaccination-v2?domainId=1&showFullyVaccinated=true"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("VaccinationControllerTest.GetVaccinationsV2.after.json")));

    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.V2.before.csv")
    void setVaccinationFactDate2() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(put("/lms/vaccination-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "id": 2,
                            "employeeView": {
                                "id": 2
                            },
                            "datePlan1": "2021-06-30",
                            "dateFact1": "2021-06-30",
                            "datePlan2": null,
                            "dateFact2": "2021-07-23",
                            "dateFired": null,
                            "medicalOutlet": null,
                            "nonappearances": 1
                        }
                        """))
                .andExpect(status().isOk());

        Optional<Vaccination> vaccination = vaccinationRepo.findByEmployeeId(2);
        Assertions.assertTrue(vaccination.isPresent());
        Assertions.assertEquals(vaccination.get().getFactDate2(), LocalDate.of(2021, 7, 23));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.V2.before.csv")
    void setVaccinationFactDate1NullAndFactDate2NotNull() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(put("/lms/vaccination-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "id": 2,
                            "employeeView": {
                                "id": 2
                            },
                            "datePlan1": null,
                            "dateFact1": null,
                            "datePlan2": null,
                            "dateFact2": "2021-08-01",
                            "dateFired": null,
                            "medicalOutlet": null,
                            "nonappearances": 1
                        }
                        """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "VaccinationControllerTest.V2.before.csv")
    void setVaccinationDifferenceBetweenFactDate2AndFactDate1LessThan21() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        mockMvc.perform(put("/lms/vaccination-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "id": 2,
                            "employeeView": {
                                "id": 2
                            },
                            "datePlan1": null,
                            "dateFact1": "2021-08-01",
                            "datePlan2": null,
                            "dateFact2": "2021-08-01",
                            "dateFired": null,
                            "medicalOutlet": null,
                            "nonappearances": 1
                        }
                        """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "VaccinationScheduleExcelTest.before.csv")
    void getVaccinationScheduleExcelInFile() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        MvcResult result = mockMvc.perform(get("/lms/vaccination/excel?domainId=1&showFullyVaccinated=true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''%D0%92%D0%B0%D0%BA%D1%86%D0%B8%D0%BD%D0%B0%D1%86%D0%B8%D0%B8_" + LocalDate.now(clock) + ".xlsx"))
                .andReturn();

        File resultFile = new File("vaccination_result.xlsx");
        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            IOUtils.copy(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()), fos);
        }
    }

    @Test
    @DbUnitDataSet(before = "VaccinationScheduleExcelTest.before.csv")
    void getVaccinationScheduleExcelInFileWithoutFullyVaccinated() throws Exception {
        mockClock(LocalDate.of(2021, 8, 1));
        MvcResult result = mockMvc.perform(get("/lms/vaccination/excel?domainId=1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''%D0%92%D0%B0%D0%BA%D1%86%D0%B8%D0%BD%D0%B0%D1%86%D0%B8%D0%B8_" + LocalDate.now(clock) + ".xlsx"))
                .andReturn();

        File resultFile = new File("vaccination_result_without_fully_vaccinated.xlsx");
        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            IOUtils.copy(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()), fos);
        }
    }
}
