package ru.yandex.market.hrms.tms.health;

import org.junit.jupiter.api.Test;

import ru.yandex.market.hrms.tms.AbstractTmsTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TmsJobStatusControllerTest extends AbstractTmsTest {
    @Test
    void shouldReturnSomethingOnJobStatus() throws Exception {
        mockMvc.perform(get("/jobStatus"))
                .andExpect(status().isOk());
    }
}
