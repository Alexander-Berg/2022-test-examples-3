package ru.yandex.market.logistics.nesu.controller.settings;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты ручки /back-office/settings/delivery-types АПИ SettingsController")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
class SettingsControllerDeliveryTypeTest extends AbstractSettingsControllerTest {

    @Test
    @DisplayName("Получение списка настроек типов доставки")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void getDeliveryTypes() throws Exception {
        mockMvc.perform(get("/back-office/settings/delivery-types"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/meta/tariff/after/default_delivery_type_response.json"));
    }

    @Test
    @DisplayName("Получение списка доп. услуг при отсутствующем типе доставки")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void getDeliveryTypeServicesTariffNotFound() throws Exception {
        mockMvc.perform(get("/back-office/settings/delivery-types/UNKNOWN"))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Неизвестный тип доставки: UNKNOWN"));
    }

    @Test
    @DisplayName("Получение списка доп. услуг типа доставки")
    @DatabaseSetup("/controller/meta/tariff/before/empty_delivery_type.xml")
    void getDeliveryTypeServices() throws Exception {
        mockMvc.perform(get("/back-office/settings/delivery-types/PICKUP"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/meta/tariff/after/delivery_type_services_response.json"));
    }
}
