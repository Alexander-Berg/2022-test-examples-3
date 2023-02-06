package ru.yandex.market.shopadminstub.stub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.shopadminstub.application.AbstractTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StubOrderStatusTest extends AbstractTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testOrderStatus() throws Exception {
        mockMvc.perform(post("/{shopId}/order/status", StubPushApiTestUtils.DEFAULT_SHOP_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void testOrderShipmentStatus() throws Exception {
        mockMvc.perform(post("/{shopId}/order/shipment/status", StubPushApiTestUtils.DEFAULT_SHOP_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void testOrderItems() throws Exception {
        mockMvc.perform(post("/{shopId}/order/items", StubPushApiTestUtils.DEFAULT_SHOP_ID))
                .andExpect(status().isOk());
    }
}
