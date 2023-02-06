package ru.yandex.travel.api.endpoints.admin;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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

import ru.yandex.travel.api.endpoints.admin.req_rsp.AdminListOrdersReqV1;
import ru.yandex.travel.api.endpoints.admin.req_rsp.ChangePhoneReqV1;
import ru.yandex.travel.api.models.admin.Order;
import ru.yandex.travel.api.models.admin.TravelAdminOrderList;
import ru.yandex.travel.commons.http.CommonHttpHeaders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
public class TravelOrdersAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    @MockBean
    private TravelOrdersAdminService orderService;

    @Test
    public void testControllerListOrders() throws Exception {
        when(orderService.listOrders(any())).thenReturn(CompletableFuture.completedFuture(new TravelAdminOrderList()));
        AdminListOrdersReqV1 request = new AdminListOrdersReqV1();
        request.setOffset(0);
        request.setLimit(20);
        MockHttpServletRequestBuilder rqBuilder = post(
                "/api/admin/v1/list_orders")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.PASSPORT_ID.getHeader(), "p-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
        MvcResult mvcResult = this.mockMvc.perform(rqBuilder)
                .andReturn();
        this.mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
    }

    @Test
    public void testControllerGetOrder() throws Exception {

        when(orderService.getOrder(any())).thenReturn(CompletableFuture.completedFuture(new Order()));
        MockHttpServletRequestBuilder rqBuilder = get(
                "/api/admin/v1/get_order?id={id}", "1")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.PASSPORT_ID.getHeader(), "p-id")
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = this.mockMvc.perform(rqBuilder)
                .andReturn();
        this.mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
    }

    @Test
    public void testEmptyGetOrder() throws Exception {
        when(orderService.getOrder(any())).thenReturn(CompletableFuture.failedFuture(new IllegalArgumentException()));
        MockHttpServletRequestBuilder rqBuilder = get(
                "/api/admin/v1/get_order")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.PASSPORT_ID.getHeader(), "p-id")
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = this.mockMvc.perform(rqBuilder)
                .andReturn();
        this.mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNotFoundGetOrder() throws Exception {
        when(orderService.getOrder(any())).thenReturn(CompletableFuture.failedFuture(new StatusRuntimeException(Status.NOT_FOUND)));
        MockHttpServletRequestBuilder rqBuilder = get(
                "/api/admin/v1/get_order?id={id}", "1")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.PASSPORT_ID.getHeader(), "p-id")
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = this.mockMvc.perform(rqBuilder)
                .andReturn();
        this.mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testControllerChangePhone() throws Exception {
        when(orderService.changePhone(any())).thenReturn(CompletableFuture.completedFuture(null));
        var req = new ChangePhoneReqV1();
        req.setOrderId("SomeOrderId");
        req.setNewPhone("SomeNewPhone");
        MockHttpServletRequestBuilder rqBuilder = post(
                "/api/admin/v1/change_phone")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                .header(CommonHttpHeaders.HeaderType.PASSPORT_ID.getHeader(), "p-id")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(req));
        MvcResult mvcResult = this.mockMvc.perform(rqBuilder)
                .andReturn();
        this.mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
    }
}
