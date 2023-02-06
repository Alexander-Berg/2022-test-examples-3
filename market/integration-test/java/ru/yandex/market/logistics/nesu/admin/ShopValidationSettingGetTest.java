package ru.yandex.market.logistics.nesu.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/repository/validation/default_validation_settings.xml")
class ShopValidationSettingGetTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получить настройки валидаций для магазина")
    void getShopValidationSetting() throws Exception {
        assertSuccessfulGetValidationSettingsResponse(
            2L,
            "controller/admin/validation/default_supplier_settings.json"
        );
    }

    @Test
    @DisplayName("Получение пустых настроек, если их нет для магазина")
    void getEmptyShopValidationSettings() throws Exception {
        assertSuccessfulGetValidationSettingsResponse(
            0L,
            "controller/admin/validation/daas_empty_settings.json"
        );
    }

    @Test
    @DisplayName("Неверный идентификатор роли")
    void wrongRoleId() throws Exception {
        getValidationSettings(10L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_ROLE] with ids [10]"));
    }

    private void assertSuccessfulGetValidationSettingsResponse(Long roleId, String jsonContentPath) throws Exception {
        getValidationSettings(roleId)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent(jsonContentPath));
    }

    @Nonnull
    private ResultActions getValidationSettings(Long roleId) throws Exception {
        return mockMvc.perform(get("/admin/validation/settings/" + roleId));
    }

}
