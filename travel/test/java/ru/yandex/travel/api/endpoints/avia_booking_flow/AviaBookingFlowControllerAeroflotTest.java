package ru.yandex.travel.api.endpoints.avia_booking_flow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

import ru.yandex.avia.booking.service.dto.AeroflotStateDTO;
import ru.yandex.travel.api.endpoints.trips.req_rsp.SelectOrdersReqV1;
import ru.yandex.travel.api.services.avia.orders.AviaOrderService;
import ru.yandex.travel.commons.http.CommonHttpHeaders;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
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
public class AviaBookingFlowControllerAeroflotTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AviaOrderService orderService;

    @Test
    public void ordersControllerGetAeroflotState() throws Exception {
        var res = new ArrayList<AeroflotStateDTO>();
        res.add(new AeroflotStateDTO());
        when(orderService.getAeroflotState(anyList())).thenAnswer((Answer<CompletableFuture<ArrayList<AeroflotStateDTO>>>) invocation -> CompletableFuture.completedFuture(res));
        var s = new SelectOrdersReqV1();
        List<UUID> r = new ArrayList<>();
        r.add(UUID.randomUUID());
        s.setOrderIds(r);
        MockHttpServletRequestBuilder rqBuilder = post("/api/avia_booking_flow/v1/orders/aeroflotState")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "0")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n" +
                        "  \"orderIds\": [\n" +
                        "    \"04be1a6d-6550-4339-a3a4-fa6b6a71dda6\"\n" +
                        "  ]\n" +
                        "}");

        MvcResult asyncResult = mockMvc.perform(rqBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
    }
}
