package ru.yandex.market.delivery.transport_manager.controller.register;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/repository/register/register_with_units.xml")
public class RegisterUnitsSearchTest extends AbstractContextualTest {

    @DisplayName("Поиск юнитов реестра: успешный ответ")
    @Test
    void searchUnitsSuccess() throws Exception {
        mockMvc.perform(put("/register-units/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/register/search/request/by_register_id.json"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/register/search/response/success.json"));
    }

    @DisplayName("Поиск юнитов реестра: успешный ответ c указанием типа")
    @Test
    void searchUnitsWithTypeSuccess() throws Exception {
        mockMvc.perform(put("/register-units/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/register/search/request/by_register_id_and_type.json"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/register/search/response/success_with_type.json"));
    }

    @DisplayName("Поиск юнитов реестра: пустой ответ")
    @Test
    void searchUnitsEmpty() throws Exception {
        mockMvc.perform(put("/register-units/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/register/search/request/empty.json"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/register/search/response/empty.json"));
    }

    @DisplayName("Поиск юнитов реестра: не задан идентификатор реестра")
    @Test
    void searchUnitsRegisterIdNull() throws Exception {
        mockMvc.perform(put("/register-units/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/register/search/request/register_id_null.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Following validation errors occurred:\n" +
                "Field: 'registerId', message: 'must not be null'"));
    }
}
