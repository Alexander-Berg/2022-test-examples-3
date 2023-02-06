package ru.yandex.market.delivery.partnerapimock.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.delivery.partnerapimock.util.FileUtils;
import ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrackerPerformanceTestingTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testGetOrderHistory() throws Exception {
        mvc.perform(
            post("/tracker/DELIVERY/get-order-history")
                .contentType(MediaType.APPLICATION_XML)
                .content(FileUtils.readFile(IntegrationTestResourcesUtil.GET_ORDER_HISTORY_REQUEST))
        )
            .andExpect(status().isOk())
            .andExpect(content().xml(FileUtils.readFile(IntegrationTestResourcesUtil.GET_ORDER_HISTORY_RESPONSE)));
    }


    @Test
    void testGetOrdersStatus() throws Exception {
        mvc.perform(
            post("/tracker/DELIVERY/get-orders-status")
                .contentType(MediaType.APPLICATION_XML)
                .content(FileUtils.readFile(IntegrationTestResourcesUtil.GET_ORDERS_STATUS_REQUEST))
        )
            .andExpect(status().isOk())
            .andExpect(content().xml(FileUtils.readFile(IntegrationTestResourcesUtil.GET_ORDERS_STATUS_RESPONSE)));
    }

    @Test
    void testPush() throws Exception {
        mvc.perform(
            post("/tracker/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileUtils.readFile(IntegrationTestResourcesUtil.PUSH_TRACKS_REQUEST))
        )
            .andExpect(status().isOk());
    }
}
