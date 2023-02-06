package ru.yandex.market.logistics.nesu.controller.order;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.base.order.AbstractCancelOrderTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Отмена заказа")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
class BackOfficeCancelOrderTest extends AbstractCancelOrderTest {

    private long shopId = 1;

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        shopId = 2;

        cancel(ORDER_ID)
            .andExpect(status().isNotFound());
    }

    @Override
    protected ResultActions cancel(Long orderId) throws Exception {
        return mockMvc.perform(delete("/back-office/orders/" + orderId)
            .param("userId", "1")
            .param("shopId", String.valueOf(shopId)));
    }
}
