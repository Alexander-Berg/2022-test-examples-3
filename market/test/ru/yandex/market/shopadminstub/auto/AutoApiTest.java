package ru.yandex.market.shopadminstub.auto;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.util.TestSerializationService;

import java.time.LocalDate;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkDeliveryOption;

public class AutoApiTest extends AbstractTestBase {
    private static final long DEFAULT_SHOP_ID = 2234562L;

    @Autowired
    private TestSerializationService testSerializationService;

    @Test
    public void shouldReturnOkOnOrderStatus() throws Exception {
        mockMvc.perform(post("/auto-shop/{shopId}/order/status", DEFAULT_SHOP_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnOkOnOrderShipmentStatus() throws Exception {
        mockMvc.perform(post("/auto-shop/{shopId}/order/shipment/status", DEFAULT_SHOP_ID))
                .andExpect(status().isOk());
    }

    @Test
    @Disabled
    public void shouldReturnOkOnOrderAccept() throws Exception {
        mockMvc.perform(post("/auto-shop/{shopId}/order/accept", DEFAULT_SHOP_ID))
                .andExpect(status().isOk())
                .andExpect(xpath("/order/@accepted").booleanValue(true))
                .andExpect(xpath("/order/@id").exists());
    }

    @Test
    public void shouldReturnOkOnCartFastDelivery() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();

        ResultActions resultActions = mockMvc.perform(post("/auto-shop/{shopId}/cart-fast-delivery", DEFAULT_SHOP_ID)
                .contentType(MediaType.APPLICATION_XML)
                .content(testSerializationService.serializeXml(cartRequest)))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/items/item/@count").number(1d))
                .andExpect(xpath("/cart/items/item/@price").number(250d))
                .andExpect(xpath("/cart/items/item/@delivery").booleanValue(true))
                .andExpect(xpath("/cart/payment-methods/payment-method").nodeCount(4))
                .andExpect(xpath("/cart/delivery-options/delivery").nodeCount(3));

        LocalDate today = LocalDate.now();

        checkDeliveryOption(resultActions, today, 1, Collections.emptyList(), "Почта России PICKUP", "PICKUP", "0", 0, 0);
        checkDeliveryOption(resultActions, today, 2, Collections.emptyList(), "Почта России POST", "POST", "250", 0, 0);
        checkDeliveryOption(resultActions, today, 3, Collections.emptyList(), "Почта России DELIVERY", "DELIVERY", "350", 0, 0);
    }

    @Test
    public void shouldReturnOkOnCart() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();

        ResultActions resultActions = mockMvc.perform(post("/auto-shop/{shopId}/cart", DEFAULT_SHOP_ID)
                .contentType(MediaType.APPLICATION_XML)
                .content(testSerializationService.serializeXml(cartRequest)))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/items/item/@count").number(1d))
                .andExpect(xpath("/cart/items/item/@price").number(250d))
                .andExpect(xpath("/cart/items/item/@delivery").booleanValue(true))
                .andExpect(xpath("/cart/payment-methods/payment-method").nodeCount(4))
                .andExpect(xpath("/cart/delivery-options/delivery").nodeCount(3));

        LocalDate today = LocalDate.now();

        checkDeliveryOption(resultActions, today, 1, Collections.emptyList(), "Почта России PICKUP", "PICKUP", "0", 0, 7);
        checkDeliveryOption(resultActions, today, 2, Collections.emptyList(), "Почта России POST", "POST", "250", 0, 7);
        checkDeliveryOption(resultActions, today, 3, Collections.emptyList(), "Почта России DELIVERY", "DELIVERY", "350", 0, 7);
    }
}
