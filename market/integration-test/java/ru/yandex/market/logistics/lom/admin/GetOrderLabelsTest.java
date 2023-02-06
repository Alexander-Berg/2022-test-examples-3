package ru.yandex.market.logistics.lom.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение списка ярлыков заказа")
@DatabaseSetup("/controller/admin/order/labels/prepare.xml")
class GetOrderLabelsTest extends AbstractContextualTest {
    @Test
    @DisplayName("Идентификатор заказа не указан")
    void orderIdNotSpecified() throws Exception {
        mockMvc.perform(defaultRequestBuilder())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Required long parameter 'orderId' is not present"));
    }

    @Test
    @DisplayName("Идентификатор заказа равен null")
    void orderIdIsNull() throws Exception {
        mockMvc.perform(defaultRequestBuilder().param("orderId", "null"))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid value=[null] for key=[orderId] specified"));
    }

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() throws Exception {
        mockMvc.perform(defaultRequestBuilder().param("orderId", "2"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with id [2]"));
    }

    @Test
    @DisplayName("Получить список ярлыков заказа")
    void getOrderLabels() throws Exception {
        mockMvc.perform(defaultRequestBuilder().param("orderId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/labels/response.json"));
    }

    @Nonnull
    private MockHttpServletRequestBuilder defaultRequestBuilder() {
        return get("/admin/orders/labels");
    }
}
