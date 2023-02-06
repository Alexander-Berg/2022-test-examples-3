package ru.yandex.market.logistics.lom.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получить стоимости заказа")
@DatabaseSetup("/controller/admin/order/costs/before/prepare.xml")
public class GetCostsTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить стоимости непредоплаченного заказа")
    void getCostsOfNotPrepaidOrder() throws Exception {
        mockMvc.perform(get("/admin/orders/costs").param("orderId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/costs/response/not_prepaid_order.json"));
    }

    @Test
    @DisplayName("Получить стоимости предоплаченного заказа")
    void getCostsOfPrepaidOrder() throws Exception {
        mockMvc.perform(get("/admin/orders/costs").param("orderId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/costs/response/prepaid_order.json"));
    }

    @Test
    @DisplayName("Получить стоимости для заказа без данных")
    void getCostsOfOrderWoCost() throws Exception {
        mockMvc.perform(get("/admin/orders/costs").param("orderId", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/costs/response/order_wo_cost.json"));
    }

    @Test
    @DisplayName("Заказ не найден")
    void getCostsOrderNotFound() throws Exception {
        mockMvc.perform(get("/admin/orders/costs").param("orderId", "4"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/admin/order/costs/response/order_not_found.json"));
    }
}
