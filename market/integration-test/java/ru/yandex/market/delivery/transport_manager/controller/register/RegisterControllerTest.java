package ru.yandex.market.delivery.transport_manager.controller.register;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup({
    "/repository/register/register.xml",
    "/repository/register_unit/register_unit_count_orders.xml"
})
public class RegisterControllerTest extends AbstractContextualTest {

    @DisplayName("Контроллер реестров: успешное получение кол-ва заказов в реестрах")
    @Test
    void ordersCountSuccess() throws Exception {
        mockMvc.perform(put("/registers/ordersCount")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/register/request/orders_count.json"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/register/response/orders_count_success.json"));
    }

    @DisplayName("Контроллер реестров: пустой ответ")
    @Test
    void ordersCountEmpty() throws Exception {
        mockMvc.perform(put("/registers/ordersCount")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/register/request/orders_count_empty.json"))
        )
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @DisplayName("Контроллер реестров: отсутствуют идентификаторы реестров")
    @Test
    void ordersCountRegisterIdsEmpty() throws Exception {
        mockMvc.perform(put("/registers/ordersCount")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/register/request/orders_count_no_register_ids.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Following validation errors occurred:\n" +
                "Field: 'registerIds', message: 'must not be empty'"));
    }
}
