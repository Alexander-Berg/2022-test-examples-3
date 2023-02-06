package ru.yandex.market.checkout.pushapi.health;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PushApiHealthControllerTest extends AbstractWebTestBase {
    @Test
    public void smokeTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/health/solomon"))
                .andDo(log())
                .andExpect(status().isOk());
    }
}
