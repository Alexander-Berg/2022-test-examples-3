package ru.yandex.market.hrms.api.controller.sc;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "ScUserControllerTest.before.csv")
public class ScUserControllerTest extends AbstractApiTest {

    @Test
    void getEmployeeBySc() throws Exception {
        mockClock(LocalDateTime.of(2022, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/sc/qr-login")
                        .queryParam("loginQuery", "svs-ser1710@hrms-sc.ru")
                        .queryParam("domainId", "1")
                ).andExpect(status().isOk())
                .andExpect(content().json("""
                         {
                         "id":1749,
                         "name":"Алёна Юдина",
                         "position":"Кладовщик",
                         "staff":"sof-alena-yudi",
                         "groupTitle":"",
                         "fired":false,
                         "newbie":false,
                         "isOffice":false,
                         "scLogin":"svs-ser1710@hrms-sc.ru"
                         }
                        """));
    }

    @Test
    void getOutstaffBySc() throws Exception {
        mockClock(LocalDateTime.of(2022, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/sc/qr-login")
                        .queryParam("loginQuery", "konstantin@hrms-sc.ru")
                        .queryParam("domainId", "2")
                ).andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                        "isActive":true,
                        "active":true,
                        "id":101,
                        "name":"вассерман андрей иванович",
                        "position":"Оператор прт",
                        "wms":"",
                        "phoneNumber":"+79153453432",
                        "urlPhoto":"fake/bucket1/photo2.jpg",
                        "workStarted":"2021-07-05",
                        "company":{
                                    "id":326611127,
                                    "name":"Company #test-login",
                                    "code":"tt"},
                        "scLogin":"konstantin@hrms-sc.ru"
                        }
                        """));
    }
}
