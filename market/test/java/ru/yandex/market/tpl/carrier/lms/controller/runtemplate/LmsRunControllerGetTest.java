package ru.yandex.market.tpl.carrier.lms.controller.runtemplate;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.carrier.lms.controller.LmsControllerTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LmsRunControllerGetTest extends LmsControllerTest {

    @SneakyThrows
    @Test
    void shouldReturn404OnNotFound() {
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/carrier/run-templates/{id}", 404))
                .andExpect(status().isNotFound());
    }
}
