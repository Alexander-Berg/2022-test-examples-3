package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.servicebus.api.external.vendor.model.schaefer.response.WcsResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class SorterOrderControllerTest extends VendorApiBaseTest {

    @Test
    public void shouldSuccessCreateSorterOrder() throws Exception {
        mockVendorBackEndResponse(HttpStatus.OK,
                "api/internal/wms/sorter-order/1/vendor_response.json",
                WcsResponse.class,
                WITHOUT_RETRY_COUNT);

        mockMvc.perform(post("/sorter-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/sorter-order/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/sorter-order/1/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/cso",
                "api/internal/wms/sorter-order/1/vendor_request.json",
                WITHOUT_RETRY_COUNT);
    }

    @Test
    public void shouldReturnInternalErrorForCreateTransportOrder() throws Exception {
        mockVendorBackEndResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "api/internal/wms/sorter-order/2/vendor_response.json",
                WcsResponse.class,
                DEFAULT_RETRY_COUNT);

        mockMvc.perform(post("/sorter-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/sorter-order/2/request.json")))
                .andExpect(status().is5xxServerError())
                .andExpect(content().json(getFileContent("api/internal/wms/sorter-order/2/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/cso",
                "api/internal/wms/sorter-order/2/vendor_request.json",
                DEFAULT_RETRY_COUNT);
    }

    @Test
    public void shouldReturnBadRequestForCreateTransportOrder() throws Exception {
        mockVendorBackEndResponse(HttpStatus.BAD_REQUEST,
                "api/internal/wms/sorter-order/3/vendor_response.json",
                WcsResponse.class,
                WITHOUT_RETRY_COUNT);

        mockMvc.perform(post("/sorter-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/sorter-order/3/request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("api/internal/wms/sorter-order/3/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/cso",
                "api/internal/wms/sorter-order/3/vendor_request.json",
                WITHOUT_RETRY_COUNT);
    }
}
