package ru.yandex.market.pers.address.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.pers.address.util.BaseWebTest;
import org.junit.jupiter.api.Test;


import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.address.config.TestConfig.TestTvmRequestAuthHandler.TestMode.DEFAULT;
import static ru.yandex.market.pers.address.config.TestConfig.TestTvmRequestAuthHandler.TestMode.FAIR_CHECK;

public class AllControllersTvmTest extends BaseWebTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldNotAuthorizeIfNoTvmTicketPresentInFairCheckMode() throws Exception {
        testTvmRequestAuthHandler.setTestMode(FAIR_CHECK);

        mockMvc.perform(get("/presets/uid/100/blue?regionId=213")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldNotAuthorizeIfInvalidTvmTicketPresentInFairCheckMode() throws Exception {
        testTvmRequestAuthHandler.setTestMode(FAIR_CHECK);

        mockMvc.perform(get("/presets/uid/100/blue?regionId=213")
                .contentType(MediaType.APPLICATION_JSON_UTF8).header("X-Ya-Service-Ticket", Collections.singleton("invalid_ticket")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldAuthorizeIfInvalidTvmTicketPresentInAllowAllMode() throws Exception {
        testTvmRequestAuthHandler.setTestMode(DEFAULT);

        mockMvc.perform(get("/presets/uid/100/blue?regionId=213")
                .contentType(MediaType.APPLICATION_JSON_UTF8).header("X-Ya-Service-Ticket", Collections.singleton("invalid_ticket")))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldAuthorizeIfTvmTicketNotPresentInAllowAllMode() throws Exception {
        testTvmRequestAuthHandler.setTestMode(DEFAULT);

        mockMvc.perform(get("/presets/uid/100/blue?regionId=213")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldAuthorizeIfValidTvmTicketPresentInFairCheckMode() throws Exception {
        testTvmRequestAuthHandler.setTestMode(FAIR_CHECK);

        mockMvc.perform(get("/presets/uid/100/blue?regionId=213")
                .contentType(MediaType.APPLICATION_JSON_UTF8).header("X-Ya-Service-Ticket", Collections.singleton("test")))
                .andExpect(status().isOk());
    }

}
