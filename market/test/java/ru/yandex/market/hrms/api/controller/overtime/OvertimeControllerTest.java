package ru.yandex.market.hrms.api.controller.overtime;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "OvertimeControllerTest.before.csv")
class OvertimeControllerTest extends AbstractApiTest {

    @BeforeEach
    void before() {
        mockClock(LocalDateTime.of(2021, 4, 11, 15, 0));
    }

    @Test
    void getAllOvertimes() throws Exception {
        mockMvc.perform(get("/lms/overtimes"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OvertimeControllerTest.getAllOvertimes.response.json"), true));
    }

    @Test
    void getNewOvertimePage() throws Exception {
        mockMvc.perform(get("/lms/overtimes/new"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OvertimeControllerTest.getNewOvertimePage.response.json"), true));
    }

    @Test
    void getCurrentOvertime() throws Exception {
        mockMvc.perform(get("/lms/overtimes/100"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OvertimeControllerTest.getCurrentOvertime.response.json"), true));
    }

    @Test
    void getPlannedOvertime() throws Exception {
        mockMvc.perform(get("/lms/overtimes/101"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OvertimeControllerTest.getPlannedOvertime.response.json"), true));
    }

    @Test
    @DbUnitDataSet(after = "OvertimeControllerTest.deletePlannedOvertime.after.csv")
    void deletePlannedOvertime() throws Exception {
        mockMvc.perform(delete("/lms/overtimes/101").cookie(new Cookie("yandex_login", "antipov93")))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(after = "OvertimeControllerTest.before.csv")
    void deleteCurrentOvertime() throws Exception {
        mockMvc.perform(delete("/lms/overtimes/100").cookie(new Cookie("yandex_login", "antipov93")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(after = "OvertimeControllerTest.createOvertime.after.csv")
    void createOvertime() throws Exception {
        mockMvc.perform(post("/lms/overtimes")
                .cookie(new Cookie("yandex_login", "antipov93"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {
                           "date": "2021-04-12",
                           "shiftType": "DAY"
                         }
                         """)
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(after = "OvertimeControllerTest.before.csv")
    void createOvertimeInPast() throws Exception {
        mockMvc.perform(post("/lms/overtimes")
                .cookie(new Cookie("yandex_login", "antipov93"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {
                           "date": "2021-04-11",
                           "shiftType": "DAY"
                         }
                         """)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(after = "OvertimeControllerTest.before.csv")
    void createDuplicatedOvertime() throws Exception {
        mockClock(LocalDate.of(2021, 4, 4));
        mockMvc.perform(post("/lms/overtimes")
                .cookie(new Cookie("yandex_login", "antipov93"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {
                           "date": "2021-04-11",
                           "shiftType": "DAY"
                         }
                         """)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "OvertimeControllerTest.overtimeEnabled.before.csv")
    void overtimeEnabled() throws Exception {
        mockMvc.perform(get("/lms/overtimes/enabled"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DbUnitDataSet(before = "OvertimeControllerTest.overtimeDisabled.before.csv")
    void overtimeDisabled() throws Exception {
        mockMvc.perform(get("/lms/overtimes/enabled"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void overtimeDoesNotSet() throws Exception {
        mockMvc.perform(get("/lms/overtimes/enabled"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
