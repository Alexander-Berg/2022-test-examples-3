package ru.yandex.market.hrms.api.controller.ispring;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "ISpringControllerGetSmsLogTest.before.csv")
public class ISpringControllerGetSmsLogTest extends AbstractApiTest {

    @Test
    public void should_GetLogs_When_IspringIdFound() throws Exception {
        mockMvc.perform(get("/lms/ispring/sms/log")
                        .queryParam("ispringId", "id1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("get_sms_logs_list_result.json")));
    }

    @Test
    public void should_ReturnEmpty_When_IspringIdNotFound() throws Exception {
        mockMvc.perform(get("/lms/ispring/sms/log")
                        .queryParam("ispringId", "id4"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("get_sms_logs_empty.json")));
    }
}
