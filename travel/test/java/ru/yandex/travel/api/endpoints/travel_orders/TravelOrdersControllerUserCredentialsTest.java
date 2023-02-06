package ru.yandex.travel.api.endpoints.travel_orders;

import java.util.concurrent.CompletableFuture;

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

import ru.yandex.travel.api.endpoints.travel_orders.req_rsp.TravelOrdersListRspV1;
import ru.yandex.travel.commons.http.CommonHttpHeaders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


//TODO (mbobrov): move this test to autoconfiguration in spring boot skeleton library

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
public class TravelOrdersControllerUserCredentialsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TravelOrdersImplV2 travelOrdersImplV2;

    @Test
    public void testControllerListOrdersNotCredentials() throws Exception {
        MockHttpServletRequestBuilder rqBuilder = get("/api/orders/v2/list_orders")
                .contentType(MediaType.APPLICATION_JSON);
        // mock mvc doesn't apply the default exception handler for filters
        this.mockMvc.perform(rqBuilder)
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testControllerListOrdersNotLoggedIn() throws Exception {
        MockHttpServletRequestBuilder rqBuilder = get(
                "/api/orders/v2/list_orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "yandex_uid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "session_key");
        this.mockMvc.perform(rqBuilder).andExpect(status().isForbidden());
    }

    @Test
    public void testControllerListOrdersLoggedIn() throws Exception {
        when(travelOrdersImplV2.listOrdersFirstPage(anyInt(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new TravelOrdersListRspV1()));
        MockHttpServletRequestBuilder rqBuilder = get(
                "/api/orders/v2/list_orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "yandex_uid")
                .header(CommonHttpHeaders.HeaderType.PASSPORT_ID.getHeader(), "1001");
        MvcResult result = this.mockMvc.perform(rqBuilder).andReturn();
        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
    }
}
