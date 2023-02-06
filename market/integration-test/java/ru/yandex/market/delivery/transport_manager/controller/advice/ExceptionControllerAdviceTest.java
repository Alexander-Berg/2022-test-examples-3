package ru.yandex.market.delivery.transport_manager.controller.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;

class ExceptionControllerAdviceTest extends AbstractContextualTest {
    @DisplayName("BindException")
    @Test
    void bindException() throws Exception {
        mockMvc.perform(get("/exception/bindException"))
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
            .andExpect(errorMessage("Following validation errors occurred:\nObject: 'abc', message: 'Some error'"));
    }

    @DisplayName("TransportationCouldNotBeCheckedException")
    @Test
    void transportationCouldNotBeCheckedException() throws Exception {
        mockMvc.perform(get("/exception/transportationCouldNotBeCheckedException"))
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
            .andExpect(errorMessage("Transportation 1 could not be checked, reason: Reason"));
    }

    @DisplayName("HttpMessageNotReadableException")
    @Test
    void httpMessageNotReadableException() throws Exception {
        mockMvc.perform(get("/exception/httpMessageNotReadableException"))
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
            .andExpect(errorMessage("Message"));
    }

    @DisplayName("HttpMessageNotReadableException")
    @Test
    void resourceNotFoundException() throws Exception {
        mockMvc.perform(get("/exception/resourceNotFoundException"))
            .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
            .andExpect(errorMessage("Failed to find [TRANSPORTATION] with ids [[1]]"));
    }
}
