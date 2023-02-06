package ru.yandex.market.delivery.rupostintegrationapp.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.rupostintegrationapp.BaseContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PingTest extends BaseContextualTest {
    private static final String PING_RESULT = "0;Ok";

    @Test
    @DisplayName("Успешный пинг")
    void pingIsHealthy() throws Exception {
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string(PING_RESULT));
    }
}
