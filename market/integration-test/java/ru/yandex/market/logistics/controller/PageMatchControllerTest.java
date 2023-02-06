package ru.yandex.market.logistics.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfiguration.class)
@DisplayName("Unit-test PageMatchControllerTest")
class PageMatchControllerTest {
    @Autowired
    protected MockMvc mockMvc;

    @Test
    @DisplayName("Проверка работы матчинга доступных методов")
    void methodsMatched() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/pageMatch"))
            .andExpect(status().isOk())
            .andExpect(content().string(
                "GET_pageMatch\tGET:/pageMatch\ttest-api-name\n"
                    + "GET_tests_entity_by-id_id\tGET:/tests/entity/by-id/<id>\ttest-api-name\n"
                    + "GET_tests_ping\tGET:/tests/ping\ttest-api-name\n"
                    + "POST_tests_save\tPOST:/tests/save\ttest-api-name"
            ));
    }
}
