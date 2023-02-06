package ru.yandex.market.logistics.lom.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получить услуги заказа")
@DatabaseSetup("/controller/admin/order/costs/before/prepare.xml")
public class GetServicesTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить услуги заказа")
    void getCostsOfNotPrepaidOrder() throws Exception {
        mockMvc.perform(get("/admin/orders/services").param("orderId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/costs/response/services.json"));
    }

    @Test
    @DisplayName("Получить услуги заказа без данных")
    void getCostsOfOrderWoCost() throws Exception {
        mockMvc.perform(get("/admin/orders/services").param("orderId", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/costs/response/order_wo_cost.json"));
    }

    @Test
    @DisplayName("Заказ не найден")
    void getCostsOrderNotFound() throws Exception {
        mockMvc.perform(get("/admin/orders/services").param("orderId", "4"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/admin/order/costs/response/order_not_found.json"));
    }
}
