package ru.yandex.market.hrms.api.controller.access;

import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccessControlControllerTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "AccessControlControllerTest.before.csv")
    void getPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/access-control")
                .queryParam("page", "0")
                .queryParam("size", "10")
                .queryParam("domainId", "1")
                .queryParam("groupId", "1")
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(loadFromFile("AccessControlControllerTestGetPage.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "AccessControlControllerTest.before.csv")
    @DbUnitDataSet(before = "AccessControlControllerTestHistory.before.csv")
    void getById() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/access-control/1")
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(loadFromFile("AccessControlControllerTestGetByEmployeeId.json"), true));
    }

    //время смещено из за настроек таймзоны для тестов на чаза вперед!!!
    @Test
    @DbUnitDataSet(before = "AccessControlControllerTest.before.csv",
                   after = "AccessControlControllerTestSkud.after.csv")
    void changeSkudSettings() throws Exception {
        mockClock(LocalDateTime.parse("2021-11-02T11:00:00"));
        mockMvc.perform(MockMvcRequestBuilders
                .post("/lms/access-control/skud")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("""
                        {
                          "employeeId": 1,
                          "skudEnabled": true,
                          "passId": 123,
                          "isUnlimited": false,
                          "expired": "2021-11-05T16:00:00"
                        }
                        """)
        )
                .andExpect(status().isOk());
    }


    //время смещено из за настроек таймзоны для тестов на чаза вперед!!!
    @Test
    @DbUnitDataSet(before = "AccessControlControllerTest.before.csv",
            after = "AccessControlControllerTestBiometrics.after.csv")
    void changeBiometricsSettings() throws Exception {
        mockClock(LocalDateTime.parse("2021-11-02T11:00:00"));
        mockMvc.perform(MockMvcRequestBuilders
                .post("/lms/access-control/biometrics")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("""
                        {
                          "employeeId": 1,
                          "biometricsEnabled": true
                        }
                        """)
        )
                .andExpect(status().isOk());
    }
}
