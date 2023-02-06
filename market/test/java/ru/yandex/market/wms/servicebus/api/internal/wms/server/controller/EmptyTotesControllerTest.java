package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.servicebus.api.external.vendor.model.schaefer.response.WcsResponse;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class EmptyTotesControllerTest extends VendorApiBaseTest {

    @Autowired
    private DbConfigService dbConfigService;

    @Test
    public void shouldSuccessRequestEmptyToteWithoutNesting() throws Exception {
        final int toteCount = 11;

        mockVendorBackEndResponse(HttpStatus.OK,
                "api/internal/wms/empty-tote/1/vendor_response.json",
                WcsResponse.class,
                toteCount);

        mockMvc.perform(post("/emptytotes/request-empty-totes-dematic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/empty-tote/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/empty-tote/1/response.json")))
                .andReturn();
    }

    @Test
    public void shouldSuccessRequestEmptyToteWithNesting() throws Exception {
        final int toteCount = 11;
        when(dbConfigService.getConfigAsBoolean(eq("YM_DEMATIC_NESTING_ENABLED"), eq(false))).thenReturn(true);

        mockVendorBackEndResponse(HttpStatus.OK,
                "api/internal/wms/empty-tote/1/vendor_response.json",
                WcsResponse.class,
                toteCount / 2 + 1);

        mockMvc.perform(post("/emptytotes/request-empty-totes-dematic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/empty-tote/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/empty-tote/1/response.json")))
                .andReturn();
    }

    @Test
    public void shouldThrowErrorRequestEmptyTote() throws Exception {
        final int toteCount = 11;

        mockVendorBackEndResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "api/internal/wms/empty-tote/2/vendor_response.json",
                WcsResponse.class,
                toteCount);

        mockMvc.perform(post("/emptytotes/request-empty-totes-dematic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/empty-tote/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/empty-tote/2/response.json")))
                .andReturn();
    }
}
