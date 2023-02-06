package ru.yandex.market.mboc.app.security;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.mboc.app.BaseWebIntegrationTestClass;
import ru.yandex.market.mboc.app.tvm.TvmIncomingRequestFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TvmServiceTicketsTest extends BaseWebIntegrationTestClass {

    @Value("${idm.tvm-service-id}")
    private int idmClientId;

    @Override
    public void setupUser() {
        // No setup for this test, it's about security so it'll use it's one
    }

    @Test
    public void mbocNotCalledForOtherHandles() throws Exception {
        mvc.perform(
            get("/ping")
                .header("Authorization", "test"))
            .andExpect(status().isOk());
    }

    @Test
    public void mbocNotCalledForIdmIfNoHeaderPassedOtherHandles() throws Exception {
        mvc.perform(
            get(TvmIncomingRequestFilter.IDM_HTTP + "/info/")
                .header("Authorization", "test"))
            .andExpect(status().isOk());
    }

    @Test
    public void idmHandleAllowsOnlyIdmWithServiceCorrectTicket() throws Exception {
        mvc.perform(
            get(TvmIncomingRequestFilter.IDM_HTTP)
                .header(TvmIncomingRequestFilter.SERVICE_TICKET_HEADER, "invalid"))
            .andExpect(status().isForbidden());
        mvc.perform(
            get(TvmIncomingRequestFilter.IDM_HTTP + "/")
                .header(TvmIncomingRequestFilter.SERVICE_TICKET_HEADER, idmClientId + 1))
            .andExpect(status().isForbidden());
        // ok
        mvc.perform(
            get(TvmIncomingRequestFilter.IDM_HTTP + "/get-all-roles/")
                .header(TvmIncomingRequestFilter.SERVICE_TICKET_HEADER, idmClientId))
            .andExpect(status().isOk());
    }

}
