package ru.yandex.market.shopadminstub.health;

import org.junit.jupiter.api.Test;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ShopadminStubHealthControllerTest extends AbstractTestBase {
    @Test
    public void smokeTest() throws Exception {
        mockMvc.perform(get("/health/solomon"))
                .andDo(log())
                .andExpect(status().isOk());
    }
}
