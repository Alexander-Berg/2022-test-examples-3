package ru.yandex.travel.api.endpoints.avia_booking_flow;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
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

import ru.yandex.avia.booking.service.dto.CompositeOrderStateDTO;
import ru.yandex.avia.booking.service.dto.OrderDTO;
import ru.yandex.avia.booking.service.dto.form.CreateOrderForm;
import ru.yandex.travel.api.services.avia.legacy.AviaLegacyJsonMapper;
import ru.yandex.travel.api.services.avia.orders.AviaOrderService;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.api.services.avia.variants.AviaVariantService;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.orders.client.HAGrpcChannelFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "avia-booking.enabled=true",
        }
)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
@MockBean(value = {AviaGeobaseCountryService.class})
@MockBean(value = {HAGrpcChannelFactory.class}, name = "OrchestratorGrpcChannelFactory")
public class AviaBookingFlowControllerHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = AviaLegacyJsonMapper.objectMapper;

    @MockBean
    private AviaOrderService orderService;

    @MockBean
    private AviaVariantService variantService;

    @Test
    public void variantsApiWorksWithoutUserCredentials() throws Exception {
        when(variantService.getVariantInfoFuture(any())).thenReturn(CompletableFuture.completedFuture(null));
        MockHttpServletRequestBuilder rqBuilder = get("/api/avia_booking_flow/v1/variants")
                .param("id", UUID.randomUUID().toString());
        MvcResult asyncResult = mockMvc.perform(rqBuilder)
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
    }

    @Test
    public void ordersControllerCreateShouldReturn401WhenCalledWithoutYandexUID() throws Exception {
        MockHttpServletRequestBuilder rqBuilder = post("/api/avia_booking_flow/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        AviaCreateOrderDataUtils.createTestObject()
                ));
        mockMvc.perform(rqBuilder)
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void ordersControllerCreateShouldReturnOrder() throws Exception {
        when(orderService.createOrder(any(), any())).thenReturn(CompletableFuture.completedFuture(new OrderDTO()));
        MockHttpServletRequestBuilder rqBuilder = post("/api/avia_booking_flow/v1/orders")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "0")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        AviaCreateOrderDataUtils.createTestObject()
                ));
        MvcResult asyncResult = mockMvc.perform(rqBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
    }

    @Test
    public void ordersControllerCreateShouldSetUserIP() throws Exception {
        String userIP = "1.2.3.4";

        final String[] newUserIP = new String[1];
        when(orderService.createOrder(any(), any())).thenAnswer((Answer<CompletableFuture<OrderDTO>>) invocation -> {
             newUserIP[0] = ((CreateOrderForm) invocation.getArguments()[0]).getUserIp();

             return CompletableFuture.completedFuture(new OrderDTO());
        });
        MockHttpServletRequestBuilder rqBuilder = post("/api/avia_booking_flow/v1/orders")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "0")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "0")
                .header(CommonHttpHeaders.HeaderType.USER_IP.getHeader(), userIP)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        AviaCreateOrderDataUtils.createTestObject()
                ));
        MvcResult asyncResult = mockMvc.perform(rqBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
        Assert.assertEquals(userIP, newUserIP[0]);
    }

    @Test
    public void ordersControllerGetStateIsForbiddenWithoutUserCredentials() throws Exception {
        when(orderService.getCompositeOrderState(any()))
                .thenReturn(CompletableFuture.completedFuture(CompositeOrderStateDTO.builder().build()));
        MockHttpServletRequestBuilder rqBuilder = get("/api/avia_booking_flow/v1/orders/state")
                .param("id", UUID.randomUUID().toString());
        mockMvc.perform(rqBuilder)
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isUnauthorized());
    }
}
