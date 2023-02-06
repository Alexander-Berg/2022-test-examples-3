package ru.yandex.travel.api.endpoints.booking_flow;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.travel.api.endpoints.booking_flow.model.RefundCalculation;
import ru.yandex.travel.api.services.hotels_booking_flow.HotelOrdersService;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.commons.http.apiclient.HttpApiRetryableException;

import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BookingFlowControllerTest extends AbstractBookingFlowIntegrationTest {
    private final static String ID = "id";

    @MockBean
    private HotelOrdersService service;

    @Autowired
    private MockMvc mockMvc;


    @Test
    public void testRetryStrategyEventualSuccess() throws Exception {
        String refundToken = "refundToken";
        when(service.calculateRefund(ID))
                .thenReturn(CompletableFuture.failedFuture(new HttpApiRetryableException("1st error")))
                .thenReturn(CompletableFuture.failedFuture(new HttpApiRetryableException("2nd error")))
                .thenReturn(CompletableFuture.completedFuture(
                        RefundCalculation.builder()
                                .refundToken(refundToken)
                                .build()));

        MockHttpServletRequestBuilder rqBuilder = get("/api/booking_flow/v1/calculate_refund")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .param("id", ID);

        MvcResult result = mockMvc.perform(rqBuilder).andReturn();
        mockMvc.perform(
                        asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString(refundToken)));
    }

    @Test
    public void testRetryStrategyHttpApiExceptionForever() throws Exception {
        when(service.calculateRefund(ID))
                .then(invocation -> CompletableFuture.failedFuture(new HttpApiRetryableException("error!!")));

        MockHttpServletRequestBuilder rqBuilder = get("/api/booking_flow/v1/calculate_refund")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .param("id", ID);

        MvcResult result = mockMvc.perform(rqBuilder).andReturn();
        mockMvc.perform(
                        asyncDispatch(result))
                .andExpect(status().is5xxServerError());
    }
}
