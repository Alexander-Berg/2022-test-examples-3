package ru.yandex.travel.api.endpoints.trains_booking_flow;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.travel.api.endpoints.trains_booking_flow.req_rsp.CreateOrderReqV2;
import ru.yandex.travel.api.endpoints.trains_booking_flow.req_rsp.CreateOrderRspV1;
import ru.yandex.travel.api.services.orders.TrainOrdersService;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.testing.misc.TestResources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TrainsBookingFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainOrdersService service;

    @Test
    public void testCreateOrderInvalidRequestData() throws Exception {
        mockMvc.perform(post("/api/trains_booking_flow/v1/create_order")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .content("{}")
                .contentType("application/json"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testCreateOrder() throws Exception {
        var futureResult = CompletableFuture.supplyAsync(CreateOrderRspV1::new);
        when(service.createOrder((CreateOrderReqV2) any())).thenReturn(futureResult);
        String query = TestResources.readResource("trains_booking_flow/create_order_request.json");

        mockMvc.perform(post("/api/trains_booking_flow/v2/create_order")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .content(query)
                .contentType("application/json"))
                .andExpect(status().isOk());
        verify(service, times(1)).createOrder((CreateOrderReqV2) any());
    }

    @Test
    public void testStartPaymentBadRequest() throws Exception {
        mockMvc.perform(post("/api/trains_booking_flow/v1/start_payment")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .content("{}")
                .contentType("application/json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testStartPaymentOk() throws Exception {
        when(service.payOrder(any(), any(), any(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(null));
        var query = TestResources.readResource("trains_booking_flow/start_payment_request.json");
        var result = mockMvc.perform(post("/api/trains_booking_flow/v1/start_payment")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .content(query)
                .contentType("application/json"))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());

        verify(service, times(1)).payOrder("SomeOrderId",
                "https://some-host.org/", null, false);
    }

    @Test
    public void testDownloadPdf() throws Exception {
        when(service.downloadBlank(any())).thenReturn(CompletableFuture.completedFuture(new byte[]{}));
        var result = mockMvc.perform(get("/api/trains_booking_flow/v1/download_blank?id=bf8afefa-fcda-40c5-94be-699dce60e2a6")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .accept("application/pdf")
                .contentType("application/json"))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());

        verify(service, times(1)).downloadBlank(argThat(r -> r.getId().equals(UUID.fromString("bf8afefa-fcda-40c5-94be-699dce60e2a6"))));
    }

    @Test
    public void testStartPaymentInternalServerError() throws Exception {
        when(service.payOrder(any(), any(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.failedFuture(new StatusRuntimeException(Status.INTERNAL)));
        var query = TestResources.readResource("trains_booking_flow/start_payment_request.json");
        var result = mockMvc.perform(post("/api/trains_booking_flow/v1/start_payment")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .content(query)
                .contentType("application/json"))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testChangeRegistrationStatusOk() throws Exception {
        when(service.changeRegistrationStatus(any())).thenReturn(CompletableFuture.completedFuture(null));
        var query = TestResources.readResource("trains_booking_flow/change_registration_status_request.json");
        var result = mockMvc.perform(post("/api/trains_booking_flow/v1/change_registration_status")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                .content(query)
                .contentType("application/json"))
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
    }
}
