package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.servicebus.api.external.vendor.model.schaefer.response.TransportUnitWcsResponse;
import ru.yandex.market.wms.servicebus.api.external.vendor.model.schaefer.response.WcsResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class TransportOrderControllerTest extends VendorApiBaseTest {

    @Test
    public void shouldSuccessCreateTransportOrder() throws Exception {
        mockVendorBackEndResponse(HttpStatus.OK,
                "api/internal/wms/transport-order/create/1/vendor_response.json",
                WcsResponse.class,
                WITHOUT_RETRY_COUNT);

        MvcResult mockResult = mockMvc.perform(post("/transport-order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/create/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/create/1/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/cto",
                "api/internal/wms/transport-order/create/vendor_request.json",
                WITHOUT_RETRY_COUNT);
    }

    @Test
    public void shouldReturnInternalErrorForCreateTransportOrder() throws Exception {
        mockVendorBackEndResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "api/internal/wms/transport-order/create/2/vendor_response.json",
                WcsResponse.class,
                DEFAULT_RETRY_COUNT);

        MvcResult mockResult = mockMvc.perform(post("/transport-order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/create/2/request.json")))
                .andExpect(status().is5xxServerError())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/create/2/response.json")))
                .andReturn();


        assertVendorRequest(HttpMethod.POST,
                "/rpc/cto",
                "api/internal/wms/transport-order/create/vendor_request.json",
                DEFAULT_RETRY_COUNT);
    }

    @Test
    public void shouldReturnBadRequestForCreateTransportOrder() throws Exception {
        mockVendorBackEndResponse(HttpStatus.BAD_REQUEST,
                "api/internal/wms/transport-order/create/3/vendor_response.json",
                WcsResponse.class,
                WITHOUT_RETRY_COUNT);

        MvcResult mockResult = mockMvc.perform(post("/transport-order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/create/3/request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/create/3/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/cto",
                "api/internal/wms/transport-order/create/vendor_request.json",
                WITHOUT_RETRY_COUNT);
    }

    // Delete Transport Order

    @Test
    public void shouldSuccessDeleteTransportOrderSchaefer() throws Exception {
        mockVendorBackEndEmptyResponse(HttpStatus.OK);

        MvcResult mockResult = mockMvc.perform(post("/transport-order/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/delete/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/delete/1/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/dto",
                "api/internal/wms/transport-order/delete/vendor_request.json",
                WITHOUT_RETRY_COUNT);
    }

    @Test
    public void shouldNotDeleteTransportOrderIfWcsResponseInternalErrorSchaefer() throws Exception {
        mockVendorBackEndResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "api/internal/wms/transport-order/delete/2/vendor_response.json",
                WcsResponse.class,
                DELETE_RETRY_COUNT);

        MvcResult mockResult = mockMvc.perform(post("/transport-order/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/delete/2/request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/delete/2/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/dto",
                "api/internal/wms/transport-order/delete/vendor_request.json",
                DELETE_RETRY_COUNT);
    }

    @Test
    public void shouldNotDeleteTransportOrderIfWcsResponseBadRequestErrorSchaefer() throws Exception {
        mockVendorBackEndResponse(HttpStatus.BAD_REQUEST,
                "api/internal/wms/transport-order/delete/3/vendor_response.json",
                WcsResponse.class,
                WITHOUT_RETRY_COUNT);

        MvcResult mockResult = mockMvc.perform(post("/transport-order/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/delete/3/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/delete/3/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/dto",
                "api/internal/wms/transport-order/delete/vendor_request.json",
                WITHOUT_RETRY_COUNT);
    }

    @Test
    public void shouldSuccessDeleteTransportOrderAfterRetryableSchaefer() throws Exception {
        mockVendorBackEndResponse(ImmutableMap.of(
                HttpStatus.INTERNAL_SERVER_ERROR, WcsResponse.builder().code(101).message("Internal error").build(),
                HttpStatus.GATEWAY_TIMEOUT, WcsResponse.builder().code(101).message("Internal error").build(),
                HttpStatus.OK, WcsResponse.builder().build())
        );

        MvcResult mockResult = mockMvc.perform(post("/transport-order/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/delete/4/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/delete/4/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.POST,
                "/rpc/dto",
                "api/internal/wms/transport-order/delete/vendor_request.json",
                DELETE_RETRY_COUNT);
    }

    // Get Transport Unit

    @Test
    public void shouldSuccessGetTransportUnit() throws Exception {
        mockVendorBackEndResponse(HttpStatus.OK,
                "api/internal/wms/transport-order/get/1/vendor_response.json",
                TransportUnitWcsResponse.class,
                WITHOUT_RETRY_COUNT);

        mockMvc.perform(post("/transport-order/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/get/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/get/1/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.GET,
                "/rpc/gtu/L00000000000001",
                "api/internal/wms/transport-order/get/vendor_empty_request.json",
                WITHOUT_RETRY_COUNT);
    }

    @Test
    public void shouldReturnInternalErrorForGetTransportUnit() throws Exception {
        mockVendorBackEndResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "api/internal/wms/transport-order/get/2/vendor_response.json",
                WcsResponse.class,
                DEFAULT_RETRY_COUNT);

        mockMvc.perform(post("/transport-order/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/get/2/request.json")))
                .andExpect(status().is5xxServerError())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/get/2/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.GET,
                "/rpc/gtu/T0000000001",
                "api/internal/wms/transport-order/get/vendor_empty_request.json",
                DEFAULT_RETRY_COUNT);
    }

    @Test
    public void shouldReturnBadRequestForGetTransportUnit() throws Exception {
        mockVendorBackEndResponse(HttpStatus.BAD_REQUEST,
                "api/internal/wms/transport-order/get/3/vendor_response.json",
                WcsResponse.class,
                WITHOUT_RETRY_COUNT);

        mockMvc.perform(post("/transport-order/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/get/3/request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("api/internal/wms/transport-order/get/3/response.json")))
                .andReturn();

        assertVendorRequest(HttpMethod.GET,
                "/rpc/gtu/L0000000001",
                "api/internal/wms/transport-order/get/vendor_empty_request.json",
                WITHOUT_RETRY_COUNT);
    }

    @Test
    public void shouldReturnBadRequestWhenIdIsBlank() throws Exception {
        mockMvc.perform(post("/transport-order/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/get/4/request.json")))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void shouldReturnBadRequestWhenIdIsNull() throws Exception {
        mockMvc.perform(post("/transport-order/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/get/5/request.json")))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void shouldReturnValidationErrorIfTransportIdLongerThanToBe() throws Exception {
        mockMvc.perform(post("/transport-order/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/transport-order/get/6/request.json")))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }
}
