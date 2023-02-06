package ru.yandex.market.delivery.transport_manager.controller.movement;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup({
    "/repository/register/register.xml",
    "/repository/register/register_dependencies.xml",
    "/repository/register/related_transportation.xml"
})
class MovementControllerTest extends AbstractContextualTest {

    @DisplayName("Контроллер movement-ов: успешное получение одного movement-а")
    @Test
    void getTransportationSuccess() throws Exception {
        mockMvc.perform(get("/movement/4"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/movement/response/success.json"));
    }

}
