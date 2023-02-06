package ru.yandex.market.hrms.api.controller.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DomainControllerTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "getDomains.before.csv")
    void getDomainsTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/domains"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("domains.json"), false));
    }
}
