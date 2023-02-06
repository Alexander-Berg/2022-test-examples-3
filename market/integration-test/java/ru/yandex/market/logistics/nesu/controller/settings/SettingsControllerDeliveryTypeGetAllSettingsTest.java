package ru.yandex.market.logistics.nesu.controller.settings;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты ручки /back-office/settings/sender/delivery-types АПИ SettingsController")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
class SettingsControllerDeliveryTypeGetAllSettingsTest extends AbstractSettingsControllerTest {

    @Test
    @DisplayName("Получение пустых настроек типов доставки сендера")
    @DatabaseSetup("/controller/meta/tariff/before/empty_delivery_type.xml")
    void getEmptyDeliveryTypeSettings() throws Exception {
        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @Test
    @DisplayName("Получение настроек типов доставки несуществующего сендера")
    @DatabaseSetup("/controller/meta/tariff/before/empty_delivery_type.xml")
    void getDeliveryTypeSettingsSenderNotFound() throws Exception {
        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "2")
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [SENDER] with ids [2]\","
                + "\"resourceType\":\"SENDER\",\"identifiers\":[2]}"));
    }

    @Test
    @DisplayName("Получение настроек доп.услуг типов доставки сендера")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void getDeliveryTypeSettings() throws Exception {
        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/meta/tariff/after/all_delivery_type_settings_response.json"));
    }
}
