package ru.yandex.travel.api.endpoints.avia_booking_flow;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.avia.booking.service.dto.form.InitOrderPaymentForm;
import ru.yandex.travel.api.services.avia.legacy.AviaLegacyJsonMapper;
import ru.yandex.travel.api.services.avia.orders.AviaOrderService;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.orders.proto.EDeviceType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"avia-booking.enabled=true"}
)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
public class AviaBookingFlowControllerPaymentTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = AviaLegacyJsonMapper.objectMapper;

    private final UUID orderId = UUID.randomUUID();

    @MockBean
    private AviaOrderService orderService;

    @MockBean
    private AviaGeobaseCountryService geobaseCountryService;

    @Test
    public void ordersControllerInitPayment() throws Exception {
        InitOrderPaymentForm form = createCorrectOrderPaymentForm();
        when(orderService.startPaymentAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        MockHttpServletRequestBuilder rqBuilder = post("/api/avia_booking_flow/v1/orders/payment")
                .param("id", orderId.toString())
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "yandex_uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "sKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form));
        MvcResult asyncResult = mockMvc.perform(rqBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
        verify(orderService, times(1)).startPaymentAsync(eq(orderId), any());
    }

    @Test
    public void ordersControllerInitPaymentValidationError() throws Exception {
        InitOrderPaymentForm form = createIncorrectOrderPaymentForm();
        MockHttpServletRequestBuilder rqBuilder = post("/api/avia_booking_flow/v1/orders/payment")
                .param("id", orderId.toString())
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "yandex_uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "sKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form));
        this.mockMvc.perform(rqBuilder)
                .andExpect(status().isBadRequest());
    }

    private InitOrderPaymentForm createCorrectOrderPaymentForm() {
        InitOrderPaymentForm result = new InitOrderPaymentForm();
        result.setPaymentRedirectUrl("payment_redirect_url");
        result.setConfirmationRedirectUrl("3ds_redirect_url");
        result.setDeviceType(EDeviceType.DT_DESKTOP);
        return result;
    }

    private InitOrderPaymentForm createIncorrectOrderPaymentForm() {
        return new InitOrderPaymentForm();
    }

}
