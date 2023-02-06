package ru.yandex.market.replenishment.autoorder.api.security;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("tvm-testing")
public class TvmSecurityOnTest extends FunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testTvmSecurityIsOnHelperEndpointIsOk() throws Exception {
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/pagematch"))
            .andExpect(status().isOk());
    }

    @Test
    public void testTvmSecurityIsOnRestrictedEndpointIsUnavailable() throws Exception {
        mockMvc.perform(get("/correction-reasons"))
            .andExpect(status().is4xxClientError());
    }
}


