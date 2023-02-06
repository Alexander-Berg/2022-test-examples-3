package ru.yandex.market.hrms.api.controller.health;

import org.junit.jupiter.api.Test;

import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PageMatcherControllerTest extends AbstractApiTest {

    @Test
    void shouldReturnOkOnPageMatchRequest() throws Exception {
        mockMvc.perform(get("/pagematch"))
                .andExpect(status().isOk());
    }
}
