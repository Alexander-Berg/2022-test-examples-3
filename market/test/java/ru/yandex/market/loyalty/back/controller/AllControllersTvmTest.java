package ru.yandex.market.loyalty.back.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.back.config.BackTestConfig.TestTvmTicketProvider.TestMode.DEFAULT;
import static ru.yandex.market.loyalty.back.config.BackTestConfig.TestTvmTicketProvider.TestMode.FAIR_CHECK;
import static ru.yandex.market.loyalty.back.security.TvmHandler.TVM_HEADER;

@TestFor(CoinsController.class)
public class AllControllersTvmTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private CoinsController coinsController;

    @Before
    public void reloadFutureCoins() {
        coinsController.reloadFutureCoins();
    }

    @Test
    public void shouldNotAuthorizeIfNoTvmTicketPresentInFairCheckMode() throws Exception {
        testTvmTicketProvider.setTestMode(FAIR_CHECK);

        mockMvc.perform(get("/coins/person?uid=1231")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldNotAuthorizeIfInvalidTvmTicketPresentInFairCheckMode() throws Exception {
        testTvmTicketProvider.setTestMode(FAIR_CHECK);

        mockMvc.perform(get("/coins/person?uid=1231")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TVM_HEADER, Collections.singleton("invalid_ticket")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldAuthorizeIfInvalidTvmTicketPresentInAllowAllMode() throws Exception {
        testTvmTicketProvider.setTestMode(DEFAULT);

        mockMvc.perform(get("/coins/person?uid=1231")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TVM_HEADER, Collections.singleton("invalid_ticket")))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldAuthorizeIfTvmTicketNotPresentInAllowAllMode() throws Exception {
        testTvmTicketProvider.setTestMode(DEFAULT);

        mockMvc.perform(get("/coins/person?uid=1231")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldAuthorizeIfValidTvmTicketPresentInFairCheckMode() throws Exception {
        testTvmTicketProvider.setTestMode(FAIR_CHECK);

        mockMvc.perform(get("/coins/person?uid=1231")
                .contentType(MediaType.APPLICATION_JSON)
                .header(TVM_HEADER, Collections.singleton("test")))
                .andExpect(status().isOk());
    }

}
