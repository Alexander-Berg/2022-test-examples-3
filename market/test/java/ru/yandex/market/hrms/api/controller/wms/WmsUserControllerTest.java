package ru.yandex.market.hrms.api.controller.wms;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "WmsUsersControllerTest.before.csv")
public class WmsUserControllerTest extends AbstractApiTest {



    @Test
    void getEmployeeByWms() throws Exception {
        mockClock(LocalDateTime.of(2022, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/wms/qr-login")
                        .queryParam("loginQuery", "sof-alena-yudi")
                        .queryParam("domainId", "1")
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("ale.json"), false));
    }

    @Test
    void getOutstaffByWms() throws Exception {
        mockClock(LocalDateTime.of(2022, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/wms/qr-login")
                        .queryParam("loginQuery", "sof-vasserman")
                        .queryParam("domainId", "2")
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("waserman.json"), false));
    }
}
