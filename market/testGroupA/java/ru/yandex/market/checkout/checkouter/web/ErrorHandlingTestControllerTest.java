package ru.yandex.market.checkout.checkouter.web;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ErrorHandlingTestControllerTest extends AbstractWebTestBase {

    @Test
    public void shouldHandleNullPointerCorrectly() throws Exception {
        mockMvc.perform(get("/test/npe"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    public void shouldHandleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/badRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("illegal argument exception"));
    }

    @Test
    public void shouldHandleNumberFormatException() throws Exception {
        mockMvc.perform(get("/test/numberFormatException/shouldBeNumber"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FORMAT"));
    }

    @Test
    public void shouldHandleConversionNotSupportedException() throws Exception {
        mockMvc.perform(get("/test/conversionNotSupportedException/shouldBeNumber"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONVERSION_NOT_SUPPORTED"));
    }


    @Test
    public void shouldHandleMismatchingMethod() throws Exception {
        mockMvc.perform(get("/test/postOnly"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("HTTP_METHOD_NOT_SUPPORTED"));
    }

    @Test
    public void missingRequiredParameter() throws Exception {
        mockMvc.perform(get("/test/missingRequiredParameter"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUEST_PARAMETER"));
    }

    @Test
    public void errorCodeException() throws Exception {
        mockMvc.perform(get("/test/errorCodeException/someCode/someMessage/322"))
                .andExpect(status().is(322))
                .andExpect(jsonPath("$.code").value("someCode"))
                .andExpect(jsonPath("$.message").value("someMessage"));
    }

    @Test
    public void orderNotFoundException() throws Exception {
        mockMvc.perform(get("/test/orderNotFoundException"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    public void orderStatusNotAllowedException() throws Exception {
        mockMvc.perform(get("/test/orderStatusNotAllowedException"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STATUS_NOT_ALLOWED"));
    }

    @Test
    public void httpMessageNotReadableException() throws Exception {
        mockMvc.perform(get("/test/httpMessageNotReadableException"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FORMAT"));
    }

    @Test
    public void typeMismatchException() throws Exception {
        mockMvc.perform(get("/test/typeMismatchException"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONVERSION_NOT_SUPPORTED"));
    }
}
