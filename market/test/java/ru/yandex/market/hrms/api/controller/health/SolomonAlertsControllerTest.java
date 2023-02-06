package ru.yandex.market.hrms.api.controller.health;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SolomonAlertsControllerTest extends AbstractApiTest {

    @Test
    void solomonAlertsShouldReturnOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/health/solomon"))
                .andExpect(status().isOk());
    }
}
