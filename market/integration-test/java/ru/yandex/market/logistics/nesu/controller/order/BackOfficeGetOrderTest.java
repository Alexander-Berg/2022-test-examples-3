package ru.yandex.market.logistics.nesu.controller.order;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.nesu.base.order.AbstractGetOrderTest;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение заказа")
@DatabaseSetup("/controller/order/get/data.xml")
class BackOfficeGetOrderTest extends AbstractGetOrderTest {

    private long shopId = 1;

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        shopId = 2;

        getOrder()
            .andExpect(status().isNotFound());
    }

    protected ResultActions getOrder() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get("/back-office/orders/" + orderId)
            .param("userId", "-100")
            .param("senderId", "-10")
            .param("shopId", String.valueOf(this.shopId)));
    }

}
