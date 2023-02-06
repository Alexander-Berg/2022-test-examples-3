package ru.yandex.market.logistics.nesu.controller.order;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.base.order.AbstractUpdateOrderTest;
import ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление черновика заказа")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
class BackOfficeUpdateOrderTest extends AbstractUpdateOrderTest {

    private Long shopId = 1L;

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        shopId = 2L;
        updateOrder(OrderDtoFactory.defaultOrderDraft(), 42L)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [ORDER] with ids [42]\","
                + "\"resourceType\":\"ORDER\",\"identifiers\":[42]}"));
    }

    @Nonnull
    @Override
    protected ResultActions updateOrder(OrderDraft orderDraft, Long orderId) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/back-office/orders/" + orderId, orderDraft)
            .param("userId", "1")
            .param("senderId", "1")
            .param("shopId", String.valueOf(shopId)));
    }
}
