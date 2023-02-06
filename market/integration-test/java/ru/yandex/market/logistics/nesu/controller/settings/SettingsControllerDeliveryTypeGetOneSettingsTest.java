package ru.yandex.market.logistics.nesu.controller.settings;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты ручки /back-office/settings/sender/delivery-types/{deliveryType} АПИ SettingsController")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
class SettingsControllerDeliveryTypeGetOneSettingsTest extends AbstractSettingsControllerTest {

    @Test
    @DisplayName("Получение пустых настроек типа доставки сендера")
    @DatabaseSetup("/controller/meta/tariff/before/empty_delivery_type.xml")
    void getEmptyOneDeliveryTypeSettings() throws Exception {
        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(content().json("{}"));
    }

    @Test
    @DisplayName("Получение настроек типа доставки несуществующего сендера")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void getOneDeliveryTypeSettingsSenderNotFound() throws Exception {
        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types/PICKUP")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "2")
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [SENDER] with ids [2]\","
                + "\"resourceType\":\"SENDER\",\"identifiers\":[2]}"));
    }

    @Test
    @DisplayName("Получение настроек доп.услуг типа доставки сендера")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void getOneDeliveryTypeSettings() throws Exception {
        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/meta/tariff/after/sender_delivery_type_settings_response.json"));
    }
}
